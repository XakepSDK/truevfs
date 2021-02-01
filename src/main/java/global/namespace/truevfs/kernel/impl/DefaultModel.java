/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import global.namespace.truevfs.kernel.api.FsAbstractModel;
import global.namespace.truevfs.kernel.api.FsModel;
import global.namespace.truevfs.kernel.api.FsMountPoint;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
final class DefaultModel extends FsAbstractModel {

    private volatile boolean mounted;

    DefaultModel(FsMountPoint mountPoint, Optional<? extends FsModel> parent) {
        super(mountPoint, parent);
    }

    @Override
    public boolean isMounted() {
        return mounted;
    }

    @Override
    public void setMounted(final boolean mounted) {
        this.mounted = mounted;
    }
}
