/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.crypto.raes;

import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * Thrown if there is an issue when reading or writing a RAES file which is
 * specific to the RAES file format.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class RaesException extends IOException {
    private static final long serialVersionUID = 8564203786508562247L;

    /**
     * Constructs a RAES exception with
     * no detail message.
     */
    public RaesException() {
    }

    /**
     * Constructs a RAES exception with
     * the given detail message.
     *
     * @param msg the detail message.
     */
    public RaesException(String msg) {
        super(msg);
    }

    /**
     * Constructs a RAES exception with
     * the given detail message and cause.
     *
     * @param msg the detail message.
     * @param cause the cause for this exception to be thrown.
     */
    public RaesException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Constructs a RAES exception with
     * the given cause.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public RaesException(Throwable cause) {
        super(cause);
    }
}
