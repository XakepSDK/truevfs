/*
 * Copyright 2007-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.output;

import de.schlichtherle.truezip.io.archive.entry.FilterArchiveEntryContainer;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.input.FilterArchiveInput;
import java.io.IOException;

/**
 * Decorates an {@code ArchiveOutput}.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @param <AE> The type of the archive entries.
 * @see FilterArchiveInput
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FilterArchiveOutput<
        AE extends ArchiveEntry,
        AO extends ArchiveOutput<AE>>
extends FilterArchiveEntryContainer<AE, AO>
implements ArchiveOutput<AE> {

    public FilterArchiveOutput(final AO target) {
        super(target);
    }

    @Override
    public ArchiveOutputSocket<AE> getOutputSocket(AE entry)
    throws IOException {
        return target.getOutputSocket(entry);
    }

    @Override
    public void close() throws IOException {
        target.close();
    }
}
