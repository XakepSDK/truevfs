/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import bali.Cache;
import bali.Lookup;
import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.Entry.Access;
import global.namespace.truevfs.comp.cio.Entry.Type;
import global.namespace.truevfs.comp.cio.InputSocket;
import global.namespace.truevfs.comp.cio.OutputSocket;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.kernel.api.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static bali.CachingStrategy.NOT_THREAD_SAFE;

/**
 * Provides read/write access to an archive file system.
 * This is a mirror of {@link global.namespace.truevfs.kernel.api.FsController} which has been customized to ease the
 * implementation.
 *
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
interface ArchiveController<E extends FsArchiveEntry> extends ArchiveModelAspect<E>, ReentrantReadWriteLockAspect {

    /**
     * Returns the parent filesystem controller.
     */
    @Lookup(param = "parent")
    FsController getParent();

    @Cache(NOT_THREAD_SAFE)
    default ReentrantReadWriteLock getLock() {
        return getModel().getLock();
    }

    Optional<? extends FsNode> node(BitField<FsAccessOption> options, FsNodeName name) throws IOException;

    void checkAccess(BitField<FsAccessOption> options, FsNodeName name, BitField<Access> types) throws IOException;

    void setReadOnly(BitField<FsAccessOption> options, FsNodeName name) throws IOException;

    boolean setTime(BitField<FsAccessOption> options, FsNodeName name, Map<Access, Long> times) throws IOException;

    boolean setTime(BitField<FsAccessOption> options, FsNodeName name, BitField<Access> types, long time) throws IOException;

    InputSocket<? extends Entry> input(BitField<FsAccessOption> options, FsNodeName name);

    OutputSocket<? extends Entry> output(BitField<FsAccessOption> options, FsNodeName name, Optional<? extends Entry> template);

    void make(BitField<FsAccessOption> options, FsNodeName name, Type type, Optional<? extends Entry> template) throws IOException;

    void unlink(BitField<FsAccessOption> options, FsNodeName name) throws IOException;

    void sync(BitField<FsSyncOption> options) throws FsSyncException;
}
