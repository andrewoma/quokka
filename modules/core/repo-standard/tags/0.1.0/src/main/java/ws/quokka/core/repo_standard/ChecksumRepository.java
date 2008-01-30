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
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.util.AnnotatedProperties;

import java.io.File;

import java.util.Collection;


/**
 *
 */
public class ChecksumRepository extends FileRepository {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private boolean verifyChecksums;
    private Repository parent;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void initialise(Object project, AnnotatedProperties properties) {
        super.initialise(project, properties);
        Assert.isTrue(getParents().size() == 1,
            "Checksum repository must have a single parent. Artifacts will be retrieved from the parent and installs will be delegated to it.");
        parent = (Repository)getParents().get(0);
        verifyChecksums = getBoolean("verifyChecksums", true);
    }

    public RepoArtifact resolve(RepoArtifactId id) {
        File artifactFile = getArtifactFile(id);
        File repositoryFile = getRepositoryFile(id);

        if (artifactFile.exists()) {
            RepoArtifact artifact = parse(id, repositoryFile);

            // This repository only stores signatures, so get the actual artifact from the parent
            ResolvedArtifact resolved = resolveFromParents(id, true, false);
            artifact.setLocalCopy(resolved.getArtifact().getLocalCopy());

            if (verifyChecksums) {
                // Check the artifact MD5 matches
                verifyChecksum(resolved.getArtifact().getLocalCopy(), artifactFile);
            }

            return artifact;
        }

        // Artifact doesn't exist, so check parents
        ResolvedArtifact resolved = resolveFromParents(id, true, isConfirmImport());
        importArtifact(resolved.getArtifact(), artifactFile, repositoryFile, resolved.getRepository());

        return resolved.getArtifact();
    }

    protected void importArtifact(RepoArtifact artifact, File artifactFile, File repositoryFile, Repository parent) {
        super.importArtifact(artifact, null, repositoryFile, parent);

        // Generate checksum in place of the actual artifact
        generateChecksum(artifact.getLocalCopy(), artifactFile);
    }

    protected File getArtifactFile(RepoArtifactId id) {
        return new File(super.getArtifactFile(id).getAbsolutePath() + ".MD5");
    }

    public void install(RepoArtifact artifact) {
        // Install locally
        generateChecksum(artifact.getLocalCopy(), getArtifactFile(artifact.getId()));
        writeRepositoryFile(artifact, getRepositoryFile(artifact.getId()));

        // Delegate installation of the actual artiact to the parent
        parent.install(artifact);
    }

    public Collection listArtifactIds() {
        return null;
    }
}
