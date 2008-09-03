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


package ws.quokka.core.main.ant.task;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.UpToDate;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.ExceptionHandler;
import ws.quokka.core.bootstrap_util.TaskLogger;
import ws.quokka.core.bootstrap_util.VoidExceptionHandler;
import ws.quokka.core.main.ant.DefaultModelFactory;
import ws.quokka.core.model.Artifact;
import ws.quokka.core.model.Dependency;
import ws.quokka.core.model.DependencySet;
import ws.quokka.core.model.ProjectModel;
import ws.quokka.core.plugin_spi.support.AntUtils;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.Repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Collection;
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
public class BuildPathTask extends Task {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private List projectCollections = new ArrayList();
    private AntUtils utils;
    private String id;
    private boolean sequence = true;
    private File cache;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void init() throws BuildException {
        utils = new AntUtils(getProject());
    }

    public void add(ResourceCollection rc) {
        projectCollections.add(rc);
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSequence(boolean sequence) {
        this.sequence = sequence;
    }

    public void setCache(File cache) {
        this.cache = cache;
    }

    public void execute() throws BuildException {
        if (id == null) {
            throw new BuildException("id attribute is mandatory for buildpath", getLocation());
        }

        if (cache == null) {
            cache = utils.normalise(getProject().getProperty("quokka.project.targetDir") + "/build-sequence/" + id
                    + ".txt");
        }

        // Collect all of the project files
        List projects = new ArrayList();

        for (Iterator i = projectCollections.iterator(); i.hasNext();) {
            ResourceCollection rc = (ResourceCollection)i.next();

            for (Iterator j = rc.iterator(); j.hasNext();) {
                Resource resource = (Resource)j.next();
                Assert.isTrue(resource instanceof FileResource, getLocation(), "Collections of files are expected");
                projects.add(((FileResource)resource).getFile());
            }
        }

        List seq = null;

        if (sequence) {
            // See if the build sequence is already generated and is up to date
            UpToDate upToDate = new UpToDate();
            upToDate.setTargetFile(cache);
            upToDate.setProject(getProject());

            for (Iterator i = projects.iterator(); i.hasNext();) {
                File file = (File)i.next();
                upToDate.addSrcfiles(utils.toFileSet(file));
            }

            // Load or generate the sequence
            if (upToDate.eval()) {
                log("Loading existing build configuration as it is up to date", Project.MSG_VERBOSE);
                seq = loadSequence(cache);

                if (!matches(projects, seq)) {
                    log("Invalidating loaded sequence as project list no longer matches", Project.MSG_VERBOSE);
                    seq = null;
                }
            }

            if (seq == null) {
                log("Generating build sequence", Project.MSG_VERBOSE);
                seq = generateSequence(projects);
                saveSequence(seq, cache);
            }
        } else {
            seq = projects;
        }

        // Convert the sequence into a path and set it as a reference
        Path path = new Path(getProject());

        for (Iterator i = seq.iterator(); i.hasNext();) {
            File file = (File)i.next();
            Path element = new Path(getProject(), file.getParentFile().getAbsolutePath());
            path.add(element);
        }

        getProject().addReference(id, path);
    }

    /**
     * Checks that the loaded sequence is the same set of files as the input list.
     * It can be a mismatch in cases where the filtering rules (e.g. includes) are changed in the task definition
     */
    private boolean matches(List projects, List sequence) {
        return toSet(projects).equals(toSet(sequence));
    }

    private Set toSet(List projects) {
        Set names = new HashSet();

        for (Iterator i = projects.iterator(); i.hasNext();) {
            File file = (File)i.next();
            names.add(file.getAbsolutePath());
        }

        return names;
    }

    private List generateSequence(List projects) {
        DefaultModelFactory factory = new DefaultModelFactory();
        Repository repository = (Repository)getProject().getReference("quokka.project.repository");
        factory.setRepository(repository);

        // 1st pass: map artifacts generated from each project to a target representing their project files
        Project tempProject = new Project();

        Map targetsByArtifact = new HashMap();
        Map targetsByProject = new HashMap();
        List models = new ArrayList();

        for (Iterator i = projects.iterator(); i.hasNext();) {
            File projectFile = (File)i.next();

            // For now, we assume the default profile will contain all dependencies
            ProjectModel model = factory.getProjectModel(projectFile, Collections.EMPTY_LIST, true, null,
                    new TaskLogger(this), getProject());
            models.add(model);

            // Create a target for each project file
            Target target = new Target();
            target.setName(projectFile.getAbsolutePath());
            tempProject.addTarget(target);
            targetsByProject.put(model.getProject().getProjectFile(), target);

            // Map the artifacts to the target
            for (Iterator j = model.getProject().getArtifacts().iterator(); j.hasNext();) {
                Artifact artifact = (Artifact)j.next();
                targetsByArtifact.put(artifact.getId(), target);
            }
        }

        // 2nd pass: Process project dependencies. Where a dependency uses an artifactId found in the 1st pass,
        // set up a dependency between the projects.
        for (Iterator i = models.iterator(); i.hasNext();) {
            ProjectModel model = (ProjectModel)i.next();
            Target projectTarget = (Target)targetsByProject.get(model.getProject().getProjectFile());

            Set dependencies = getDependencies(model);

            // Where there is a dependency between projects, mirror it in the targets
            for (Iterator j = dependencies.iterator(); j.hasNext();) {
                RepoArtifactId id = (RepoArtifactId)j.next();
                Target dependencyTarget = (Target)targetsByArtifact.get(id);

                if (dependencyTarget != null) {
                    projectTarget.addDependency(dependencyTarget.getName());
                }
            }
        }

        // Use ANT's target dependency mechanism to produce a build sequence
        String anyTarget = ((Target)targetsByArtifact.values().iterator().next()).getName();
        Collection sequencedTargets = tempProject.topoSort(anyTarget, tempProject.getTargets());

        // Transform targets into a list of files
        List sequence = new ArrayList();

        for (Iterator i = sequencedTargets.iterator(); i.hasNext();) {
            Target seqTarget = (Target)i.next();
            sequence.add(new File(seqTarget.getName()));
        }

        return sequence;
    }

    private Set getDependencies(ProjectModel model) {
        Set dependencies = new HashSet();
        getDependencies(dependencies, model.getProject().getDependencySet());

        return dependencies;
    }

    private void getDependencies(Set dependencies, DependencySet dependencySet) {
        for (Iterator i = dependencySet.getDependencies().iterator(); i.hasNext();) {
            Dependency dependency = (Dependency)i.next();

            if (dependency.getClass().equals(Dependency.class)) {
                dependencies.add(dependency.getId());
            }
        }

        for (Iterator i = dependencySet.getSubsets().iterator(); i.hasNext();) {
            DependencySet subSet = (DependencySet)i.next();

            //            dependencies.add(subSet.getArtifact().getId());
            getDependencies(dependencies, subSet);
        }
    }

    private void saveSequence(final List seq, final File file) {
        new VoidExceptionHandler() {
                public void run() throws IOException {
                    utils.mkdir(file.getParentFile());

                    Writer writer = new BufferedWriter(new FileWriter(file));

                    try {
                        for (Iterator i = seq.iterator(); i.hasNext();) {
                            File file2 = (File)i.next();
                            writer.write(file2.getAbsolutePath() + "\n");
                        }
                    } finally {
                        writer.close();
                    }
                }
            };
    }

    private List loadSequence(final File file) {
        return (List)new ExceptionHandler() {
                public Object run() throws IOException {
                    List sequence = new ArrayList();
                    BufferedReader reader = new BufferedReader(new FileReader(file));

                    try {
                        while (true) {
                            String line = reader.readLine();

                            if (line == null) {
                                break;
                            }

                            sequence.add(new File(line));
                        }
                    } finally {
                        reader.close();
                    }

                    return sequence;
                }
            }.soften();
    }
}
