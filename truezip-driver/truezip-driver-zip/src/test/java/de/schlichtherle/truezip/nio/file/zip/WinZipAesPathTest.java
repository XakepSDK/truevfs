/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.nio.file.zip;

import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.file.zip.TestWinZipAesDriver;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.key.MockView;
import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import de.schlichtherle.truezip.nio.file.TPathTestSuite;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class WinZipAesPathTest extends TPathTestSuite<TestWinZipAesDriver> {

    private @Nullable MockView<AesPbeParameters> view;

    @Override
    protected String getSuffixList() {
        return "zip";
    }

    @Override
    protected TestWinZipAesDriver newArchiveDriver() {
        return new TestWinZipAesDriver(IO_POOL_PROVIDER, view);
    }

    @Override
    public void setUp() throws Exception {
        this.view = new MockView<AesPbeParameters>();
        super.setUp();
        final TConfig config = TConfig.get();
        config.setOutputPreferences(config.getOutputPreferences().set(ENCRYPT));
        final AesPbeParameters key = new AesPbeParameters();
        key.setPassword("secret".toCharArray());
        view.setKey(key);
    }
}
