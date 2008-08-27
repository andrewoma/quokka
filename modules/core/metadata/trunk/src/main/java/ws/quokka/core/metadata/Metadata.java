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


package ws.quokka.core.metadata;

import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoType;

import java.io.File;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *
 */
public interface Metadata {
    //~ Methods --------------------------------------------------------------------------------------------------------

    List getArtifactIds(String type);

    RepoArtifactId getArtifactId(String name, String type);

    List getArtifactIds();

    List getExportedArtifacts();

    Map getLocalLicenses();

    RepoType getType(String id);

    List getProjectPath(String id);

    List getProjectPath(String id, boolean mergeWithCore, boolean flatten);

    Map getExportedPaths(RepoArtifactId id);

    boolean hasReleasableBootStrapper();

    /**
     * Gets a merged set of dependencies for the project using the current project model (therefore will the
     * currently active profiles).
     */
    Set getDependencies();

    /**
     * Gets a merged set of dependencies for the project using the profiles specified, or all
     * profiles if null is specified.
     */
    Set getDependencies(Set profiles);

    /**
     * Rolls over the version of the project in the project file. It attempts to change the version text
     * only without modifying the rest of the xml file (i.e. without reformatting). Also not that it does not
     * does not affect the running instance's version, only the file.
     */
    void rolloverVersion(String version);

    /**
     * Translates an id into a string, using the pattern supplied.
     * e.g. #{group}_#{name}_#{type}_#{version}.#{extension}
     */
    String translate(RepoArtifactId id, String pattern);

    /**
     * Copies a path to a destination
     * @param path a list of RepoArtifacts
     * @param pattern as per translate above, may be null then defaults to #{group}_#{name}_#{type}_#{version}.#{extension}
     */
    void copyPath(List path, File destination, String pattern);

    /**
     * Adds an artifact to the project
     * @param id
     * @param description
     * @param exportedPaths a map of paths to export with this artifact, the key is the project path id
     * and the value is the exported path id
     */
    void addArtifact(RepoArtifactId id, String description, Map exportedPaths);
}
