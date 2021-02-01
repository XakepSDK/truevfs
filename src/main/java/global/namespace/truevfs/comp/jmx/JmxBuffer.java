/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.jmx;

import global.namespace.truevfs.comp.cio.IoBuffer;
import global.namespace.truevfs.comp.inst.InstrumentingBuffer;

import javax.management.ObjectName;
import java.io.IOException;

/**
 * A controller for an {@linkplain IoBuffer I/O buffer}.
 *
 * @param  <M> the type of the JMX mediator.
 * @author Christian Schlichtherle
 */
public class JmxBuffer<M extends JmxMediator<M>> extends InstrumentingBuffer<M> implements JmxComponent {

    public JmxBuffer(M mediator, IoBuffer entry) {
        super(mediator, entry);
    }

    private ObjectName getObjectName() {
        return mediator
                .nameBuilder(IoBuffer.class)
                .put("name", ObjectName.quote(getName()))
                .get();
    }

    protected Object newView() { return new JmxBufferView<>(entry); }

    @Override
    public void activate() { mediator.register(getObjectName(), newView()); }

    @Override
    public void release() throws IOException {
        try { entry.release(); }
        finally { mediator.deregister(getObjectName()); }
    }
}
