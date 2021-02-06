/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.zip.raes;

import global.namespace.truevfs.comp.io.AbstractSource;
import global.namespace.truevfs.comp.io.OneTimeSink;
import global.namespace.truevfs.comp.io.OneTimeSource;
import global.namespace.truevfs.comp.zip.ZipFile;
import global.namespace.truevfs.comp.zip.ZipOutputStream;
import global.namespace.truevfs.driver.zip.raes.crypto.MockType0RaesParameters;
import global.namespace.truevfs.driver.zip.raes.crypto.RaesOutputStream;
import global.namespace.truevfs.driver.zip.raes.crypto.RaesParameters;
import global.namespace.truevfs.driver.zip.raes.crypto.RaesReadOnlyChannel;
import global.namespace.truevfs.it.zip.base.ZipITSuite;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;

import static java.nio.file.Files.newByteChannel;

/**
 * Tests compression and encryption of data.
 *
 * @author Christian Schlichtherle
 */
public final class ZipRaesIT extends ZipITSuite {

    /** Cipher text shorter than this gets authenticated. */
    private static final int AUTHENTICATION_TRIGGER = 512 * 1024;

    // Must not be static to enable parallel testing!
    private final RaesParameters raesParameters = new MockType0RaesParameters();

    @Override
    protected ZipOutputStream newZipOutputStream(final OutputStream out)
    throws IOException {
        final OutputStream ros = RaesOutputStream
                .create(raesParameters, new OneTimeSink(out));
        try {
            return new ZipOutputStream(ros);
        } catch (final Throwable ex) {
            try {
                ros.close();
            } catch (final IOException ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }

    @Override
    protected ZipOutputStream newZipOutputStream(
            final OutputStream out, final Charset cs)
    throws IOException {
        final OutputStream ros = RaesOutputStream
                .create(raesParameters, new OneTimeSink(out));
        try {
            return new ZipOutputStream(ros, cs);
        } catch (final Throwable ex) {
            try {
                ros.close();
            } catch (final IOException ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }

    private RaesReadOnlyChannel newRaesReadOnlyChannel(final Path file)
    throws IOException {
        return RaesReadOnlyChannel.create(
                raesParameters,
                new AbstractSource() {
                    @Override
                    public SeekableByteChannel channel() throws IOException {
                        return newByteChannel(file);
                    }
                });
    }

    @Override
    protected ZipFile newZipFile(final Path file)
    throws IOException {
        final RaesReadOnlyChannel rroc = newRaesReadOnlyChannel(file);
        try {
            if (rroc.size() < AUTHENTICATION_TRIGGER) // heuristic
                rroc.authenticate();
            return new ZipFile(rroc);
        } catch (final Throwable ex) {
            try {
                rroc.close();
            } catch (final IOException ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }

    @Override
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    protected ZipFile newZipFile(
            final Path file, final Charset charset)
    throws IOException {
        Objects.requireNonNull(charset);
        new String(new byte[0], charset); // may throw UnsupportedEncodingExceoption!
        final RaesReadOnlyChannel rroc = newRaesReadOnlyChannel(file);
        try {
            if (rroc.size() < AUTHENTICATION_TRIGGER) // heuristic
                rroc.authenticate();
            return new ZipFile(rroc, charset);
        } catch (final Throwable ex) {
            try {
                rroc.close();
            } catch (final IOException ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }

    @Override
    protected ZipFile newZipFile(final SeekableByteChannel sbc)
    throws IOException {
        final RaesReadOnlyChannel rroc = RaesReadOnlyChannel.create(
                raesParameters, new OneTimeSource(sbc));
        try {
            if (rroc.size() < AUTHENTICATION_TRIGGER) // heuristic
                rroc.authenticate();
            return new ZipFile(rroc);
        } catch (final Throwable ex) {
            try {
                rroc.close();
            } catch (final IOException ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }

    @Override
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    protected ZipFile newZipFile(
            final SeekableByteChannel channel,
            final Charset charset)
    throws IOException {
        Objects.requireNonNull(charset);
        new String(new byte[0], charset); // may throw UnsupportedEncodingExceoption!
        final RaesReadOnlyChannel rroc = RaesReadOnlyChannel.create(
                raesParameters, new OneTimeSource(channel));
        try {
            if (rroc.size() < AUTHENTICATION_TRIGGER) // heuristic
                rroc.authenticate();
            return new ZipFile(rroc, charset);
        } catch (final Throwable ex) {
            try {
                rroc.close();
            } catch (final IOException ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }

    /**
     * Skipped because the test is specific to plain ZIP files.
     */
    @Override
    public void testBadGetCheckedInputStream() { }

    /**
     * Skipped because appending to RAES encrypted ZIP files is not possible
     * by design.
     */
    @Override
    public void testAppending() { }
}
