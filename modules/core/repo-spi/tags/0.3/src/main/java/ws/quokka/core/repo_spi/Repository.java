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

import java.util.Collection;


/**
 * Repository defines both the interface for implementing a Repository and the API for
 * accessing it. A Repository instance will be created by the {@link RepositoryFactory}
 * implementation. The factory will create an instance of the repository implementation
 * and call {@link #setName(String)} and {@link #setFactory(RepositoryFactory)}, followed
 * by a call to {@link #initialise()}. The repository instance should then be ready to use.
 *
 */
public interface Repository {
    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Called by {@link ws.quokka.core.repo_spi.RepositoryFactory} on creation - should not
     * be used by clients such as plugins.
     */
    void initialise();

    /**
     * Called by {@link ws.quokka.core.repo_spi.RepositoryFactory} prior to {@link #initialise()}  - should not
     * be used by clients such as plugins.
     */
    void setFactory(RepositoryFactory factory);

    /**
     * Called by {@link ws.quokka.core.repo_spi.RepositoryFactory} prior to {@link #initialise()}  - should not
     * be used by clients such as plugins.
     */
    void setName(String name);

    /**
     * Returns the factory that created this repository
     */
    RepositoryFactory getFactory();

    /**
     * Resolves an artifact from the repository. Equivalent to {@link #resolve(RepoArtifactId, boolean)}
     * with the retrieveArtifact set to true.
     */
    RepoArtifact resolve(RepoArtifactId artifactId);

    /**
     * Resolves an artifact from the repository.
     * @param retrieveArtifact if true, the artifact will be retrieved so that it is accessible as a file
     * on the local file system. If false, if the artifact does not exist on the local file system, it will
     * not be retrieved. In this instance, most implementations should return a hash of the remote artifact
     * retrievable via {@link RepoArtifact#getHash()}
     */
    RepoArtifact resolve(RepoArtifactId artifactId, boolean retrieveArtifact);

    /**
     * Installs the artifact into the repository
     */
    void install(RepoArtifact artifact);

    /**
     * Removes the artifact from the repository. Note: the artifact will only be removed from this repository
     * instance. If there artifact is available in other repositories referenced by this repository, the
     * artifacts in other repositories will NOT be removed.
     */
    void remove(RepoArtifactId artifactId);

    /**
     * List artifact ids that are resolvable from this repository.
     * @param includeReferenced if true, the ids will include artifacts that may exist in other repositories
     * referenced by this one.
     */
    Collection listArtifactIds(boolean includeReferenced);

    /**
     * A convenience methods to return all artifact ids that match the given group, name
     * and type. This method may result in performance improvement over {@link #listArtifactIds(boolean)}
     * depending on the underlying implementation.
     * @param group If not null, the group must match, otherwise any group will match
     * @param name If not null, the name must match, otherwise any name will match
     * @param type If not null, the type must match, otherwise any type with match
     * @param includeReferenced if true, the ids will include artifacts that may exist in other repositories
     * referenced by this one.
     */
    Collection listArtifactIds(String group, String name, String type, boolean includeReferenced);

    /**
     * Returns true if this repository can resolve this artifact. Note: this does not mean that artifact
     * exists in the repository, only that it conceivable could exist. e.g. If the repository supports
     * snapshots, it would return true for any snapshot id regardless of whether it exists in the repository.
     */
    boolean supportsResolve(RepoArtifactId artifactId);

    /**
     * Returns true if the repository can install this artifact. e.g. it would return true if the
     * repository stores snapshots and the id given has a snapshot version.
     */
    boolean supportsInstall(RepoArtifactId artifactId);

    /**
     * Returns the name of the repository
     */
    String getName();

    /**
     * Update the snapshot to the latest version available in this repository or any parents
     *
     * @param artifact the snapshot artifact to update
     * @return the updated snapshot, or null if there are no newer artifacts available. If should NOT
     *         throw UnresolvedArtifactException
     */
    RepoArtifact updateSnapshot(RepoArtifact artifact);

    /**
     * If the repository uses any form of caching internally, this should force it to rebuild them.
     * e.g. IndexedRepositories will rebuild the index, BundledRepositories will re-extract their bundles
     * and UrlRepositories will download their indexes again.
     */
    void rebuildCaches();
}
