/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.zipdriver;

import global.namespace.truevfs.comp.cio.IoBufferPool;
import global.namespace.truevfs.comp.key.api.KeyManagerMap;
import global.namespace.truevfs.comp.key.api.common.AesPbeParameters;
import global.namespace.truevfs.comp.key.api.prompting.TestView;
import global.namespace.truevfs.kernel.api.FsTestConfig;

/**
 * @author Christian Schlichtherle
 */
public final class TestWinZipAesDriver extends ZipDriver {

    private final TestKeyManagerMap keyManagerMap = new TestKeyManagerMap();

    @Override
    public IoBufferPool getPool() { return FsTestConfig.get().getPool(); }

    @Override
    public KeyManagerMap getKeyManagerMap() { return keyManagerMap; }

    public TestView<AesPbeParameters> getView() { return keyManagerMap.getView(); }
}
