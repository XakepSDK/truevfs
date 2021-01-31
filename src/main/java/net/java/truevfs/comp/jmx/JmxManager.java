/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import net.java.truevfs.comp.inst.InstrumentingManager;
import net.java.truevfs.kernel.spec.FsManager;

import javax.management.ObjectName;

/**
 * A controller for a {@linkplain FsManager file system manager}.
 *
 * @param  <M> the type of the JMX mediator.
 * @author Christian Schlichtherle
 */
public class JmxManager<M extends JmxMediator<M>>
extends InstrumentingManager<M> implements JmxComponent {

    public JmxManager(M mediator, FsManager manager) {
        super(mediator, manager);
    }

    private ObjectName getObjectName() {
        return mediator.nameBuilder(FsManager.class).get();
    }

    protected Object newView() { return new JmxManagerView<>(manager); }

    @Override
    public void activate() { mediator.register(getObjectName(), newView()); }
}
