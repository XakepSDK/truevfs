/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.sfx;

import global.namespace.truevfs.comp.cio.InputContainer;
import global.namespace.truevfs.comp.cio.OutputContainer;
import global.namespace.truevfs.comp.util.BitField;
import global.namespace.truevfs.comp.zipdriver.ZipDriver;
import global.namespace.truevfs.comp.zipdriver.ZipDriverEntry;
import global.namespace.truevfs.kernel.api.*;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * An archive driver which reads Self Executable (SFX/EXE) ZIP files,
 * but doesn't support to create or update them because this would spoil the
 * SFX code in its preamble.
 * <p>
 * Subclasses must be thread-safe.
 * 
 * @author Christian Schlichtherle
 */
public class ReadOnlySfxDriver extends ZipDriver {

    /**
     * The character set for entry names and comments, which is the default
     * character set.
     */
    public static final Charset SFX_CHARSET = Charset.defaultCharset();

    /**
     * {@inheritDoc}
     * 
     * @return {@link #SFX_CHARSET}.
     */
    @Override
    public Charset getCharset() {
        return SFX_CHARSET;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipDriver}
     * returns {@code true}.
     * 
     * @return {@code true}
     */
    @Override
    public final boolean getPreambled() {
        return true;
    }

    @Override
    public final OutputContainer<ZipDriverEntry> newOutput(
            FsModel model,
            BitField<FsAccessOption> options,
            FsController controller,
            FsNodeName name,
            @CheckForNull InputContainer<ZipDriverEntry> input)
    throws IOException {
        throw new FsReadOnlyFileSystemException(model.getMountPoint());
    }
}
