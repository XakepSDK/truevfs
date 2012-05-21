/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.raes;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;
import net.truevfs.driver.zip.*;
import net.truevfs.driver.zip.raes.crypto.RaesOutputStream;
import net.truevfs.driver.zip.raes.crypto.RaesParameters;
import net.truevfs.driver.zip.raes.crypto.RaesReadOnlyChannel;
import net.truevfs.kernel.FsAccessOption;
import static net.truevfs.kernel.FsAccessOption.*;
import net.truevfs.kernel.FsController;
import net.truevfs.kernel.FsEntryName;
import net.truevfs.kernel.FsModel;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.Entry.Type;
import net.truevfs.kernel.cio.InputService;
import net.truevfs.kernel.cio.MultiplexingOutputService;
import net.truevfs.kernel.cio.OutputService;
import net.truevfs.kernel.io.AbstractSink;
import net.truevfs.kernel.io.AbstractSource;
import net.truevfs.kernel.io.Sink;
import net.truevfs.kernel.io.Source;
import net.truevfs.kernel.util.BitField;

/**
 * An abstract archive driver for RAES encrypted ZIP files which optionally
 * authenticates the cipher data of the input archive files presented to it.
 * <p>
 * Sub-classes must be thread-safe and should be immutable!
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class ZipRaesDriver extends JarDriver {

    /**
     * {@inheritDoc}
     * <p>
     * Since TrueVFS 7.3, the implementation in the class {@link ZipRaesDriver}
     * returns {@code true} for future use.
     * 
     * @return {@code true}
     */
    @Override
    public final boolean getPreambled() {
        return true;
    }

    /**
     * Returns the RAES parameters for the given file system model.
     * <p>
     * The implementation in the class {@link ZipRaesDriver} returns
     * {@code new KeyManagerRaesParameters(getKeyManagerProvider().getKeyManager(AesPbeParameters.class), mountPointUri(model))}.
     * 
     * @param  model the file system model.
     * @return The RAES parameters for the given file system model.
     */
    protected RaesParameters raesParameters(FsModel model) {
        return new KeyManagerRaesParameters(getKeyManagerProvider(),
                                            mountPointUri(model));
    }

    /**
     * Returns the value of the property {@code authenticationTrigger}.
     * <p>
     * If the cipher text length of an input RAES file is smaller than or equal
     * to this value, then the Hash-based Message Authentication Code (HMAC)
     * for the entire cipher text is computed and verified in order to
     * authenticate the input RAES file.
     * <p>
     * Otherwise, if the cipher text length of an input RAES file is greater
     * than this value, then initially only the cipher key and the cipher text
     * length getKeyManager authenticated.
     * In addition, whenever an entry is subsequently accessed, then it's
     * CRC-32 value is checked.
     * <p>
     * Consequently, if the value of this property is set to a negative value,
     * then the entire cipher text gets <em>never</em> authenticated (CRC-32
     * checking only), and if set to {@link Long#MAX_VALUE}, then the entire
     * cipher text gets <em>always</em> authenticated (no CRC-32 checking).
     *
     * @return The value of the property {@code authenticationTrigger}.
     */
    protected abstract long getAuthenticationTrigger();

    @Override
    protected final boolean check(ZipDriverEntry entry, ZipInputService input) {
        // Optimization: If the cipher text alias the encrypted ZIP file is
        // smaller than the authentication trigger, then its entire cipher text
        // has already been authenticated by {@link ZipRaesDriver#zipInput}.
        // Hence, checking the CRC-32 value of the entry is redundant.
        return input.length() > getAuthenticationTrigger();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipRaesDriver} decorates the
     * given controller with a package private controller which keeps track of
     * the encryption keys.
     * This should getKeyManager overridden in order to return just {@code controller} if
     * and only if you are overriding
     * {@link #raesParameters(FsModel)}, too, and do not want to use the
     * locatable key manager to resolve passwords for RAES encryption.
     */
    @Override
    public <M extends FsModel> FsController<M>
    decorate(FsController<M> controller) {
        return new ZipRaesKeyController<>(controller, this);
    }

    @Override
    protected ZipInputService newZipInput(
            final FsModel model,
            final Source source)
    throws IOException {
        final class Source extends AbstractSource {
            @Override
            public SeekableByteChannel channel() throws IOException {
                final RaesReadOnlyChannel channel = RaesReadOnlyChannel
                        .create(raesParameters(model), source);
                try {
                    if (channel.size() <= getAuthenticationTrigger())
                        channel.authenticate();
                    return channel;
                } catch (final Throwable ex) {
                    try {
                        channel.close();
                    } catch (final IOException ex2) {
                        ex.addSuppressed(ex2);
                    }
                    throw ex;
                }
            }
        } // Source

        return new ZipInputService(model, new Source(), this);
    }

    @Override
    protected OutputService<ZipDriverEntry> newOutput(
            final FsModel model,
            final Sink sink,
            final @CheckForNull @WillNotClose InputService<ZipDriverEntry> input)
    throws IOException {
        final class Sink extends AbstractSink {
            @Override
            public OutputStream stream() throws IOException {
                return RaesOutputStream.create(raesParameters(model), sink);
            }
        } // Sink

        final ZipInputService zis = (ZipInputService) input;
        return new MultiplexingOutputService<>(getIoPool(),
                new ZipOutputService(model, new Sink(), zis, this));
    }

    /**
     * Sets {@link FsAccessOption#STORE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    protected final OptionOutputSocket sink(
            BitField<FsAccessOption> options,
            final FsController<?> controller,
            final FsEntryName name) {
        options = options.clear(GROW);
        // Leave FsAccessOption.COMPRESS untouched - the controller shall have the
        // opportunity to apply its own preferences to sort out such a conflict.
        return new OptionOutputSocket(
                controller.output(options.set(STORE), name, null),
                options); // use modified options!
    }

    /**
     * Returns a new {@link ZipDriverEntry}, enforcing that the data gets
     * {@code DEFLATED} when written, even if copying data from a
     * {@code STORED} source entry.
     * This feature strengthens the security level of the authentication
     * process and inhibits the use of an unencrypted temporary I/O entry
     * (usually a temporary file) in case the sink is not copied from a file
     * system entry as its input.
     * <p>
     * Furthermore, the method {@link ZipDriverEntry#clearEncryption()} is
     * called in order to prevent adding a redundant encryption layer for the
     * individual ZIP entry because this would confuse users, increase the size
     * of the resulting archive file and unecessarily heat the CPU.
     */
    @Override
    public ZipDriverEntry newEntry(
            final BitField<FsAccessOption> options,
            final String path,
            final Type type,
            final @CheckForNull Entry template) {
        final ZipDriverEntry entry
                = super.newEntry(options.set(COMPRESS), path, type, template);
        // Fix for http://java.net/jira/browse/TRUEZIP-176 :
        // Entry level encryption is enabled if mknod.getKeyManager(ENCRYPTED) is true
        // OR template is an instance of ZipEntry
        // AND ((ZipEntry) template).isEncrypted() is true.
        // Now switch off entry level encryption because encryption is already
        // provided by the RAES wrapper file format.
        entry.clearEncryption();
        return entry;
    }
}
