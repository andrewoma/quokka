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
import org.apache.tools.ant.taskdefs.Checksum;
import org.apache.tools.ant.taskdefs.Input;
import org.apache.tools.ant.util.FileUtils;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.Logger;
import ws.quokka.core.bootstrap_util.ProjectLogger;
import ws.quokka.core.bootstrap_util.QuokkaEntityResolver;
import ws.quokka.core.repo_spi.AbstractRepository;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoType;
import ws.quokka.core.repo_spi.RepoXmlConverter;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_spi.UnresolvedArtifactException;
import ws.quokka.core.util.AnnotatedProperties;
import ws.quokka.core.util.Strings;
import ws.quokka.core.util.URLs;
import ws.quokka.core.util.xml.Document;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 *
 */
public abstract class AbstractStandardRepository extends AbstractRepository {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    public static final String PREFIX = "quokka.repo.";

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Project project;
    private Properties properties;
    private Map classes = new HashMap();
    private String name;
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

    public void initialise(Object antProject, AnnotatedProperties properties) {
        this.project = (Project)antProject;
        log = new ProjectLogger(project);
        this.properties = properties;
        name = (name == null) ? getProperty("name", true) : name; // Note: this must be set parents and is used as a namespace for properties
        hierarchical = getBoolean("hierarchical", true);
        confirmImport = getBoolean("confirmImport", true);
        snapshots = getBoolean("snapshots", false);
        installSnapshots = getBoolean("installSnapshots", snapshots);
        releases = getBoolean("releases", true);
        installReleases = getBoolean("installReleases", releases);
        supports = Strings.commaSepList(getProperty("supports", null));
        installSupports = Strings.commaSepList(getProperty("installSupports", null));
        installSupports.addAll((installSupports.size() == 0) ? supports : Collections.EMPTY_LIST);

        // Add aliases for known implementations
        classes.put("file", "ws.quokka.core.repo_standard.FileRepository");
        classes.put("url", "ws.quokka.core.repo_standard.UrlRepository");
        classes.put("checksum", "ws.quokka.core.repo_standard.ChecksumRepository");
        classes.put("delegating", "ws.quokka.core.repo_standard.DelegatingRepository");

        // Initialise any parents
        List names = Strings.commaSepList(getProperty("parents", false));

        for (Iterator i = names.iterator(); i.hasNext();) {
            String name = (String)i.next();
            Repository parent = create(name);
            properties.put(PREFIX + "name", name);
            parent.initialise(antProject, properties);

            for (Iterator j = getTypes().values().iterator(); j.hasNext();) {
                parent.registerType((RepoType)j.next());
            }

            parents.add(parent);
        }
    }

    public void registerType(RepoType type) {
        super.registerType(type);

        for (Iterator i = parents.iterator(); i.hasNext();) {
            Repository parent = (Repository)i.next();
            parent.registerType(type);
        }
    }

    protected Project getProject() {
        return project;
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

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected Repository create(String name) {
        String className = getProperty(name, "class", true);

        if (className == null) {
            return null;
        }

        if (classes.containsKey(className)) {
            className = (String)classes.get(className);
        }

        try {
            return (Repository)Class.forName(className).newInstance();
        } catch (Exception e) {
            throw new BuildException(e);
        }
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
        return getProperty(name, key, mandatory);
    }

    protected String getProperty(String name, String key, boolean mandatory) {
        key = PREFIX + ((name == null) ? "" : (name + ".")) + key;

        String value = properties.getProperty(key);
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
        Input input = (Input)getProject().createTask("input");
        input.setValidargs("y,n");
        input.setDefaultvalue("y");
        input.setAddproperty("result");
        input.setMessage(id.toShortString() + " is not available in repository '" + getName() + "'. Import from '"
            + from + "'? ");
        input.execute();

        return ("y".equals(getProject().getProperty("result")));
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
        try {
            Assert.isTrue(repositoryFile.getParentFile().exists() || repositoryFile.getParentFile().mkdirs(),
                "Unable to create repository directory: " + repositoryFile.getParent());

            Writer writer = new BufferedWriter(new FileWriter(repositoryFile));

            try {
                RepoXmlConverter.toXml(artifact, writer);
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    protected void copyArtifact(RepoArtifact artifact, File artifactFile) {
        copy(artifact.getLocalCopy(), artifactFile, false, true);
        artifact.setLocalCopy(artifactFile);
    }

    protected RepoArtifact parse(RepoArtifactId id, File xml) {
        //        System.out.println("Attempting to parse: " + xml.toString());
        if (!xml.exists()) {
            if (id.getVersion().getRepositoryVersion() == 0) {
                return new RepoArtifact(id); // OK not to define if there are no dependencies
            }

            throw new UnresolvedArtifactException(id, URLs.toURL(xml), "repository xml missing");
        }

        QuokkaEntityResolver resolver = new QuokkaEntityResolver();
        resolver.addVersion("repository", "0.1");

        RepoArtifact artifact = (RepoArtifact)RepoXmlConverter.getXmlConverter().fromXml(RepoArtifact.class,
                Document.parse(xml, resolver).getRoot());
        artifact.setId(id);

        return artifact;
    }

    protected static void generateChecksum(File file, File checksum) {
        stringToFile(checksum(file), checksum);
    }

    protected static void verifyChecksum(File file, File checksum) {
        Assert.isTrue(checksum(file).equals(fileToString(checksum)),
            "Checksum of artifact does not match: " + file.getAbsolutePath() + " against checksum in "
            + checksum.getAbsolutePath());
    }

    private static String fileToString(File file) {
        try {
            StringWriter writer = new StringWriter();
            Reader reader = new BufferedReader(new FileReader(file));

            try {
                while (true) {
                    int ch = reader.read();

                    if (ch == -1) {
                        return writer.toString();
                    }

                    writer.write(ch);
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    private static void stringToFile(String string, File file) {
        try {
            StringReader reader = new StringReader(string);
            Writer writer = new BufferedWriter(new FileWriter(file));

            try {
                while (true) {
                    int ch = reader.read();

                    if (ch == -1) {
                        return;
                    }

                    writer.write(ch);
                }
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
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

    //    public boolean supportsReslove(RepoArtifactId artifactId) {
    //        return artifactId.getVersion().getQualifier() == null || !artifactId.getVersion().getQualifier().endsWith("-ss");
    //    }
    protected File normalise(File file) {
        return getFileUtils().normalize(file.getAbsolutePath());
    }

    public ResolvedArtifact resolveFromParents(RepoArtifactId artifactId, boolean throwIfUnresolved,
        boolean confirmImport) {
        // Artifact doesn't exist at this level, so try the parents
        List urls = new ArrayList();

        for (Iterator i = parents.iterator(); i.hasNext();) {
            Repository repository = (Repository)i.next();

            try {
                RepoArtifact artifact = repository.resolve(artifactId);

                if (confirmImport) {
                    if (confirmImport(artifactId, repository)) {
                        return new ResolvedArtifact(artifact, repository);
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
            return id.getGroup().replace('.', '/') + "/" + id.getVersion() + "/" + id.getName() + "_" + extension;
        } else {
            return id.getGroup() + "_" + id.getVersion() + "_" + id.getName() + "_" + extension;
        }
    }

    public void debug(String message) {
        getProject().log(message, Project.MSG_DEBUG);
    }

    public boolean supportsReslove(RepoArtifactId artifactId) {
        return ((snapshots && artifactId.isSnapShot()) || (releases && !artifactId.isSnapShot()))
        && supports(supports, artifactId);
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
        // TODO: use reflection to apply regex match if jdk > 1.4
        return false;
    }

    public boolean supportsInstall(RepoArtifactId artifactId) {
        return ((installSnapshots && artifactId.isSnapShot()) || (installReleases && !artifactId.isSnapShot()))
        && supports(installSupports, artifactId);
    }

    public Collection getReferencedRepositories() {
        return parents;
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
