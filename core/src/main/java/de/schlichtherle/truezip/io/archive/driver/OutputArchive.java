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

package de.schlichtherle.truezip.io.archive.driver;

import de.schlichtherle.truezip.io.archive.controller.OutputArchiveMetaData;
import de.schlichtherle.truezip.io.socket.OutputStreamSocketProvider;
import java.io.Closeable;
import java.io.FileNotFoundException;

/**
 * A container which supports writing archive entries to an arbitrary output
 * destination.
 * <p>
 * All methods of this interface must reflect all entries, including those
 * which have just been partially written yet, i.e. which have not already
 * received a call to their {@code close()} method.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @see     InputArchive
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface OutputArchive<AE extends ArchiveEntry>
extends ArchiveEntryContainer<AE>,
        OutputStreamSocketProvider<AE, ArchiveEntry>,
        Closeable {

    /**
     * {@inheritDoc}
     * <p>
     * It is an error to write an archive entry header or adding the archive
     * entry merely upon the call to this method.
     *
     * @param entry a non-{@code null} archive entry.
     */
    @Override
    ArchiveOutputStreamSocket<? extends AE> getOutputStreamSocket(AE entry)
    throws FileNotFoundException;

    /**
     * Returns the meta data for this output archive.
     * The default value is {@code null}.
     *
     * @deprecated
     */
    OutputArchiveMetaData getMetaData();

    /**
     * Sets the meta data for this output archive.
     *
     * @param metaData The meta data - may not be {@code null}.
     * @deprecated
     */
    void setMetaData(OutputArchiveMetaData metaData);
}
