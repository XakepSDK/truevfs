/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import static de.schlichtherle.truezip.fs.FsSyncOptions.SYNC;
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
import java.util.Map;
import javax.swing.Icon;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * A decorating file system controller which performs a
 * {@link FsController#sync(BitField) sync} operation on the
 * file system if and only if any decorated file system controller throws an
 * {@link FsNeedsSyncException}.
 * 
 * @see     FsNeedsSyncException
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsSyncController
extends FsLockModelDecoratingController<FsController<? extends FsLockModel>> {

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    /**
     * Times out waiting for resources after
     * {@link FsLockModelDecoratingController#WAIT_TIMEOUT_MILLIS} milliseconds.
     */
    /*private static final BitField<FsSyncOption>
            QUICK_SYNC = SYNC.clear(WAIT_CLOSE_INPUT).clear(WAIT_CLOSE_OUTPUT);*/

    /**
     * Constructs a new file system sync controller.
     *
     * @param controller the decorated file system controller.
     */
    public FsSyncController(FsController<? extends FsLockModel> controller) {
        super(controller);
    }

    private void sync() throws IOException {
        checkWriteLockedByCurrentThread();
        delegate.sync(SYNC);
        // This makes the CPU busy waiting.
        // It's not quite clear if this is required at all.
        /*try {
            delegate.sync(QUICK_SYNC);
        } catch (final FsSyncException ex) {
            final IOException cause = ex.getCause();
            if (cause instanceof FsThreadsIOBusyException)
                throw new FsNeedsLockRetryException();
            throw ex;
        }*/
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        while (true) {
            try {
                return delegate.getOpenIcon();
            } catch (FsNeedsSyncException ex) {
                sync();
            }
        }
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        while (true) {
            try {
                return delegate.getClosedIcon();
            } catch (FsNeedsSyncException ex) {
                sync();
            }
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        while (true) {
            try {
                return delegate.isReadOnly();
            } catch (FsNeedsSyncException ex) {
                sync();
            }
        }
    }

    @Override
    public FsEntry getEntry(final FsEntryName name)
    throws IOException {
        while (true) {
            try {
                return delegate.getEntry(name);
            } catch (FsNeedsSyncException ex) {
                sync();
            }
        }
    }

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return delegate.isReadable(name);
            } catch (FsNeedsSyncException ex) {
                sync();
            }
        }
    }

    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return delegate.isWritable(name);
            } catch (FsNeedsSyncException ex) {
                sync();
            }
        }
    }

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        while (true) {
            try {
                return delegate.isExecutable(name);
            } catch (FsNeedsSyncException ex) {
                sync();
            }
        }
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        while (true) {
            try {
                delegate.setReadOnly(name);
                return;
            } catch (FsNeedsSyncException ex) {
                sync();
            }
        }
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times,
            final BitField<FsOutputOption> options)
    throws IOException {
        while (true) {
            try {
                return delegate.setTime(name, times, options);
            } catch (FsNeedsSyncException ex) {
                sync();
            }
        }
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value,
            final BitField<FsOutputOption> options)
    throws IOException {
        while (true) {
            try {
                return delegate.setTime(name, types, value, options);
            } catch (FsNeedsSyncException ex) {
                sync();
            }
        }
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsInputOption> options) {
        return SOCKET_FACTORY.newInputSocket(this,
                delegate.getInputSocket(name, options));
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsOutputOption> options,
            final Entry template) {
        return SOCKET_FACTORY.newOutputSocket(this,
                delegate.getOutputSocket(name, options, template));
    }

    @Override
    public void mknod(
            final @NonNull FsEntryName name,
            final @NonNull Type type,
            final @NonNull BitField<FsOutputOption> options,
            final @CheckForNull Entry template)
    throws IOException {
        while (true) {
            try {
                delegate.mknod(name, type, options, template);
                return;
            } catch (FsNeedsSyncException ex) {
                sync();
            }
        }
    }

    @Override
    public void unlink(
            final FsEntryName name,
            final BitField<FsOutputOption> options)
    throws IOException {
        while (true) {
            try {
                delegate.unlink(name, options);
                return;
            } catch (FsNeedsSyncException ex) {
                sync();
            }
        }
    }

    @Override
    public <X extends IOException> void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        // No sync for sync, please.
        delegate.sync(options, handler);
    }

    @Immutable
    private enum SocketFactory {
        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    final FsSyncController controller,
                    final InputSocket<?> input) {
                return controller.new Input(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    final FsSyncController controller,
                    final OutputSocket<?> output) {
                return controller.new Output(output);
            }
        },

        NIO2() {
            @Override
            InputSocket<?> newInputSocket(
                    final FsSyncController controller,
                    final InputSocket<?> input) {
                return controller.new Nio2Input(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    final FsSyncController controller,
                    final OutputSocket<?> output) {
                return controller.new Nio2Output(output);
            }
        };

        abstract InputSocket<?> newInputSocket(
                final FsSyncController controller,
                final InputSocket <?> input);
        
        abstract OutputSocket<?> newOutputSocket(
                final FsSyncController controller,
                final OutputSocket <?> output);
    } // SocketFactory

    private final class Nio2Input
    extends Input {
        Nio2Input(final InputSocket<?> input) {
            super(input);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().newSeekableByteChannel();
                } catch (FsNeedsSyncException ex) {
                    sync();
                }
            }
        }
    } // Nio2Input

    private class Input
    extends DecoratingInputSocket<Entry> {
        Input(final InputSocket<?> input) {
            super(input);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().getLocalTarget();
                } catch (FsNeedsSyncException ex) {
                    sync();
                }
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().newReadOnlyFile();
                } catch (FsNeedsSyncException ex) {
                    sync();
                }
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().newInputStream();
                } catch (FsNeedsSyncException ex) {
                    sync();
                }
            }
        }
    } // Input

    private final class Nio2Output
    extends Output {
        Nio2Output(final OutputSocket<?> output) {
            super(output);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().newSeekableByteChannel();
                } catch (FsNeedsSyncException ex) {
                    sync();
                }
            }
        }
    } // Nio2Output

    private class Output
    extends DecoratingOutputSocket<Entry> {
        Output(final OutputSocket<?> output) {
            super(output);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().getLocalTarget();
                } catch (FsNeedsSyncException ex) {
                    sync();
                }
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().newOutputStream();
                } catch (FsNeedsSyncException ex) {
                    sync();
                }
            }
        }
    } // Output
}
