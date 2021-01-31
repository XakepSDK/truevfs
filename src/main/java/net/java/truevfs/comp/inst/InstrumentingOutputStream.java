/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import net.java.truecommons.io.DecoratingOutputStream;

import java.io.OutputStream;
import java.util.Objects;

/**
 * @param  <M> the type of the mediator.
 * @see    InstrumentingInputStream
 * @author Christian Schlichtherle
 */
public class InstrumentingOutputStream<M extends Mediator<M>>
extends DecoratingOutputStream {

    protected final M mediator;

    public InstrumentingOutputStream(
            final M mediator,
            final OutputStream out) {
        super(out);
        this.mediator = Objects.requireNonNull(mediator);
    }
}
