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

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * BuildResourcesListener recreates extracted build resources if they have been cleared.
 * This can happen if the temporary directory is deleted prior to the execution of a
 * target. This usually occurs if a target is executed immediately after "clean".
 */
public class BuildResourcesListener implements BuildListener {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private DefaultBuildResources buildResources;
    private Set resources = new HashSet();
    private Project project;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public BuildResourcesListener(DefaultBuildResources buildResources, Project project) {
        this.buildResources = buildResources;
        this.project = project;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public DefaultBuildResources getBuildResources() {
        return buildResources;
    }

    public void addResource(String key) {
        resources.add(key);
    }

    public void buildStarted(BuildEvent event) {
    }

    public void buildFinished(BuildEvent event) {
    }

    public void targetStarted(BuildEvent event) {
        if ((project == event.getProject()) && (resources.size() != 0) && !buildResources.getTempDir().exists()) {
            event.getProject().log("Extracting build resources", Project.MSG_DEBUG);

            for (Iterator i = resources.iterator(); i.hasNext();) {
                String key = (String)i.next();
                buildResources.getFileOrDir(key);
            }
        }
    }

    public void targetFinished(BuildEvent event) {
    }

    public void taskStarted(BuildEvent event) {
    }

    public void taskFinished(BuildEvent event) {
    }

    public void messageLogged(BuildEvent event) {
    }
}
