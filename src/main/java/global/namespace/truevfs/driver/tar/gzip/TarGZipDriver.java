/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.tar.gzip;

import global.namespace.truevfs.comp.cio.InputContainer;
import global.namespace.truevfs.comp.cio.MultiplexingOutputContainer;
import global.namespace.truevfs.comp.cio.OutputContainer;
import global.namespace.truevfs.comp.io.AbstractSink;
import global.namespace.truevfs.comp.io.AbstractSource;
import global.namespace.truevfs.comp.io.Streams;
import global.namespace.truevfs.comp.tardriver.TarDriver;
import global.namespace.truevfs.comp.tardriver.TarDriverEntry;
import global.namespace.truevfs.comp.tardriver.TarInputContainer;
import global.namespace.truevfs.comp.tardriver.TarOutputContainer;
import global.namespace.truevfs.comp.util.BitField;
import global.namespace.truevfs.kernel.api.*;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static global.namespace.truevfs.kernel.api.FsAccessOption.STORE;

/**
 * An archive driver for GZIP compressed TAR files (TAR.GZIP).
 * <p>
 * Subclasses must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
public class TarGZipDriver extends TarDriver {

    /**
     * Returns the size of the I/O buffer.
     * <p>
     * The implementation in the class {@link TarGZipDriver} returns
     * {@link Streams#BUFFER_SIZE}.
     *
     * @return The size of the I/O buffer.
     */
    public int getBufferSize() {
        return Streams.BUFFER_SIZE;
    }

    /**
     * Returns the compression level to use when writing a GZIP sink stream.
     * <p>
     * The implementation in the class {@link TarGZipDriver} returns
     * {@link Deflater#BEST_COMPRESSION}.
     *
     * @return The compression level to use when writing a GZIP sink stream.
     */
    public int getLevel() {
        return Deflater.BEST_COMPRESSION;
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
                    return new GZIPInputStream(in, getBufferSize());
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
                    return new FixedGZIPOutputStream(out, getBufferSize(), getLevel());
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

    /**
     * Extends its super class to set the deflater level.
     */
    private static final class FixedGZIPOutputStream extends GZIPOutputStream {

        boolean closed;

        FixedGZIPOutputStream(OutputStream out, int size, int level) throws IOException {
            super(out, size);
            def.setLevel(level);
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
