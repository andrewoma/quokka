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
import java.util.Iterator;


/**
 *
 */
public abstract class AbstractRepository implements Repository {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    public static final String PREFIX = "quokka.repo.";

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private RepositoryFactory factory;
    private String name;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public RepositoryFactory getFactory() {
        return factory;
    }

    public void setFactory(RepositoryFactory factory) {
        this.factory = factory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String prefix() {
        return PREFIX + name + ".";
    }

    public RepoArtifact resolve(RepoArtifactId artifactId) {
        return resolve(artifactId, true);
    }

    /**
     * Remove any ids that can't actually be resolved by this repository
     */
    protected Collection filterUnresolvable(Collection ids) {
        for (Iterator i = ids.iterator(); i.hasNext();) {
            RepoArtifactId id = (RepoArtifactId)i.next();

            if (!supportsReslove(id)) {
                i.remove();
            }
        }

        return ids;
    }
}
