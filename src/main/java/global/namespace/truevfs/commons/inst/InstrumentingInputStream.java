/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.inst;

import global.namespace.truevfs.commons.io.DecoratingInputStream;

import java.io.InputStream;
import java.util.Objects;

/**
 * @param  <M> the type of the mediator.
 * @see    InstrumentingOutputStream
 * @author Christian Schlichtherle
 */
public class InstrumentingInputStream<M extends Mediator<M>>
extends DecoratingInputStream {

    protected final M mediator;

    public InstrumentingInputStream(
            final M mediator,
            final InputStream in) {
        super(in);
        this.mediator = Objects.requireNonNull(mediator);
    }
}
