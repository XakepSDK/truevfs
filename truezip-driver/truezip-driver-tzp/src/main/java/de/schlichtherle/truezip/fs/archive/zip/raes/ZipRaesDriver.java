/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.crypto.raes.RaesOutputStream;
import de.schlichtherle.truezip.crypto.raes.RaesParameters;
import de.schlichtherle.truezip.crypto.raes.RaesReadOnlyFile;
import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.crypto.raes.param.KeyManagerRaesParameters;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.fs.archive.zip.JarArchiveEntry;
import de.schlichtherle.truezip.fs.archive.zip.JarDriver;
import de.schlichtherle.truezip.fs.archive.zip.OptionOutputSocket;
import de.schlichtherle.truezip.fs.archive.zip.ZipArchiveEntry;
import de.schlichtherle.truezip.fs.archive.zip.ZipInputShop;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.PromptingKeyProvider;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.LazyOutputSocket;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStream;
import net.jcip.annotations.Immutable;

/**
 * An abstract archive driver which builds RAES encrypted ZIP files
 * and optionally authenticates the cipher data of the input archive files
 * presented to it.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public abstract class ZipRaesDriver extends JarDriver {

    private final KeyManagerProvider keyManagerProvider;

    /**
     * Constructs a new RAES encrypted ZIP file driver.
     *
     * @param ioPoolProvider the I/O entry pool provider for allocating
     *        temporary I/O entries (buffers).
     * @param keyManagerProvider the key manager provider for accessing
     *        protected resources (cryptography).
     */
    public ZipRaesDriver(   IOPoolProvider ioPoolProvider,
                            final KeyManagerProvider keyManagerProvider) {
        super(ioPoolProvider);
        if (null == keyManagerProvider)
            throw new NullPointerException();
        this.keyManagerProvider = keyManagerProvider;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Since TrueZIP 7.3, this method returns {@code true} for future use.
     * 
     * @return {@code true}
     */
    @Override
    protected final boolean getPreambled() {
        return true;
    }

    /**
     * Returns the provider for key managers for accessing protected resources
     * (encryption).
     * <p>
     * The implementation in {@link ZipRaesDriver} always returns the parameter
     * provided to the constructor.
     * 
     * @return The provider for key managers for accessing protected resources
     *         (encryption).
     * @since  TrueZIP 7.3.
     */
    @Override
    protected final KeyManagerProvider getKeyManagerProvider() {
        return keyManagerProvider;
    }

    /**
     * Returns the key manager for accessing RAES encrypted data.
     * 
     * @return The key manager for accessing RAES encrypted data.
     */
    protected final KeyManager<AesCipherParameters> getKeyManager() {
        return keyManagerProvider.get(AesCipherParameters.class);
    }

    /**
     * Returns the RAES parameters for the given file system model
     * or {@code null} if not available.
     * <p>
     * The implementation in the class {@link ZipRaesDriver} returns
     * {@code new KeyManagerRaesParameters(getKeyManager(), mountPointUri(model))}.
     * 
     * @param  model the file system model.
     * @return The RAES parameters for the given file system model
     *         or {@code null} if not available.
     */
    protected @CheckForNull RaesParameters raesParameters(FsModel model) {
        return new KeyManagerRaesParameters(
                getKeyManager(),
                mountPointUri(model));
    }

    /**
     * Returns the key provider sync strategy,
     * which is {@link KeyProviderSyncStrategy#RESET_CANCELLED_KEY}.
     *
     * @return The key provider sync strategy.
     */
    protected KeyProviderSyncStrategy getKeyProviderSyncStrategy() {
        return KeyProviderSyncStrategy.RESET_CANCELLED_KEY;
    }

    /**
     * Constructs a new abstract ZIP.RAES driver which
     * uses the given byte
     * size to trigger verification of the Message Authentication Code (MAC).
     * Note that the given parameter only affects the authentication of the
     * <em>cipher text</em> in input archives - the <em>cipher key</em> and
     * <em>file length</em> are always authenticated with RAES.
     *
     * Returns the value of the property {@code authenticationTrigger}.
     * If the size of an input file is smaller than or equal to this value,
     * the Message Authentication Code (MAC) for the entire
     * <em>cipher text</em> is computed and verified in order to authenticate
     * the file.
     * Otherwise, only the <em>cipher key</em> and the <em>file length</em>
     * get authenticated.
     * <p>
     * Consequently, if the value of this property is set to a negative value,
     * the cipher text gets <em>never</em> verified, and if set to
     * {@link Long#MAX_VALUE}, the cipher text gets <em>always</em>
     * authenticated.
     *
     * @return The value of the property {@code authenticationTrigger}.
     */
    protected abstract long getAuthenticationTrigger();

    @Override
    public final FsController<?>
    newController(FsModel model, FsController<?> parent) {
        return new ZipRaesController(
                super.newController(model, parent), this);
    }

    /**
     * Creates a new {@link JarArchiveEntry}, enforcing that the data gets
     * {@code DEFLATED} when written, even if copying data from a
     * {@code STORED} source entry.
     * This feature strengthens the security level of the authentication
     * process and inhibits the use of an unencrypted temporary I/O entry
     * (usually a temporary file) in case the output is not copied from a file
     * system entry as its input.
     * <p>
     * Furthermore, the output option preference {@link FsOutputOption#ENCRYPT}
     * is cleared in order to prevent adding a redundant encryption layer for
     * the individual ZIP entry.
     * This would not have any effect on the security level, but increase the
     * size of the resulting archive file and heat the CPU.
     */
    @Override
    public JarArchiveEntry
    newEntry(   String path,
                Type type,
                Entry template,
                BitField<FsOutputOption> mknod)
    throws CharConversionException {
        return super.newEntry(path, type, template,
                mknod.set(COMPRESS).clear(ENCRYPT));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link ZipRaesDriver} calls
     * {@link #raesParameters}, with which it initializes a new
     * {@link RaesReadOnlyFile}.
     * Next, if the gross file length of the archive is smaller than or equal
     * to the authentication trigger, the MAC authentication on the cipher
     * text is performed.
     * Finally, the {@link RaesReadOnlyFile} is passed on to the super
     * class implementation.
     */
    @Override
    public final InputShop<ZipArchiveEntry>
    newInputShop(   final FsModel model,
                    final InputSocket<?> input)
    throws IOException {
        final ReadOnlyFile rof = input.newReadOnlyFile();
        try {
            final RaesReadOnlyFile rrof = RaesReadOnlyFile.getInstance(
                    rof, raesParameters(model));
            if (rof.length() <= getAuthenticationTrigger()) { // compare rof, not rrof!
                // Note: If authentication fails, this is reported through some
                // sort of IOException, not a FileNotFoundException!
                // This allows the client to treat the tampered archive like an
                // ordinary file which may be read, written or deleted.
                rrof.authenticate();
            }
            return newInputShop(model, rrof);
        } catch (IOException ex) {
            rof.close();
            throw ex;
        }
    }

    /**
     * Sets {@link FsOutputOption#STORE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    public final OptionOutputSocket getOutputSocket(
            final FsController<?> controller,
            final FsEntryName name,
            BitField<FsOutputOption> options,
            final @CheckForNull Entry template) {
        options = options.clear(GROW);
        // Leave FsOutputOption.COMPRESS untouched - the driver shall be given
        // opportunity to apply its own preferences to sort out such a conflict.
        BitField<FsOutputOption> options2 = options.set(STORE);
        return new OptionOutputSocket(
                controller.getOutputSocket(name, options2, template),
                options); // use modified options!
    }

    @Override
    protected OutputShop<ZipArchiveEntry> newOutputShop(
            final FsModel model,
            final OptionOutputSocket output,
            final @CheckForNull ZipInputShop source)
    throws IOException {
        final OutputStream out = new LazyOutputSocket<Entry>(output)
                .newOutputStream();
        try {
            final RaesOutputStream ros = RaesOutputStream.getInstance(
                    out, raesParameters(model));
            return newOutputShop(model, ros, source);
        } catch (IOException ex) {
            out.close();
            throw ex;
        }
    }

    /**
     * Defines strategies for updating a key provider once a RAES encrypted
     * ZIP file has been successfully synchronized.
     */
    public enum KeyProviderSyncStrategy {

        /**
         * Calls {@link PromptingKeyProvider#resetCancelledKey}
         * if and only if the given provider is a {@link PromptingKeyProvider}.
         */
        RESET_CANCELLED_KEY {
            @Override
            void sync(KeyProvider<?> provider) {
                if (provider instanceof PromptingKeyProvider<?>)
                    ((PromptingKeyProvider<?>) provider).resetCancelledKey();
            }
        },

        /**
         * Calls {@link PromptingKeyProvider#resetUnconditionally}
         * if and only if the given provider is a {@link PromptingKeyProvider}.
         */
        RESET_UNCONDITIONALLY {
            @Override
            void sync(KeyProvider<?> provider) {
                if (provider instanceof PromptingKeyProvider<?>)
                    ((PromptingKeyProvider<?>) provider).resetUnconditionally();
            }
        };

        /**
         * This method is called upon a call to
         * {@link ZipRaesController#sync} after a successful
         * synchronization of a RAES encrypted ZIP file.
         *
         * @param provider the key provider for the RAES encrypted ZIP file
         *        which has been successfully synchronized.
         */
        abstract void sync(KeyProvider<?> provider);
    } // KeyProviderSyncStrategy
}
