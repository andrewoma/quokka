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
import ws.quokka.core.version.VersionRangeUnion;


/**
 *
 */
public class Override extends AnnotatedObject {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    public static final String SCOPE_ALL = "all";

    // TODO: work out if these still make sense
    public static final String SCOPE_CONFLICT = "conflict";
    public static final String SCOPE_ALL_BUT_EXPLICIT = "allButExplicit";

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String group;
    private String name;
    private String type;
    private VersionRangeUnion versionRangeUnion;
    private Version with;
    private String scope = SCOPE_ALL;

    //~ Methods --------------------------------------------------------------------------------------------------------

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

    public VersionRangeUnion getVersionRangeUnion() {
        return versionRangeUnion;
    }

    public void setVersionRangeUnion(VersionRangeUnion versionRangeUnion) {
        this.versionRangeUnion = versionRangeUnion;
    }

    public Version getWith() {
        return with;
    }

    public void setWith(Version with) {
        this.with = with;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean matches(String scope, RepoArtifactId id) {
        return this.scope.equals(scope) && group.equals(id.getGroup()) && name.equals(id.getName())
        && type.equals(id.getType()) && versionRangeUnion.isInRange(id.getVersion());
    }
}
