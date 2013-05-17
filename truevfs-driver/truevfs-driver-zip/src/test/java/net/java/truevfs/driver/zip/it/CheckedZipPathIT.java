/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.it;

import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.access.it.TPathITSuite;
import net.java.truevfs.comp.zipdriver.CheckedZipDriver;
import net.java.truevfs.kernel.spec.TestConfig;

/**
 * @author Christian Schlichtherle
 */
public final class CheckedZipPathIT extends TPathITSuite<CheckedZipDriver> {

    @Override
    protected String getExtensionList() { return "zip"; }

    @Override
    protected CheckedZipDriver newArchiveDriver() {
        return new CheckedZipDriver() {
            @Override
            public IoBufferPool getPool() {
                return TestConfig.get().getPool();
            }
        };
    }
}
