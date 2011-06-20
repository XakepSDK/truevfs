/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.nio.file;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.fs.FsEntryName;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.io.Paths;
import static de.schlichtherle.truezip.fs.FsUriModifier.*;
import de.schlichtherle.truezip.io.Paths.Splitter;
import de.schlichtherle.truezip.util.QuotedInputUriSyntaxException;
import de.schlichtherle.truezip.util.UriBuilder;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.NotThreadSafe;

/**
 * Scans {@link URI}s for prospective archive files with the help of a
 * {@link TArchiveDetector}.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
final class TUriScanner {
    private static final String DOT_DOT_SEPARATOR = ".." + SEPARATOR_CHAR;
    private static final URI DOT = URI.create(".");
    private static final URI DOT_DOT = URI.create("..");

    private final TArchiveDetector detector;
    private final Splitter splitter = new Splitter(SEPARATOR_CHAR, false);
    private FsPath root;
    private String memberQuery;
    private final UriBuilder uri = new UriBuilder();

    /**
     * Constructs a new URI scanner which uses the given
     * {@link TArchiveDetector}.
     * 
     * @param detector the archive detector to use.
     */
    TUriScanner(TArchiveDetector detector) {
        assert null != detector;
        this.detector = detector;
    }

    /**
     * Constructs a new {@link FsPath} from the given {@code parent} and scans
     * the given {@code member} for prospective archive files.
     * <p>
     * {@code member} must not be opaque and must not define a fragment
     * component.
     * A scheme component gets ignored.
     * If an authority component or an absolute path is present, the authority
     * and path components of {@code parent} get discarded.
     * An authority component gets copied to the result.
     * A path component gets normalized and scanned for prospective archive
     * files using the {@link TArchiveDetector} provided to the constructor and
     * rewritten according to the syntax constraints for an {@link FsPath}.
     * {@code ".."} segments at the beginning of the normalized path component
     * are resolved against the scheme specific part of {@code parent}
     * according to the syntax constraints for an {@link FsPath}.
     * A query component is copied to the result.
     * 
     * @param  parent the file system path to use as the parent.
     * @param  member the URI to scan for prospective archive files.
     * @return the file system path combined from the given {@code parent} and
     *         {@code member}, possibly decorated as an opaque URI to address
     *         prospective archive files.
     * @throws IllegalArgumentException if any precondition is violated.
     */
    FsPath resolve(FsPath parent, URI member) {
        try {
            if (member.isOpaque())
                throw new QuotedInputUriSyntaxException(member, "Opaque URI");
            if (null != member.getFragment())
                throw new QuotedInputUriSyntaxException(member, "Fragment component defined");
            member = member.normalize();
            String mp;
            while ((mp = member.getPath()).startsWith(DOT_DOT_SEPARATOR)) {
                parent = parent(parent);
                member = new UriBuilder(member)
                        .path(mp.substring(3))
                        .getUri();
            }
            if ("..".equals(mp))
                return parent(parent);
            final URI pu = parent.toUri();
            String mssp = member.getSchemeSpecificPart();
            int mpl = Paths.prefixLength(mssp, SEPARATOR_CHAR, true);
            if (0 < mpl) {
                String ma = member.getAuthority();
                if (null != ma)
                    mpl -= 2 + ma.length();
                this.root = new FsPath(
                        new UriBuilder()
                            .scheme(pu.getScheme())
                            .authority(ma)
                            .path(mp.substring(0, mpl))
                            .getUri());
                mp = mp.substring(mpl);
            } else {
                this.root = parent;
            }
            this.memberQuery = member.getQuery();
            return scan(mp);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Returns the parent file system path for the given path.
     * 
     * @param  path a file system path.
     * @return The parent file system path.
     * @throws URISyntaxException 
     */
    static FsPath parent(FsPath path)
    throws URISyntaxException {
        while (true) {
            FsMountPoint pmp = path.getMountPoint();
            FsEntryName  pen = path.getEntryName();
            if (pen.isRoot()) {
                if (null == pmp)
                    throw new QuotedInputUriSyntaxException(path,
                            "An empty path has no parent.");
                path = pmp.getPath();
                if (null == path)
                    return new FsPath(pmp.toUri().resolve(DOT_DOT));
                continue;
            } else {
                pen = new FsEntryName(pen.toUri().resolve(DOT), CANONICALIZE);
                /*if (pen.isRoot() && null != pmp) {
                    path = pmp.getPath();
                    if (null == path)
                        return new FsPath(pmp.toUri(), CANONICALIZE);
                }*/
                return new FsPath(pmp, pen);
            }
        }
    }

    private FsPath scan(final String path) throws URISyntaxException {
        splitter.split(path);
        final String ps = splitter.getParentPath();
        final FsEntryName men;
        final FsPath pp;
        if (null != ps) {
            men = FsEntryName.create(
                    uri.path(splitter.getMemberName()).getUri(),
                    NULL);
            pp = scan(ps);
        } else {
            men = FsEntryName.create(
                    uri.path(path).query(memberQuery).getUri(),
                    CANONICALIZE);
            pp = root;
        }
        URI ppu;
        FsPath mp;
        if (men.isRoot() || (ppu = pp.toUri()).isOpaque() || !ppu.isAbsolute()) {
            mp = pp.resolve(men);
        } else {
            final String pup = ppu.getPath();
            if (!pup.endsWith(SEPARATOR))
                ppu = new UriBuilder(ppu).path(pup + SEPARATOR_CHAR).getUri();
            mp = new FsPath(FsMountPoint.create(ppu), men);
        }
        final FsScheme s = detector.getScheme(men.toString());
        if (null != s)
            mp = new FsPath(FsMountPoint.create(s, mp), ROOT);
        return mp;
    }
}
