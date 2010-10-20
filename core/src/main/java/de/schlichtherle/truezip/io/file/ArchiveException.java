/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.file;

import de.schlichtherle.truezip.io.ChainableIOException;

/**
 * Represents a chain of exceptions thrown by the {@link File#umount} and
 * {@link File#update} methods to indicate an exceptional condition which
 * <em>does</em> incur loss of data.
 * 
 * <p>Both methods catch any exceptions occuring throughout their processing
 * and store them in an exception chain until all archive files have been
 * synchronized.
 * Finally, if the exception chain is not empty, it's reordered and thrown
 * so that if its head is an instance of {@code ArchiveWarningException},
 * only instances of this class or its subclasses are in the chain, but no
 * instances of {@code ArchiveException} or its subclasses (except
 * {@code ArchiveWarningException}, of course).
 *
 * <p>This enables client applications to do a simple case distinction with a
 * try-catch-block like this to react selectively:</p>
 * <pre>{@code 
 * try {
 *     File.umount();
 * } catch (ArchiveWarningException warning) {
 *     // Only warnings have occured and no data has been lost - ignore this.
 * } catch (ArchiveException error) {
 *     // Some data has been lost - panic!
 *     error.printStackTrace();
 * }
 * }</pre>
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.0
 */
public class ArchiveException extends ChainableIOException {

    private static final long serialVersionUID = 1L;

    ArchiveException(String message) {
        super(message);
    }

    ArchiveException(String message, int priority) {
        super(message, priority);
    }
}
