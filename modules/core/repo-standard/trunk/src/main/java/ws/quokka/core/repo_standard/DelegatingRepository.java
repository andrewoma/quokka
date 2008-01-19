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


package ws.quokka.core.repo_standard;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoType;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_spi.UnresolvedArtifactException;
import ws.quokka.core.util.AnnotatedProperties;
import ws.quokka.core.util.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 *
 */
public class DelegatingRepository extends AbstractStandardRepository {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private List repositories = new ArrayList();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void initialise(Object antProject, AnnotatedProperties properties) {
        setName("delegating");
        super.initialise(antProject, properties);

        List repoNames = new ArrayList();
        String roots = properties.getProperty("quokka.project.repoRoots",
                properties.getProperty("quokka.global.repoRoots", null));
        Assert.isTrue(roots != null,
            "quokka.project.repoRoots and/or quokka.global.repoRoots must be set to specify the root repositories");
        repoNames.addAll(Strings.commaSepList(roots));

        Assert.isTrue(getParents().size() == 0, "Parents should not be specified for DelegatingRepository");

        for (Iterator i = repoNames.iterator(); i.hasNext();) {
            String name = (String)i.next();
            Repository repository = create(name);

            if (repository instanceof AbstractStandardRepository) {
                ((AbstractStandardRepository)repository).setName(name);
            }

            repository.initialise(antProject, properties);
            repositories.add(repository);
        }
    }

    public void registerType(RepoType type) {
        for (Iterator i = repositories.iterator(); i.hasNext();) {
            Repository repository = (Repository)i.next();
            repository.registerType(type);
        }
    }

    public RepoType getType(String id) {
        return ((Repository)repositories.iterator().next()).getType(id);
    }

    public RepoArtifact resolve(RepoArtifactId artifactId) {
        List urls = new ArrayList();

        for (Iterator i = repositories.iterator(); i.hasNext();) {
            Repository repository = (Repository)i.next();

            if (repository.supportsReslove(artifactId)) {
                try {
                    return repository.resolve(artifactId);
                } catch (UnresolvedArtifactException e) {
                    // Ignore ... try other repos
                    urls.addAll(e.getUrls());
                }
            }
        }

        throw new UnresolvedArtifactException(artifactId, urls);
    }

    public void install(RepoArtifact artifact) {
        for (Iterator i = repositories.iterator(); i.hasNext();) {
            Repository repository = (Repository)i.next();

            if (repository.supportsInstall(artifact.getId())) {
                repository.install(artifact);

                break;
            }
        }
    }

    public void remove(RepoArtifactId artifactId) {
        throw new UnsupportedOperationException("Remove is not supported for the DelegatingRepository");
    }

    public Collection listArtifactIds() {
        Set ids = new HashSet();

        for (Iterator i = repositories.iterator(); i.hasNext();) {
            Repository repository = (Repository)i.next();
            ids.addAll(repository.listArtifactIds());
        }

        return ids;
    }

    public boolean supportsInstall(RepoArtifactId artifactId) {
        for (Iterator i = repositories.iterator(); i.hasNext();) {
            Repository repository = (Repository)i.next();

            if (repository.supportsInstall(artifactId)) {
                return true;
            }
        }

        return false;
    }

    public boolean supportsReslove(RepoArtifactId artifactId) {
        for (Iterator i = repositories.iterator(); i.hasNext();) {
            Repository repository = (Repository)i.next();

            if (repository.supportsReslove(artifactId)) {
                return true;
            }
        }

        return false;
    }

    public Collection getReferencedRepositories() {
        return Collections.unmodifiableList(repositories);
    }
}
