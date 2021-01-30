/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import net.java.truevfs.ext.insight.stats.IoStats;
import net.java.truevfs.ext.insight.stats.IoStatsView;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanInfo;

/**
 * A view for {@link IoStats}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class I5tIoStatsView extends I5tStatsView {

    I5tIoStatsView(I5tIoStatsController controller) {
        super(controller, IoStatsView.class, true);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "A log of I/O statistics.";
    }
}
