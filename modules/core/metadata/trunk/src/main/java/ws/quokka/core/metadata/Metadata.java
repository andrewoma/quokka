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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Metadata provides access to select parts of the project model for use in plugins.
 * It provides a layer between the actual model and plugins so that the underlying model can change
 * while still honouring the same Metadata interface
 */
public interface Metadata {
    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Returns a list of artifact ids for the given type
     */
    List getArtifactIds(String type);

    /**
     * Returns a list of artifact ids for all the types given
     */
    List getArtifactIds(List types);

    /**
     * Returns the artifact id for the specific name and type
     */
    RepoArtifactId getArtifactId(String name, String type);

    /**
     * Returns all of the artifact ids for this project
     */
    List getArtifactIds();

    /**
     * Returns a list of RepoArtifacts that correspond to the artifacts in this project.
     * They are suitable for installation into a repository (together with the actual artifact after packaging)
     */
    List getExportedArtifacts();

    /**
     * Returns a map of the paths that have been exported for a given artifact.
     * The key is project path id and the value is the exported path id
     */
    Map getExportedPaths(RepoArtifactId id);

    /**
     * Returns a map of licenses that are defined locally and require installation.
     * The key is the artifact id and the value is the license file
     */
    Map getLocalLicenses();

    /**
     * Returns the repository type for the given type id
     */
    RepoType getType(String id);

    /**
     * Returns a flattened project path as a list of RepoArtifacts
     */
    List getProjectPath(String id);

    /**
     * Returns a project path as a list of RepoArtifacts
     * @param mergeWithCore if true, the path will be merged with the quokka core. This is useful
     * if the path is going to be used in conjuction with the core class path
     * @param flatten if true, duplicates are removed
     */
    List getProjectPath(String id, boolean mergeWithCore, boolean flatten);

    /**
     * Returns true if the bootstrapping is define for this project and it is enabled
     */
    boolean hasReleasableBootStrapper();

    /**
     * Gets a merged set of dependencies for the project using the current project model (therefore will use the
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
     * @param includeLicenses if true, the licenses will be copied along with the artifacts
     */
    void copyPath(List path, File destination, String pattern, boolean includeLicenses);

    /**
     * Adds an artifact to the project
     * @param id
     * @param description
     * @param exportedPaths a map of paths to export with this artifact, the key is the project path id
     * and the value is the exported path id
     */
    void addArtifact(RepoArtifactId id, String description, Map exportedPaths);
}
