/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import lombok.val;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.Entry.Access;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.OutputSocket;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.FilteringIterator;
import net.java.truevfs.kernel.spec.*;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.*;
import java.util.*;

import static net.java.truecommons.cio.Entry.Access.*;
import static net.java.truecommons.cio.Entry.Size.DATA;
import static net.java.truecommons.cio.Entry.Type.*;
import static net.java.truecommons.cio.Entry.UNKNOWN;
import static net.java.truevfs.kernel.spec.FsAccessOption.CACHE;
import static net.java.truevfs.kernel.spec.FsAccessOption.EXCLUSIVE;
import static net.java.truevfs.kernel.spec.FsNodeName.SEPARATOR;
import static net.java.truevfs.kernel.spec.FsSyncOptions.UMOUNT;

/**
 * A {@link FileSystem} implementation for use with NIO.2.
 *
 * @author Christian Schlichtherle
 */
public final class TFileSystem extends FileSystem {

    private final FsController controller;
    private final TFileSystemProvider provider;

    TFileSystem(final TPath path) {
        assert null != path;
        this.controller = TConfig
                .current()
                .getManager()
                .controller(path.getArchiveDetector(), path.getMountPoint());
        this.provider = TFileSystemProvider.get(path.getName());

        assert invariants();
    }

    private boolean invariants() {
        assert null != getController();
        assert null != provider();
        return true;
    }

    private FsController getController() {
        return controller;
    }

    FsMountPoint getMountPoint() {
        return getController().getModel().getMountPoint();
    }

    @Override
    public TFileSystemProvider provider() {
        return provider;
    }

    /**
     * Commits all pending changes for this (federated) file system and all its
     * federated child file systems to their respective parent file system,
     * closes their associated target (archive) file in order to allow access
     * by third parties (e.g.&#160;other processes), cleans up any temporary
     * allocated resources (e.g.&#160;temporary files) and purges any cached
     * data.
     * <p>
     * Calling this method is equivalent to
     * {@link #sync(BitField) sync(FsSyncOptions.UMOUNT)}.
     *
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *                                apply.
     *                                This implies that the respective parent file system has been
     *                                synchronized with constraints, e.g. if an unclosed archive entry
     *                                stream gets forcibly closed.
     * @throws FsSyncException        if any error conditions apply.
     *                                This implies some loss of data!
     * @see #sync(BitField)
     */
    @Override
    public void close() throws FsSyncWarningException, FsSyncException {
        sync(UMOUNT);
    }

    /**
     * Commits all pending changes for this (federated) file system and all its
     * federated child file systems to their respective parent file system with
     * respect to the given options.
     *
     * @param options a bit field of options for the synchronization operation.
     * @throws IllegalArgumentException if the combination of synchronization
     *                                  options is illegal, e.g. if
     *                                  {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *                                  {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set or if
     *                                  {@code FsSyncOption.ABORT_CHANGES} is set.
     * @throws FsSyncWarningException   if <em>only</em> warning conditions
     *                                  apply.
     *                                  This implies that the respective parent file system has been
     *                                  synchronized with constraints, e.g. if
     *                                  {@code FsSyncOption.FORCE_CLOSE_INPUT} or
     *                                  {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set and an unclosed
     *                                  archive entry stream gets forcibly closed.
     * @throws FsSyncException          if any error conditions apply.
     *                                  This implies some loss of data!
     */
    public void sync(BitField<FsSyncOption> options) throws FsSyncWarningException, FsSyncException {
        TVFS.sync(getMountPoint(), options);
    }

    /**
     * Returns {@code true}.
     *
     * @return {@code true}.
     */
    @Override
    public boolean isOpen() {
        return true;
    }

    /**
     * Returns {@code false}.
     *
     * @return {@code false}.
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * Returns {@link File#separator}.
     *
     * @return {@link File#separator}.
     */
    @Override
    public String getSeparator() {
        return File.separator;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singleton((Path)
                new TPath(getMountPoint().toHierarchicalUri().resolve(SEPARATOR)));
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public Iterable<FileStore> getFileStores() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Collections.singleton("basic");
    }

    /**
     * Constructs a new path from the given sub path strings.
     * <p>
     * This method scans the {@link TPath#toString() path name} resulting
     * from the segment parameters to detect prospective archive files using
     * the current archive detector
     * {@code TConfig.current().getArchiveDetector()}.
     * <p>
     * The supported path name separators are "{@link File#separator}" and
     * "{@code /}".
     * Any leading and trailing separators in the resulting path name current
     * discarded.
     *
     * @param first the first sub path string.
     * @param more  optional sub path strings.
     */
    @Override
    public TPath getPath(String first, String... more) {
        return new TPath(this, first, more);
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public WatchService newWatchService() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    SeekableByteChannel newByteChannel(
            final TPath path,
            final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs)
            throws IOException {
        final FsNodeName name = path.getNodeName();
        final FsController controller = getController();
        if (options.isEmpty() || options.contains(StandardOpenOption.READ)) {
            final BitField<FsAccessOption>
                    o = path.inputOptions(options).set(CACHE);
            return controller
                    .input(o, name)
                    .channel(Optional.empty());
        } else {
            final BitField<FsAccessOption>
                    o = path.outputOptions(options).set(CACHE);
            try {
                return controller
                        .output(o, name, Optional.empty())
                        .channel(Optional.empty());
            } catch (final IOException ex) {
                // TODO: Filter FileAlreadyExistsException.
                if (o.get(EXCLUSIVE) && controller.node(o, name).isPresent())
                    throw (IOException) new FileAlreadyExistsException(path.toString())
                            .initCause(ex);
                throw ex;
            }
        }
    }

    InputStream newInputStream(TPath path, OpenOption... options)
            throws IOException {
        return getController()
                .input(path.inputOptions(options), path.getNodeName())
                .stream(Optional.empty());
    }

    OutputStream newOutputStream(TPath path, OpenOption... options)
            throws IOException {
        return getController()
                .output(path.outputOptions(options), path.getNodeName(), Optional.empty())
                .stream(Optional.empty());
    }

    DirectoryStream<Path> newDirectoryStream(final TPath path, final Filter<? super Path> filter) throws IOException {
        val entry = stat(path);
        final Set<String> set;
        if (!entry.isPresent() || null == (set = entry.get().getMembers())) {
            throw new NotDirectoryException(path.toString());
        }

        class Adapter implements Iterator<Path> {
            final Iterator<String> it = set.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Path next() {
                return path.resolve(it.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        class FilterIterator extends FilteringIterator<Path> {

            FilterIterator() {
                super(new Adapter());
            }

            @Override
            protected boolean accept(Path element) {
                try {
                    return filter.accept(element);
                } catch (IOException ex) {
                    throw new DirectoryIteratorException(ex);
                }
            }
        }

        return new Stream(new FilterIterator());
    }

    private static final class Stream implements DirectoryStream<Path> {
        final Iterator<Path> it;
        boolean consumed;

        Stream(final Iterator<Path> it) {
            this.it = it;
        }

        @Override
        public Iterator<Path> iterator() {
            if (consumed) {
                throw new IllegalStateException();
            }
            consumed = true;
            return it;
        }

        @Override
        public void close() {
            consumed = true;
        }
    }

    void createDirectory(final TPath path, final FileAttribute<?>... attrs)
            throws IOException {
        if (0 < attrs.length)
            throw new UnsupportedOperationException();
        final FsController controller = getController();
        final FsNodeName name = path.getNodeName();
        final BitField<FsAccessOption> options = path.getAccessPreferences();
        try {
            controller.make(
                    options, name,
                    DIRECTORY,
                    Optional.empty());
        } catch (IOException ex) {
            if (controller.node(options, name).isPresent()) {
                throw (IOException) new FileAlreadyExistsException(path.toString()).initCause(ex);
            }
            throw ex;
        }
    }

    void delete(TPath path) throws IOException {
        getController().unlink(path.getAccessPreferences(), path.getNodeName());
    }

    Optional<? extends FsNode> stat(TPath path) throws IOException {
        return getController().node(path.getAccessPreferences(), path.getNodeName());
    }

    InputSocket<?> input(TPath path,
                         BitField<FsAccessOption> options) {
        return getController().input(options, path.getNodeName());
    }

    OutputSocket<?> output(TPath path,
                           BitField<FsAccessOption> options,
                           @CheckForNull Entry template) {
        return getController().output(options, path.getNodeName(), Optional.ofNullable(template));
    }

    void checkAccess(final TPath path, final AccessMode... modes)
            throws IOException {
        final FsNodeName name = path.getNodeName();
        final BitField<FsAccessOption> options = path.getAccessPreferences();
        final BitField<Access> types = types(modes);
        getController().checkAccess(options, name, types);
    }

    private static BitField<Access> types(final AccessMode... modes) {
        final EnumSet<Access> access = EnumSet.noneOf(Access.class);
        for (final AccessMode mode : modes) {
            switch (mode) {
                case READ:
                    access.add(READ);
                    break;
                case WRITE:
                    access.add(WRITE);
                    break;
                case EXECUTE:
                    access.add(EXECUTE);
                    break;
            }
        }
        return BitField.copyOf(access);
    }

    @Nullable
    <V extends FileAttributeView> V getFileAttributeView(
            TPath path,
            Class<V> type,
            LinkOption... options) {
        if (type.isAssignableFrom(BasicFileAttributeView.class))
            return type.cast(new FsNodeAttributeView(path));
        return null;
    }

    <A extends BasicFileAttributes> A readAttributes(
            TPath path,
            Class<A> type,
            LinkOption... options)
            throws IOException {
        if (type.isAssignableFrom(BasicFileAttributes.class))
            return type.cast(new FsNodeAttributes(path));
        throw new UnsupportedOperationException();
    }

    private final class FsNodeAttributeView implements BasicFileAttributeView {
        private final TPath path;

        FsNodeAttributeView(final TPath path) {
            this.path = path;
        }

        @Override
        public String name() {
            return "basic";
        }

        @Override
        public BasicFileAttributes readAttributes() throws IOException {
            return new FsNodeAttributes(path);
        }

        @Override
        public void setTimes(final FileTime lastModifiedTime,
                             final FileTime lastAccessTime,
                             final FileTime createTime)
                throws IOException {
            final FsController controller = getController();
            final Map<Access, Long> times = new EnumMap<>(Access.class);
            if (null != lastModifiedTime)
                times.put(WRITE, lastModifiedTime.toMillis());
            if (null != lastAccessTime)
                times.put(READ, lastAccessTime.toMillis());
            if (null != createTime)
                times.put(CREATE, createTime.toMillis());
            controller.setTime(
                    path.getAccessPreferences(), path.getNodeName(),
                    times);
        }
    }

    private final class FsNodeAttributes implements BasicFileAttributes {

        private final FsNode entry;

        FsNodeAttributes(final TPath path) throws IOException {
            val on = getController().node(path.getAccessPreferences(), path.getNodeName());
            if (!on.isPresent()) {
                throw new NoSuchFileException(path.toString());
            }
            entry = on.get();
        }

        @Override
        public FileTime lastModifiedTime() {
            return FileTime.fromMillis(entry.getTime(WRITE));
        }

        @Override
        public FileTime lastAccessTime() {
            return FileTime.fromMillis(entry.getTime(READ));
        }

        @Override
        public FileTime creationTime() {
            return FileTime.fromMillis(entry.getTime(CREATE));
        }

        @Override
        public boolean isRegularFile() {
            return entry.isType(FILE);
        }

        @Override
        public boolean isDirectory() {
            return entry.isType(DIRECTORY);
        }

        @Override
        public boolean isSymbolicLink() {
            return entry.isType(SYMLINK);
        }

        @Override
        public boolean isOther() {
            return entry.isType(SPECIAL);
        }

        @Override
        public long size() {
            final long size = entry.getSize(DATA);
            return UNKNOWN == size ? 0 : size;
        }

        /**
         * @throws UnsupportedOperationException always
         */
        @Override
        public Object fileKey() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}