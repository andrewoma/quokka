/*
 * Copyright 2007-2008 Andrew O'Malley
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


package ws.quokka.core.repo_spi;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.util.AnnotatedObject;
import ws.quokka.core.version.Version;
import ws.quokka.core.version.VersionRangeUnion;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 *
 */
public class RepoOverride extends AnnotatedObject {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Set paths = new HashSet();
    private String group;
    private String name;
    private String type;
    private VersionRangeUnion version;
    private Version withVersion;
    private Set withPathSpecs = new HashSet();

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public RepoOverride(Set paths, String group, String name, String type, VersionRangeUnion version,
        Version withVersion) {
        this(paths, group, name, type, version, withVersion, new HashSet());
    }

    public RepoOverride(Set paths, String group, String name, String type, VersionRangeUnion version,
        Version withVersion, Set withPathSpecs) {
        this.paths = paths;
        this.group = group;
        this.name = name;
        this.type = type;
        this.version = version;
        this.withVersion = withVersion;
        this.withPathSpecs.addAll(withPathSpecs); // In case source is immutable
    }

    public RepoOverride() {
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public Set getWithPathSpecs() {
        return Collections.unmodifiableSet(withPathSpecs);
    }

    public void addWithPathSpec(RepoPathSpec pathSpec) {
        withPathSpecs.add(pathSpec);
    }

    public Set getPaths() {
        return Collections.unmodifiableSet(paths);
    }

    public void addPath(String path) {
        paths.add(path);
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public VersionRangeUnion getVersion() {
        return version;
    }

    public void setVersion(VersionRangeUnion version) {
        this.version = version;
    }

    public Version getWithVersion() {
        return withVersion;
    }

    public void setWithVersion(Version withVersion) {
        this.withVersion = withVersion;
    }

    public boolean matches(RepoArtifactId id) {
        return ((group == null) || group.equals(id.getGroup())) && ((name == null) || name.equals(id.getName()))
        && ((type == null) || type.equals(id.getType())) && ((version == null) || version.isInRange(id.getVersion()));
    }

    public boolean matches(String pathId) {
        return paths.contains(pathId) || paths.contains("*");
    }

    public RepoPathSpec getOverridden(RepoPathSpec spec) {
        for (Iterator i = withPathSpecs.iterator(); i.hasNext();) {
            RepoPathSpec override = (RepoPathSpec)i.next();

            if (nsEquals(override.getFrom(), spec.getFrom())) {
                return override;
            }
        }

        return null;
    }

    private boolean nsEquals(Object lhs, Object rhs) {
        return (lhs == null) ? (rhs == null) : lhs.equals(rhs);
    }

    public boolean equalsExcludingPaths(RepoOverride other) {
        return nsEquals(group, other.group) && nsEquals(name, other.name) && nsEquals(type, other.type)
        && nsEquals(version, other.version) && nsEquals(withVersion, other.withVersion)
        && nsEquals(withPathSpecs, other.withPathSpecs);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        RepoOverride other = (RepoOverride)o;

        return nsEquals(group, other.group) && nsEquals(name, other.name) && nsEquals(type, other.type)
        && nsEquals(version, other.version) && nsEquals(withVersion, other.withVersion)
        && nsEquals(withPathSpecs, other.withPathSpecs) && nsEquals(paths, other.paths);
    }

    public int hashCode() {
        int result;
        result = ((paths != null) ? paths.hashCode() : 0);
        result = (31 * result) + ((group != null) ? group.hashCode() : 0);
        result = (31 * result) + ((name != null) ? name.hashCode() : 0);
        result = (31 * result) + ((type != null) ? type.hashCode() : 0);
        result = (31 * result) + ((version != null) ? version.hashCode() : 0);
        result = (31 * result) + ((withVersion != null) ? withVersion.hashCode() : 0);
        result = (31 * result) + ((withPathSpecs != null) ? withPathSpecs.hashCode() : 0);

        return result;
    }

    public void validate() {
        Assert.isTrue(!((withVersion == null) && (withPathSpecs.size() == 0)), getLocator(),
            "Either with or with-paths must be set");

        Assert.isTrue((withVersion == null) || ((group != null) && (name != null)), getLocator(),
            "Group and name must be set if with is set");

        boolean singleSpecWithoutOptions = (withPathSpecs.size() == 1)
            && (((RepoPathSpec)withPathSpecs.iterator().next()).getOptions() == null);

        Assert.isTrue((group != null) || singleSpecWithoutOptions, getLocator(),
            "Group must be set unless a single path spec without options is specified");

        // TODO ... encode valid rules to prevent user from hanging themselves
    }
}
