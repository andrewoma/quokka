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

import org.apache.tools.ant.Project;

import ws.quokka.core.bootstrap_util.Logger;
import ws.quokka.core.main.parser.ProjectParser;
import ws.quokka.core.main.parser.StandardPluginParser;
import ws.quokka.core.model.ModelFactory;
import ws.quokka.core.model.Profiles;
import ws.quokka.core.model.ProjectModel;
import ws.quokka.core.plugin_spi.PluginState;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.util.AnnotatedProperties;

import java.io.File;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 *
 */
public class DefaultModelFactory implements ModelFactory {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Repository repository;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Set getProfiles(File projectFile) {
        // TODO: implement
        return Collections.EMPTY_SET;
    }

    public ProjectModel getProjectModel(File projectFile, List activeProfiles, boolean topLevel,
        AnnotatedProperties projectProperties, Logger log, Project project) {
        if (projectProperties == null) {
            // Only really passed in to prevent reloading. If null load
            projectProperties = ProjectParser.getProjectProperties(projectFile, new HashMap());
        }

        DefaultProjectModel model = new DefaultProjectModel();
        model.setModel(this);

        Profiles profiles = new Profiles(new HashSet(activeProfiles));
        model.setProject(new ProjectParser(projectFile, profiles, repository, topLevel, projectProperties, log).parse());
        model.setRepository(repository);
        model.setProfiles(profiles);

        if (project == null) {
            project = new Project();
            project.init();
        }

        model.setAntProject(project);
        project.setName(model.getProject().getName());

        //        project.setBaseDir(projectFile.getParentFile());
        project.addReference("quokka.pluginState", new PluginState());
        project.setProperty("quokka.project.file", projectFile.getAbsolutePath());

        model.setPluginParser(new StandardPluginParser());
        model.initialise();

        return model;
    }
}
