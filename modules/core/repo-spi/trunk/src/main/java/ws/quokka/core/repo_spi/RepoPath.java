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


/**
 * RepoPath defines a path that dependencies can be assigned to. (Assignment is performed using path
 * specification on dependencies).
 */
public class RepoPath extends AnnotatedObject {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String id;
    private String description;
    private boolean descendDefault;
    private boolean mandatoryDefault;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public RepoPath() {
        this(null, null);
    }

    /**
     * Calls {@link #RepoPath(String, String, boolean, boolean)} with descendDefault and mandatoryDefault
     * set to true.
     */
    public RepoPath(String id, String description) {
        this(id, description, true, true);
    }

    /**
     * @param id the id, will be used in path specifications, so should be concise
     * @param description describes what the path is for
     * @param descendDefault if true, when a dependency is added to this path without modifiers transitive dependencies
     * will be included by default. Otherwise only the dependency specified will be included. This value can
     * be overridden when the dependency is added.
     * @param mandatoryDefault if true, when a dependency is added to this path it will be considered mandatory be default.
     * Otherwise it will be optional by default. This value can be overridden when the dependency is added.
     */
    public RepoPath(String id, String description, boolean descendDefault, boolean mandatoryDefault) {
        this.id = id;
        this.description = description;
        this.descendDefault = descendDefault;
        this.mandatoryDefault = mandatoryDefault;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDescendDefault() {
        return descendDefault;
    }

    public boolean isMandatoryDefault() {
        return mandatoryDefault;
    }

    public String toShortString() {
        return id;
    }

    public void setDescendDefault(boolean descendDefault) {
        this.descendDefault = descendDefault;
    }

    public void setMandatoryDefault(boolean mandatoryDefault) {
        this.mandatoryDefault = mandatoryDefault;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        RepoPath repoPath = (RepoPath)o;

        if (descendDefault != repoPath.descendDefault) {
            return false;
        }

        if (mandatoryDefault != repoPath.mandatoryDefault) {
            return false;
        }

        if ((description != null) ? (!description.equals(repoPath.description)) : (repoPath.description != null)) {
            return false;
        }

        return !((id != null) ? (!id.equals(repoPath.id)) : (repoPath.id != null));
    }

    public int hashCode() {
        int result;
        result = ((id != null) ? id.hashCode() : 0);
        result = (31 * result) + ((description != null) ? description.hashCode() : 0);
        result = (31 * result) + (descendDefault ? 1 : 0);
        result = (31 * result) + (mandatoryDefault ? 1 : 0);

        return result;
    }
}
