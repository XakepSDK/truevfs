/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import net.java.truecommons.cio.DecoratingInputSocket;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.OutputSocket;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

/**
 * @param  <M> the type of the mediator.
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    InstrumentingOutputSocket
 * @author Christian Schlichtherle
 */
public class InstrumentingInputSocket<
        M extends Mediator<M>,
        E extends Entry>
extends DecoratingInputSocket<E> {

    protected final M mediator;

    public InstrumentingInputSocket(
            final M mediator,
            final InputSocket<? extends E> socket) {
        super(socket);
        this.mediator = Objects.requireNonNull(mediator);
    }

    @Override
    public InputStream stream(@CheckForNull OutputSocket<? extends Entry> peer)
    throws IOException {
        return mediator.instrument(this, socket.stream(peer));
    }

    @Override
    public SeekableByteChannel channel(
            @CheckForNull OutputSocket<? extends Entry> peer)
    throws IOException {
        return mediator.instrument(this, socket.channel(peer));
    }
}
