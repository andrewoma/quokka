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

import org.apache.tools.ant.taskdefs.Expand;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.util.Strings;

import java.io.File;

import java.util.Collection;


/**
 * BundledRepository allows a repository to be distributed as a zipped bundle. On first use, it is
 * extracted and used as a local file repository. The main use for such repositories is to allow a
 * whole set of dependendencies to be included locally in a project as a group. e.g. plugin packs
 * are supplied with quokka that include all quokka plugins as a bundle released monthly.
 */
public class BundledRepository extends FileRepository {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private RepoArtifactId bundleId;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void initialise() {
        bundleId = getArtifactId(getProperty("root", true));

        File rootDir = getRootDir(bundleId);
        getProperties().setProperty(PREFIX + getName() + ".root", rootDir.getPath());
        getProperties().setProperty(PREFIX + getName() + ".hierarchical", "false");

        super.initialise();
    }

    protected void extractBundle() {
        if (!getRootDir().exists()) {
            String repositoryName = getProperty("repository", true);
            Repository repository = getFactory().getOrCreate(repositoryName, true);
            RepoArtifact artifact = repository.resolve(bundleId);

            Expand expand = new Expand();
            expand.setProject(getProject());
            expand.setDest(getRootDir());
            expand.setSrc(artifact.getLocalCopy());
            expand.execute();
        }
    }

    protected File getRootDir(RepoArtifactId id) {
        String cache = getProperty("cache",
                normalise(new File(getProperties().get("q.cacheDir") + "/bundled-repos")).getPath());

        return new File(cache, id.toPathString());
    }

    protected RepoArtifactId getArtifactId(String artifact) {
        String[] tokens = Strings.split(artifact, ":");
        Assert.isTrue((tokens.length == 3) || (tokens.length == 2),
            "'artifact' property should be in the format group[:name]:version");

        String name = (tokens.length == 3) ? tokens[1] : tokens[0].substring(tokens[0].lastIndexOf(".") + 1);
        String version = (tokens.length == 3) ? tokens[2] : tokens[1];

        return new RepoArtifactId(tokens[0], name, "jar", version);
    }

    public RepoArtifact resolve(RepoArtifactId id, boolean retrieveArtifact) {
        extractBundle();

        return super.resolve(id, retrieveArtifact);
    }

    public void remove(RepoArtifactId artifactId) {
        throw new UnsupportedOperationException();
    }

    public void install(RepoArtifact artifact) {
        throw new UnsupportedOperationException();
    }

    public Collection listArtifactIds(boolean includeReferenced) {
        extractBundle();

        return super.listArtifactIds(includeReferenced);
    }

    public boolean supportsInstall(RepoArtifactId artifactId) {
        return false;
    }
}
