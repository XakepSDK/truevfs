/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.insight;

import global.namespace.truevfs.comp.jmx.JmxComponent;
import global.namespace.truevfs.ext.insight.stats.FsStats;
import global.namespace.truevfs.ext.insight.stats.IoStats;
import global.namespace.truevfs.ext.insight.stats.SyncStats;

import javax.management.ObjectName;

import static java.util.Locale.ENGLISH;

/**
 * A base controller for {@link IoStats} or {@link SyncStats}.
 *
 * @author Christian Schlichtherle
 */
abstract class I5tStatsController implements JmxComponent {

    private final I5tMediator mediator;
    private final int offset;

    I5tStatsController(I5tMediator mediator, int offset) {
        assert (0 <= offset);
        this.mediator = mediator;
        this.offset = offset;
    }

    @Override
    public final void activate() {
        mediator.register(getObjectName(), newView());
    }

    private ObjectName getObjectName() {
        return mediator
                .nameBuilder(FsStats.class)
                .put("subject", getSubject())
                .put("offset", mediator.formatOffset(offset))
                .get();
    }

    final String getSubject() {
        return mediator.getSubject();
    }

    final FsStats getStats() {
        return mediator.stats(offset);
    }

    final void rotate() {
        mediator.rotateStats(this);
    }

    abstract I5tStatsView newView();

    @Override
    public final String toString() {
        return String.format(
                ENGLISH,
                "%s[subject=%s, offset=%d, mediator=%s]",
                getClass().getName(),
                getSubject(),
                offset,
                mediator);
    }
}
