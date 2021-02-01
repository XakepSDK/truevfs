/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.zip.raes;

import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.Entry.Type;
import global.namespace.truevfs.comp.cio.InputContainer;
import global.namespace.truevfs.comp.cio.OutputContainer;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.comp.zipdriver.JarDriver;
import global.namespace.truevfs.comp.zipdriver.JarDriverEntry;
import global.namespace.truevfs.comp.zipdriver.ZipInputContainer;
import global.namespace.truevfs.comp.zipdriver.ZipOutputContainer;
import global.namespace.truevfs.driver.zip.raes.crypto.RaesOutputStream;
import global.namespace.truevfs.driver.zip.raes.crypto.RaesParameters;
import global.namespace.truevfs.driver.zip.raes.crypto.RaesReadOnlyChannel;
import global.namespace.truevfs.kernel.api.*;
import global.namespace.truevfs.kernel.api.cio.MultiplexingOutputContainer;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

import static global.namespace.truevfs.kernel.api.FsAccessOption.*;

/**
 * An abstract archive driver for RAES encrypted ZIP files which optionally
 * authenticates the cipher data of the input archive files presented to it.
 * <p>
 * Sub-classes must be thread-safe and should be immutable!
 *
 * @author Christian Schlichtherle
 */
public abstract class ZipRaesDriver extends JarDriver {

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipRaesDriver} returns
     * {@code true} for future use.
     *
     * @return {@code true}
     */
    @Override
    public final boolean getPreambled() { return true; }

    /**
     * Returns the RAES parameters for the given file system model.
     * <p>
     * The implementation in the class {@link ZipRaesDriver} returns
     * {@code new KeyManagerRaesParameters(getKeyManagerMap().getKeyManager(AesPbeParameters.class), mountPointUri(model))}.
     *
     * @param  model the file system model.
     * @return The RAES parameters for the given file system model.
     */
    protected RaesParameters raesParameters(FsModel model) {
        return new KeyManagerRaesParameters(getKeyManagerMap(),
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
    public final boolean check(JarDriverEntry local, ZipInputContainer<JarDriverEntry> input) {
        // Optimization: If the cipher text alias the encrypted ZIP file is
        // smaller than the authentication trigger, then its entire cipher text
        // has already been authenticated by {@link ZipRaesDriver#zipInput}.
        // Hence, checking the CRC-32 value of the entry is redundant.
        return input.length() > getAuthenticationTrigger();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipRaesDriver} decorates the given controller with a package private
     * controller which keeps track of the AES PBE parameters, e.g. the encryption passwords.
     * This method should be overridden in order to return just {@code controller} if and only if you are overriding
     * {@link #raesParameters(FsModel)}, too, and do not want to use a locatable key manager to resolve
     * passwords for RAES encryption.
     */
    @Override
    public FsController decorate(FsController controller) { return new ZipRaesKeyController(controller, this); }

    @Override
    protected ZipInputContainer<JarDriverEntry> newZipInput(
            final FsModel model,
            final FsInputSocketSource source)
    throws IOException {
        final class Source extends FsInputSocketSource {
            Source() { super(source); }

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
        }
        return new ZipInputContainer<>(model, new Source(), this);
    }

    @Override
    protected OutputContainer<JarDriverEntry> newOutput(
            final FsModel model,
            final FsOutputSocketSink sink,
            final @CheckForNull InputContainer<JarDriverEntry> input)
    throws IOException {
        final ZipInputContainer<JarDriverEntry> zis = (ZipInputContainer<JarDriverEntry>) input;
        return new MultiplexingOutputContainer<>(getPool(),
                new ZipOutputContainer<>(model, new RaesSocketSink(model, sink), zis, this));
    }

    @SuppressWarnings("PackageVisibleInnerClass")
    final class RaesSocketSink extends FsOutputSocketSink {
        private final FsModel model;
        private final FsOutputSocketSink sink;

        RaesSocketSink(final FsModel model, final FsOutputSocketSink sink) {
            super(sink);
            this.model = model;
            this.sink = sink;
        }

        @Override
        public OutputStream stream() throws IOException {
            return RaesOutputStream.create(raesParameters(model), sink);
        }

        @Override
        public SeekableByteChannel channel() throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Sets {@link FsAccessOption#STORE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    protected final FsOutputSocketSink sink(
            BitField<FsAccessOption> options,
            final FsController controller,
            final FsNodeName name) {
        // Leave FsAccessOption.COMPRESS untouched - the driver shall be given
        // opportunity to apply its own preferences to sort out such a conflict.
        options = options.set(STORE);
        // The RAES file format cannot support GROWing.
        options = options.clear(GROW);
        return new FsOutputSocketSink(options, controller.output(options, name, Optional.empty()));
    }

    /**
     * Returns a new {@link JarDriverEntry}, requesting that the data gets
     * {@code DEFLATED} if no template is provided.
     * This feature strengthens the security level of the authentication
     * process and inhibits the use of an unencrypted temporary I/O entry
     * (usually a temporary file) in case the sink is not copied from a file
     * system entry as its input.
     * <p>
     * Furthermore, the method {@link JarDriverEntry#clearEncryption()} is
     * called in order to prevent adding a redundant encryption layer for the
     * individual ZIP entry because this would confuse users, increase the size
     * of the resulting archive file and unecessarily heat the CPU.
     */
    @Override
    public JarDriverEntry newEntry(
            final BitField<FsAccessOption> options,
            final String name,
            final Type type,
            final @CheckForNull Entry template) {
        final JarDriverEntry entry
                = super.newEntry(options.set(COMPRESS), name, type, template);
        // Fix for http://java.net/jira/browse/TRUEZIP-176 :
        // Entry level encryption is enabled if make.getKeyManager(ENCRYPTED) is true
        // OR template is an instance of ZipEntry
        // AND ((ZipEntry) template).isEncrypted() is true.
        // Now switch off entry level encryption because encryption is already
        // provided by the RAES wrapper file format.
        entry.clearEncryption();
        return entry;
    }
}
