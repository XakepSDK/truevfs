/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.crypto;

import de.schlichtherle.truezip.io.DecoratingOutputStream;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;

/**
 * Similar to {@code javax.crypto.CipherOutputStream} with some exceptions:
 * <ul>
 * <li>This implementation is based on Bouncy Castle's lightweight crypto API
 *     and uses a {@link BufferedBlockCipher} for ciphering.
 * <li>The {@link #cipher} used for encryption or decryption is accessible to
 *     subclasses.
 * <li>The {@code flush()} method just flushes the underlying output stream
 *     and has no effect on the cipher.
 * <li>A {@link #finish()} method has been added to allow finishing the output
 *     (probably producing padding bytes) without closing the output.
 *     This could be used in a subclass to produce a trailer with additional
 *     information about the ciphered data (e.g. a MAC).
 * </ul>
 *
 * @see     CipherReadOnlyFile
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class CipherOutputStream extends DecoratingOutputStream {

    /** The buffered block cipher used for preprocessing the output. */
    protected @Nullable BufferedBlockCipher cipher;

    /**
     * The cipher output buffer used for preprocessing the output
     * to the decorated stream.
     * This buffer is autosized to the largest buffer written to this stream.
     */
    private byte[] cipherOut = new byte[0];

    /** Whether this stream has been closed or not. */
    private boolean closed;

    /**
     * Creates a new cipher output stream.
     * Please note that unlike {@code javax.crypto.CipherOutputStream},
     * the cipher does not need to be initialized before calling this
     * constructor.
     * However, the cipher must be initialized before anything is actually
     * written to this stream or before this stream is closed.
     *
     * @param out The output stream to write the encrypted or decrypted data to.
     *        Maybe {@code null} for subsequent initialization by a sub-class.
     * @param cipher The cipher to use for encryption or decryption.
     *        Maybe {@code null} for subsequent initialization by a sub-class.
     */
    public CipherOutputStream(  @CheckForNull OutputStream out,
                                final @CheckForNull BufferedBlockCipher cipher) {
        super(out);
        this.cipher = cipher;
    }

    /**
     * Asserts that this cipher output stream is in open state, which requires
     * that {@link #cipher} is not {@code null}.
     *
     * @throws IOException If the preconditions do not hold.
     */
    private void assertOpen() throws IOException {
        if (null == cipher)
            throw new IOException("cipher output stream is not in open state");
    }

    /**
     * Ciphers and writes the given byte to the underlying output stream.
     *
     * @param  b The byte to cipher and write.
     * @throws IOException If out or cipher aren't properly initialized,
     *         the stream has been closed or an I/O error occured.
     */
    @Override
    public void write(final int b)
    throws IOException {
        assertOpen();

        int cipherLen = cipher.getUpdateOutputSize(1);
        byte[] cipherOut = this.cipherOut;
        if (cipherLen > cipherOut.length)
            cipherOut = this.cipherOut = new byte[cipherLen];
        cipherLen = cipher.processByte((byte) b, cipherOut, 0);
        if (cipherLen > 0)
            delegate.write(cipherOut, 0, cipherLen);
    }

    /**
     * Ciphers and writes the contents of the given byte array to the
     * underlying output stream.
     *
     * @param  buf The buffer holding the data to cipher and write.
     * @param  off The start offset of the data in the buffer.
     * @param  len The number of bytes to cipher and write.
     * @throws IOException If out or cipher aren't properly initialized,
     *         the stream has been closed or an I/O error occured.
     */
    @Override
    public void write(final byte[] buf, final int off, final int len)
    throws IOException {
        assertOpen();

        int cipherLen = cipher.getUpdateOutputSize(len);
        byte[] cipherOut = this.cipherOut;
        if (cipherLen > cipherOut.length)
            cipherOut = this.cipherOut = new byte[cipherLen];
        cipherLen = cipher.processBytes(buf, off, len, cipherOut, 0);
        delegate.write(cipherOut, 0, cipherLen);
    }

    /**
     * Finishes this stream and resets it to it's initial state.
     * Calling this method causes all remaining buffered bytes to be written,
     * padding to be added if necessary and the underlying output stream to
     * get flushed.
     * <p>
     * Please note that subsequent calls to any write operations after this
     * method may cause an error in the output data if padding is used!
     *
     * @throws IOException If out or cipher aren't properly initialized,
     *         the stream has been closed, an I/O error occured the cipher
     *         text is invalid, i.e. required padding information is missing.
     */
    protected void finish() throws IOException {
        assertOpen();

        int cipherLen = cipher.getOutputSize(0);
        byte[] cipherOut = this.cipherOut;
        if (cipherLen > cipherOut.length)
            cipherOut = this.cipherOut = new byte[cipherLen];
        try {
            cipherLen = cipher.doFinal(cipherOut, 0);
        } catch (InvalidCipherTextException ex) {
            throw new IOException(ex);
        }
        delegate.write(cipherOut, 0, cipherLen);
        delegate.flush();
        //outBuf = new byte[0];
    }

    /**
     * Closes this output stream and releases any resources associated with it.
     * This method calls {@link #finish()} and then closes and nullifies
     * the underlying output stream {@link #delegate} and the cipher
     * {@link #cipher}.
     *
     * @throws IOException On any I/O error.
     */
    @Override
    public void close() throws IOException {
        // Order is important here!
        if (closed)
            return;
        closed = true;
        try {
            finish();
        } finally {
            cipher = null;
            delegate.close();
        }
    }
}
