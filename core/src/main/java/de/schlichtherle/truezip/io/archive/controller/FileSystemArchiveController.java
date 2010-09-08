/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.util.Operation;
import java.io.IOException;

/**
 * This archive controller implements the automounting functionality.
 * It is up to the sub class to implement the actual mounting/unmounting
 * strategy.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
abstract class FileSystemArchiveController extends ArchiveController {

    /** The mount state of the archive file system. */
    private AutoMounter autoMounter = new ResetFileSystem();

    /**
     * Creates a new instance of FileSystemArchiveController
     */
    FileSystemArchiveController(
            java.io.File target,
            ArchiveController enclController,
            String enclEntryName,
            ArchiveDriver driver) {
        super(target, enclController, enclEntryName, driver);
    }

    @Override
    final boolean isTouched() {
        final ArchiveFileSystem fileSystem = getFileSystem();
        return fileSystem != null && fileSystem.isTouched();
    }

    @Override
    public final ArchiveFileSystem autoMount(final boolean create)
    throws FalsePositiveException, IOException {
        assert readLock().isLockedByCurrentThread() || writeLock().isLockedByCurrentThread();
        return autoMounter.autoMount(create);
    }

    final ArchiveFileSystem getFileSystem() {
        return autoMounter.getFileSystem();
    }

    final void setFileSystem(ArchiveFileSystem fileSystem) {
        autoMounter.setFileSystem(fileSystem);
    }

    /**
     * Represents the mount state of the archive file system.
     * This is an abstract class: The state is implemented in the sub classes.
     */
    private static abstract class AutoMounter {
        abstract ArchiveFileSystem autoMount(boolean create)
        throws FalsePositiveException, IOException;

        ArchiveFileSystem getFileSystem() {
            return null;
        }

        abstract void setFileSystem(ArchiveFileSystem fileSystem);
    } // class AutoMounter

    private class ResetFileSystem extends AutoMounter {
        @Override
        ArchiveFileSystem autoMount(final boolean create)
        throws FalsePositiveException, IOException {
            try {
                class Mounter implements Operation<Exception> {
                    @Override
                    public void run() throws FalsePositiveException, IOException {
                        // Check state again: Another thread may have changed
                        // it while we released all read locks in order to
                        // acquire the write lock!
                        if (autoMounter == ResetFileSystem.this) {
                            mount(create);
                            assert autoMounter instanceof MountedFileSystem;
                        } else {
                            assert autoMounter != null;
                            assert !(autoMounter instanceof ResetFileSystem);
                        }
                    }
                } // class Mounter

                runWriteLocked(new Mounter());
            } catch (FalsePositiveException fpe) {
                // Catch and cache exceptions for uncacheable false positives.
                // The state is reset when File.delete() is called on the false
                // positive archive file or File.update() or File.sync().
                //   This is an important optimization: When hitting a false
                // positive archive file, a client application might perform
                // a lot of tests on it (isDirectory(), isFile(), isExisting(),
                // getLength(), etc). If the exception were not cached, each call
                // would run the file system initialization again, only to
                // result in another instance of the same exception type again.
                //   Note that it is important to cache the exceptions for
                // cacheable false positives only: Otherwise, side effects
                // of the archive driver may not be accounted for.
                if (fpe.isCacheable())
                    autoMounter = new FalsePositiveFileSystem(fpe);
                throw fpe;
            } catch (IOException ioe) {
                throw ioe;
            } catch (Exception cannotHappen) {
                throw new AssertionError(cannotHappen);
            }

            assert autoMounter != this;
            // DON'T just call autoMounter.getFileSystem()!
            // This would return null if autoMounter is an instance of
            // FalsePositiveFileSystem.
            return autoMounter.autoMount(create);
        }

        @Override
        void setFileSystem(ArchiveFileSystem fileSystem) {
            // Passing in null may happen by reset().
            if (fileSystem != null)
                autoMounter = new MountedFileSystem(fileSystem);
        }
    } // class ResetFileSystem

    private class MountedFileSystem extends AutoMounter {
        private final ArchiveFileSystem fileSystem;

        private MountedFileSystem(final ArchiveFileSystem fileSystem) {
            if (fileSystem == null)
                throw new NullPointerException();
            this.fileSystem = fileSystem;
        }

        @Override
        ArchiveFileSystem autoMount(boolean create) {
            return fileSystem;
        }

        @Override
        ArchiveFileSystem getFileSystem() {
            return fileSystem;
        }

        @Override
        void setFileSystem(final ArchiveFileSystem fileSystem) {
            if (fileSystem != null)
                throw new IllegalArgumentException("File system already mounted!");
            autoMounter = new ResetFileSystem();
        }
    } // class MountedFileSystem

    private class FalsePositiveFileSystem extends AutoMounter {
        private final FalsePositiveException exception;

        private FalsePositiveFileSystem(final FalsePositiveException exception) {
            if (exception == null)
                throw new NullPointerException();
            this.exception = exception;
        }

        @Override
        ArchiveFileSystem autoMount(boolean create)
        throws FalsePositiveException {
            throw exception;
        }

        @Override
        void setFileSystem(final ArchiveFileSystem fileSystem) {
            if (fileSystem != null)
                throw new IllegalArgumentException("False positive archive file cannot have file system!");
            autoMounter = new ResetFileSystem();
        }
    } // class FalsePositiveFileSystem

    /**
     * Mounts the virtual file system from the target file.
     * This method is called while the write lock to mount the file system
     * for this controller is acquired.
     * <p>
     * Upon normal termination, this method is expected to have called
     * {@link setFileSystem} to assign the fully initialized file system
     * to this controller.
     * Other than this, the method must not have any side effects on the
     * state of this class or its super class.
     * It may, however, have side effects on the state of the sub class.
     *
     * @param create If the archive file does not exist and this is
     *        {@code true}, a new file system with only a virtual root
     *        directory is created with its last modification time set to the
     *        system's current time.
     * @throws FalsePositiveException
     * @throws IOException On any other I/O related issue with the target file
     *         or the target file of any enclosing archive file's controller.
     */
    abstract void mount(boolean create)
    throws FalsePositiveException, IOException;

    @Override
    void reset(final SyncExceptionHandler handler)
    throws SyncException {
        setFileSystem(null);
    }
}
