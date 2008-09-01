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
 * RepoArtifact represents a repository artifact, including metadata about dependencies, paths
 * and licenses
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

    /**
     * @see #getId()
     */
    public void setId(RepoArtifactId id) {
        this.id = id;
    }

    /**
     * Returns the artifact id of this artifact
     */
    public RepoArtifactId getId() {
        return id;
    }

    /**
     * The original id should contain the original group, name and type (and possibly version) of the artifact when it was
     * first put in the repository. This should be set when an artifact is renamed. e.g. migrates from
     * sourceforge to it's own domain. This allows conflict resolution to detect what are essentially
     * different versions of the same artifact, albeit with different groups, names and/or types.
     */
    public RepoArtifactId getOriginalId() {
        return originalId;
    }

    /**
     * @see #getOriginalId()
     */
    public void setOriginalId(RepoArtifactId originalId) {
        this.originalId = originalId;
    }

    /**
     * Adds an override
     */
    public void addOverride(RepoOverride override) {
        overrides.add(override);
    }

    /**
     * Returns a read-only list of overrides
     */
    public List getOverrides() {
        return Collections.unmodifiableList(overrides);
    }

    /**
     * Returns a read-only set of dependencies
     */
    public Set getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    /**
     * Adds a dependency
     */
    public void addDependency(RepoDependency dependency) {
        dependencies.add(dependency);
    }

    /**
     * Returns true if this is a stub. Stub should be set to true when the actual artifact cannot be
     * stored in the repository due to licensing restrictions. It is important to mark stub so that
     * verification of repositories can tell the difference between a broken repository and one
     * left intentionally empty.
     */
    public boolean isStub() {
        return stub;
    }

    /**
     * @see #isStub()
     */
    public void setStub(boolean stub) {
        this.stub = stub;
    }

    /**
     * Adds a license to the artifact (licenses are also stored in the repository with a type of 'license')
     */
    public void addLicense(RepoArtifactId id) {
        licenses.add(id);
    }

    /**
     * Returns a read-only set of licenses
     */
    public Set getLicenses() {
        return Collections.unmodifiableSet(licenses);
    }

    /**
     * Returns a read-only set of paths
     */
    public Set getPaths() {
        return Collections.unmodifiableSet(paths);
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

    /**
     * Adds a path to the artifact
     */
    public void addPath(RepoPath path) {
        paths.add(path);
    }

    /**
     * Returns the local copy of the artifact. This may be null if it is a stub or an artifact of
     * type 'paths'.
     */
    public File getLocalCopy() {
        return localCopy;
    }

    /**
     * @see #getLocalCopy()
     */
    public void setLocalCopy(File localCopy) {
        this.localCopy = localCopy;
    }

    /**
     * Returns a short description of this artifact, currently just the id
     */
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

    /**
     * Returns the description. Like javadoc comments, the first sentence of the description should be
     * a meaningful description that can be used on summary displays.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @see #getDescription()
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the date the artifact was installed in the repository if it is a snapshot artifact, or null otherwise
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * @see #getTimestamp()
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns a string describing where this artifact was imported from. It has no effect on the general
     * operation of quokka and is only used as a mechanism to verify a quokka artifact against the source
     * it was imported from. Currently, it is set when artifacts are imported using quokka.maven:import
     * and read via quokka.maven:verify
     */
    public String getImportedFrom() {
        return importedFrom;
    }

    /**
     * @see #getImportedFrom()
     */
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

    /**
     * Returns true if this artifact is newer than the one given according to timestamps
     */
    public boolean isNewerThan(RepoArtifact other) {
        // Comparison shouldn't need to handle nulls, but is useful during migration
        Date thisDate = (timestamp != null) ? timestamp : new Date(0);
        Date otherDate = (other.timestamp != null) ? other.timestamp : new Date(0);

        return thisDate.compareTo(otherDate) > 0;
    }

    /**
     * Optional field for some repository types. In general, if you are not returning the actual artifact
     * you should set the hash (a MD5 hash converted to a hex string).
     * At present this is done by UrlRepositories when listing remote artifacts
     */
    public String getHash() {
        return hash;
    }

    /**
     * @see #getHash()
     */
    public void setHash(String hash) {
        this.hash = hash;
    }
}
