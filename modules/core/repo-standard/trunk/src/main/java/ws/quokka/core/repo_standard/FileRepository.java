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

import java.io.File;

import java.util.Collection;
import java.util.Iterator;


/**
 *
 */
public class FileRepository extends AbstractStandardRepository {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private File rootDir;
    private File installRootDir;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void initialise() {
        super.initialise();
        rootDir = normalise(new File(getProperty("root", true)));

        String installRootDirName = getProperty("installRoot", null);
        installRootDir = (installRootDirName == null) ? null : normalise(new File(installRootDirName));
    }

    public RepoArtifact resolve(RepoArtifactId id) {
        File artifactFile = getArtifactFile(id);
        debug("Attempting to resolve: " + artifactFile.getAbsolutePath());

        File repositoryFile = getRepositoryFile(id);
        debug("Looking for repository file: " + repositoryFile);

        if (artifactFile.exists() || repositoryFile.exists()) {
            Assert.isTrue(id.getType().equals("paths") || artifactFile.exists(),
                "Repository is corrupt. repository.xml exists, but artifact is missing: " + artifactFile.getPath());

            RepoArtifact artifact = repositoryFile.exists() ? parse(id, repositoryFile) : new RepoArtifact(id);
            artifact.setLocalCopy(artifactFile);

            return artifact;
        }

        // Artifact doesn't exist, so check parents
        ResolvedArtifact resolved = resolveFromParents(id, true, isConfirmImport());
        importArtifact(resolved.getArtifact(), artifactFile, repositoryFile, resolved.getRepository());

        return resolved.getArtifact();
    }

    protected File getArtifactFile(RepoArtifactId id) {
        RepoType type = getFactory().getType(id.getType());
        String extension = type.getId() + "." + type.getExtension();

        return normalise(new File(rootDir, getRelativePath(id, extension)));
    }

    protected File getRepositoryFile(RepoArtifactId id) {
        RepoType type = getFactory().getType(id.getType());

        return normalise(new File(rootDir, getRelativePath(id, type.getId() + "_repository.xml")));
    }

    public void install(RepoArtifact artifact) {
        File oldRootDir = rootDir;

        try {
            rootDir = (installRootDir == null) ? rootDir : installRootDir;
            copyArtifact(artifact, getArtifactFile(artifact.getId()));
            writeRepositoryFile(artifact, getRepositoryFile(artifact.getId()));
        } finally {
            rootDir = oldRootDir;
        }
    }

    public void remove(RepoArtifactId artifactId) {
        File artifactFile = getArtifactFile(artifactId);

        if (artifactFile.exists()) {
            Assert.isTrue(artifactFile.delete(), "Unable to delete artifact file: " + artifactFile.getPath());
        }

        File repositoryFile = getRepositoryFile(artifactId);

        if (repositoryFile.exists()) {
            Assert.isTrue(repositoryFile.delete(), "Unable to delete repository file: " + repositoryFile.getPath());
        }
    }

    public Collection listArtifactIds() {
        throw new UnsupportedOperationException();
    }

    protected File getRootDir() {
        return rootDir;
    }

    protected File getInstallRootDir() {
        return installRootDir;
    }

    public RepoArtifact updateSnapshot(RepoArtifact artifact) {
        RepoArtifact latest = null;

        try {
            // Check this repository
            RepoArtifact updated = resolve(artifact.getId()); // Cheap operation ...

            if (updated.isNewerThan(artifact)) {
                latest = updated;
            }
        } catch (UnresolvedArtifactException e) {
            // Ignore ... just means the artifact is not in this repository
        }

        // Check parents
        for (Iterator i = getParents().iterator(); i.hasNext();) {
            Repository parent = (Repository)i.next();
            RepoArtifact current = (latest == null) ? artifact : latest;
            RepoArtifact updated = parent.updateSnapshot(current);

            if ((updated != null) && updated.isNewerThan(current)) {
                latest = updated;
            }
        }

        return latest;
    }
}
