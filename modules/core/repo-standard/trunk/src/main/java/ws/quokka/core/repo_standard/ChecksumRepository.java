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
import ws.quokka.core.bootstrap_util.IOUtils;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_spi.UnresolvedArtifactException;

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

    public void initialise() {
        super.initialise();
        Assert.isTrue(getParents().size() <= 1,
            "Checksum repository must have at most a single parent. Artifacts will be retrieved from the parent and installs will be delegated to it.");
        parent = (Repository)((getParents().size() == 0) ? null : getParents().get(0));
        verifyChecksums = getBoolean("verifyChecksums", false);
    }

    public RepoArtifact resolve(RepoArtifactId id, boolean retrieveArtifact) {
        Assert.isTrue(!retrieveArtifact || (parent != null),
            "Incorrect configuration: a checksum repository must have a parent with retrieving artifacts");

        File artifactFile = getArtifactFile(id);
        File repositoryFile = getRepositoryFile(id);

        if (artifactFile.exists() || repositoryFile.exists()) {
            Assert.isTrue(id.getType().equals("paths") || artifactFile.exists(),
                "Repository is corrupt. repository.xml exists, but artifact is missing: " + artifactFile.getPath());

            RepoArtifact artifact = repositoryFile.exists() ? parse(id, repositoryFile) : new RepoArtifact(id);

            if (retrieveArtifact) {
                // This repository only stores signatures, so get the actual artifact from the parent
                ResolvedArtifact resolved = resolveFromParents(id, true, false, true);
                artifact.setLocalCopy(resolved.getArtifact().getLocalCopy());

                if (verifyChecksums) {
                    // Check the artifact MD5 matches
                    verifyChecksum(resolved.getArtifact().getLocalCopy(), artifactFile);
                }
            } else {
                // Make the hash available when the artifact isn't retrieved
                if (artifactFile.exists()) {
                    artifact.setHash(new IOUtils().fileToString(artifactFile, "UTF8"));
                }
            }

            return artifact;
        }

        // Artifact doesn't exist, so check parents
        if (parent == null) {
            throw new UnresolvedArtifactException(id);
        }

        ResolvedArtifact resolved = resolveFromParents(id, true, isConfirmImport(), retrieveArtifact);

        if (retrieveArtifact) {
            importArtifact(resolved.getArtifact(), artifactFile, repositoryFile, resolved.getRepository());
        }

        return resolved.getArtifact();
    }

    protected void importArtifact(RepoArtifact artifact, File artifactFile, File repositoryFile, Repository parent) {
        super.importArtifact(artifact, null, repositoryFile, parent);

        // Generate checksum in place of the actual artifact
        generateChecksum(artifact.getLocalCopy(), artifactFile);
    }

    public File getArtifactFile(RepoArtifactId id) {
        return new File(super.getArtifactFile(id).getAbsolutePath() + ".MD5");
    }

    public void install(RepoArtifact artifact, boolean skipParent) {
        // Create the parent dir
        File dest = getArtifactFile(artifact.getId());
        File parent = dest.getParentFile();
        Assert.isTrue(parent.exists() || parent.mkdirs(), "Could not create: " + parent);

        // Generate the checksum if there is an artifact
        if (artifact.getLocalCopy() != null) {
            generateChecksum(artifact.getLocalCopy(), dest);
        }

        writeRepositoryFile(artifact, getRepositoryFile(artifact.getId()));

        // Delegate installation of the actual artiact to the parent
        if (!skipParent) {
            this.parent.install(artifact);
        }
    }

    public void install(RepoArtifact artifact) {
        if (parent == null) {
            throw new UnsupportedOperationException();
        }

        install(artifact, false);
    }

    public void remove(RepoArtifactId artifactId) {
        if (parent == null) {
            throw new UnsupportedOperationException();
        }

        super.remove(artifactId);
        parent.remove(artifactId);
    }
}
