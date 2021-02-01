/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import global.namespace.truevfs.comp.shed.ReadWriteLockAspect;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A mixin which provides some features of its reentrant read/write {@link #getLock()}.
 *
 * @author Christian Schlichtherle
 */
interface ReentrantReadWriteLockAspect extends ReadWriteLockAspect<ReentrantReadWriteLock> {

    /**
     * Returns {@code true} if and only if the read lock is held by the current thread.
     * This method should only get used for assertions, not for lock control.
     *
     * @return {@code true} if and only if the read lock is held by the current thread.
     */
    default boolean isReadLockedByCurrentThread() {
        return 0 < getLock().getReadHoldCount();
    }

    /**
     * Returns {@code true} if and only if the write lock is held by the current thread.
     * This method should only get used for assertions, not for lock control.
     *
     * @return {@code true} if and only if the write lock is held by the current thread.
     */
    default boolean isWriteLockedByCurrentThread() {
        return getLock().isWriteLockedByCurrentThread();
    }

    /**
     * Checks that the write lock is held by the current thread.
     * Use this method for lock control.
     *
     * @throws NeedsWriteLockException if the {@link #getWriteLock()} is not held by the current thread.
     * @see #isWriteLockedByCurrentThread
     */
    default void checkWriteLockedByCurrentThread() {
        if (!isWriteLockedByCurrentThread()) {
            throw NeedsWriteLockException.apply();
        }
    }
}
