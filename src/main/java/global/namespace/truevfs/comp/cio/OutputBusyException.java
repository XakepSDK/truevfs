/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import javax.annotation.Nullable;

/**
 * Indicates that an entity (an entry or container) could not get written because the entity or its container is busy.
 * This exception is recoverable, meaning it should be possible to repeat the operation successfully as soon as the
 * entity or its container is not busy anymore and no other exceptional conditions apply.
 *
 * @see    InputBusyException
 * @author Christian Schlichtherle
 */
public class OutputBusyException extends BusyException {

    private static final long serialVersionUID = 0;

    public OutputBusyException(@Nullable String message) {
        super(message);
    }

    public OutputBusyException(@Nullable Throwable cause) {
        super(cause);
    }
}
