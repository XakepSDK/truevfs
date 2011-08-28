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
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.io.InputClosedException;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import net.jcip.annotations.NotThreadSafe;

/**
 * Decorates another input shop in order to disconnect any entry resources
 * when this input shop gets closed.
 *
 * @see     DisconnectingOutputShop
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public final class DisconnectingInputShop<E extends Entry>
extends DecoratingInputShop<E, InputShop<E>> {

    private boolean closed;

    /**
     * Constructs a disconnecting input shop.
     *
     * @param input the shop to decorate.
     */
    public DisconnectingInputShop(InputShop<E> input) {
        super(input);
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        try {
            delegate.close();
        } finally {
            closed = true;
        }
    }

    private void assertNotClosed() throws IOException {
        if (closed)
            throw new InputClosedException();
    }

    @Override
    public InputSocket<? extends E> getInputSocket(final String name) {
        if (null == name)
            throw new NullPointerException();

        class Input extends DecoratingInputSocket<E> {
            Input() {
                super(DisconnectingInputShop.super.getInputSocket(name));
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                assertNotClosed();
                return new DisconnectableReadOnlyFile(
                        getBoundSocket().newReadOnlyFile());
            }

            // TODO: Implement newSeekableByteChannel()

            @Override
            public InputStream newInputStream() throws IOException {
                assertNotClosed();
                return new DisconnectableInputStream(
                        getBoundSocket().newInputStream());
            }
        } // Input

        return new Input();
    }

    private final class DisconnectableReadOnlyFile
    extends DecoratingReadOnlyFile {
        DisconnectableReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public long length() throws IOException {
            assertNotClosed();
            return delegate.length();
        }

        @Override
        public long getFilePointer() throws IOException {
            assertNotClosed();
            return delegate.getFilePointer();
        }

        @Override
        public void seek(long pos) throws IOException {
            assertNotClosed();
            delegate.seek(pos);
        }

        @Override
        public int read() throws IOException {
            assertNotClosed();
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            assertNotClosed();
            return delegate.read(b, off, len);
        }

        /*@Override
        public void readFully(byte[] b, int off, int len) throws IOException {
            assertNotClosed();
            delegate.readFully(b, off, len);
        }*/

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            delegate.close();
        }
    } // DisconnectableReadOnlyFile

    private final class DisconnectableInputStream
    extends DecoratingInputStream {
        DisconnectableInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            assertNotClosed();
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            assertNotClosed();
            return delegate.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            assertNotClosed();
            return delegate.skip(n);
        }

        @Override
        public int available() throws IOException {
            assertNotClosed();
            return delegate.available();
        }

        @Override
        public void mark(int readlimit) {
            if (!closed)
                delegate.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            assertNotClosed();
            delegate.reset();
        }

        @Override
        public boolean markSupported() {
            return !closed && delegate.markSupported();
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            delegate.close();
        }
    } // DisconnectableInputStream
}
