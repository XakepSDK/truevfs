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

package de.schlichtherle.truezip.io.archive;

/**
 * Describes general properties of any archive.
 * A single instance of this interface is created for every
 * canonical path name representation of an archive file.
 * <p>
 * <b>Warning:</b> This class is <em>not</em> intended for public use!
 * Client applications should never implement this interface because more
 * features may be added in future.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface Archive {

    /**
     * Returns the <em>canonical</em> path name of the archive file.
     * A canonical path is both absolute and unique within the virtual file
     * system.
     * The precise definition depends on the platform, but all elements in
     * a canonical path are separated by {@link java.io.File#separator}s.
     * <p>
     * This property may be used to determine some archive file specific
     * parameters, such as passwords or similar.
     * However, implementations must not assume that the file denoted by the
     * path actually exists as a file in the real file system!
     *
     * @return A string representing the canonical path of this archive
     *         - never {@code null}.
     * @see #getEnclArchive
     */
    String getCanonicalPath();

    /**
     * @return The enclosing archive or {@code null} if this archive is
     *         not enclosed in another archive
     */
    Archive getEnclArchive();
}
