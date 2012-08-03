/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import net.java.truevfs.component.instrumentation.InstrumentingController;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsModel;

/**
 * @author  Christian Schlichtherle
 */
abstract class JmxController<M extends FsModel>
extends InstrumentingController<JmxDirector, M> {

    JmxController(JmxDirector director, FsController<M> controller) {
        super(director, controller);
    }

    abstract JmxIoStatistics getIOStatistics();
}
