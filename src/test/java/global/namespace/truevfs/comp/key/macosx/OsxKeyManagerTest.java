/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.macosx;

import global.namespace.truevfs.comp.key.api.common.AesKeyStrength;
import global.namespace.truevfs.comp.key.api.common.AesPbeParameters;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static global.namespace.truevfs.comp.key.macosx.OsxKeyManager.deserialize;
import static global.namespace.truevfs.comp.key.macosx.OsxKeyManager.serialize;
import static global.namespace.truevfs.comp.shed.Buffers.string;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Christian Schlichtherle
 */
public class OsxKeyManagerTest {

    private static final Logger logger = LoggerFactory.getLogger(OsxKeyManagerTest.class);

    @Test
    public void testXmlSerialization() {
        final AesPbeParameters original = new AesPbeParameters();
        original.setChangeRequested(true);
        original.setKeyStrength(AesKeyStrength.BITS_256);
        original.setPassword("föo".toCharArray());
        final ByteBuffer xml = serialize(original); // must not serialize password!

        logger.trace("Serialized form ({} bytes):\n{}", xml.remaining(), string(xml));

        final AesPbeParameters clone = deserialize(xml);
        assertNull(clone.getPassword());
        original.setPassword(null);
        assertEquals(original, clone);
    }
}
