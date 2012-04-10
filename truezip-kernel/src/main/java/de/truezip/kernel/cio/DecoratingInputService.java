/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;

/**
 * An abstract decorator for an input service.
 *
 * @param  <E> the type of the entries in the decorated input service.
 * @param  <I> the type of the decorated input service.
 * @see    DecoratingOutputService
 * @author Christian Schlichtherle
 */
public abstract class DecoratingInputService<   E extends Entry,
                                                I extends InputService<E>>
extends DecoratingContainer<E, I>
implements InputService<E> {

    @CreatesObligation
    protected DecoratingInputService(
            @CheckForNull @WillCloseWhenClosed I input) {
        super(input);
    }

    @Override
    public InputSocket<E> getInputSocket(String name) {
        return container.getInputSocket(name);
    }

    @Override
    public void close() throws IOException {
        container.close();
    }
}
