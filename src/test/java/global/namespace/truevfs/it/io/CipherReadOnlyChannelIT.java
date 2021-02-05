/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.io;

import global.namespace.truevfs.commons.zip.crypto.CipherReadOnlyChannel;
import global.namespace.truevfs.commons.zip.crypto.SeekableBlockCipher;
import org.bouncycastle.crypto.engines.NullEngine;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Christian Schlichtherle
 */
public class CipherReadOnlyChannelIT extends ReadOnlyChannelITSuite {

    @Override
    protected SeekableByteChannel newChannel(Path path) throws IOException {
        return new CipherReadOnlyChannel(new SeekableNullEngine(), Files.newByteChannel(path));
    }

    private static final class SeekableNullEngine extends NullEngine implements SeekableBlockCipher {

        long blockCounter;

        SeekableNullEngine() { init(true, null); }

        @Override
        public void setBlockCounter(final long blockCounter) { this.blockCounter = blockCounter; }

        @Override
        public long getBlockCounter() { return blockCounter; }
    }
}
