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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
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

    public Set getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    public void addDependency(RepoDependency dependency) {
        dependencies.add(dependency);
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
     * Returns a shallow copy, with the exception of annotations, which is cloned.
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

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        RepoArtifact artifact = (RepoArtifact)o;

        if ((dependencies != null) ? (!dependencies.equals(artifact.dependencies)) : (artifact.dependencies != null)) {
            return false;
        }

        if ((id != null) ? (!id.equals(artifact.id)) : (artifact.id != null)) {
            return false;
        }

        if ((localCopy != null) ? (!localCopy.equals(artifact.localCopy)) : (artifact.localCopy != null)) {
            return false;
        }

        return !((paths != null) ? (!paths.equals(artifact.paths)) : (artifact.paths != null));
    }

    public int hashCode() {
        int result;
        result = ((id != null) ? id.hashCode() : 0);
        result = (31 * result) + ((dependencies != null) ? dependencies.hashCode() : 0);
        result = (31 * result) + ((localCopy != null) ? localCopy.hashCode() : 0);
        result = (31 * result) + ((paths != null) ? paths.hashCode() : 0);

        return result;
    }
}
