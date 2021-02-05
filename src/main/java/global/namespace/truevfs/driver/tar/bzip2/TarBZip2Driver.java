/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.tar.bzip2;

import global.namespace.truevfs.commons.cio.InputContainer;
import global.namespace.truevfs.commons.cio.MultiplexingOutputContainer;
import global.namespace.truevfs.commons.cio.OutputContainer;
import global.namespace.truevfs.commons.io.AbstractSink;
import global.namespace.truevfs.commons.io.AbstractSource;
import global.namespace.truevfs.commons.io.Streams;
import global.namespace.truevfs.commons.shed.BitField;
import global.namespace.truevfs.commons.tardriver.*;
import global.namespace.truevfs.kernel.api.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import javax.annotation.CheckForNull;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import static global.namespace.truevfs.kernel.api.FsAccessOption.STORE;

/**
 * An archive driver for BZIP2 compressed TAR files (TAR.BZIP2).
 * <p>
 * Subclasses must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
public class TarBZip2Driver extends TarDriver {

    /**
     * Returns the size of the I/O buffer.
     * <p>
     * The implementation in the class {@link TarBZip2Driver} returns
     * {@link Streams#BUFFER_SIZE}.
     *
     * @return The size of the I/O buffer.
     */
    public int getBufferSize() {
        return Streams.BUFFER_SIZE;
    }

    /**
     * Returns the compression level to use when writing a BZIP2 sink stream.
     * <p>
     * The implementation in the class {@link TarBZip2Driver} returns
     * {@link org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream#MAX_BLOCKSIZE}.
     *
     * @return The compression level to use when writing a BZIP2 sink stream.
     */
    public int getLevel() {
        return BZip2CompressorOutputStream.MAX_BLOCKSIZE;
    }

    @Override
    protected InputContainer<TarDriverEntry> newInput(
            final FsModel model,
            final FsInputSocketSource source)
            throws IOException {

        class Source extends AbstractSource {

            @Override
            public InputStream stream() throws IOException {
                final InputStream in = source.stream();
                try {
                    return new BZip2CompressorInputStream(
                            new BufferedInputStream(in, getBufferSize()));
                } catch (final Throwable t1) {
                    try {
                        in.close();
                    } catch (final Throwable t2) {
                        t1.addSuppressed(t2);
                    }
                    throw t1;
                }
            }
        }

        return new TarInputContainer(model, new Source(), this);
    }

    @Override
    protected OutputContainer<TarDriverEntry> newOutput(
            final FsModel model,
            final FsOutputSocketSink sink,
            final @CheckForNull InputContainer<TarDriverEntry> input)
            throws IOException {

        class Sink extends AbstractSink {

            @Override
            public OutputStream stream() throws IOException {
                final OutputStream out = sink.stream();
                try {
                    return new FixedBZip2CompressorOutputStream(
                            new FixedBufferedOutputStream(out, getBufferSize()),
                            getLevel());
                } catch (final Throwable t1) {
                    try {
                        out.close();
                    } catch (Throwable t2) {
                        t1.addSuppressed(t2);
                    }
                    throw t1;
                }
            }
        }

        return new MultiplexingOutputContainer<>(getPool(), new TarOutputContainer(model, new Sink(), this));
    }

    /**
     * Sets {@link FsAccessOption#STORE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    protected FsOutputSocketSink sink(
            BitField<FsAccessOption> options,
            final FsController controller,
            final FsNodeName name) {
        // Leave FsAccessOption.COMPRESS untouched - the driver shall be given
        // opportunity to apply its own preferences to sort out such a conflict.
        options = options.set(STORE);
        return new FsOutputSocketSink(options, controller.output(options, name, Optional.empty()));
    }

    private static final class FixedBZip2CompressorOutputStream extends BZip2CompressorOutputStream {

        final OutputStream out;
        boolean closed;

        FixedBZip2CompressorOutputStream(final OutputStream out, final int level) throws IOException {
            super(out, level);
            this.out = out;
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                out.close(); // enable recovery
            } else {
                closed = true;
                super.close();
            }
        }
    }
}
