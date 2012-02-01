/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.crypto.raes;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that no suitable cryptographic parameters have been
 * provided or something is wrong with these parameters.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class RaesParametersException extends RaesException {
    private static final long serialVersionUID = 1605398165986459281L;

    /**
     * Constructs a RAES parameters exception with
     * the given detail message.
     *
     * @param msg the detail message.
     */
    public RaesParametersException(@CheckForNull String msg) {
        super(msg);
    }

    /**
     * Constructs a RAES parameters exception with
     * the given cause.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public RaesParametersException(@CheckForNull Throwable cause) {
        super(cause);
    }
}
