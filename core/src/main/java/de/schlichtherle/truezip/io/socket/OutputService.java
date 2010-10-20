/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.entry.CommonEntryContainer;
import de.schlichtherle.truezip.io.entry.CommonEntry;

/**
 * A container and output socket factory for common entries.
 * <p>
 * All methods of this interface must reflect all entries, including those
 * which have only been partially written yet, i.e. which have not already
 * received a call to their {@code close()} method.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <CE> The type of the common entries.
 * @see     InputService
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface OutputService<CE extends CommonEntry>
extends CommonEntryContainer<CE> {

    /**
     * Returns a non-{@code null} output socket for write access to the given
     * common entry.
     *
     * @param  entry the non-{@code null} local target.
     * @throws NullPointerException if {@code target} is {@code null}.
     * @return A non-{@code null} output socket for writing to the local
     *         target.
     */
    OutputSocket<? extends CE> getOutputSocket(CE entry);
}
