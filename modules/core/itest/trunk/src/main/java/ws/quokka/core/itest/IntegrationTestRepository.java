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


package ws.quokka.core.itest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import ws.quokka.core.repo_spi.AbstractRepository;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoType;
import ws.quokka.core.repo_spi.RepoXmlConverter;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_standard.DelegatingRepository;
import ws.quokka.core.util.AnnotatedProperties;
import ws.quokka.core.util.ServiceFactory;
import ws.quokka.core.util.URLs;
import ws.quokka.core.util.xml.Document;
import ws.quokka.core.version.Version;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;


/**
 *
 */
public class IntegrationTestRepository extends AbstractRepository implements Repository {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Repository repository;
    private Map overrides = new HashMap();
    private AnnotatedProperties properties;
    private Project project;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void initialise(Object project, AnnotatedProperties properties) {
        this.project = (Project)project;
        this.properties = properties;

        try {
            getRepository(properties);
        } catch (Exception e) {
            throw new BuildException(e);
        }

        repository.initialise(project, properties);
        addOverrides(properties);
    }

    private void getRepository(AnnotatedProperties properties) {
        String original;

        try {
            original = properties.getProperty("quokka.itest.repository");

            if (original == null) {
                properties.remove(Repository.class.getName());
                System.setProperty(Repository.class.getName(), "");
            } else {
                properties.setProperty(Repository.class.getName(), original);
                System.setProperty(Repository.class.getName(), original);
            }

            repository = (Repository)new ServiceFactory().getService(Repository.class, properties);

            if (repository == null) {
                repository = new DelegatingRepository();
            }
        } finally {
            System.setProperty(Repository.class.getName(), this.getClass().getName());
        }
    }

    private void addOverrides(Properties properties) {
        for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String key = (String)entry.getKey();
            String prefix = "itest.override.";

            if (key.startsWith(prefix)) {
                String idString = key.substring(prefix.length(), key.length());
                RepoArtifactId id = RepoArtifactId.parse(idString);
                overrides.put(id.toPathString(), entry.getValue());
            }
        }
    }

    public void registerType(RepoType type) {
        super.registerType(type);
        repository.registerType(type);
    }

    public RepoArtifact resolve(RepoArtifactId artifactId) {
        // Check for a specific override
        if (overrides.containsKey(artifactId.toPathString())) {
            String override = ((String)overrides.get(artifactId.toPathString())).trim();
            File dir = new File(override);
            project.log("Resolve: using specific override for " + artifactId + ", dir=" + dir.getAbsolutePath(),
                Project.MSG_DEBUG);

            return resolveFromDir(artifactId, dir);
        }

        // See if a module exists in a relative location
        String prefix = "quokka.";

        if (artifactId.getGroup().startsWith(prefix) && !artifactId.getGroup().equals("quokka.bundle.core")) {
            String moduleHome = (String)properties.get("quokka.core.itest.moduleHome");
            moduleHome = new File(moduleHome).getParentFile().getParentFile().getAbsolutePath();
            moduleHome += (
                "/" + artifactId.getGroup().substring(prefix.length()).replace('.', '/') + "/target/compile"
            );

            File artifactClasses = new File(moduleHome.replace('/', File.separatorChar));

            //                    System.out.println(artifactClasses.getAbsolutePath());
            if (artifactClasses.exists()) {
                Properties properties = getProperties(artifactClasses, artifactId);
                String version = properties.getProperty("artifact.id.version");

                if (version == null) {
                    throw new BuildException("Version not found for " + artifactId.toShortString());
                }

                if (Version.parse(version).equals(artifactId.getVersion())) {
                    project.log("Resolve: using relative override for " + artifactId + ", dir="
                        + artifactClasses.getAbsolutePath(), Project.MSG_DEBUG);

                    return resolveFromDir(artifactId, artifactClasses);
                }
            } else {
                System.out.println("In project: " + project.getName());
                throw new BuildException("Override not found for " + artifactId); // These should always be available
            }
        }

        RepoArtifact repoArtifact = repository.resolve(artifactId);
        project.log("Resolve: using parent, file=" + repoArtifact.getLocalCopy().getAbsolutePath(), Project.MSG_DEBUG);

        return repoArtifact;
    }

    private Properties getProperties(File base, RepoArtifactId id) {
        Properties properties = new Properties();
        String name = "META-INF/quokka/" + id.getGroup() + "_" + id.getName() + "_" + id.getType()
            + "_artifacts.properties";
        File file = new File(base, name.replace('/', File.separatorChar));

        if (file.exists()) {
            try {
                InputStream in = new BufferedInputStream(new FileInputStream(file));

                try {
                    properties.load(in);
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }

        return properties;
    }

    private RepoArtifact resolveFromDir(RepoArtifactId artifactId, File dir) {
        RepoArtifact artifact = new RepoArtifact(artifactId);
        artifact.setLocalCopy(dir);

        URL url = URLs.toURL(dir, "META-INF/quokka/" + artifactId.toPathString() + "/repository.xml");

        if (url == null) {
            return artifact;
        }

        //        QuokkaEntityResolver resolver = new QuokkaEntityResolver();
        //        resolver.addVersion("project", "1.0-m01");
        artifact = (RepoArtifact)RepoXmlConverter.getXmlConverter().fromXml(RepoArtifact.class,
                Document.parse(url, new Document.NullEntityResolver()).getRoot());
        artifact.setId(artifactId);
        artifact.setLocalCopy(dir);

        return artifact;
    }

    public void install(RepoArtifact artifact) {
        repository.install(artifact);
    }

    public void remove(RepoArtifactId artifactId) {
        repository.remove(artifactId);
    }

    public Collection listArtifactIds() {
        return repository.listArtifactIds();
    }

    public boolean supportsReslove(RepoArtifactId artifactId) {
        return repository.supportsReslove(artifactId);
    }

    public boolean supportsInstall(RepoArtifactId artifactId) {
        return repository.supportsInstall(artifactId);
    }

    public String getName() {
        return getClass().getName();
    }

    public Collection getReferencedRepositories() {
        return repository.getReferencedRepositories();
    }
}
