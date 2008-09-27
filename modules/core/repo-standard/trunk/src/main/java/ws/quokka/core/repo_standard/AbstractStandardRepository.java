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
import org.apache.tools.ant.Target;
import org.apache.tools.ant.input.InputRequest;
import org.apache.tools.ant.input.MultipleChoiceInputRequest;
import org.apache.tools.ant.taskdefs.Checksum;
import org.apache.tools.ant.util.FileUtils;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.IOUtils;
import ws.quokka.core.bootstrap_util.Logger;
import ws.quokka.core.bootstrap_util.ProjectLogger;
import ws.quokka.core.bootstrap_util.QuokkaEntityResolver;
import ws.quokka.core.repo_spi.AbstractRepository;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoXmlConverter;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_spi.UnresolvedArtifactException;
import ws.quokka.core.util.AnnotatedProperties;
import ws.quokka.core.util.Strings;
import ws.quokka.core.util.xml.Document;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;


/**
 *
 */
public abstract class AbstractStandardRepository extends AbstractRepository {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private boolean hierarchical;
    private List parents = new ArrayList();
    private boolean confirmImport;
    private Logger log;
    private boolean snapshots;
    private boolean installSnapshots;
    private boolean releases;
    private boolean installReleases;
    private List supports;
    private List installSupports;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void initialise() {
        log = new ProjectLogger(getProject());

        hierarchical = getBoolean("hierarchical", true);
        confirmImport = getBoolean("confirmImport", false);
        snapshots = getBoolean("snapshots", false);
        installSnapshots = getBoolean("installSnapshots", snapshots);
        releases = getBoolean("releases", !snapshots);
        installReleases = getBoolean("installReleases", releases);
        supports = Strings.commaSepList(getProperty("supports", null));
        installSupports = Strings.commaSepList(getProperty("installSupports", null));
        installSupports.addAll((installSupports.size() == 0) ? supports : Collections.EMPTY_LIST);

        // Initialise any parents
        List names = Strings.commaSepList(getProperty("parents", false));

        for (Iterator i = names.iterator(); i.hasNext();) {
            String name = (String)i.next();

            if (!name.equals("")) {
                parents.add(getFactory().getOrCreate(name, true));
            }
        }
    }

    public Logger log() {
        return log;
    }

    protected Project getProject() {
        return getFactory().getProject();
    }

    protected boolean isHierarchical() {
        return hierarchical;
    }

    protected List getParents() {
        return parents;
    }

    protected boolean isConfirmImport() {
        return confirmImport;
    }

    protected boolean getBoolean(String key, boolean defaultValue) {
        String value = getProperty(key, false);

        return (value == null) ? defaultValue : Boolean.valueOf(value).booleanValue();
    }

    protected boolean getBoolean(String key) {
        return Boolean.valueOf(getProperty(key, true)).booleanValue();
    }

    protected String getProperty(String key, String defalultValue) {
        String value = getProperty(key, false);

        return (value == null) ? defalultValue : value;
    }

    protected String getProperty(String key, boolean mandatory) {
        return getProperty(getName(), key, mandatory);
    }

    protected String getProperty(String name, String key, boolean mandatory) {
        key = PREFIX + ((name == null) ? "" : (name + ".")) + key;

        String value = getProperties().getProperty(key);
        Assert.isTrue(!mandatory || (value != null), "Mandatory property is missing: " + key);

        return value;
    }

    protected FileUtils getFileUtils() {
        return FileUtils.getFileUtils();
    }

    protected void copy(File source, File destination, boolean overwrite, boolean preserveLastModified) {
        try {
            getProject().log("Copying " + source.getPath() + " to " + destination.getPath(), Project.MSG_DEBUG);
            getFileUtils().copyFile(source, destination, null, overwrite, preserveLastModified);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    protected boolean confirmImport(RepoArtifactId id, Repository parent) {
        String from = parent.getName();
        Vector choices = new Vector();
        choices.add("y");
        choices.add("n");

        InputRequest request = new MultipleChoiceInputRequest(id.toShortString() + " is not available in repository '"
                + getName() + "'. Import from '" + from + "'? ", choices);
        request.setDefaultValue("y");
        getProject().getInputHandler().handleInput(request);

        return ("y".equals(request.getInput()));
    }

    protected void importArtifact(RepoArtifact artifact, File artifactFile, File repositoryFile, Repository parent) {
        log.info("Importing artifact from " + parent.getName() + " to " + getName() + ": "
            + artifact.getId().toShortString());

        // Copy artifact
        if (artifactFile != null) {
            copyArtifact(artifact, artifactFile);
        }

        // Persist the repository.xml
        if (repositoryFile != null) {
            writeRepositoryFile(artifact, repositoryFile);
        }
    }

    protected void writeRepositoryFile(RepoArtifact artifact, File repositoryFile) {
        Assert.isTrue(repositoryFile.getParentFile().exists() || repositoryFile.getParentFile().mkdirs(),
            "Unable to getOrCreate repository directory: " + repositoryFile.getParent());
        RepoXmlConverter.toXml(artifact, repositoryFile);
    }

    protected void copyArtifact(RepoArtifact artifact, File artifactFile) {
        if (artifact.getLocalCopy() != null) {
            copy(artifact.getLocalCopy(), artifactFile, false, true);
            artifact.setLocalCopy(artifactFile);
        }
    }

    protected RepoArtifact parse(RepoArtifactId id, File xml) {
        QuokkaEntityResolver resolver = new QuokkaEntityResolver();
        resolver.addVersion("repository", new String[] { "0.1", "0.2" });

        RepoArtifact artifact = (RepoArtifact)RepoXmlConverter.getXmlConverter().fromXml(RepoArtifact.class,
                Document.parse(xml, resolver).getRoot());
        artifact.setId(id);

        return artifact;
    }

    protected static void generateChecksum(File file, File checksum) {
        new IOUtils().stringToFile(checksum(file), checksum, "UTF8");
    }

    protected static void verifyChecksum(File file, File checksum) {
        Assert.isTrue(checksum(file).equals(new IOUtils().fileToString(checksum)),
            "Checksum of artifact does not match: " + file.getAbsolutePath() + " against checksum in "
            + checksum.getAbsolutePath());
    }

    private static String checksum(File file) {
        Project project = new Project();
        project.init();

        Checksum checksum = new Checksum();
        checksum.setProject(project);
        checksum.setOwningTarget(new Target());
        checksum.setFile(file);

        String property = "checksum";
        checksum.setProperty(property);
        checksum.execute();

        return project.getProperty(property);
    }

    protected File normalise(File file) {
        return getFileUtils().normalize(file.getAbsolutePath());
    }

    public ResolvedArtifact resolveFromParents(RepoArtifactId artifactId, boolean throwIfUnresolved,
        boolean confirmImport, boolean retrieveArtifact) {
        // Artifact doesn't exist at this level, so try the parents
        List urls = new ArrayList();

        for (Iterator i = parents.iterator(); i.hasNext();) {
            Repository repository = (Repository)i.next();

            if (!repository.supportsReslove(artifactId)) {
                continue;
            }

            try {
                RepoArtifact artifact = repository.resolve(artifactId, retrieveArtifact);

                if (retrieveArtifact && confirmImport) {
                    if (confirmImport(artifactId, repository)) {
                        return new ResolvedArtifact(artifact, repository);
                    } else {
                        continue;
                    }
                }

                return new ResolvedArtifact(artifact, repository);
            } catch (UnresolvedArtifactException e) {
                // Try next parent
                urls.addAll(e.getUrls());
            }
        }

        if (throwIfUnresolved) {
            throw new UnresolvedArtifactException(artifactId, urls);
        }

        return null;
    }

    protected String getRelativePath(RepoArtifactId id, String extension) {
        if (hierarchical) {
            return id.getGroup().replace('.', '/') + "/" + id.getVersion().toString() + "/" + id.getName() + "_"
            + extension;
        } else {
            return id.getGroup() + "_" + id.getVersion().toString() + "_" + id.getName() + "_" + extension;
        }
    }

    public void debug(String message) {
        getProject().log(message, Project.MSG_DEBUG);
    }

    public boolean supportsReslove(RepoArtifactId artifactId) {
        return (
            (snapshots && artifactId.getVersion().isSnapShot()) || (releases && !artifactId.getVersion().isSnapShot())
        ) && supports(supports, artifactId);
    }

    protected boolean supports(List supports, RepoArtifactId artifactId) {
        if (supports.size() == 0) {
            return true;
        }

        for (Iterator i = supports.iterator(); i.hasNext();) {
            String pattern = (String)i.next();

            if (matches(pattern, artifactId)) {
                return true;
            }
        }

        return false;
    }

    protected boolean matches(String pattern, RepoArtifactId artifactId) {
        return artifactId.toShortString().matches(pattern);
    }

    public boolean supportsInstall(RepoArtifactId artifactId) {
        return (
            (installSnapshots && artifactId.getVersion().isSnapShot())
            || (installReleases && !artifactId.getVersion().isSnapShot())
        ) && supports(installSupports, artifactId);
    }

    public AnnotatedProperties getProperties() {
        return getFactory().getProperties();
    }

    protected Collection listParentIds() {
        Set ids = new TreeSet();

        for (Iterator i = getParents().iterator(); i.hasNext();) {
            Repository repository = (Repository)i.next();
            ids.addAll(repository.listArtifactIds(true));
        }

        return ids;
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public static class ResolvedArtifact {
        public RepoArtifact artifact;
        public Repository repository;

        public ResolvedArtifact(RepoArtifact artifact, Repository repository) {
            this.artifact = artifact;
            this.repository = repository;
        }

        public RepoArtifact getArtifact() {
            return artifact;
        }

        public Repository getRepository() {
            return repository;
        }
    }
}
