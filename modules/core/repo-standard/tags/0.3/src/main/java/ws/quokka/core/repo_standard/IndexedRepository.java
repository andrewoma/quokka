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

import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.util.FileUtils;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.Logger;
import ws.quokka.core.bootstrap_util.ProjectLogger;
import ws.quokka.core.repo_spi.AbstractRepository;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.Repository;

import java.io.File;

import java.util.Collection;
import java.util.Iterator;


/**
 * IndexedRepository maintains an index of the underlying repository (including all metadata and hashes).
 * The index will be automatically updated on install or remove and can be rebuilt via rebuildCaches.
 * Note that the index itself if never queries via this repository - it is just maintained to be a mirror
 * of the underlying cache. It's intial purpose is to provide an index that be uploaded for use
 * via UrlRepositories.
 */
public class IndexedRepository extends AbstractRepository {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Repository repository;
    private ChecksumRepository indexRepository;
    private File indexRoot;
    private Logger log;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void initialise() {
        log = new ProjectLogger(getFactory().getProject());

        // Allow either direct instantiation or configuration via properties
        if (repository == null) {
            String root = getFactory().getProperties().getProperty(prefix() + "root");
            repository = getFactory().getOrCreate(root, true);
        }

        if (indexRoot == null) {
            String root = getFactory().getProperties().getProperty(prefix() + "indexRoot");

            if ((root == null) && repository instanceof FileRepository) { // Support the common case
                root = ((FileRepository)repository).getRootDir().getPath();
            }

            Assert.isTrue(root != null, "Property 'indexRoot' must be set to the location of the index directory");
            indexRoot = FileUtils.getFileUtils().normalize(root + "/_index");
        }

        if (indexRepository == null) {
            String url = "checksum:" + indexRoot.getPath() + ";hierarchical=true;parents=" + repository.getName();
            String id = getName() + "-index";
            getFactory().getProperties().put("q.repo." + id + ".url", url);
            indexRepository = (ChecksumRepository)getFactory().getOrCreate(id, true);
        }
    }

    public void setIndexRoot(File indexRoot) {
        this.indexRoot = indexRoot;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public ChecksumRepository getIndexRepository() {
        return indexRepository;
    }

    public void setIndexRepository(ChecksumRepository indexRepository) {
        this.indexRepository = indexRepository;
    }

    public RepoArtifact resolve(RepoArtifactId artifactId, boolean retrieveArtifact) {
        return repository.resolve(artifactId, retrieveArtifact);
    }

    public void install(RepoArtifact artifact) {
        indexRepository.install(artifact);
        archiveIndex();
    }

    private void archiveIndex() {
        // Create temp file first to protect against interruptions
        Tar tar = (Tar)getFactory().getProject().createTask("tar");
        tar.setBasedir(indexRoot);

        Tar.TarCompressionMethod method = new Tar.TarCompressionMethod();
        method.setValue("bzip2");
        tar.setCompression(method);

        File temp = new File(indexRoot.getParentFile(), "_index.tar.bz2.tmp");
        Assert.isTrue(!temp.exists() || temp.delete(), "Cannot delete: " + temp.getPath());
        tar.setDestFile(temp);
        tar.execute();

        File out = new File(indexRoot.getParentFile(), "_index.tar.bz2");
        Assert.isTrue(!out.exists() || out.delete(), "Cannot delete: " + out.getPath());
        Assert.isTrue(temp.renameTo(out), "Cannot rename " + temp.getPath());
    }

    public void remove(RepoArtifactId artifactId) {
        indexRepository.remove(artifactId);
        archiveIndex();
    }

    public Collection listArtifactIds(boolean includeReferenced) {
        return repository.listArtifactIds(includeReferenced);
    }

    public boolean supportsInstall(RepoArtifactId artifactId) {
        return repository.supportsInstall(artifactId);
    }

    public boolean supportsResolve(RepoArtifactId artifactId) {
        return repository.supportsResolve(artifactId);
    }

    public RepoArtifact updateSnapshot(RepoArtifact artifact) {
        return repository.updateSnapshot(artifact);
    }

    public Collection listArtifactIds(String group, String name, String type, boolean includeReferenced) {
        return repository.listArtifactIds(group, name, type, includeReferenced);
    }

    public void rebuildCaches() {
        log.info("Rebuilding index for '" + getName() + "' repository");

        Delete delete = (Delete)getFactory().getProject().createTask("delete");
        delete.setDir(indexRoot);
        delete.execute();

        for (Iterator i = repository.listArtifactIds(false).iterator(); i.hasNext();) {
            RepoArtifactId id = (RepoArtifactId)i.next();
            RepoArtifact repoArtifact = repository.resolve(id);
            indexRepository.install(repoArtifact, true);
        }

        archiveIndex();
    }
}
