/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.http;

import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.Entry.Access;
import net.java.truecommons.cio.Entry.Type;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.IoBufferPool;
import net.java.truecommons.cio.OutputSocket;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.*;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.util.Optional;

import static net.java.truecommons.cio.Entry.Access.READ;
import static net.java.truecommons.cio.Entry.Type.FILE;

/**
 * A file system controller for the HTTP(S) schemes.
 *
 * @author Christian Schlichtherle
 */
public class HttpController extends FsAbstractController {

    private static final BitField<Access> READ_ONLY = BitField.of(READ);

    private final HttpDriver driver;

    HttpController(final HttpDriver driver, final FsModel model) {
        super(model);
        if (model.getParent().isPresent()) {
            throw new IllegalArgumentException();
        }
        assert null != driver;
        this.driver = driver;
    }

    final IoBufferPool getPool() {
        return driver.getPool();
    }

    final HttpResponse executeHead(HttpNode entry) throws IOException {
        return driver.executeHead(entry);
    }

    final HttpResponse executeGet(HttpNode entry) throws IOException {
        return driver.executeGet(entry);
    }

    protected HttpNode newEntry(FsNodeName name) {
        return new HttpNode(this, name);
    }

    final FsNodePath resolve(FsNodeName name) {
        return getMountPoint().resolve(name);
    }

    @Override
    public Optional<? extends FsController> getParent() {
        return Optional.empty();
    }

    @Override
    public Optional<? extends HttpNode> node(
            final BitField<FsAccessOption> options,
            final FsNodeName name
    ) throws IOException {
        HttpNode entry = newEntry(name);
        return entry.isType(FILE) ? Optional.of(entry) : Optional.empty();
    }

    @Override
    public void checkAccess(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final BitField<Access> types
    ) throws IOException {
        if (!types.isEmpty() && !READ_ONLY.equals(types)) {
            throw new FsReadOnlyFileSystemException(getMountPoint());
        }
        executeHead(newEntry(name));
    }

    @Override
    public void setReadOnly(BitField<FsAccessOption> options, FsNodeName name)
            throws IOException {
    }

    @Override
    public boolean setTime(
            BitField<FsAccessOption> options, FsNodeName name, BitField<Access> types, long value)
            throws IOException {
        throw new FsReadOnlyFileSystemException(getMountPoint());
    }

    @Override
    public InputSocket<?> input(
            BitField<FsAccessOption> options,
            FsNodeName name) {
        return newEntry(name).input(options);
    }

    @Override
    public OutputSocket<?> output(
            BitField<FsAccessOption> options,
            FsNodeName name,
            Optional<? extends Entry> template) {
        return newEntry(name).output(options, template);
    }

    @Override
    public void make(
            BitField<FsAccessOption> options,
            FsNodeName name,
            Type type,
            Optional<? extends Entry> template
    ) throws IOException {
        throw new FsReadOnlyFileSystemException(getMountPoint());
    }

    @Override
    public void unlink(
            BitField<FsAccessOption> options,
            FsNodeName name
    ) throws IOException {
        throw new FsReadOnlyFileSystemException(getMountPoint());
    }

    @Override
    public void sync(BitField<FsSyncOption> options) {
    }
}
