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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


/**
 * MockRepository provides an in-memory mock implementation of a repository to ease testing
 * in modules that require repositories.
 */
public class MockRepository extends AbstractRepository {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Map artifacts = new HashMap();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public RepoArtifact resolve(RepoArtifactId id, boolean retrieveArtifact) {
        RepoArtifact artifact = (RepoArtifact)artifacts.get(id);

        if (artifact == null) {
            throw new UnresolvedArtifactException(id);
        }

        return artifact;
    }

    public void initialise() {
    }

    public void install(RepoArtifact artifact) {
        artifacts.put(artifact.getId(), artifact);
    }

    public void remove(RepoArtifactId artifactId) {
        artifacts.remove(artifactId);
    }

    public Collection listArtifactIds(boolean includeReferenced) {
        return new HashSet(artifacts.keySet());
    }

    public boolean supportsResolve(RepoArtifactId artifactId) {
        return true;
    }

    public boolean supportsInstall(RepoArtifactId artifactId) {
        return true;
    }

    public RepoArtifact updateSnapshot(RepoArtifact artifact) {
        return null;
    }

    public void rebuildCaches() {
    }
}
