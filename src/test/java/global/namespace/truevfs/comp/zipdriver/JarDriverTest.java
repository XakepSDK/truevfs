/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.zipdriver;

import global.namespace.truevfs.comp.cio.IoBufferPool;
import global.namespace.truevfs.kernel.api.FsArchiveDriverTestSuite;
import global.namespace.truevfs.kernel.api.FsTestConfig;

/**
 * @author Christian Schlichtherle
 */
public final class JarDriverTest extends FsArchiveDriverTestSuite<JarDriverEntry, JarDriver> {

    @Override
    protected JarDriver newArchiveDriver() {
        return new JarDriver() {
            @Override
            public IoBufferPool getPool() {
                return FsTestConfig.get().getPool();
            }
        };
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
