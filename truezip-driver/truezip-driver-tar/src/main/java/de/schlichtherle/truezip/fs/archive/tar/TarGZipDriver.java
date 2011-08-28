/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import java.util.zip.Deflater;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import net.jcip.annotations.Immutable;

/**
 * An archive driver which builds GZIP compressed TAR files (TAR.GZIP).
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public class TarGZipDriver extends TarDriver {

    public TarGZipDriver(IOPoolProvider provider) {
        super(provider);
    }

    public static final int BUFFER_SIZE = 4096;

    /**
     * Returns the size of the I/O buffer.
     * <p>
     * The implementation in the class {@link TarGZipDriver} returns
     * {@link #BUFFER_SIZE}.
     *
     * @return The size of the I/O buffer.
     */
    public int getBufferSize() {
        return BUFFER_SIZE;
    }

    /**
     * Returns the compression level to use when writing a GZIP output stream.
     * <p>
     * The implementation in the class {@link TarBZip2Driver} returns
     * {@link Deflater#BEST_COMPRESSION}.
     * 
     * @return The compression level to use when writing a GZIP output stream.
     */
    public final int getLevel() {
        return Deflater.BEST_COMPRESSION;
    }

    /**
     * Sets {@link FsOutputOption#STORE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    public OutputSocket<?> getOutputSocket( FsController<?> controller,
                                            FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            @CheckForNull Entry template) {
        return controller.getOutputSocket(name, options.set(STORE), template);
    }

    @Override
    protected TarInputShop newTarInputShop(FsModel model, InputStream in)
    throws IOException {
        return super.newTarInputShop(model,
                new GZIPInputStream(in, getBufferSize()));
    }

    @Override
    protected TarOutputShop newTarOutputShop(
            final FsModel model,
            final OutputStream out,
            final TarInputShop source)
    throws IOException {
        return super.newTarOutputShop(model,
                new GZIPOutputStream(out, getBufferSize(), getLevel()),
                source);
    }

    /** Extends its super class to set the deflater level. */
    private static class GZIPOutputStream
    extends java.util.zip.GZIPOutputStream {
        GZIPOutputStream(OutputStream out, int size, int level)
        throws IOException {
            super(out, size);
            def.setLevel(level);
        }
    }
}
