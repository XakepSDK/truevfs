/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jul;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;
import net.truevfs.extension.jmxjul.InstrumentingInputSocket;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.InputSocket;
import net.truevfs.kernel.cio.OutputSocket;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JulInputSocket<E extends Entry>
extends InstrumentingInputSocket<E> {

    JulInputSocket(JulDirector director, InputSocket<? extends E> model) {
        super(director, model);
    }

    @Override
    public InputStream stream(OutputSocket<? extends Entry> peer)
    throws IOException {
        return new JulInputStream(socket(), peer);
    }

    @Override
    public SeekableByteChannel channel(OutputSocket<? extends Entry> peer)
    throws IOException {
        return new JulInputChannel(socket(), peer);
    }
}
