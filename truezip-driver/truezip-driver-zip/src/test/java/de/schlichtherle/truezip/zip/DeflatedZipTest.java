/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import static de.schlichtherle.truezip.zip.ZipEntry.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class DeflatedZipTest extends ZipTestSuite {
    @Override
    public ZipEntry newEntry(String name) {
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(DEFLATED);
        return entry;
    }
}
