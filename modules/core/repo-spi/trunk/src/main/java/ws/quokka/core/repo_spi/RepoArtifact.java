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

import ws.quokka.core.util.AnnotatedObject;
import ws.quokka.core.util.Annotations;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 *
 */
public class RepoArtifact extends AnnotatedObject implements Cloneable {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private RepoArtifactId id;
    private Set dependencies = new HashSet();
    private File localCopy;
    private Set paths = new HashSet();
    private RepoArtifactId originalId; // The original group, name & type. Should be set if the artifact is renamed
    private List overrides = new ArrayList();
    private String description;
    private Date timestamp; // Only set for snapshots to allow updates from parents
    private String importedFrom;
    private boolean stub = false;
    private Set licenses = new HashSet();
    private String hash; // Optional MD5 hash encoded as a hex string (lowercase)

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public RepoArtifact() {
    }

    public RepoArtifact(RepoArtifactId id) {
        this.id = id;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void setId(RepoArtifactId id) {
        this.id = id;
    }

    public RepoArtifactId getId() {
        return id;
    }

    /**
     * The original id should contain the original group, name and type of the artifact when it was
     * first put in the repository. This should be set when an artifact is renamed. e.g. migrates from
     * sourceforge to it's own domain. This allows conflict resolution to detect what are essentially
     * different versions of the same artifact, albeit with different groups, names and/or types.
     */
    public RepoArtifactId getOriginalId() {
        return originalId;
    }

    public void setOriginalId(RepoArtifactId originalId) {
        this.originalId = originalId;
    }

    public void addOverride(RepoOverride override) {
        overrides.add(override);
    }

    public List getOverrides() {
        return Collections.unmodifiableList(overrides);
    }

    public Set getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    public void addDependency(RepoDependency dependency) {
        dependencies.add(dependency);
    }

    public boolean isStub() {
        return stub;
    }

    public void setStub(boolean stub) {
        this.stub = stub;
    }

    public void addLicense(RepoArtifactId id) {
        licenses.add(id);
    }

    public Set getLicenses() {
        return licenses;
    }

    public Set getPaths() {
        return paths;
    }

    public RepoPath getPath(String id) {
        for (Iterator i = paths.iterator(); i.hasNext();) {
            RepoPath path = (RepoPath)i.next();

            if (path.getId().equals(id)) {
                return path;
            }
        }

        return null;
    }

    public void addPath(RepoPath path) {
        paths.add(path);
    }

    public File getLocalCopy() {
        return localCopy;
    }

    public void setLocalCopy(File localCopy) {
        this.localCopy = localCopy;
    }

    public String toShortString() {
        return id.toShortString();
    }

    /**
     * Returns a shallow copy, with the exception of annotations and id, which are cloned.
     */
    public Object clone() {
        try {
            RepoArtifact clone = (RepoArtifact)super.clone();
            clone.setAnnotations((Annotations)clone.getAnnotations().clone());
            clone.setId((RepoArtifactId)clone.getId().clone());

            return clone;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace(); // Never happens

            return null;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getImportedFrom() {
        return importedFrom;
    }

    public void setImportedFrom(String importedFrom) {
        this.importedFrom = importedFrom;
    }

    public boolean equals(Object o) {
        // Note: localCopy, hash and annotations are excluded from equality testing
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        RepoArtifact artifact = (RepoArtifact)o;

        if (stub != artifact.stub) {
            return false;
        }

        if ((dependencies != null) ? (!dependencies.equals(artifact.dependencies)) : (artifact.dependencies != null)) {
            return false;
        }

        if ((description != null) ? (!description.equals(artifact.description)) : (artifact.description != null)) {
            return false;
        }

        if ((id != null) ? (!id.equals(artifact.id)) : (artifact.id != null)) {
            return false;
        }

        if ((importedFrom != null) ? (!importedFrom.equals(artifact.importedFrom)) : (artifact.importedFrom != null)) {
            return false;
        }

        if ((licenses != null) ? (!licenses.equals(artifact.licenses)) : (artifact.licenses != null)) {
            return false;
        }

        if ((originalId != null) ? (!originalId.equals(artifact.originalId)) : (artifact.originalId != null)) {
            return false;
        }

        if ((overrides != null) ? (!overrides.equals(artifact.overrides)) : (artifact.overrides != null)) {
            return false;
        }

        if ((paths != null) ? (!paths.equals(artifact.paths)) : (artifact.paths != null)) {
            return false;
        }

        if ((timestamp != null) ? (!timestamp.equals(artifact.timestamp)) : (artifact.timestamp != null)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = ((id != null) ? id.hashCode() : 0);
        result = (31 * result) + ((dependencies != null) ? dependencies.hashCode() : 0);
        result = (31 * result) + ((paths != null) ? paths.hashCode() : 0);
        result = (31 * result) + ((originalId != null) ? originalId.hashCode() : 0);
        result = (31 * result) + ((overrides != null) ? overrides.hashCode() : 0);
        result = (31 * result) + ((description != null) ? description.hashCode() : 0);
        result = (31 * result) + ((timestamp != null) ? timestamp.hashCode() : 0);
        result = (31 * result) + ((importedFrom != null) ? importedFrom.hashCode() : 0);
        result = (31 * result) + (stub ? 1 : 0);
        result = (31 * result) + ((licenses != null) ? licenses.hashCode() : 0);

        return result;
    }

    public boolean isNewerThan(RepoArtifact other) {
        // Comparison shouldn't need to handle nulls, but is useful during migration
        Date thisDate = (timestamp != null) ? timestamp : new Date(0);
        Date otherDate = (other.timestamp != null) ? other.timestamp : new Date(0);

        return thisDate.compareTo(otherDate) > 0;
    }

    /**
     * Optional field for some repository types. In general, if you are not returning the actual artifact
     * you should set the hash. At present this is done by UrlRepositories when listing remote artifacts
     */
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
