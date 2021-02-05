/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.sample.raes;

import global.namespace.truevfs.commons.io.AbstractSink;
import global.namespace.truevfs.commons.io.AbstractSource;
import global.namespace.truevfs.commons.io.Streams;
import global.namespace.truevfs.commons.key.api.sl.KeyManagerMapLocator;
import global.namespace.truevfs.driver.zip.raes.KeyManagerRaesParameters;
import global.namespace.truevfs.driver.zip.raes.crypto.RaesOutputStream;
import global.namespace.truevfs.driver.zip.raes.crypto.RaesReadOnlyChannel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Saves and restores the contents of arbitrary files to and from the RAES
 * file format for encryption and decryption.
 * This class cannot get instantiated outside its package.
 * <p>
 * Note that this class is not intended to access RAES encrypted ZIP files -
 * use the TrueVFS client API modules for this task instead.
 *
 * @author Christian Schlichtherle
 */
public final class Raes {

    private Raes() { }

    /**
     * Encrypts the given plain file to the given RAES file.
     */
    public static void encrypt(final Path plain, final Path cipher)
    throws IOException {
        Streams.copy(
                new AbstractSource() {
                    @Override
                    public InputStream stream() throws IOException {
                        return Files.newInputStream(plain);
                    }
                },
                new AbstractSink() {
                    @Override
                    public OutputStream stream() throws IOException {
                        return RaesOutputStream.create(
                                new KeyManagerRaesParameters(
                                    KeyManagerMapLocator.SINGLETON,
                                    cipher/*.getCanonicalFile()*/.toUri()),
                                new AbstractSink() {
                                    @Override
                                    public OutputStream stream() throws IOException {
                                        return Files.newOutputStream(cipher);
                                    }
                                });
                    }
                });
    }

    /**
     * Decrypts the given RAES file to the given plain file.
     *
     * @param authenticate If this is {@code true}, the channel data get
     *        authenticated.
     *        Note that this operation has linear complexity.
     *        If this is {@code false}, only the key/password and the file
     *        length get authenticated.
     */
    public static void decrypt(
            final Path cipherFile,
            final Path plainFile,
            final boolean authenticate)
    throws IOException {
        Streams.copy(
                new AbstractSource() {
                    @Override
                    public SeekableByteChannel channel() throws IOException {
                        final RaesReadOnlyChannel
                                channel = RaesReadOnlyChannel.create(
                                    new KeyManagerRaesParameters(
                                        KeyManagerMapLocator.SINGLETON,
                                        cipherFile/*.getCanonicalFile()*/.toUri()),
                                    new AbstractSource() {
                                        @Override
                                        public SeekableByteChannel channel() throws IOException {
                                            return Files.newByteChannel(cipherFile);
                                        }
                                    });
                        if (authenticate)
                            channel.authenticate();
                        return channel;
                    }
                },
                new AbstractSink() {
                    @Override
                    public OutputStream stream() throws IOException {
                        return Files.newOutputStream(plainFile);
                    }
                });
    }
}
