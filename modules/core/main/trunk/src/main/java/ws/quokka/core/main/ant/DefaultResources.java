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
import org.apache.tools.ant.types.Path;

import ws.quokka.core.bootstrap_util.Logger;
import ws.quokka.core.model.Target;
import ws.quokka.core.plugin_spi.BuildResources;
import ws.quokka.core.plugin_spi.PluginState;
import ws.quokka.core.plugin_spi.Resources;


/**
 *
 */
public class DefaultResources implements Resources {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Target target;
    private Project antProject;
    private PluginState pluginState;
    private DefaultProjectModel projectModel;
    private Logger logger;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public DefaultResources(DefaultProjectModel runner, Target target, Project antProject, PluginState pluginState,
        Logger logger) {
        this.target = target;
        this.antProject = antProject;
        this.pluginState = pluginState;
        this.projectModel = runner;
        this.logger = logger;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public Path getPathGroupAsPath(String name) {
        return projectModel.toAntPath(projectModel.resolvePathGroup(target, name));
    }

    public BuildResources getBuildResources() {
        return projectModel.getBuildResources();
    }

    public String getNameSpace() {
        return target.getPlugin().getNameSpace();
    }

    public boolean isTargetEnabled(String name) {
        Target target = (Target)projectModel.getTargets().get(name);

        return (target != null) && (!target.isAbstract() || (target.isAbstract() && target.isImplemented()));
    }

    public PluginState getPluginState() {
        return pluginState;
    }

    public org.apache.tools.ant.Project getProject() {
        return antProject;
    }

    public org.apache.tools.ant.Project getParentProject() {
        return (org.apache.tools.ant.Project)antProject.getReference("quokka.parentProject");
    }

    public String getTargetName() {
        return (target.getTemplateName() != null) ? target.getTemplateName() : target.getName();
    }

    public Path getProjectPath(String name) {
        return projectModel.toAntPath(projectModel.getProjectPath(name, false, true));
    }

    public String getPrefix() {
        return target.getPrefix();
    }

    public BuildResources getLocalResources() {
        return target.getPlugin().getLocalResources();
    }

    public Logger getLogger() {
        return logger;
    }
}
