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

import java.util.Collections;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.InputStream;
import de.schlichtherle.truezip.io.socket.FilterInputSocket;
import de.schlichtherle.truezip.io.socket.Caches;
import java.util.HashMap;
import de.schlichtherle.truezip.io.socket.Cache;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.socket.FilterOutputSocket;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.util.Map;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.entry.CommonEntry.Type.FILE;
import static de.schlichtherle.truezip.io.archive.controller.SyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.io.archive.controller.SyncOption.FLUSH_CACHE;

/**
 * A caching archive controller implements a caching strategy for entries
 * within its target archive file.
 * Decorating an archive controller with this class has the following effects:
 * <ul>
 * <li>Upon the first read operation, the data will be read from the archive
 *     entry and stored in the cache.
 *     Subsequent or concurrent read operations will be served from the cache
 *     without re-reading the data from the archive entry again until the
 *     target archive file gets {@link #sync synced}.
 * <li>Any data written to the cache will get written to the target archive
 *     file if and only if the target archive file gets {@link #sync synced}.
 * <li>After a write operation, the data will be stored in the cache for
 *     subsequent read operations until the target archive file gets
 *     {@link #sync synced}.
 * </ul>
 * <p>
 * Caching an archive entry is automatically activated once an
 * {@link #getInputSocket input socket} with {@link InputOption#CACHE} or an
 * {@link #getOutputSocket output socket} with {@link InputOption#CACHE}
 * is acquired. Subsequent read/write operations for the archive entry will
 * then use the cache regardless if these options were set when the respective
 * socket was acquired or not.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class CachingArchiveController<AE extends ArchiveEntry>
extends FilterArchiveController<AE> {

    private final Map<String, EntryCache> caches
            = Collections.synchronizedMap(new HashMap<String, EntryCache>());

    CachingArchiveController(ArchiveController<? extends AE> controller) {
        super(controller);
    }

    final void ensureWriteLockedByCurrentThread()
    throws NotWriteLockedException {
        getModel().ensureWriteLockedByCurrentThread();
    }

    @Override
    public <E extends IOException>
    void sync(ExceptionBuilder<? super SyncException, E> builder, BitField<SyncOption> options)
    throws E, ArchiveControllerException {
        final boolean flush = options.get(FLUSH_CACHE);
        assert options.get(ABORT_CHANGES) != flush;
        for (final EntryCache cache : caches.values()) {
            try {
                try {
                    if (flush)
                        cache.flush();
                } finally {
                    cache.clear();
                }
            } catch (IOException ex) {
                throw builder.fail(new SyncException(getModel(), ex));
            }
        }
        caches.clear();
        super.sync(builder, options);
    }

    @Override
    public InputSocket<AE> getInputSocket(
            final String path,
            final BitField<InputOption> options) {
        return new Input(path, options);
    }

    private class Input extends FilterInputSocket<AE> {
        final String path;
        final BitField<InputOption> options;

        Input(final String path, final BitField<InputOption> options) {
            super(getController().getInputSocket(path, options));
            this.path = path;
            this.options = options;
        }

        @Override
        public InputSocket<? extends AE> getBoundSocket() throws IOException {
            Cache<AE> cache = null;
            if (!options.get(InputOption.CACHE)
                    && null == (cache = caches.get(path))) {
                return super.getBoundSocket();
            }
            ensureWriteLockedByCurrentThread();
            return (null != cache ? cache : new EntryCache(path, options, null))
                    .getInputSocket()
                    .bind(this);
        }
    } // class Input

    @Override
    public OutputSocket<AE> getOutputSocket(
            final String path,
            final BitField<OutputOption> options,
            final CommonEntry template) {
        return new Output(path, options, template);
    }

    private class Output extends FilterOutputSocket<AE> {
        final String path;
        final BitField<OutputOption> options;
        final CommonEntry template;

        Output( final String path,
                final BitField<OutputOption> options,
                final CommonEntry template) {
            super(getController().getOutputSocket(path, options, template));
            this.path = path;
            this.options = options;
            this.template = template;
        }

        @Override
        public OutputSocket<? extends AE> getBoundSocket() throws IOException {
            Cache<AE> cache = null;
            if (!options.get(OutputOption.CACHE)
                    && null == (cache = caches.get(path))
                    || options.get(OutputOption.APPEND) || null != template) {
                if (null != cache) {
                    try {
                        cache.flush();
                    } finally {
                        final Cache<AE> cache2 = caches.remove(path);
                        assert cache2 == cache;
                        cache.clear();
                    }
                }
                return super.getBoundSocket();
            }

            getController().mknod(path, FILE, options, null);
            return (null != cache ? cache : new EntryCache(path, null, options))
                    .getOutputSocket()
                    .bind(this);
        }
    } // class Output

    @Override
    public void unlink(final String path) throws IOException {
        super.unlink(path);
        final Cache<AE> cache = caches.remove(path);
        if (null != cache)
            cache.clear();
    }

    private final class EntryCache implements Cache<AE> {
        final String path;
        final Cache<AE> cache;

        EntryCache( final String path,
                    final BitField<InputOption > inputOptions,
                    final BitField<OutputOption> outputOptions) {
            this.path = path;
            this.cache = Caches.newInstance(    new Input(inputOptions),
                                                new Output(outputOptions));
        }

        @Override
        public InputSocket<AE> getInputSocket() {
            return cache.getInputSocket();
        }

        @Override
        public OutputSocket<AE> getOutputSocket() {
            return cache.getOutputSocket();
        }

        @Override
        public void flush() throws IOException {
            cache.flush();
        }

        @Override
        public void clear() throws IOException {
            cache.clear();
        }

        class Input extends FilterInputSocket<AE> {
            Input(final BitField<InputOption> inputOptions) {
                super(getController().getInputSocket(path,
                        null != inputOptions
                            ? inputOptions.clear(InputOption.CACHE)
                            : BitField.noneOf(InputOption.class)));
            }

            @Override
            public InputStream newInputStream() throws IOException {
                final InputStream in = super.newInputStream();
                caches.put(path, EntryCache.this);
                return in;
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                final ReadOnlyFile rof = super.newReadOnlyFile();
                caches.put(path, EntryCache.this);
                return rof;
            }
        } // class Input

        class Output extends FilterOutputSocket<AE> {
            Output(BitField<OutputOption> outputOptions) {
                super(getController().getOutputSocket(path,
                        null != outputOptions
                            ? outputOptions.clear(OutputOption.CACHE)
                            : BitField.noneOf(OutputOption.class),
                        null));
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                final OutputStream out = super.newOutputStream();
                caches.put(path, EntryCache.this);
                return out;
            }
        } // class Output
    }
}
