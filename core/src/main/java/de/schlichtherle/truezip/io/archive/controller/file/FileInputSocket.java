/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.archive.controller.file;

import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.SimpleReadOnlyFile;
import de.schlichtherle.truezip.io.socket.input.CommonInputSocket;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @see FileOutputSocket
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class FileInputSocket extends CommonInputSocket<FileEntry> {
    private final FileEntry entry;

    public FileInputSocket(final FileEntry entry) {
        this.entry = entry;
        if (null == entry)
            throw new NullPointerException();
    }

    @Override
    public FileEntry getTarget() {
        return entry;
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return new FileInputStream(entry);
    }

    @Override
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        return new SimpleReadOnlyFile(entry);
    }
}
