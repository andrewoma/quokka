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
import ws.quokka.core.util.AnnotatedProperties;

import java.io.File;

import java.util.Collection;


/**
 *
 */
public class FileRepository extends AbstractStandardRepository {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private File rootDir;
    private File installRootDir;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void initialise(Object project, AnnotatedProperties properties) {
        super.initialise(project, properties);
        rootDir = normalise(new File(getProperty("rootDir", true)));

        String installRootDirName = getProperty("installRootDir", null);
        installRootDir = (installRootDirName == null) ? null : normalise(new File(installRootDirName));
    }

    public RepoArtifact resolve(RepoArtifactId id) {
        File artifactFile = getArtifactFile(id);
        debug("Attempting to resolve: " + artifactFile.getAbsolutePath());

        File repositoryFile = getRepositoryFile(id);

        //        System.out.println("Looking for repository file: " + repositoryFile);
        debug("Looking for repository file: " + repositoryFile);

        if (artifactFile.exists()) {
            RepoArtifact artifact = parse(id, repositoryFile);
            artifact.setLocalCopy(artifactFile);

            return artifact;
        }

        // Artifact doesn't exist, so check parents
        ResolvedArtifact resolved = resolveFromParents(id, true, isConfirmImport());
        importArtifact(resolved.getArtifact(), artifactFile, repositoryFile, resolved.getRepository());

        return resolved.getArtifact();
    }

    protected File getArtifactFile(RepoArtifactId id) {
        RepoType type = getType(id.getType());
        String extension = type.getId() + "." + type.getExtension();

        return normalise(new File(rootDir, getRelativePath(id, extension)));
    }

    protected File getRepositoryFile(RepoArtifactId id) {
        RepoType type = getType(id.getType());

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
        return null;
    }
}
