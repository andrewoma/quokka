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


package ws.quokka.core.bootstrap.resources;

import org.apache.tools.ant.BuildException;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.version.Version;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;


/**
 *
 */
public class DependencyResource {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String group;
    private String name;
    private Version version;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public DependencyResource(String group, String name, Version version) {
        this.group = group;
        this.name = name;
        this.version = version;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public Version getVersion() {
        return version;
    }

    public String toFileName() {
        return group + "_" + version + "_" + name + "_jar.jar";
    }

    public static DependencyResource parse(String artifactId) {
        StringTokenizer tokenizer = new StringTokenizer(artifactId, "_");
        DependencyResource resource;

        try {
            String group = tokenizer.nextToken();
            String version = tokenizer.nextToken();
            String name = tokenizer.nextToken();
            resource = new DependencyResource(group, name, Version.parse(version));
        } catch (NoSuchElementException e) {
            throw new BuildException("Too few tokens in artifact id: " + artifactId);
        }

        return resource;
    }

    public String toString() {
        return group + ":" + name + ":" + version;
    }
}
