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


package ws.quokka.core.main.ant;

import org.apache.tools.ant.BuildException;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.ProjectLogger;
import ws.quokka.core.bootstrap_util.VoidExceptionHandler;
import ws.quokka.core.main.ant.task.CopyPathTask;
import ws.quokka.core.metadata.Metadata;
import ws.quokka.core.model.Artifact;
import ws.quokka.core.model.Dependency;
import ws.quokka.core.model.DependencySet;
import ws.quokka.core.model.License;
import ws.quokka.core.model.ModelFactory;
import ws.quokka.core.model.Path;
import ws.quokka.core.model.PathGroup;
import ws.quokka.core.model.PluginDependency;
import ws.quokka.core.model.Project;
import ws.quokka.core.model.ProjectModel;
import ws.quokka.core.model.Target;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoDependency;
import ws.quokka.core.repo_spi.RepoOverride;
import ws.quokka.core.repo_spi.RepoPath;
import ws.quokka.core.repo_spi.RepoPathSpec;
import ws.quokka.core.repo_spi.RepoType;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *
 */
public class ProjectMetadata implements Metadata {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private DefaultProjectModel projectModel;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public ProjectMetadata(DefaultProjectModel projectModel) {
        this.projectModel = projectModel;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public List getProjectPath(String id) {
        return projectModel.getProjectPath(id, false, true);
    }

    public List getProjectPath(String id, boolean mergeWithCore, boolean flatten) {
        return projectModel.getProjectPath(id, mergeWithCore, flatten);
    }

    public RepoType getType(String id) {
        return projectModel.getRepository().getFactory().getType(id);
    }

    public List getArtifactIds(String type) {
        return getArtifactIds(Collections.singletonList(type));
    }

    public List getArtifactIds(List types) {
        List ids = new ArrayList();

        for (Iterator i = projectModel.getProject().getArtifacts().iterator(); i.hasNext();) {
            RepoArtifactId id = ((Artifact)i.next()).getId();

            if (types.contains(id.getType())) {
                ids.add(id);
            }
        }

        return ids;
    }

    public RepoArtifactId getArtifactId(String name, String type) {
        for (Iterator i = projectModel.getProject().getArtifacts().iterator(); i.hasNext();) {
            RepoArtifactId id = ((Artifact)i.next()).getId();

            if (id.getType().equals(type) && id.getName().equals(name)) {
                return id;
            }
        }

        throw new BuildException("Artifact with name='" + name + "' and type='" + type
            + "' is not defined in the project");
    }

    public List getArtifactIds() {
        List ids = new ArrayList();

        for (Iterator i = projectModel.getProject().getArtifacts().iterator(); i.hasNext();) {
            Artifact artifact = (Artifact)i.next();
            ids.add(artifact.getId());
        }

        return ids;
    }

    public List getExportedArtifacts() {
        List exportedList = new ArrayList();

        for (Iterator i = projectModel.getProject().getArtifacts().iterator(); i.hasNext();) {
            Artifact artifact = (Artifact)i.next();

            exportedList.add(createExported(artifact));
        }

        return exportedList;
    }

    public Map getLocalLicenses() {
        Map licenses = new HashMap();

        for (Iterator i = projectModel.getLicenses().iterator(); i.hasNext();) {
            License license = (License)i.next();

            if (license.getFile() != null) {
                licenses.put(license.getId(), license.getFile());
            }
        }

        return licenses;
    }

    public Map getExportedPaths(RepoArtifactId id) {
        Map paths = new HashMap();

        for (Iterator i = projectModel.getProject().getArtifacts().iterator(); i.hasNext();) {
            Artifact artifact = (Artifact)i.next();

            if (artifact.getId().equals(id)) {
                for (Iterator j = artifact.getExportedPaths().iterator(); j.hasNext();) {
                    Artifact.PathMapping mapping = (Artifact.PathMapping)j.next();
                    paths.put(mapping.getFrom(), mapping.getTo());
                }
            }
        }

        return paths;
    }

    private RepoArtifact createExported(Artifact artifact) {
        RepoArtifact exported = new RepoArtifact(artifact.getId());
        exported.setDescription(artifact.getDescription());

        // Add licenses
        if (!artifact.getId().getType().equals("license")) {
            for (Iterator i = projectModel.getLicenses().iterator(); i.hasNext();) {
                License license = (License)i.next();
                exported.addLicense(license.getId());
            }
        }

        // Add the paths
        for (Iterator i = artifact.getExportedPaths().iterator(); i.hasNext();) {
            Artifact.PathMapping mapping = (Artifact.PathMapping)i.next();
            Path path = (Path)projectModel.getResolvedPaths().get(mapping.getFrom());
            Assert.isTrue(path != null, mapping.getLocator(),
                "From path '" + mapping.getFrom() + "' does not exist in the project");

            RepoPath exportedPath = new RepoPath(mapping.getTo(), path.getDescription(), path.isDescendDefault(),
                    path.isMandatoryDefault());
            exported.addPath(exportedPath);

            // Add any overrides that apply
            List appliedOverrides = new ArrayList();
            projectModel.getReslovedProjectPath(mapping.getFrom(), false, false, false, appliedOverrides);

            for (Iterator j = appliedOverrides.iterator(); j.hasNext();) {
                RepoOverride override = (RepoOverride)j.next();

                // Check if an existing override matches, if so add this path to it
                boolean added = false;

                for (Iterator k = exported.getOverrides().iterator(); k.hasNext();) {
                    RepoOverride existing = (RepoOverride)k.next();

                    if (existing.equalsExcludingPaths(override)) {
                        existing.addPath(mapping.getTo());
                        added = true;

                        break;
                    }
                }

                // Add the override to the exported artifact
                if (!added) {
                    Set paths = new HashSet();
                    paths.add(mapping.getTo());
                    exported.addOverride(new RepoOverride(paths, override.getGroup(), override.getName(),
                            override.getType(), override.getVersion(), override.getWithVersion(),
                            override.getWithPathSpecs()));
                }
            }
        }

        // Add any dependencies for the path
        addDependencies(exported, artifact.getExportedPaths(), projectModel.getProject().getDependencySet());

        return exported;
    }

    private void addDependencies(RepoArtifact exported, List exportedPaths, DependencySet dependencySet) {
        for (Iterator i = dependencySet.getSubsets().iterator(); i.hasNext();) {
            DependencySet subset = (DependencySet)i.next();
            addDependencies(exported, exportedPaths, subset);
        }

        for (Iterator i = dependencySet.getDependencies().iterator(); i.hasNext();) {
            Dependency dependency = (Dependency)i.next();

            if (dependency instanceof PluginDependency) {
                continue;
            }

            addDependencies(exported, exportedPaths, dependency);
        }
    }

    private void addDependencies(RepoArtifact exported, List exportedPaths, Dependency dependency) {
        RepoDependency repoDependency = null;

        for (Iterator i = dependency.getPathSpecs().iterator(); i.hasNext();) {
            RepoPathSpec pathSpec = (RepoPathSpec)i.next();

            for (Iterator j = exportedPaths.iterator(); j.hasNext();) {
                Artifact.PathMapping mapping = (Artifact.PathMapping)j.next();

                if (pathSpec.getTo().equals(mapping.getFrom())) {
                    if (repoDependency == null) {
                        repoDependency = new RepoDependency();
                        repoDependency.setId(dependency.getId());
                        exported.addDependency(repoDependency);
                    }

                    RepoPathSpec exportedPathSpec = new RepoPathSpec(pathSpec.getFrom(), mapping.getTo(),
                            pathSpec.getOptions(), pathSpec.isDescend(), pathSpec.isMandatory());
                    repoDependency.addPathSpec(exportedPathSpec);
                }
            }
        }
    }

    public boolean hasReleasableBootStrapper() {
        return (projectModel.getBootStrapper() != null) && projectModel.getBootStrapper().isReleasable();
    }

    public Set getDependencies() {
        Set dependencies = new HashSet();
        getDependencies(projectModel, dependencies);

        return dependencies;
    }

    public Set getDependencies(Set profiles) {
        Set dependencies = new HashSet();

        ModelFactory factory = projectModel.getModelFactory();
        File projectFile = projectModel.getProject().getProjectFile();

        if (profiles == null) { // Discover all profiles
            profiles = factory.getProfiles(projectFile);
        }

        for (Iterator i = profiles.iterator(); i.hasNext();) {
            String profile = (String)i.next();
            List profilesList = new ArrayList();
            profilesList.add(profile);

            ProjectModel model = factory.getProjectModel(projectFile, profilesList, true, null,
                    new ProjectLogger(projectModel.getAntProject()), projectModel.getAntProject());
            getDependencies(model, dependencies);
        }

        return dependencies;
    }

    private void getDependencies(ProjectModel projectModel, Set dependencies) {
        // Project paths
        for (Iterator i = projectModel.getProjectPaths().keySet().iterator(); i.hasNext();) {
            String id = (String)i.next();
            List path = projectModel.getProjectPath(id, false, false);
            addDependencies(dependencies, path);
        }

        // Plugin paths
        for (Iterator i = projectModel.getTargets().values().iterator(); i.hasNext();) {
            Target target = (Target)i.next();

            for (Iterator j = target.getPathGroups().iterator(); j.hasNext();) {
                PathGroup group = (PathGroup)j.next();
                List path = projectModel.getPathGroup(target, group.getId());
                addDependencies(dependencies, path);
            }
        }

        // Recursively add ids referenced by dependency sets
        addDependencySet(dependencies, projectModel.getProject().getDependencySet());
    }

    private void addDependencySet(Set dependencies, DependencySet dependencySet) {
        if (dependencySet.getArtifact() != null) {
            dependencies.add(dependencySet.getArtifact().getId());
        }

        for (Iterator i = dependencySet.getDependencies().iterator(); i.hasNext();) {
            Dependency dependency = (Dependency)i.next();
            dependencies.add(dependency.getId());
        }

        for (Iterator i = dependencySet.getSubsets().iterator(); i.hasNext();) {
            DependencySet subset = (DependencySet)i.next();
            addDependencySet(dependencies, subset);
        }
    }

    private void addDependencies(Set dependencies, List path) {
        for (Iterator j = path.iterator(); j.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)j.next();
            dependencies.add(artifact.getId());
        }
    }

    public void rolloverVersion(String version) {
        rolloverVersion(projectModel.getProject(), version);
    }

    protected char[] rolloverVersion(char[] buffer, String version) {
        for (int i = 0; i < buffer.length; i++) {
            String startComment = "<!--";

            if (matches(startComment, buffer, i)) {
                i = skipTo("-->", buffer, i + startComment.length());
                Assert.isTrue(i != -1, "Invalid xml? End of comment not found");

                continue;
            }

            String startArtifacts = "<artifacts";

            if (matches(startArtifacts, buffer, i)) {
                int end = skipTo(">", buffer, i + startArtifacts.length());
                Assert.isTrue(end != -1, "Invalid xml? End of artifacts tag not found");

                String artifactsEl = new String(buffer, i, end - i);
                String replacement = replaceVersion(artifactsEl, version);
                char[] newBuffer = new char[buffer.length - artifactsEl.length() + replacement.length()];
                System.arraycopy(buffer, 0, newBuffer, 0, i);
                System.arraycopy(replacement.toCharArray(), 0, newBuffer, i, replacement.length());
                System.arraycopy(buffer, i + artifactsEl.length(), newBuffer, i + replacement.length(),
                    buffer.length - i - artifactsEl.length());

                return newBuffer;
            }
        }

        throw new BuildException("Could not find artifacts element");
    }

    protected String replaceVersion(String artifactsElement, String version) {
        char[] buffer = artifactsElement.toCharArray();

        // States
        final int initial = 0;
        final int inName = 1;
        final int inValue = 2;

        char quote = 0;
        int start = 0;
        String attribute = "";

        int state = initial;

        for (int i = "<artifacts".length(); i < buffer.length; i++) {
            char ch = buffer[i];

            switch (state) {
            case initial:

                if (Character.isWhitespace(ch)) {
                    continue;
                } else {
                    start = i;
                    state = inName;
                    attribute = null;
                }

                continue;

            case inName:

                if ((Character.isWhitespace(ch) || (ch == '=')) && (attribute == null)) {
                    attribute = new String(buffer, start, i - start);
                } else if ((ch == '"') || (ch == '\'')) {
                    quote = ch;
                    state = inValue;
                    start = i + 1;
                }

                continue;

            case inValue:

                if (ch == quote) {
                    String value = new String(buffer, start, i - start);
                    state = initial;

                    if (attribute.equals("version")) {
                        char[] newBuffer = new char[buffer.length - value.length() + version.length()];
                        System.arraycopy(buffer, 0, newBuffer, 0, start);
                        System.arraycopy(version.toCharArray(), 0, newBuffer, start, version.length());
                        System.arraycopy(buffer, i, newBuffer, start + version.length(), buffer.length - i);

                        return new String(newBuffer);
                    }
                }
            }
        }

        throw new BuildException("Version attribute not found");
    }

    private void rolloverVersion(final Project project, final String version) {
        // TODO: improve safety by writing to a temp file first and renaming. Still, original should be under version control ...
        new VoidExceptionHandler() {
                public void run() throws IOException {
                    File file = project.getProjectFile();
                    char[] buffer = new char[(int)file.length()];
                    Reader reader = new FileReader(file);

                    try {
                        reader.read(buffer);
                    } finally {
                        reader.close();
                    }

                    buffer = rolloverVersion(buffer, version);

                    Writer writer = new FileWriter(file);

                    try {
                        writer.write(buffer);
                    } finally {
                        writer.close();
                    }
                }
            };
    }

    protected int skipTo(String string, char[] buffer, int pos) {
        for (; pos < buffer.length; pos++) {
            if (matches(string, buffer, pos)) {
                return pos + string.length();
            }
        }

        return -1;
    }

    protected boolean matches(String string, char[] buffer, int pos) {
        int i = 0;

        for (; (i < string.length()) && (pos < buffer.length); i++, pos++) {
            if (string.charAt(i) != buffer[pos]) {
                return false;
            }
        }

        return i == string.length();
    }

    public String translate(RepoArtifactId id, String pattern) {
        return CopyPathTask.translate(projectModel.getAntProject(), id, pattern);
    }

    public void copyPath(List path, File destination, String pattern, boolean includeLicenses) {
        CopyPathTask.copyPath(projectModel.getAntProject(), path, destination, pattern);
    }

    public void addArtifact(RepoArtifactId id, String description, Map exportedPaths) {
        Artifact artifact = new Artifact(id);
        artifact.setDescription(description);

        for (Iterator i = exportedPaths.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            artifact.addExportedPath((String)entry.getKey(), (String)entry.getValue());
        }

        projectModel.getProject().addArtifact(artifact);
    }
}
