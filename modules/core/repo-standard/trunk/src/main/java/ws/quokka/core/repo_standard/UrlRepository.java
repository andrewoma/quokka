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
import org.apache.tools.ant.taskdefs.Get;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.UnresolvedArtifactException;
import ws.quokka.core.util.AnnotatedProperties;
import ws.quokka.core.util.URLs;

import java.io.File;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.Collection;


/**
 *
 */
public class UrlRepository extends AbstractStandardRepository {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private URL url;
    private String user;
    private String password;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void initialise(Object project, AnnotatedProperties properties) {
        super.initialise(project, properties);

        try {
            String url = getProperty("url", false);
            this.url = (url == null) ? null : new URL(url);
        } catch (MalformedURLException e) {
            throw new BuildException(e);
        }

        user = getProperty("user", false);
        password = getProperty("password", false);
        Assert.isTrue(getParents().size() == 0, "Url repositories cannot have parents");
    }

    public RepoArtifact resolve(RepoArtifactId id) {
        File artifactFile;
        File repositoryFile;

        try {
            artifactFile = File.createTempFile(id.toPathString(), getType(id.getType()).getExtension());
            repositoryFile = File.createTempFile(id.toPathString(), "_repository.xml");
        } catch (IOException e) {
            throw new BuildException(e);
        }

        if (getRemoteArtifact(id, artifactFile)) {
            RepoArtifact artifact = new RepoArtifact(id);

            if (getRemoteRepositoryFile(id, repositoryFile)) {
                artifact = parse(id, repositoryFile);
                Assert.isTrue(repositoryFile.delete(),
                    "Unable to delete temporary repository file: " + repositoryFile.getAbsolutePath());
            }

            artifact.setLocalCopy(artifactFile);

            return artifact;
        }

        // Artifact doesn't exist
        throw new UnresolvedArtifactException(id, URLs.toURL(artifactFile));
    }

    private boolean getRemoteRepositoryFile(RepoArtifactId id, File repositoryFile) {
        return getRemoteFile(getRelativePath(id, "_repository.xml"), repositoryFile);
    }

    private boolean getRemoteArtifact(RepoArtifactId id, File artifactFile) {
        return getRemoteFile(getRelativePath(id, "." + getType(id.getType()).getExtension()), artifactFile);
    }

    private boolean getRemoteFile(String relativePath, File detination) {
        URL url;

        try {
            url = new URL(this.url, relativePath);
        } catch (MalformedURLException e) {
            throw new BuildException(e);
        }

        Get get = (Get)getProject().createTask("get");
        get.setSrc(url);
        get.setUsername(user);
        get.setPassword(password);
        get.setDest(detination);
        get.setUseTimestamp(true);
        get.setIgnoreErrors(false);

        try {
            get.execute();
        } catch (BuildException e) {
            if (notFound(url)) {
                return false;
            } else {
                throw e;
            }
        }

        return true;
    }

    /**
     * Tries to work out if the error is because the url is not found versus other errors.
     * Currently only supports HTTP
     * TODO: Add support for other protocols such as FTP by checking if the root url is accessible
     * and assuming that if it is the error is not found.
     */
    protected boolean notFound(URL url) {
        if (!url.getProtocol().toLowerCase().equals("http")) {
            return false;
        }

        try {
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();

            try {
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

    public Collection listArtifactIds() {
        throw new UnsupportedOperationException();
    }
}
