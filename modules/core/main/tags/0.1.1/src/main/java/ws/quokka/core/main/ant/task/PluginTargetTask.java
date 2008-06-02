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
import org.apache.tools.ant.Task;

import ws.quokka.core.bootstrap_util.TaskLogger;
import ws.quokka.core.main.ant.DefaultProjectModel;
import ws.quokka.core.model.Target;

import java.util.Properties;


/**
 *
 */
public class PluginTargetTask extends Task {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Target pluginTarget;
    private DefaultProjectModel projectModel;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void setPluginTarget(Target pluginTarget) {
        this.pluginTarget = pluginTarget;
    }

    public void setProjectModel(DefaultProjectModel projectModel) {
        this.projectModel = projectModel;
    }

    public void execute() throws BuildException {
        log("Executing quokka target '" + getTaskName() + "'", Project.MSG_DEBUG);

        if (!pluginTarget.isAbstract()) {
            log("Executing '" + pluginTarget.getName() + "' from plugin '"
                + pluginTarget.getPlugin().getArtifact().getId().toShortString() + "'", Project.MSG_VERBOSE);

            Runnable task = projectModel.createTargetInstance(pluginTarget, new Properties(), new TaskLogger(this));
            task.run();
        }
    }
}
