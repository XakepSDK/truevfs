/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.util.BitField;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Indicates that a file system controller needs to get
 * {@linkplain FsController#sync(BitField) synced} before the operation can
 * get retried.
 *
 * @since  TrueZIP 7.3
 * @see    FsSyncController
 * @author Christian Schlichtherle
 */
@Immutable
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
public final class FsNeedsSyncException extends FsControllerException {

    private static final @Nullable FsNeedsSyncException
            SINGLETON = TRACEABLE ? null : new FsNeedsSyncException();

    public static FsNeedsSyncException get( final FsModel model,
                                            final FsEntryName name,
                                            final @CheckForNull Access access) {
        return TRACEABLE    ? new FsNeedsSyncException(model,
                                (null == access ? "touch" : access.toString())
                                    + ' ' + name,
                                null)
                            : SINGLETON;
    }

    public static FsNeedsSyncException get( final FsModel model,
                                            final String name,
                                            final Throwable cause) {
        return TRACEABLE    ? new FsNeedsSyncException(model, name, cause)
                            : SINGLETON;
    }

    private FsNeedsSyncException() { }

    private FsNeedsSyncException(   final FsModel model,
                                    final String message,
                                    final @CheckForNull Throwable cause) {
        super(model, message, cause);
    }
}
