/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.tar.bzip2;

import global.namespace.truevfs.comp.tardriver.TarDriverEntry;
import global.namespace.truevfs.kernel.api.FsArchiveDriverTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class TarBZip2DriverTest extends FsArchiveDriverTestSuite<TarDriverEntry, TarBZip2Driver> {

    @Override
    protected TarBZip2Driver newArchiveDriver() {
        return new TestTarBZip2Driver();
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
