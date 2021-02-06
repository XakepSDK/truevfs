/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.base;

import global.namespace.truevfs.comp.cio.*;
import global.namespace.truevfs.comp.io.DecoratingInputStream;
import global.namespace.truevfs.comp.io.DecoratingOutputStream;
import global.namespace.truevfs.comp.io.DecoratingSeekableChannel;
import global.namespace.truevfs.comp.io.PowerBuffer;
import global.namespace.truevfs.comp.util.BitField;
import global.namespace.truevfs.driver.mock.MockController;
import global.namespace.truevfs.kernel.api.*;
import lombok.val;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.*;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import static global.namespace.truevfs.comp.cio.Entry.Access.*;
import static global.namespace.truevfs.comp.cio.Entry.Size.DATA;
import static global.namespace.truevfs.comp.cio.Entry.Size.STORAGE;
import static global.namespace.truevfs.comp.cio.Entry.Type.FILE;
import static global.namespace.truevfs.comp.cio.Entry.UNKNOWN;
import static global.namespace.truevfs.comp.util.Throwables.contains;
import static global.namespace.truevfs.kernel.api.FsAccessOptions.NONE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * @param <E> The type of the archive entries.
 * @param <D> The type of the archive driver.
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class FsArchiveDriverTestSuite<E extends FsArchiveEntry, D extends FsArchiveDriver<E>>
        extends FsArchiveDriverTestBase<D> {

    private static final Logger logger = LoggerFactory.getLogger(FsArchiveDriverTestSuite.class);

    private static final FsNodeName name = FsNodeName.create(URI.create("archive"));

    private static final String US_ASCII_CHARACTERS;

    static {
        final StringBuilder builder = new StringBuilder(128);
        for (char c = 0; c < 128; c++) {
            builder.append(c);
        }
        US_ASCII_CHARACTERS = builder.toString();
    }

    private FsModel model;
    private FsController parent;

    @Override
    public void setUp() throws IOException {
        super.setUp();
        // Order is important here!
        final FsTestConfig config = FsTestConfig.get();
        config.setDataSize(getMaxArchiveLength());
        config.setPool(null); // reset
        model = newArchiveModel();
        parent = newParentController(model.getParent().get());
        assert !UTF_8.equals(getArchiveDriver().getCharset()) || null == getUnencodableName() : "Bad test setup!";
    }

    /**
     * Returns an unencodable name or {@code null} if all characters are
     * encodable in entry names for this archive type.
     *
     * @return An unencodable name or {@code null} if all characters are
     * encodable in entry names for this archive type.
     */
    protected abstract @CheckForNull
    String getUnencodableName();

    @Test
    public void testCharsetMustNotBeNull() {
        assertThat(getArchiveDriver().getCharset(), notNullValue());
    }

    @Test
    public void testUnencodableCharacters() {
        final String name = getUnencodableName();
        if (null != name) {
            assertFalse(getArchiveDriver().getCharset().newEncoder().canEncode(name));
        }
    }

    @Test
    public void testAllUsAsciiCharactersMustBeEncodable() {
        getArchiveDriver().getCharset().newEncoder().canEncode(US_ASCII_CHARACTERS);
    }

    @Test
    public void testArchiveDriverProperty() {
        assertTrue(getArchiveDriver().isArchiveDriver());
    }

    @Test
    public void testIoPoolMustNotBeNull() {
        assertNotNull(getArchiveDriver().getPool());
    }

    @Test
    public void testIoPoolShouldBeConstant() {
        final IoBufferPool p1 = getArchiveDriver().getPool();
        final IoBufferPool p2 = getArchiveDriver().getPool();
        if (p1 != p2) {
            logger.warn("{} returns different I/O buffer pools upon multiple invocations of getPool()!", getArchiveDriver().getClass());
        }
    }

    /*@Test(expected = NullPointerException.class)
    public void testNewControllerMustNotTolerateNullModel() {
        getArchiveDriver().newController(newManager(), null, parent);
    }

    @Test(expected = NullPointerException.class)
    public void testNewControllerMustNotTolerateNullParent() {
        getArchiveDriver().newController(newManager(), model, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewControllerMustCheckParentMemberMatch1() {
        getArchiveDriver().newController(newManager(), model.getParent(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewControllerMustCheckParentMemberMatch2() {
        getArchiveDriver().newController(newManager(), model.getParent(), parent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewControllerMustCheckParentMemberMatch3() {
        getArchiveDriver().newController(newManager(), model, newController(model));
    }

    @Test
    public void testNewControllerMustNotReturnNull() {
        assertNotNull(getArchiveDriver().newController(newManager(), model, parent));
    }

    @Test
    public void testNewControllerMustMeetPostConditions() {
        final FsController<?> c = getArchiveDriver().newController(newManager(), model, parent);
        assertNotNull(c);
        assertEquals(model.getMountPoint(), c.getModel().getMountPoint());
        assertSame(parent, c.getParent());
    }*/

    @Test(expected = NullPointerException.class)
    public void testNewInputMustNotTolerateNullModel() throws IOException {
        getArchiveDriver().newInput(null, NONE, parent, name);
    }

    @Test(expected = NullPointerException.class)
    public void testNewInputMustNotTolerateNullParentController() throws IOException {
        getArchiveDriver().newInput(model, NONE, null, name);
    }

    @Test(expected = NullPointerException.class)
    public void testNewInputMustNotTolerateNullEntryName() throws IOException {
        getArchiveDriver().newInput(model, NONE, parent, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNewInputMustNotTolerateNullOptions() throws IOException {
        getArchiveDriver().newInput(model, null, parent, name);
    }

    @Test(expected = NullPointerException.class)
    public void testNewOutputMustNotTolerateNullModel() throws IOException {
        getArchiveDriver().newOutput(null, NONE, parent, name, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNewOutputMustNotTolerateNullParentController() throws IOException {
        getArchiveDriver().newOutput(model, NONE, null, name, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNewOutputMustNotTolerateNullEntryName() throws IOException {
        getArchiveDriver().newOutput(model, NONE, parent, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNewOutputMustNotTolerateNullOptions() throws IOException {
        getArchiveDriver().newOutput(model, null, parent, name, null);
    }

    @Test
    public void testEmptyRoundTripPersistence() throws IOException {
        roundTripPersistence(0);
    }

    @Test
    public void testStandardRoundTripPersistence() throws IOException {
        roundTripPersistence(getNumEntries());
    }

    private void roundTripPersistence(int numEntries) throws IOException {
        output(numEntries);
        input(numEntries);
    }

    private void output(final int numEntries) throws IOException {
        final OutputContainer<E> service = getArchiveDriver().newOutput(model, NONE, parent, name, null);
        try {
            final Closeable[] streams = new Closeable[numEntries];
            try {
                for (int i = 0; i < streams.length; i++) {
                    streams[i] = output(service, i);
                }
            } finally {
                close(streams);
            }
            check(service, numEntries);
        } finally {
            final IOException expected = new IOException();
            trigger(TestCloseable.class, expected);
            try {
                // This call may succeed if the archive driver is not using the
                // parent controller (i.e. the MockArchiveDriver).
                service.close();
                //fail();
            } catch (final IOException got) {
                if (!contains(got, expected)) {
                    throw got;
                }
            } finally {
                clear(TestCloseable.class);
            }
            service.close();
        }
    }

    private OutputStream output(final OutputContainer<E> service, final int i) throws IOException {
        final String name = name(i);
        final E entry = newEntry(name);
        final OutputSocket<E> output = service.output(entry);
        assertSame(entry, output.getTarget());

        assertFalse(service.entry(name).isPresent());
        assertEquals(i, service.entries().size());

        boolean failure = true;
        final OutputStream out = output.stream(Optional.empty());
        try {
            assertSame(entry, service.entry(name).get());
            assertEquals(i + 1, service.entries().size());
            out.write(getData());
            failure = false;
        } finally {
            if (failure) out.close();
        }
        return out;
    }

    private void input(final int numEntries) throws IOException {
        final InputContainer<E> service = getArchiveDriver().newInput(model, NONE, parent, name);
        try {
            check(service, numEntries);
            final Closeable[] streams = new Closeable[numEntries];
            try {
                for (int i = 0; i < streams.length; i++) {
                    input(service, i).close(); // first attempt
                    streams[i] = input(service, i); // second attempt
                }
            } finally {
                close(streams);
            }
        } finally {
            final IOException expected = new IOException();
            trigger(TestCloseable.class, expected);
            try {
                // This call may succeed if the archive driver is not using the
                // parent controller (i.e. the MockArchiveDriver) or has been
                // reading the archive file upfront (e.g. the TAR driver).
                service.close();
                //fail();
            } catch (final IOException got) {
                if (!contains(got, expected)) {
                    throw got;
                }
            } finally {
                clear(TestCloseable.class);
            }
            service.close();
        }
    }

    private InputStream input(final InputContainer<E> service, final int i) throws IOException {
        final InputSocket<E> input = service.input(name(i));

        {
            final PowerBuffer<?> buf = PowerBuffer.allocate(getDataLength());
            SeekableByteChannel channel;
            try {
                channel = input.channel(Optional.empty());
            } catch (final UnsupportedOperationException ex) {
                channel = null;
                logger.trace(input.getClass().getName(), ex);
            }
            if (null != channel) {
                try {
                    buf.load(channel);
                    assertEquals(channel.position(), channel.size());
                } finally {
                    channel.close();
                }
                channel.close(); // expect no issues
                assertArrayEquals(getData(), buf.array());
            }
        }

        {
            final byte[] buf = new byte[getDataLength()];
            boolean failure = true;
            final DataInputStream in = new DataInputStream(input.stream(Optional.empty()));
            try {
                in.readFully(buf);
                assertArrayEquals(getData(), buf);
                assertEquals(-1, in.read());
                failure = false;
            } finally {
                if (failure) {
                    in.close();
                }
            }
            return in;
        }
    }

    private static void close(final Closeable[] resources) throws IOException {
        IOException ex = null;
        for (final Closeable resource : resources) {
            if (null == resource) {
                continue;
            }
            try {
                try {
                    resource.close();
                } finally {
                    resource.close(); // must be idempotent on side effects
                }
            } catch (final IOException ex2) {
                if (null != ex) {
                    ex.addSuppressed(ex2);
                } else {
                    ex = ex2;
                }
            }
        }
        if (null != ex) {
            throw ex;
        }
    }

    private <E extends FsArchiveEntry> void check(
            final Container<E> container,
            final int numEntries
    ) throws IOException {
        assertEquals(numEntries, container.entries().size());
        final Iterator<E> it = container.entries().iterator();
        for (int i = 0; i < numEntries; i++) {
            final E e = it.next();
            assertNotNull(e);
            assertEquals(name(i), e.getName());
            assertSame(FILE, e.getType());
            assertEquals(getDataLength(), e.getSize(DATA));
            final long storage = e.getSize(STORAGE);
            assertTrue(UNKNOWN == storage || getDataLength() <= storage); // random data is not compressible!
            assertTrue(UNKNOWN != e.getTime(WRITE));
            try {
                it.remove();
                fail();
            } catch (UnsupportedOperationException expected) {
            }
            assertSame(e, container.entry(e.getName()).get());
        }
        assertFalse(it.hasNext());
        try {
            it.next();
            fail();
        } catch (NoSuchElementException expected) {
        }
        try {
            it.remove();
            fail();
        } catch (UnsupportedOperationException expected) {
        }
        assertEquals(numEntries, container.entries().size());
    }

    private E newEntry(final String name) throws CharConversionException {
        final E e = getArchiveDriver().newEntry(name, Entry.Type.FILE, null);
        assertNotNull(e);
        assertEquals(name, e.getName());
        assertSame(FILE, e.getType());
        assertSame((long) UNKNOWN, e.getSize(DATA));
        assertSame((long) UNKNOWN, e.getSize(STORAGE));
        assertSame((long) UNKNOWN, e.getTime(WRITE));
        assertSame((long) UNKNOWN, e.getTime(READ));
        assertSame((long) UNKNOWN, e.getTime(CREATE));
        return e;
    }

    private static String name(int i) {
        return Integer.toString(i);
    }

    private MockController newParentController(final FsModel model) {
        val pm = model.getParent();
        val pc = pm.map(this::newParentController);
        return new ParentController(model, pc);
    }

    private FsModel newArchiveModel() {
        final FsModel parent = newNonArchiveModel();
        return newModel(
                FsMountPoint.create(URI.create("scheme:" + parent.getMountPoint() + name + "!/")),
                Optional.of(parent)
        );
    }

    private FsModel newNonArchiveModel() {
        return newModel(
                FsMountPoint.create(URI.create("file:/")),
                Optional.empty());
    }

    protected FsModel newModel(FsMountPoint mountPoint, Optional<? extends FsModel> parent) {
        return new FsTestModel(mountPoint, parent);
    }

    private int getMaxArchiveLength() {
        return getNumEntries() * getDataLength() * 4 / 3; // account for archive type specific overhead
    }

    private Throwable trigger(Class<?> from, Throwable toThrow) {
        return getThrowControl().trigger(from, toThrow);
    }

    private Throwable clear(Class<?> from) {
        return getThrowControl().clear(from);
    }

    private void checkAllExceptions(final Object thiz) throws IOException {
        final FsThrowManager ctl = getThrowControl();
        ctl.check(thiz, IOException.class);
        ctl.check(thiz, RuntimeException.class);
        ctl.check(thiz, Error.class);
    }

    private FsThrowManager getThrowControl() {
        return FsTestConfig.get().getThrowControl();
    }

    private int getNumEntries() {
        return FsTestConfig.get().getNumEntries();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final class ParentController extends MockController {

        ParentController(FsModel model, Optional<? extends FsController> parent) {
            super(model, parent, Optional.of(FsTestConfig.get()));
        }

        @Override
        public InputSocket<?> input(
                final BitField<FsAccessOption> options,
                final FsNodeName name
        ) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(options);
            return new DecoratingInputSocket<Entry>() {

                {
                    socket = ParentController.super.input(options, name);
                }

                @Override
                public InputStream stream(
                        Optional<? extends OutputSocket<? extends Entry>> peer
                ) throws IOException {
                    return new TestInputStream(getSocket().stream(peer));
                }

                @Override
                public SeekableByteChannel channel(
                        Optional<? extends OutputSocket<? extends Entry>> peer
                ) throws IOException {
                    return new TestSeekableChannel(getSocket().channel(peer));
                }
            };
        }

        @Override
        public OutputSocket<?> output(
                final BitField<FsAccessOption> options,
                final FsNodeName name,
                final Optional<? extends Entry> template
        ) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(options);
            return new DecoratingOutputSocket<Entry>() {

                {
                    socket = ParentController.super.output(options, name, template);
                }

                @Override
                public SeekableByteChannel channel(
                        Optional<? extends InputSocket<? extends Entry>> peer
                ) throws IOException {
                    return new TestSeekableChannel(getSocket().channel(peer));
                }

                @Override
                public OutputStream stream(
                        Optional<? extends InputSocket<? extends Entry>> peer
                ) throws IOException {
                    return new TestOutputStream(getSocket().stream(peer));
                }
            };
        }
    }

    @SuppressWarnings("MarkerInterface")
    private interface TestCloseable extends Closeable {
    }

    private final class TestInputStream extends DecoratingInputStream implements TestCloseable {

        TestInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            checkAllExceptions(this);
            in.close();
        }
    }

    private final class TestOutputStream extends DecoratingOutputStream implements TestCloseable {

        TestOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            checkAllExceptions(this);
            out.close();
        }
    }

    private final class TestSeekableChannel extends DecoratingSeekableChannel implements TestCloseable {

        TestSeekableChannel(SeekableByteChannel channel) {
            super(channel);
        }

        @Override
        public void close() throws IOException {
            checkAllExceptions(this);
            channel.close();
        }
    }
}
