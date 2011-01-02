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
package de.schlichtherle.truezip.io.filesystem;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static de.schlichtherle.truezip.util.Link.Type.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FilterFileSystemManagerTest extends FileSystemManagerTestCase {

    @Override
    public void setUp() {
        manager = new FilterFileSystemManager(
                new FederatedFileSystemManager(),
                MountPoint.create("file:/"));
    }

    @Test
    public void testFiltering() {
        for (final String[][] params : new String[][][] {
            { { "tar:zip:file:/foo!/bar!/" }, { "zip:file:/foo!/" }, { } },
            { { "file:/foo/bar/" }, { "zip:file:/foo!/" }, { } },
            { { "tar:file:/foo!/" }, { "zip:file:/foo!/" }, { "zip:file:/foo!/" } },
            { { "zip:file:/foo!/" }, { "zip:file:/foo!/" }, { "zip:file:/foo!/" } },
            { { "file:/foo/" }, { "zip:file:/foo!/" }, { "zip:file:/foo!/" } },
            { { "file:/" }, { "zip:file:/foo!/" }, { "zip:file:/foo!/" } },
        }) {
            assert params[0].length == 1;

            final FileSystemManager manager = new FederatedFileSystemManager(
                    STRONG);
            for (final String param : params[1])
                manager.getController(MountPoint.create(param), driver);
            assertThat(manager.getSize(), is(params[1].length));

            final Set<MountPoint> set = new HashSet<MountPoint>();
            for (final String param : params[2])
                set.add(MountPoint.create(param));

            final FileSystemManager filter = new FilterFileSystemManager(
                    manager, MountPoint.create(params[0][0]));
            assertThat(filter.getSize(), is(params[2].length));
            for (final FileSystemController<?> controller : filter)
                assertTrue(set.contains(controller.getModel().getMountPoint()));
        }
    }
}
