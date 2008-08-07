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


package ws.quokka.core.bootstrap.constraints;

import ws.quokka.core.bootstrap.resources.DependencyResource;
import ws.quokka.core.version.VersionRangeUnion;


/**
 *
 */
public class DependencyConstraint {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String group;
    private String name;
    private VersionRangeUnion version;
    private String file;
    private String url;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public DependencyConstraint(String group, String name, VersionRangeUnion version, String file, String url) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.file = file;
        this.url = url;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public VersionRangeUnion getVersion() {
        return version;
    }

    public boolean matches(DependencyResource resource) {
        return group.equals(resource.getGroup()) && name.equals(resource.getName())
        && version.isInRange(resource.getVersion());
    }

    public String toString() {
        return group + ":" + name + ":" + version;
    }

    public String getFile() {
        return file;
    }

    public String getUrl() {
        return url;
    }
}
