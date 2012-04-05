/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsController;
import de.truezip.kernel.FsResourceOpenException;
import de.truezip.kernel.FsSyncException;
import de.truezip.kernel.FsSyncWarningException;
import de.truezip.kernel.addr.FsEntryName;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.DecoratingInputStream;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.option.AccessOption;
import de.truezip.kernel.option.SyncOption;
import static de.truezip.kernel.option.SyncOption.FORCE_CLOSE_IO;
import static de.truezip.kernel.option.SyncOption.WAIT_CLOSE_IO;
import de.truezip.kernel.rof.DecoratingReadOnlyFile;
import de.truezip.kernel.rof.ReadOnlyFile;
import de.truezip.kernel.io.DecoratingSeekableByteChannel;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Accounts input and output resources returned by its decorated controller.
 * 
 * @see    FsResourceAccountant
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class FsResourceController
extends FsDecoratingLockModelController<FsController<? extends FsLockModel>> {

    private @CheckForNull FsResourceAccountant accountant;

    /**
     * Constructs a new file system resource controller.
     *
     * @param controller the decorated file system controller.
     */
    FsResourceController(FsController<? extends FsLockModel> controller) {
        super(controller);
    }

    private FsResourceAccountant getAccountant() {
        assert isWriteLockedByCurrentThread();
        final FsResourceAccountant a = accountant;
        return null != a ? a : (accountant = new FsResourceAccountant(writeLock()));
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<AccessOption> options) {
        @NotThreadSafe
        final class Input extends DecoratingInputSocket<Entry> {
            Input() {
                super(controller.getInputSocket(name, options));
            }

            @Override
            public InputStream newStream() throws IOException {
                return new ResourceInputStream(getBoundSocket().newStream());
            }

            @Override
            public SeekableByteChannel newChannel() throws IOException {
                return new ResourceSeekableByteChannel(getBoundSocket().newChannel());
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                return new ResourceReadOnlyFile(getBoundSocket().newReadOnlyFile());
            }
        } // Input

        return new Input();
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<AccessOption> options,
            final @CheckForNull Entry template) {
        @NotThreadSafe
        final class Output extends DecoratingOutputSocket<Entry> {
            Output() {
                super(controller.getOutputSocket(name, options, template));
            }

            @Override
            public OutputStream newStream() throws IOException {
                return new ResourceOutputStream(getBoundSocket().newStream());
            }

            @Override
            public SeekableByteChannel newChannel() throws IOException {
                return new ResourceSeekableByteChannel(getBoundSocket().newChannel());
            }
        } // Output

        return new Output();
    }

    @Override
    public <X extends IOException> void
    sync(   final BitField<SyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        assert isWriteLockedByCurrentThread();
        waitIdle(options, handler);
        closeAll(handler);
        controller.sync(options, handler);
    }

    /**
     * Waits for all entry input and output resources to close or forces
     * them to close, dependending on the {@code options}.
     * Mind that this method deliberately handles entry input and output
     * streams equally because {@link FsResourceAccountant#waitForeignResources}
     * WILL NOT WORK if any two resource accountants share the same lock!
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
    private <X extends IOException> void
    waitIdle(   final BitField<SyncOption> options,
                final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        // HC SUNT DRACONES!
        final FsResourceAccountant a = accountant;
        if (null == a)
            return;
        final boolean force = options.get(FORCE_CLOSE_IO);
        final int local = a.localResources();
        final IOException cause;
        if (0 != local && !force) {
            cause = new FsResourceOpenException(a.totalResources(), local);
            throw handler.fail(new FsSyncException(getModel(), cause));
        }
        final boolean wait = options.get(WAIT_CLOSE_IO);
        final int total = a.waitForeignResources(wait ? 0 : WAIT_TIMEOUT_MILLIS);
        if (0 == total)
            return;
        cause = new FsResourceOpenException(total, local);
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
    private <X extends IOException> void
    closeAll(final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        final class IOExceptionHandler
        implements ExceptionHandler<IOException, X> {
            @Override
            public X fail(IOException shouldNotHappen) {
                throw new AssertionError(shouldNotHappen);
            }

            @Override
            public void warn(IOException cause) throws X {
                assert !(cause instanceof FsControllerException);
                handler.warn(new FsSyncWarningException(getModel(), cause));
            }
        } // IOExceptionHandler

        final FsResourceAccountant acc = accountant;
        if (null != acc)
            acc.closeAllResources(new IOExceptionHandler());
    }

    private final class ResourceReadOnlyFile
    extends DecoratingReadOnlyFile {
        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceReadOnlyFile(@WillCloseWhenClosed ReadOnlyFile rof) {
            super(rof);
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            getAccountant().stopAccountingFor(this);
            rof.close();
        }
    } // ResourceReadOnlyFile

    private final class ResourceSeekableByteChannel
    extends DecoratingSeekableByteChannel {
        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceSeekableByteChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            getAccountant().stopAccountingFor(this);
            sbc.close();
        }
    } // ResourceSeekableByteChannel

    private final class ResourceInputStream
    extends DecoratingInputStream {
        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            getAccountant().stopAccountingFor(this);
            in.close();
        }
    } // ResourceInputStream

    private final class ResourceOutputStream
    extends DecoratingOutputStream {
        @CreatesObligation
        @SuppressWarnings("LeakingThisInConstructor")
        ResourceOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            getAccountant().stopAccountingFor(this);
            out.close();
        }
    } // ResourceOutputStream
}