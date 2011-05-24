/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a cp of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.file;

import java.io.File;
import de.schlichtherle.truezip.io.Paths;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.IOSocket;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Arrays;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.fs.FsOutputOption.*;

/**
 * Provides fast bulk I/O operations for {@link File}s and {@link TFile}s.
 * <p>
 * Note that in contrast to the {@link TFile} class, the methods in this
 * class accept plain old {@link File} objects.
 * However, full advantage is taken if a parameter is a {@link TFile} object.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
@Immutable
final class TBIO {

    private static final BitField<FsInputOption>
            NO_INPUT_OPTIONS = BitField.noneOf(FsInputOption.class);
    private static final BitField<FsOutputOption>
            NO_OUTPUT_OPTIONS = BitField.noneOf(FsOutputOption.class);

    /** You cannot instantiate this class. */
    private TBIO() {
    }

    /**
     * Moves the source file or directory tree to the destination file or
     * directory tree by performing a recursive cp-then-delete operation.
     * Hence, this file system operation works even with archive files or
     * entries within archive files, but is <em>not</em> atomic.
     *
     * @param  src the source directory tree or file.
     *         This file system entity needs to exist.
     * @param  dst the destination directory tree or file.
     *         This file systeme entity may or may not exist.
     *         If it does, its contents are overwritten.
     * @param  detector the object used to detect any archive files in the
     *         source and destination paths.
     * @throws IOException if the source path contains the destination path
     *         or an elementary operation fails for any reason.
     */
    public static void
    mv(final File src, final File dst, final TArchiveDetector detector)
    throws IOException {
        if (dst.exists())
            throw new IOException(dst + " (destination exists already)");
        checkContains(src, dst);
        mv0(src, dst, detector);
    }

    /** Unchecked parameters version. */
    private static void
    mv0(final File src, final File dst, final TArchiveDetector detector)
    throws IOException {
        if (src.isDirectory()) {
            final long srcLastModified = src.lastModified();
            final boolean srcIsArchived = src instanceof TFile
                    && null != ((TFile) src).getInnerArchive();
            final boolean dstIsArchived = dst instanceof TFile
                    && null != ((TFile) dst).getInnerArchive();
            final boolean srcIsGhost = srcIsArchived
                    && 0 >= srcLastModified;
            if (!srcIsGhost || !dstIsArchived || !TFile.isLenient())
                if (!dst.mkdir() && !dst.isDirectory())
                    throw new IOException(dst + " (not a directory)");
            final String[] members = src.list();
            if (!srcIsArchived && dstIsArchived) {
                // Create sorted entries if writing a new archive file.
                // This is courtesy only, so natural order is sufficient.
                Arrays.sort(members);
            }
            for (final String member : members)
                mv0(    new TFile(src, member, detector),
                        new TFile(dst, member, detector),
                        detector);
            if (!srcIsGhost)
                if (!dst.setLastModified(srcLastModified))
                    throw new IOException(dst + " (cannot set last modification time)");
        } else if (src.isFile()) {
            if (dst.exists() && !dst.isFile())
                throw new IOException(dst + " (not a file)");
            cp0(true, src, dst);
        } else if (src.isFile()) {
            throw new IOException(src + " (cannot copy special file)");
        } else {
            throw new IOException(src + " (missing file)");
        }
        if (!src.delete())
            throw new IOException(src + " (cannot delete)");
    }

    /**
     * Recursively copies the source directory tree or file to the destination
     * directory tree or file.
     *
     * @param  preserve if an elementary cp operation shall cp as much
     *         properties of the source file to the destination file, too.
     *         Currently, only the last modification time is preserved.
     *         Note that this property set may get extended over time.
     * @param  src the source directory tree or file.
     *         This file system entity needs to exist.
     * @param  dst the destination directory tree or file.
     *         This file systeme entity may or may not exist.
     *         If it does, its contents are overwritten.
     * @param  srcDetector the object used to detect any archive files in the
     *         source path.
     * @param  dstDetector the object used to detect any archive files in the
     *         destination path.
     * @throws IOException if the source path contains the destination path
     *         or an elementary operation fails for any reason.
     */
    public static void
    cp_r(   final boolean preserve,
            final File src,
            final File dst,
            final TArchiveDetector srcDetector,
            final TArchiveDetector dstDetector)
    throws IOException {
        checkContains(src, dst);
        cp_r0(preserve, src, dst, srcDetector, dstDetector);
    }

    /* Unchecked parameters version. */
    private static void
    cp_r0(  final boolean preserve,
            final File src,
            final File dst,
            final TArchiveDetector srcDetector,
            final TArchiveDetector dstDetector)
    throws IOException {
        if (src.isDirectory()) {
            final long srcLastModified = src.lastModified();
            final boolean srcArchived = src instanceof TFile
                    && null != ((TFile) src).getInnerArchive();
            final boolean dstArchived = dst instanceof TFile
                    && null != ((TFile) dst).getInnerArchive();
            final boolean srcIsGhost = srcArchived
                    && 0 >= srcLastModified;
            if (!srcIsGhost || !dstArchived || !TFile.isLenient())
                if (!dst.mkdir() && !dst.isDirectory())
                    throw new IOException(dst + " (not a directory)");
            final String[] members = src.list();
            if (!srcArchived && dstArchived) {
                // Create sorted entries if writing a new archive.
                // This is a courtesy only, so natural order is sufficient.
                Arrays.sort(members);
            }
            for (final String member : members)
                cp_r0(  preserve,
                        new TFile(src, member, srcDetector),
                        new TFile(dst, member, dstDetector),
                        srcDetector, dstDetector);
            if (preserve && !srcIsGhost)
                if (!dst.setLastModified(srcLastModified))
                    throw new IOException(dst + " (cannot set last modification time)");
        } else if (src.isFile()) {
            if (dst.exists() && !dst.isFile())
                throw new IOException(dst + " (not a file)");
            cp0(preserve, src, dst);
        } else if (src.isFile()) {
            throw new IOException(src + " (cannot copy special file)");
        } else {
            throw new IOException(src + " (missing file)");
        }
    }

    /**
     * Copies a single source file to a destination file.
     * The name of this method is inspired by the Unix command line utility
     * {@code cp}.
     *
     * @param  preserve if an elementary cp operation shall cp as much
     *         properties of the source file to the destination file, too.
     *         Currently, only the last modification time is preserved.
     *         Note that this property set may get extended over time.
     * @param  src the source file.
     *         This file system entity needs to exist.
     * @param  dst the destination file.
     *         This file systeme entity may or may not exist.
     *         If it does, its contents are overwritten.
     * @throws IOException if the source path contains the destination path
     *         or an elementary operation fails for any reason.
     */
    public static void
    cp(final boolean preserve, final File src, final File dst)
    throws IOException {
        checkContains(src, dst);
        cp0(preserve, src, dst);
    }

    /* Unchecked parameters version. */
    private static void
    cp0(final boolean preserve, final File src, final File dst)
    throws IOException {
        final InputSocket<?> input = getInputSocket(src, NO_INPUT_OPTIONS);
        final OutputSocket<?> output = getOutputSocket(dst,
                NO_OUTPUT_OPTIONS.set(CREATE_PARENTS, TFile.isLenient()),
                preserve ? input.getLocalTarget() : null);
        IOSocket.copy(input, output);
    }

    /**
     * Recursively deletes the given file or directory tree.
     *
     * @param  node the file or directory tree to delete recursively.
     * @throws IOException if an elementary operation fails for any reason.
     */
    public static void rm_r(final File node, final TArchiveDetector detector)
    throws IOException {
        if (node.isDirectory()) {
            for (final String member : node.list())
                rm_r(new TFile(node, member, detector), detector);
        }
        if (!node.delete())
            throw new IOException(node + " (cannot delete)");
    }

    /**
     * Throws an {@code IOException} if and only if the path represented by
     * {@code a} contains the path represented by {@code b}, where a path is
     * said to contain another path if and only if it is equal or an ancestor
     * of the other path.
     * <p>
     * Note that this method uses the absolute path of both files as if by
     * calling {@link File#getAbsolutePath()}.
     *
     * @param a a file.
     * @param b another file.
     */
    private static void checkContains(File a, File b) throws IOException {
        if (Paths.contains( a.getAbsolutePath(),
                            b.getAbsolutePath(),
                            File.separatorChar))
            throw new IOException(b + " (contained in " + a + ")");
    }

    static InputSocket<?>
    getInputSocket(final File src, final BitField<FsInputOption> options) {
        if (src instanceof TFile) {
            // This block could get removed in order to use the more general
            // pattern below. However, it's kept for better performance.
            // TODO: Removing this block yields occasional racing problems - fix this!
            final TFile file = (TFile) src;
            final TFile archive = file.getInnerArchive();
            if (null != archive)
                return archive.getController()
                        .getInputSocket(file.getInnerEntryName0(), options);
        }
        final FsPath path = new FsPath(src);
        return TFile.manager
                .getController(path.getMountPoint(), TFile.getDefaultArchiveDetector())
                .getInputSocket(path.getEntryName(), options);
    }

    static OutputSocket<?>
    getOutputSocket(final File dst,
                    final BitField<FsOutputOption> options,
                    final @CheckForNull Entry template) {
        if (dst instanceof TFile) {
            // This block could get removed in order to use the more general
            // pattern below. However, it's kept for better performance.
            // TODO: Removing this block yields occasional racing problems - fix this!
            final TFile file = (TFile) dst;
            final TFile archive = file.getInnerArchive();
            if (null != archive)
                return archive.getController()
                        .getOutputSocket(file.getInnerEntryName0(), options, template);
        }
        final FsPath path = new FsPath(dst);
        return TFile.manager
                .getController(path.getMountPoint(), TFile.getDefaultArchiveDetector())
                .getOutputSocket(path.getEntryName(), options, template);
    }
}
