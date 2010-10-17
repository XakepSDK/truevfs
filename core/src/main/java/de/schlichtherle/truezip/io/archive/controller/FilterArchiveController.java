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
package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.io.IOException;
import javax.swing.Icon;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
class      FilterArchiveController<AE extends ArchiveEntry>
implements ArchiveController      <AE> {

    private final ArchiveController<? extends AE> controller;

    /**
     * Constructs a new filter archive controller.
     *
     * @param controller the non-{@code null} archive controller.
     */
    FilterArchiveController(final ArchiveController<? extends AE> controller) {
        assert null != controller;
        this.controller = controller;
    }

    final ArchiveController<? extends AE> getController() {
        return controller;
    }

    @Override
    public final ArchiveModel getModel() {
        return getController().getModel();
    }

    @Override
    public final boolean isTouched() {
        return getModel().isTouched();
    }

    @Override
    public <E extends IOException>
    void sync(ExceptionBuilder<? super SyncException, E> builder, BitField<SyncOption> options)
    throws E, ArchiveControllerException {
        getController().sync(builder, options);
    }

    @Override
    public Icon getOpenIcon()
    throws ArchiveControllerException {
        return getController().getOpenIcon();
    }

    @Override
    public Icon getClosedIcon()
    throws ArchiveControllerException {
        return getController().getClosedIcon();
    }

    @Override
    public boolean isReadOnly()
    throws ArchiveControllerException {
        return getController().isReadOnly();
    }

    @Override
    public Entry<? extends AE> getEntry(String path)
    throws ArchiveControllerException {
        return getController().getEntry(path);
    }

    @Override
    public boolean isReadable(String path)
    throws ArchiveControllerException {
        return getController().isReadable(path);
    }

    @Override
    public boolean isWritable(String path)
    throws ArchiveControllerException {
        return getController().isWritable(path);
    }

    @Override
    public void setReadOnly(String path)
    throws IOException {
        getController().setReadOnly(path);
    }

    @Override
    public boolean setTime(String path, BitField<Access> types, long value)
    throws IOException {
        return getController().setTime(path, types, value);
    }

    @Override
    public InputSocket<? extends AE> getInputSocket(
            final String path,
            final BitField<InputOption> options)
    throws IOException {
        return getController().getInputSocket(path, options);
    }

    @Override
    public OutputSocket<? extends AE> getOutputSocket(
            String path,
            CommonEntry template,
            BitField<OutputOption> options)
    throws IOException {
        return getController().getOutputSocket(path, template, options);
    }

    @Override
    public boolean mknod(   String path,
                            Type type,
                            CommonEntry template,
                            BitField<OutputOption> options)
    throws IOException {
        return getController().mknod(path, type, template, options);
    }

    @Override
    public void unlink(String path)
    throws IOException {
        getController().unlink(path);
    }
}
