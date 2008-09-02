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
import ws.quokka.core.repo_spi.AbstractRepository;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_spi.UnresolvedArtifactException;
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
public class DelegatingRepository extends AbstractRepository {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private List repositories = new ArrayList();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void initialise() {
        String root = getFactory().getProperties().getProperty(prefix() + "root");
        String[] roots = Strings.trim(Strings.split(root, ","));
        Assert.isTrue((roots != null) && (roots.length != 0),
            "'" + prefix() + "root' property must contain a comma separated list of repository ids");

        for (int i = 0; i < roots.length; i++) {
            root = roots[i];
            repositories.add(getFactory().getOrCreate(root, true));
        }
    }

    public RepoArtifact resolve(RepoArtifactId artifactId, boolean retrieveArtifact) {
        List urls = new ArrayList();

        for (Iterator i = repositories.iterator(); i.hasNext();) {
            Repository repository = (Repository)i.next();

            if (repository.supportsReslove(artifactId)) {
                try {
                    return repository.resolve(artifactId, retrieveArtifact);
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

    public Collection listArtifactIds(boolean includeReferenced) {
        if (!includeReferenced) {
            return Collections.EMPTY_LIST; // Delegating repositories don't actually contain any artifacts
        }

        Set ids = new HashSet();

        for (Iterator i = repositories.iterator(); i.hasNext();) {
            Repository repository = (Repository)i.next();
            ids.addAll(repository.listArtifactIds(true));
        }

        return filterUnresolvable(ids);
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

    public RepoArtifact updateSnapshot(RepoArtifact artifact) {
        RepoArtifact latest = null;

        for (Iterator i = repositories.iterator(); i.hasNext();) {
            Repository repository = (Repository)i.next();

            if (repository.supportsReslove(artifact.getId())) {
                RepoArtifact updated = repository.updateSnapshot(artifact);

                if (updated != null) {
                    if (latest == null) {
                        latest = updated;
                    } else {
                        latest = latest.isNewerThan(updated) ? latest : updated;
                    }
                }
            }
        }

        return latest;
    }

    public void rebuildCaches() {
        for (Iterator i = repositories.iterator(); i.hasNext();) {
            Repository repository = (Repository)i.next();
            repository.rebuildCaches();
        }
    }
}
