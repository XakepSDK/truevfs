/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.jmx;

import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.Entry.Size;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.kernel.api.*;
import global.namespace.truevfs.kernel.api.sl.FsDriverMapLocator;
import global.namespace.truevfs.kernel.api.sl.FsManagerLocator;
import lombok.val;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.StandardMBean;
import java.io.IOException;
import java.util.*;

import static global.namespace.truevfs.comp.cio.Entry.Access.*;
import static global.namespace.truevfs.comp.cio.Entry.Size.DATA;
import static global.namespace.truevfs.comp.cio.Entry.Size.STORAGE;
import static global.namespace.truevfs.comp.cio.Entry.UNKNOWN;

/**
 * A view for a {@linkplain FsModel file system model}.
 *
 * @param <M> the type of the file system model.
 * @author Christian Schlichtherle
 */
public class JmxModelView<M extends FsModel> extends StandardMBean implements JmxModelMXBean {

    private static final FsCompositeDriver DRIVER = new FsSimpleCompositeDriver(FsDriverMapLocator.SINGLETON);

    protected final M model;

    public JmxModelView(M model) {
        this(model, JmxModelMXBean.class);
    }

    protected JmxModelView(final M model, final Class<? extends JmxModelMXBean> type) {
        super(type, true);
        this.model = Objects.requireNonNull(model);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "A file system model.";
    }

    @Override
    protected String getDescription(final MBeanAttributeInfo info) {
        switch (info.getName()) {
            case "Mounted":
                return "Whether or not this file system is mounted.";
            case "MountPoint":
                return "The mount point URI of this file system.";
            case "MountPointOfParent":
                return "The mount point URI of the parent file system.";
            case "SizeOfData":
                return "The data size of this file system.";
            case "SizeOfStorage":
                return "The storage size of this file system.";
            case "TimeCreatedDate":
                return "The time this file system has been created.";
            case "TimeCreatedMillis":
                return "The time this file system has been created in milliseconds.";
            case "TimeReadDate":
                return "The last time this file system has been read or accessed.";
            case "TimeReadMillis":
                return "The last time this file system has been read or accessed in milliseconds.";
            case "TimeWrittenDate":
                return "The last time this file system has been written.";
            case "TimeWrittenMillis":
                return "The last time this file system has been written in milliseconds.";
            default:
                return null;
        }
    }

    @Override
    protected String getDescription(final MBeanOperationInfo info) {
        return "sync".equals(info.getName())
                ? "Synchronizes this file system and all enclosed file systems and eventually unmounts them."
                : null;
    }

    @Override
    public boolean isMounted() {
        return model.isMounted();
    }

    @Override
    public String getMountPoint() {
        return model.getMountPoint().toString();
    }

    @Override
    public String getMountPointOfParent() {
        final Optional<? extends FsModel> parent = model.getParent();
        return parent.map(p -> p.getMountPoint().toString()).orElse(null);
    }

    @Override
    public long getSizeOfData() {
        return sizeOf(DATA);
    }

    @Override
    public long getSizeOfStorage() {
        return sizeOf(STORAGE);
    }

    private long sizeOf(Size type) {
        return node().getSize(type);
    }

    @Override
    public String getTimeCreatedDate() {
        final long time = node().getTime(CREATE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public Long getTimeCreatedMillis() {
        final long time = node().getTime(CREATE);
        return UNKNOWN == time ? null : time;
    }

    @Override
    public String getTimeReadDate() {
        final long time = node().getTime(READ);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public Long getTimeReadMillis() {
        final long time = node().getTime(READ);
        return UNKNOWN == time ? null : time;
    }

    @Override
    public String getTimeWrittenDate() {
        final long time = node().getTime(WRITE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public Long getTimeWrittenMillis() {
        final long time = node().getTime(WRITE);
        return UNKNOWN == time ? null : time;
    }

    protected FsNode node() {
        val mmp = model.getMountPoint();
        val opmp = mmp.getParent();
        assert opmp.isPresent() == mmp.getPath().isPresent();
        final FsMountPoint mp;
        final FsNodeName en;
        if (opmp.isPresent()) {
            mp = opmp.get();
            en = mmp.getPath().get().getNodeName();
        } else {
            mp = mmp;
            en = FsNodeName.ROOT;
        }
        Optional<? extends FsNode> on;
        try {
            on = FsManagerLocator
                    .SINGLETON
                    .get()
                    .controller(DRIVER, mp)
                    .node(FsAccessOptions.NONE, en);
        } catch (IOException ex) {
            on = Optional.empty();
        }
        if (on.isPresent()) {
            return on.get();
        }

        class DummyNode extends FsAbstractNode {

            @Override
            public String getName() {
                return en.toString();
            }

            @Override
            public BitField<Type> getTypes() {
                return Entry.NO_TYPES;
            }

            @Override
            public Set<String> getMembers() {
                return Collections.emptySet();
            }

            @Override
            public long getSize(Size type) {
                return UNKNOWN;
            }

            @Override
            public long getTime(Access type) {
                return UNKNOWN;
            }

            @Override
            public Boolean isPermitted(Access type, Entity entity) {
                return null;
            }
        }

        return new DummyNode();
    }

    @Override
    public void sync() throws FsSyncException {
        new FsSync().filter(FsControllerFilter.forPrefix(model.getMountPoint())).run();
    }
}
