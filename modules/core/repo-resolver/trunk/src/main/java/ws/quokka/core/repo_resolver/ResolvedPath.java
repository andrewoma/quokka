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


package ws.quokka.core.repo_resolver;

import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 *
 */
public class ResolvedPath {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String id;
    private List artifacts = new ArrayList();

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public ResolvedPath() {
    }

    public ResolvedPath(List artifacts) {
        this.artifacts = artifacts;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void add(RepoArtifact artifact) {
        artifacts.add(artifact);
    }

    public List getArtifacts() {
        return artifacts;
    }

    public boolean contains(RepoArtifactId id) {
        for (Iterator i = artifacts.iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();

            if (artifact.getId().equals(id)) {
                return true;
            }
        }

        return false;
    }
}
