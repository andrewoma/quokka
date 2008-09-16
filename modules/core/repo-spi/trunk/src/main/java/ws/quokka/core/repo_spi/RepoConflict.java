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


/**
 * RepoConflict is used to indicate that this artifact should conflict with versions of
 * other artifacts. This may occur if an artifact is renamed, or if the artifact is
 * an implmentation of
 */
public class RepoConflict extends AnnotatedObject {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    /**
     * Renamed, but version numbering maintained. e.g. sf.wicket => apache.wicket
     */
    public static final String RENAMED = "renamed";

    /**
     * Renamed, but version numbering reset
     */
    public static final String RENAMED_RESET = "renamed-reset";

    /**
     * Implementation of the same thing, e.g. Geronmino, Jetty & Sun implementation of the servlet API
     */
    public static final String EQUIVALENT = "equivalent";

    /**
     * Is a copy of the named artifact
     */
    public static final String ALIAS = "alias";

    /**
     * Contains a bundled version of the artifact, so versions are incompatible
     */
    public static final String BUNDLED = "bundled";

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private RepoArtifactId id;
    private String kind;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public RepoConflict(RepoArtifactId id, String kind) {
        this.id = id;
        this.kind = kind;
    }

    public RepoConflict() {
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public RepoArtifactId getId() {
        return id;
    }

    public void setId(RepoArtifactId id) {
        this.id = id;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        RepoConflict that = (RepoConflict)o;

        if ((id != null) ? (!id.equals(that.id)) : (that.id != null)) {
            return false;
        }

        if ((kind != null) ? (!kind.equals(that.kind)) : (that.kind != null)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = ((id != null) ? id.hashCode() : 0);
        result = (31 * result) + ((kind != null) ? kind.hashCode() : 0);

        return result;
    }

    /**
     * Ensures the object is valid. e.g. checks the kind is defined
     */
    public void validate() {
        Assert.isTrue((kind != null)
            && (
                kind.equals(RENAMED) || kind.equals(RENAMED_RESET) || kind.equals(EQUIVALENT) || kind.equals(ALIAS)
                || kind.equals(BUNDLED)
            ), "Invalid kind for conflict: " + kind);
    }
}
