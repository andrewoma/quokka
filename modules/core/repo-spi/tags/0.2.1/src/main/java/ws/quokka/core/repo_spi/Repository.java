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
 *
 */
public interface Repository {
    //~ Methods --------------------------------------------------------------------------------------------------------

    void initialise();

    void setFactory(RepositoryFactory factory);

    RepositoryFactory getFactory();

    RepoArtifact resolve(RepoArtifactId artifactId);

    RepoArtifact resolve(RepoArtifactId artifactId, boolean retrieveArtifact);

    void install(RepoArtifact artifact);

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

    boolean supportsReslove(RepoArtifactId artifactId);

    boolean supportsInstall(RepoArtifactId artifactId);

    String getName();

    void setName(String name);

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
