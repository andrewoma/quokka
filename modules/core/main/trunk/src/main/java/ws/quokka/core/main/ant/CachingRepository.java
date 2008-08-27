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


package ws.quokka.core.main.ant;

import org.apache.tools.ant.Project;

import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_spi.RepositoryFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 *
 */
public class CachingRepository implements Repository {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Repository repository;
    private final Map cache = Collections.synchronizedMap(new HashMap());
    private Project project;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public CachingRepository(Project project, Repository repository) {
        this.project = project;
        this.repository = repository;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void initialise() {
    }

    public RepoArtifact resolve(RepoArtifactId artifactId) {
        return resolve(artifactId, true);
    }

    public RepoArtifact resolve(RepoArtifactId artifactId, boolean retrieveArtifact) {
        project.log("\nResolving: " + artifactId.toShortString(), Project.MSG_DEBUG);

        synchronized (cache) {
            RepoArtifact artifact = (RepoArtifact)cache.get(artifactId);

            if (artifact != null) {
                return artifact;
            }

            artifact = repository.resolve(artifactId, retrieveArtifact);

            if (retrieveArtifact || (artifact.getLocalCopy() != null)) {
                // Make sure cache doesn't contain artifacts without content
                // TODO: Add a separate cache for them ...
                cache.put(artifactId, artifact);
            }

            project.log("Resolved: " + artifact, Project.MSG_DEBUG);

            return artifact;
        }
    }

    public void install(RepoArtifact artifact) {
        repository.install(artifact);
    }

    public void remove(RepoArtifactId artifactId) {
        repository.remove(artifactId);
    }

    public Collection listArtifactIds(boolean includeReferenced) {
        return repository.listArtifactIds(false);
    }

    public boolean supportsReslove(RepoArtifactId artifactId) {
        return repository.supportsReslove(artifactId);
    }

    public boolean supportsInstall(RepoArtifactId artifactId) {
        return repository.supportsInstall(artifactId);
    }

    public void setFactory(RepositoryFactory factory) {
    }

    public RepositoryFactory getFactory() {
        return repository.getFactory();
    }

    public void setName(String name) {
    }

    public String getName() {
        return "Cached " + repository.getName();
    }

    public RepoArtifact updateSnapshot(RepoArtifact artifact) {
        return repository.updateSnapshot(artifact);
    }

    public Collection availableVersions(String group, String name, String type) {
        return repository.availableVersions(group, name, type);
    }

    public void rebuildCaches() {
        repository.rebuildCaches();
    }
}
