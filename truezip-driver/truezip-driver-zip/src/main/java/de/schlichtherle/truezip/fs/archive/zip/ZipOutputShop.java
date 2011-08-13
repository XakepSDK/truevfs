/*
 * Copyright (C) 2009-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsModel;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import de.schlichtherle.truezip.fs.archive.FsMultiplexedOutputShop;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.JointIterator;
import de.schlichtherle.truezip.zip.RawZipOutputStream;
import de.schlichtherle.truezip.zip.ZipCryptoParameters;
import static de.schlichtherle.truezip.zip.ZipEntry.DEFLATED;
import static de.schlichtherle.truezip.zip.ZipEntry.STORED;
import static de.schlichtherle.truezip.zip.ZipEntry.UNKNOWN;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Iterator;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import net.jcip.annotations.NotThreadSafe;

/**
 * An output shop for writing ZIP archive files.
 * This output shop can only write one entry at a time.
 * Archive drivers may wrap this class in a
 * {@link FsMultiplexedOutputShop} to overcome this limitation.
 * 
 * @see     ZipInputShop
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public final class ZipOutputShop
extends RawZipOutputStream<ZipArchiveEntry>
implements OutputShop<ZipArchiveEntry> {

    private final ZipDriver driver;
    private final FsModel model;
    private @CheckForNull IOPool.Entry<?> postamble;
    private @CheckForNull ZipArchiveEntry tempEntry;

    public ZipOutputShop(   final ZipDriver driver,
                            final FsModel model,
                            final OutputStream out,
                            final @CheckForNull ZipInputShop source)
    throws IOException {
        super(  out,
                null != source && source.isAppendee() ? source : null,
                driver.getCharset());
        if (null == model)
            throw new NullPointerException();
        super.setMethod(driver.getMethod());
        super.setLevel(driver.getLevel());
        this.driver = driver;
        this.model = model;
        if (null != source) {
            if (!source.isAppendee()) {
                // Retain comment and preamble of input ZIP archive.
                super.setComment(source.getComment());
                if (0 < source.getPreambleLength()) {
                    final InputStream in = source.getPreambleInputStream();
                    try {
                        Streams.cat(in,
                                source.offsetsConsiderPreamble() ? this : out);
                    } finally {
                        in.close();
                    }
                }
            }
            // Retain postamble of input ZIP archive.
            if (0 < source.getPostambleLength()) {
                this.postamble = getPool().allocate();
                Streams.copy(   source.getPostambleInputStream(),
                                postamble.getOutputSocket().newOutputStream());
            }
        }
    }

    /**
     * Returns the file system model provided to the constructor.
     * 
     * @return The file system model provided to the constructor.
     * @since  TrueZIP 7.3
     */
    public FsModel getModel() {
        return model;
    }

    private IOPool<?> getPool() {
        return driver.getPool();
    }

    @Override
    public Charset getRawCharset() {
        return super.getRawCharset();
    }

    @Override
    protected ZipCryptoParameters getCryptoParameters() {
        return driver.zipCryptoParameters(this);
    }

    @Override
    public int getSize() {
        return super.size() + (null != this.tempEntry ? 1 : 0);
    }

    @Override
    public Iterator<ZipArchiveEntry> iterator() {
        final ZipArchiveEntry tempEntry = this.tempEntry;
        if (null == tempEntry)
            return super.iterator();
        return new JointIterator<ZipArchiveEntry>(
                super.iterator(),
                Collections.singletonList(tempEntry).iterator());
    }

    @Override
    public @CheckForNull ZipArchiveEntry getEntry(final String name) {
        ZipArchiveEntry entry = super.getEntry(name);
        if (null != entry)
            return entry;
        entry = tempEntry;
        return null != entry && name.equals(entry.getName()) ? entry : null;
    }

    @Override
    public OutputSocket<ZipArchiveEntry> getOutputSocket(final ZipArchiveEntry lt) { // local target
        if (null == lt)
            throw new NullPointerException();

        class Output extends OutputSocket<ZipArchiveEntry> {
            @Override
            public ZipArchiveEntry getLocalTarget() {
                return lt;
            }

            @Override
            public OutputStream newOutputStream()
            throws IOException {
                if (isBusy())
                    throw new OutputBusyException(lt.getName());
                final Entry pt;
                final long size;
                if (lt.isDirectory()) {
                    lt.setMethod(STORED);
                    lt.setCrc(0);
                    lt.setCompressedSize(0);
                    lt.setSize(0);
                    return new EntryOutputStream(lt, true);
                } else if (null != (pt = getPeerTarget())
                        && UNKNOWN != (size = pt.getSize(DATA))) {
                    lt.setSize(size);
                    if (pt instanceof ZipArchiveEntry) {
                        // Set up entry attributes for Direct Data Copying (DDC).
                        // A preset method in the entry takes priority.
                        // The ZIP.RAES drivers use this feature to enforce
                        // deflation for enhanced authentication security.
                        final ZipArchiveEntry zpt = (ZipArchiveEntry) pt;
                        lt.setPlatform(zpt.getPlatform());
                        lt.setEncrypted(zpt.isEncrypted());
                        //if (entry.getMethod() == UNKNOWN)
                            lt.setMethod(zpt.getMethod());
                        lt.setCrc(zpt.getCrc());
                        //if (entry.getMethod() == zipPeer.getMethod())
                            lt.setCompressedSize(zpt.getCompressedSize());
                        lt.setExtra(zpt.getExtra());
                        return new EntryOutputStream(lt,
                                false /*lt.isEncrypted() || zpt.isEncrypted()*/); // FIXME!
                    }
                }
                switch (lt.getMethod()) {
                    case UNKNOWN:
                        lt.setMethod(DEFLATED);
                        break;
                    case STORED:
                        if (       UNKNOWN == lt.getCrc()
                                || UNKNOWN == lt.getCompressedSize()
                                || UNKNOWN == lt.getSize())
                            return new BufferedEntryOutputStream(
                                    getPool().allocate(), lt);
                        break;
                    case DEFLATED:
                        break;
                    default:
                        assert false : "unsupported method";
                }
                return new EntryOutputStream(lt, true);
            }
        } // Output

        return new Output();
    }

    /**
     * Returns whether this output archive is busy writing an archive entry
     * or not.
     */
    @Override
    public final boolean isBusy() {
        return super.isBusy() || null != this.tempEntry;
    }

    /**
     * Retains the postamble of the source source ZIP file, if any.
     */
    @Override
    public void close() throws IOException {
        try {
            final IOPool.Entry<?> postamble = this.postamble;
            if (null != postamble) {
                this.postamble = null;
                try {
                    final InputSocket<?> input = postamble.getInputSocket();
                    final InputStream in = input.newInputStream();
                    try {
                    // Second, if the output ZIP compatible file differs in length from
                    // the input ZIP compatible file pad the output to the next four byte
                    // boundary before appending the postamble.
                    // This might be required for self extracting files on some platforms
                    // (e.g. Wintel).
                    final long ol = length();
                    final long ipl = input.getLocalTarget().getSize(DATA);
                    if ((ol + ipl) % 4 != 0)
                        write(new byte[4 - (int) (ol % 4)]);

                        Streams.cat(in, this);
                    } finally {
                        in.close();
                    }
                } finally {
                    postamble.release();
                }
            }
        } finally {
            super.close();
        }
    }

    /**
     * This entry output stream writes directly to this output shop.
     * It can only be used if this output shop is not currently busy with
     * writing another entry and the entry holds enough information to write
     * the entry header.
     * These preconditions are checked by
     * {@link #getOutputSocket(ZipArchiveEntry)}.
     */
    private final class EntryOutputStream extends DecoratingOutputStream {
        EntryOutputStream(ZipArchiveEntry entry, boolean process)
        throws IOException {
            super(ZipOutputShop.this);
            putNextEntry(entry, process);
        }

        @Override
        public void close() throws IOException {
            closeEntry();
        }
    } // EntryOutputStream

    /**
     * This entry output stream writes the ZIP archive entry to an
     * {@link de.schlichtherle.truezip.socket.IOPool.Entry I/O pool entry}.
     * When the stream gets closed, the I/O pool entry is then copied to this
     * output shop and finally deleted.
     */
    private final class BufferedEntryOutputStream extends CheckedOutputStream {
        final IOPool.Entry<?> temp;
        boolean closed;

        BufferedEntryOutputStream(
                final IOPool.Entry<?> temp,
                final ZipArchiveEntry entry)
        throws IOException {
            super(temp.getOutputSocket().newOutputStream(), new CRC32());
            assert STORED == entry.getMethod();
            this.temp = temp;
            ZipOutputShop.this.tempEntry = entry;
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            closed = true;
            try {
                try {
                    super.close();
                } finally {
                    final long length = temp.getSize(DATA);
                    final ZipArchiveEntry tempEntry = ZipOutputShop.this.tempEntry;
                    assert null != tempEntry;
                    assert STORED == tempEntry.getMethod();
                    tempEntry.setCrc(getChecksum().getValue());
                    tempEntry.setCompressedSize(length);
                    tempEntry.setSize(length);
                    store();
                }
            } finally {
                ZipOutputShop.this.tempEntry = null;
            }
        }

        void store() throws IOException {
            try {
                final InputStream in = temp.getInputSocket().newInputStream();
                try {
                    final ZipArchiveEntry tempEntry = ZipOutputShop.this.tempEntry;
                    assert null != tempEntry;
                    putNextEntry(tempEntry);
                    try {
                        Streams.cat(in, ZipOutputShop.this);
                    } finally {
                        closeEntry();
                    }
                } finally {
                    in.close();
                }
            } finally {
                temp.release();
            }
        }
    } // BufferedEntryOutputStream
}
