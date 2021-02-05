/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api;

import global.namespace.truevfs.commons.cio.Entry;
import global.namespace.truevfs.commons.cio.OutputSocket;
import global.namespace.truevfs.commons.io.Sink;
import global.namespace.truevfs.commons.shed.BitField;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import java.util.Optional;

/**
 * An adapter from an output socket to a sink with access options.
 *
 * @author Christian Schlichtherle
 */
public class FsOutputSocketSink implements Sink {

    private final BitField<FsAccessOption> options;
    private final OutputSocket<? extends Entry> socket;

    public FsOutputSocketSink(
            final BitField<FsAccessOption> options,
            final OutputSocket<? extends Entry> socket) {
        this.options = Objects.requireNonNull(options);
        this.socket = Objects.requireNonNull(socket);
    }

    public FsOutputSocketSink(final FsOutputSocketSink sink) {
        this.options = sink.getOptions();
        this.socket = sink.getSocket();
    }

    public BitField<FsAccessOption> getOptions() {
        return options;
    }

    public OutputSocket<? extends Entry> getSocket() {
        return socket;
    }

    @Override
    public OutputStream stream() throws IOException {
        return getSocket().stream(Optional.empty());
    }

    @Override
    public SeekableByteChannel channel() throws IOException {
        return getSocket().channel(Optional.empty());
    }
}
