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
import org.apache.tools.ant.taskdefs.Get;
import org.apache.tools.ant.util.Base64Converter;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoType;
import ws.quokka.core.repo_spi.UnresolvedArtifactException;
import ws.quokka.core.util.AnnotatedProperties;

import java.io.File;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.Collection;
import java.util.Locale;


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
            String url = getProperty("url", true);
            url = url.endsWith("/") ? url : (url + "/"); // Force trailing slash
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
            artifactFile = File.createTempFile(id.toPathString(), "." + getType(id.getType()).getExtension());
            artifactFile.deleteOnExit();
            repositoryFile = File.createTempFile(id.toPathString(), "_repository.xml");
            repositoryFile.deleteOnExit();
        } catch (IOException e) {
            throw new BuildException(e);
        }

        if (getRemoteArtifact(id, artifactFile)) {
            RepoArtifact artifact = new RepoArtifact(id);

            if (getRemoteRepositoryFile(id, repositoryFile)) {
                artifact = parse(id, repositoryFile);
            }

            artifact.setLocalCopy(artifactFile);

            return artifact;
        }

        // Artifact doesn't exist
        throw new UnresolvedArtifactException(id);
    }

    private boolean getRemoteRepositoryFile(RepoArtifactId id, File repositoryFile) {
        RepoType type = getType(id.getType());

        return getRemoteFile(getRelativePath(id, type.getId() + "_repository.xml"), repositoryFile);
    }

    private boolean getRemoteArtifact(RepoArtifactId id, File artifactFile) {
        RepoType type = getType(id.getType());
        String extension = type.getId() + "." + type.getExtension();

        return getRemoteFile(getRelativePath(id, extension), artifactFile);
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
        get.setUseTimestamp(false);
        get.setIgnoreErrors(false);

        try {
            // Get is incredibly noisy ... this limits it's logging to verbose level only
            get.doGet(Project.MSG_VERBOSE, null);
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

    public Collection listArtifactIds() {
        throw new UnsupportedOperationException();
    }
}
