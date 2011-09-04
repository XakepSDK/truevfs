/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.fs.FsSyncOption.*;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import de.schlichtherle.truezip.util.JSE7;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;

/**
 * Accounts input and output resources returned by its decorated controller.
 * 
 * @see     FsResourceAccountant
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsResourceController
extends FsDecoratingConcurrentModelController<
        FsController<? extends FsConcurrentModel>> {

    private static final AccountingSocketFactory
            ACCOUNTING_SOCKET_FACTORY = JSE7.AVAILABLE
                ? AccountingSocketFactory.NIO2
                : AccountingSocketFactory.OIO;

    private @CheckForNull FsResourceAccountant accountant;

    /**
     * Constructs a new file system resource controller.
     *
     * @param controller the decorated concurrent file system controller.
     */
    public FsResourceController(
            FsController<? extends FsConcurrentModel> controller) {
        super(controller);
    }

    private FsResourceAccountant getAccountant() {
        final FsResourceAccountant accountant = this.accountant;
        return null != accountant
                ? accountant
                : (this.accountant = new FsResourceAccountant(writeLock()));
    }

    @Override
    public InputSocket<?> getInputSocket(   FsEntryName name,
                                            BitField<FsInputOption> options) {
        return ACCOUNTING_SOCKET_FACTORY.newInputSocket(this,
                delegate.getInputSocket(name, options));
    }

    @Override
    public OutputSocket<?> getOutputSocket( FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            Entry template) {
        return ACCOUNTING_SOCKET_FACTORY.newOutputSocket(this,
                delegate.getOutputSocket(name, options, template));
    }

    @Override
    public <X extends IOException> void sync(
            final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        assert isWriteLockedByCurrentThread();
        waitOtherThreads(options, handler);
        closeAllResources(handler);
        delegate.sync(options, handler);
    }

    /**
     * Waits for all entry input and output resources to close or forces
     * them to close, dependending on the {@code options}.
     * Mind that this method deliberately handles entry input and output
     * resources in an equal manner because
     * {@link FsResourceAccountant#waitOtherThreads} WILL NOT WORK if any two
     * resource accountants share the same lock!
     *
     * @param  options a bit field of synchronization options.
     * @param  handler the exception handling strategy for consuming input
     *         {@code FsSyncException}s and/or assembling output
     *         {@code IOException}s.
     * @param  <X> The type of the {@code IOException} to throw at the
     *         discretion of the exception {@code handler}.
     * @throws IOException at the discretion of the exception {@code handler}
     *         upon the occurence of an {@link FsSyncException}.
     */
    private <X extends IOException> void waitOtherThreads(
            final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        final FsResourceAccountant accountant = this.accountant;
        if (null == accountant)
            return;
        final boolean wait = options.get(WAIT_CLOSE_INPUT)
                || options.get(WAIT_CLOSE_OUTPUT);
        final int resources = accountant.waitOtherThreads(wait ? 0 : 50);
        if (0 >= resources)
            return;
        final IOException cause = new OutputBusyException("Number of open entry resources: " + resources);
        final boolean force = options.get(FORCE_CLOSE_INPUT)
                || options.get(FORCE_CLOSE_OUTPUT);
        if (!force)
            throw handler.fail(new FsSyncException(getModel(), cause));
        handler.warn(new FsSyncWarningException(getModel(), cause));
    }

    /**
     * Closes and disconnects all entry streams of the output and input
     * archive.
     *
     * @param  handler the exception handling strategy for consuming input
     *         {@code FsSyncException}s and/or assembling output
     *         {@code IOException}s.
     * @param  <X> The type of the {@code IOException} to throw at the
     *         discretion of the exception {@code handler}.
     * @throws IOException at the discretion of the exception {@code handler}
     *         upon the occurence of an {@link FsSyncException}.
     */
    private <X extends IOException> void closeAllResources(
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        class FilterExceptionHandler
        implements ExceptionHandler<IOException, X> {
            @Override
            public X fail(IOException shouldNotHappen) {
                throw new AssertionError(shouldNotHappen);
            }

            @Override
            public void warn(IOException cause) throws X {
                handler.warn(new FsSyncWarningException(getModel(), cause));
            }
        } // FilterExceptionHandler

        final FsResourceAccountant accountant = this.accountant;
        if (null != accountant)
            accountant.closeAllResources(new FilterExceptionHandler());
    }

    @Immutable
    private enum AccountingSocketFactory {
        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsResourceController controller,
                    InputSocket<?> input) {
                return controller.new Input(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsResourceController controller,
                    OutputSocket<?> output) {
                return controller.new Output(output);
            }
        },

        NIO2() {
            @Override
            InputSocket<?> newInputSocket(
                    FsResourceController controller,
                    InputSocket<?> input) {
                return controller.new Nio2Input(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsResourceController controller,
                    OutputSocket<?> output) {
                return controller.new Nio2Output(output);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsResourceController controller,
                InputSocket <?> input);
        
        abstract OutputSocket<?> newOutputSocket(
                FsResourceController controller,
                OutputSocket <?> output);
    } // AccountingSocketFactory

    private final class Nio2Input
    extends Input {
        Nio2Input(InputSocket<?> input) {
            super(input);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            assert isWriteLockedByCurrentThread();
            return new AccountedSeekableByteChannel(
                    getBoundSocket().newSeekableByteChannel());
        }
    } // Nio2Input

    private class Input
    extends DecoratingInputSocket<Entry> {
        Input(InputSocket<?> input) {
            super(input);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getLocalTarget();
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            assert isWriteLockedByCurrentThread();
            return new AccountedReadOnlyFile(
                    getBoundSocket().newReadOnlyFile());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            assert isWriteLockedByCurrentThread();
            return new AccountedInputStream(
                    getBoundSocket().newInputStream());
        }
    } // Input

    private final class Nio2Output
    extends Output {
        Nio2Output(OutputSocket<?> output) {
            super(output);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            assert isWriteLockedByCurrentThread();
            return new AccountedSeekableByteChannel(
                    getBoundSocket().newSeekableByteChannel());
        }
    } // Nio2Output

    private class Output
    extends DecoratingOutputSocket<Entry> {
        Output(OutputSocket<?> output) {
            super(output);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getLocalTarget();
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            assert isWriteLockedByCurrentThread();
            return new AccountedOutputStream(
                    getBoundSocket().newOutputStream());
        }
    } // Output

    private final class AccountedReadOnlyFile
    extends DecoratingReadOnlyFile {
        @SuppressWarnings("LeakingThisInConstructor")
        AccountedReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            assert isWriteLockedByCurrentThread();
            getAccountant().stopAccountingFor(this);
            delegate.close();
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                //getAccountant().stopAccountingFor(this); // superfluous - done by GC!
                delegate.close();
            } finally {
                super.finalize();
            }
        }
    } // AccountedReadOnlyFile

    private final class AccountedSeekableByteChannel
    extends DecoratingSeekableByteChannel {
        @SuppressWarnings("LeakingThisInConstructor")
        AccountedSeekableByteChannel(SeekableByteChannel sbc) {
            super(sbc);
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            assert isWriteLockedByCurrentThread();
            getAccountant().stopAccountingFor(this);
            delegate.close();
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                //getAccountant().stopAccountingFor(this); // superfluous - done by GC!
                delegate.close();
            } finally {
                super.finalize();
            }
        }
    } // AccountedSeekableByteChannel

    private final class AccountedInputStream
    extends DecoratingInputStream {
        @SuppressWarnings("LeakingThisInConstructor")
        AccountedInputStream(InputStream in) {
            super(in);
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            assert isWriteLockedByCurrentThread();
            getAccountant().stopAccountingFor(this);
            delegate.close();
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                //getAccountant().stopAccountingFor(this); // superfluous - done by GC!
                delegate.close();
            } finally {
                super.finalize();
            }
        }
    } // AccountedInputStream

    private final class AccountedOutputStream
    extends DecoratingOutputStream {
        @SuppressWarnings("LeakingThisInConstructor")
        AccountedOutputStream(OutputStream out) {
            super(out);
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            assert isWriteLockedByCurrentThread();
            getAccountant().stopAccountingFor(this);
            delegate.close();
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                //getAccountant().stopAccountingFor(this); // superfluous - done by GC!
                delegate.close();
            } finally {
                super.finalize();
            }
        }
    } // AccountedOutputStream
}
