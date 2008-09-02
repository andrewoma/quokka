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


package ws.quokka.core.model;

import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.util.AnnotatedObject;
import ws.quokka.core.version.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 *
 */
public class Artifact extends AnnotatedObject {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private RepoArtifactId id;
    private List exportedPaths = new ArrayList();
    private String description;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public Artifact() {
    }

    public Artifact(RepoArtifactId id) {
        this.id = id;
    }

    /**
     * Convenience constructor, constructs and id and assigns it internally
     */
    public Artifact(String group, String name, String type, String version) {
        this(group, name, type, new Version(version));
    }

    public Artifact(String group, String name, String type, Version version) {
        id = new RepoArtifactId(group, name, type, version);
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(RepoArtifactId id) {
        this.id = id;
    }

    public RepoArtifactId getId() {
        return id;
    }

    public void addExportedPath(String pathId, String exportedId) {
        exportedPaths.add(new PathMapping(pathId, exportedId));
    }

    public List getExportedPaths() {
        return Collections.unmodifiableList(exportedPaths);
    }

    public String toShortString() {
        return id.toShortString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        Artifact artifact = (Artifact)o;

        return exportedPaths.equals(artifact.exportedPaths) && id.equals(artifact.id);
    }

    public int hashCode() {
        int result;
        result = id.hashCode();
        result = (31 * result) + exportedPaths.hashCode();

        return result;
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public static class PathMapping extends AnnotatedObject {
        private String from;
        private String to;

        public PathMapping(String from, String to) {
            this.from = from;
            this.to = to;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String toShortString() {
            return from + "=>" + to;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if ((o == null) || (getClass() != o.getClass())) {
                return false;
            }

            PathMapping that = (PathMapping)o;

            return from.equals(that.from) && to.equals(that.to);
        }

        public int hashCode() {
            int result;
            result = from.hashCode();
            result = (31 * result) + to.hashCode();

            return result;
        }
    }
}
