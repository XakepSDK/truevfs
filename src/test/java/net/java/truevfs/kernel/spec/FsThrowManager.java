/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static net.java.truecommons.shed.Throwables.wrap;

/**
 * @author Christian Schlichtherle
 */
public final class FsThrowManager {

    private final Map<Class<?>, Throwable> throwables;

    /** Default constructor. */
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public FsThrowManager() {
        this.throwables = new HashMap<>();
    }

    /**
     * Copy constructor.
     * 
     * @param template The template to copy.
     */
    public FsThrowManager(final FsThrowManager template) {
        this.throwables = new HashMap<>(template.throwables);
    }

    public Throwable trigger(Throwable toThrow) {
        return trigger(Object.class, toThrow);
    }

    public Throwable trigger(final Class<?> from, final Throwable toThrow) {
        Objects.requireNonNull(from);
        wrap(toThrow); // test
        // DON'T put wrap(toThrow): We want the stack trace of the call to check(*), not the call to this method!
        return throwables.put(from, toThrow);
    }

    public Throwable clear(Class<?> from) {
        return throwables.remove(from);
    }

    public <X extends Throwable> void check(Object thiz, Class<X> throwz)
    throws X {
        check(thiz.getClass(), throwz);
    }

    private <X extends Throwable> void check(   final Class<?> thiz,
                                                final Class<X> throwz)
    throws X {
        final Throwable toThrow = throwables.remove(thiz);
        if (null != toThrow)
            if (throwz.isInstance(toThrow)) throw throwz.cast(wrap(toThrow));
            else throwables.put(thiz, toThrow); // restore
        // No match, now recursively check interfaces first and then super
        // classes.
        // This may result in redundant checks for interfaces.
        for (final Class<?> ic : thiz.getInterfaces())
            check(ic, throwz);
        final Class<?> sc = thiz.getSuperclass();
        if (null != sc)
            check(sc, throwz);
    }
}