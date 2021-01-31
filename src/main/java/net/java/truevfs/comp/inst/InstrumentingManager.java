/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import net.java.truevfs.kernel.spec.*;

import java.util.Objects;

/**
 * @param  <M> the type of the mediator.
 * @author Christian Schlichtherle
 */
public class InstrumentingManager<M extends Mediator<M>>
extends FsDecoratingManager {

    protected final M mediator;

    public InstrumentingManager(
            final M mediator,
            final FsManager manager) {
        super(manager);
        this.mediator = Objects.requireNonNull(mediator);
    }

    @Override
    public FsController controller(
            FsCompositeDriver driver,
            FsMountPoint mountPoint) {
        return mediator.instrument(this,
                manager.controller(
                    mediator.instrument(this, driver),
                    mountPoint));
    }
}
