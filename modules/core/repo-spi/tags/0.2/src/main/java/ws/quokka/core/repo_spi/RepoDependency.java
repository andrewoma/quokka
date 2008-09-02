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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * RepoDependency defines and dependency on another artifact.
 * The dependency may be assigned to multiple paths via path specifications.
 */
public class RepoDependency extends AnnotatedObject {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private RepoArtifactId id;
    private Set pathSpecs = new HashSet();

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Returns the id of the dependency
     */
    public RepoArtifactId getId() {
        return id;
    }

    /**
     * @see #getId()
     */
    public void setId(RepoArtifactId id) {
        this.id = id;
    }

    /**
     * Returns the read-only set of path specifications
     */
    public Set getPathSpecs() {
        return Collections.unmodifiableSet(pathSpecs);
    }

    public Set getPathSpecsTo(String toId) {
        Set specs = new HashSet();

        for (Iterator i = pathSpecs.iterator(); i.hasNext();) {
            RepoPathSpec pathSpec = (RepoPathSpec)i.next();

            if (pathSpec.getTo().equals(toId)) {
                specs.add(pathSpec);
            }
        }

        return specs;
    }

    /**
     * Adds a path specification (automatically setting dependency attribute of the path spec in the process)
     */
    public void addPathSpec(RepoPathSpec pathSpec) {
        pathSpec.setDependency(this);
        pathSpecs.add(pathSpec);
    }

    public String toShortString() {
        return (id == null) ? "null" : id.toShortString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        RepoDependency that = (RepoDependency)o;

        if ((id != null) ? (!id.equals(that.id)) : (that.id != null)) {
            return false;
        }

        return !((pathSpecs != null) ? (!pathSpecs.equals(that.pathSpecs)) : (that.pathSpecs != null));
    }

    public int hashCode() {
        int result;
        result = ((id != null) ? id.hashCode() : 0);
        result = (31 * result) + ((pathSpecs != null) ? pathSpecs.hashCode() : 0);

        return result;
    }
}
