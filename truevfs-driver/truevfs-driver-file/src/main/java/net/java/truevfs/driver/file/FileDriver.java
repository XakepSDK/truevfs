/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.FsManagerWithControllerFactory;

/**
 * A file system driver for the FILE scheme.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class FileDriver extends FsDriver {

    @Override
    public FsController newController(
            final FsManagerWithControllerFactory manager,
            final FsModel model,
            final @CheckForNull FsController parent) {
        assert null == parent;
        return new FileController(model);
    }
}
