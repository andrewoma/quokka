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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Get;
import org.apache.tools.ant.util.Base64Converter;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.IOUtils;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoType;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_spi.UnresolvedArtifactException;

import java.io.File;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;


/**
 *
 */
public class UrlRepository extends AbstractStandardRepository {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private URL url;
    private String user;
    private String password;
    private File index;
    private File indexArchive;
    private File indexExpanded;
    private long indexExpiry;
    private ChecksumRepository indexRepository;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void initialise() {
        super.initialise();

        try {
            String url = getProperty("root", true);
            url = url.endsWith("/") ? url : (url + "/"); // Force trailing slash
            this.url = (url == null) ? null : new URL(url);
        } catch (MalformedURLException e) {
            throw new BuildException(e);
        }

        user = getProperty("user", false);
        password = getProperty("password", false);

        String defaultIndex = getProperties().get("q.cacheDir") + "index-repo/" + getName() + "-index";
        index = normalise(new File(getProperty("index", defaultIndex)));
        indexArchive = new File(index, "_index.zip");
        indexExpanded = new File(index, "_index");
        indexExpiry = Integer.parseInt(getProperty("indexExpiry", "360")); // Defaults to hourly

        String indexUrl = "checksum:" + indexExpanded.getPath() + ";hierarchical=false";
        String indexId = getName() + "-index";
        getFactory().getProperties().put("q.repo." + indexId + ".url", indexUrl);
        indexRepository = (ChecksumRepository)getFactory().getOrCreate(indexId, true);

        Assert.isTrue(getParents().size() == 0, "Url repositories cannot have parents");
    }

    public RepoArtifact resolve(RepoArtifactId id, boolean retrieveArtifact) {
        // Try the index when called when not retrieving artifacts as this is
        // called when retrieving metadata for artifacts not stored locally.
        if (!retrieveArtifact) {
            updateIndex();

            return indexRepository.resolve(id, false);
        }

        File artifactFile = getRemoteArtifact(id);
        RepoArtifact remoteArtifact = getRemoteRepositoryFile(id);

        if ((artifactFile != null) || (remoteArtifact != null)) {
            RepoArtifact artifact = (remoteArtifact == null) ? new RepoArtifact(id) : remoteArtifact;
            Assert.isTrue(id.getType().equals("paths") || artifact.isStub() || (artifactFile != null),
                "Repository is corrupt. repository.xml exists, but artifact is missing: " + id.toShortString());

            if (artifactFile != null) {
                artifactFile.deleteOnExit();
            }

            artifact.setLocalCopy(artifactFile);

            return artifact;
        }

        // Artifact doesn't exist
        throw new UnresolvedArtifactException(id);
    }

    private boolean getRemoteRepositoryFile(RepoArtifactId id, File repositoryFile) {
        RepoType type = getFactory().getType(id.getType());

        return getRemoteFile(getRelativePath(id, type.getId() + "_repository.xml"), repositoryFile);
    }

    private File getRemoteArtifact(RepoArtifactId id) {
        File artifactFile = new IOUtils().createTempFile(id.toPathString(),
                "." + getFactory().getType(id.getType()).getExtension());

        RepoType type = getFactory().getType(id.getType());
        String extension = type.getId() + "." + type.getExtension();

        boolean exists = getRemoteFile(getRelativePath(id, extension), artifactFile);

        if (exists) {
            return artifactFile;
        } else {
            artifactFile.delete();

            return null;
        }
    }

    protected boolean getRemoteFile(String relativePath, File destination) {
        URL url;

        try {
            url = new URL(this.url, relativePath);
            log().verbose("Attempting to get: " + url.toString());
        } catch (MalformedURLException e) {
            throw new BuildException(e);
        }

        try {
            new IOUtils().download(getProject(), url, user, password, destination);
        } catch (Exception e) {
            if (notFound(url)) {
                return false;
            } else {
                throw new BuildException("Unable to get " + url.toString() + ": " + e.getMessage(), e);
            }
        }

        return true;
    }

    /**
     * Tries to work out if the error is because the url is not found versus other errors.
     * Currently only supports HTTP.
     * <p/>
     * TODO: Reimplement get so that reconnection isn't required to check the not found case.
     */
    protected boolean notFound(URL url) {
        if (!url.getProtocol().toLowerCase(Locale.US).equals("http")) {
            return false;
        }

        try {
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();

            try {
                if ((user != null) || (password != null)) {
                    String encoding = new Base64Converter().encode((user + ":" + password).getBytes());
                    connection.setRequestProperty("Authorization", "Basic " + encoding);
                }

                return connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND;
            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            return false;
        }
    }

    public void install(RepoArtifact artifact) {
        throw new UnsupportedOperationException();
    }

    public void remove(RepoArtifactId artifactId) {
        throw new UnsupportedOperationException();
    }

    public Collection listArtifactIds(boolean includeReferenced) {
        updateIndex();

        return indexRepository.listArtifactIds(includeReferenced);
    }

    protected void updateIndex() {
        boolean exists = indexArchive.exists();
        boolean expired = (System.currentTimeMillis() - indexArchive.lastModified()) > (indexExpiry * 1000);

        if (exists && !expired) {
            return;
        }

        boolean available = getRemoteIndex();

        if (!available && !exists) {
            throw new UnsupportedOperationException(); // Not an indexed repository
        }

        if (available) {
            extractIndex();
        }
    }

    private boolean getRemoteIndex() {
        boolean available;

        try {
            available = getRemoteFile("_index.zip", indexArchive); // Ant's get uses temp file internally, so is safe
        } catch (Exception e) {
            // Ignore ... there may not be an index, or we might be off line
            log().debug("Unable to get index for '" + getName() + "': " + e.getMessage());
            available = false;
        }

        return available;
    }

    protected void extractIndex() {
        File temp = new File(indexExpanded.getPath() + ".tmp");
        Delete delete = (Delete)getProject().createTask("delete");
        delete.setDir(temp);
        delete.execute();

        Expand expand = (Expand)getProject().createTask("unzip");
        expand.setSrc(indexArchive);
        expand.setDest(temp);
        expand.execute();

        delete.setDir(indexExpanded);
        delete.execute();

        Assert.isTrue(temp.renameTo(indexExpanded),
            "Could not rename " + temp.getPath() + " to " + indexExpanded.getPath());
    }

    public URL getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    protected RepoArtifact getRemoteRepositoryFile(RepoArtifactId id) {
        File repositoryFile = new IOUtils().createTempFile(id.toPathString(), "_repository.xml");

        try {
            if (getRemoteRepositoryFile(id, repositoryFile)) {
                return parse(id, repositoryFile);
            }
        } finally {
            repositoryFile.delete();
        }

        return null;
    }

    public RepoArtifact updateSnapshot(RepoArtifact artifact) {
        RepoArtifact latest = null;

        // Check this repository
        boolean artifactRequired = false;
        RepoArtifact updated = getRemoteRepositoryFile(artifact.getId()); // Get the repository.xml only

        if ((updated != null) && updated.isNewerThan(artifact)) {
            latest = updated;
            artifactRequired = true;
        }

        // Check parents
        for (Iterator i = getParents().iterator(); i.hasNext();) {
            Repository parent = (Repository)i.next();
            RepoArtifact current = (latest == null) ? artifact : latest;
            updated = parent.updateSnapshot(current);

            if ((updated != null) && updated.isNewerThan(current)) {
                latest = updated;
                artifactRequired = false;
            }
        }

        if (artifactRequired) {
            // This repo was newer, so need to fetch the actual artifact
            latest.setLocalCopy(getRemoteArtifact(artifact.getId()));
        }

        return latest;
    }

    public void rebuildCaches() {
        boolean available = getRemoteIndex();

        if (available) {
            extractIndex();
        }
    }
}
