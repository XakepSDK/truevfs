/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file;

import de.truezip.driver.file.TempFilePool;
import de.truezip.driver.file.TempFilePoolService;
import de.truezip.kernel.spi.IOPoolService;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class TempFilePoolServiceTest {

    private IOPoolService instance;
    
    @Before
    public void setUp() {
        instance = new TempFilePoolService();
    }

    @Test
    public void testGet() {
        assertSame(instance.getIOPool(), TempFilePool.INSTANCE);
    }
}