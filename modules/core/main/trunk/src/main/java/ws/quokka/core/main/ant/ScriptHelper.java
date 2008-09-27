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

import ws.quokka.core.bootstrap_util.ProjectLogger;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.version.Version;

import java.util.Map;


/**
 * ScriptHelper exposes functionality to scripts and is bound to the 'quokka' object.
 */
public class ScriptHelper {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private DefaultProjectModel projectModel;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public ScriptHelper(DefaultProjectModel projectModel) {
        this.projectModel = projectModel;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void execute(String pluginId, String template, Map properties) {
        RepoArtifactId id = RepoArtifactId.parse(pluginId).mergeDefaults();
        id = new RepoArtifactId(id.getGroup(), id.getName(), "plugin", (Version)null);

        TargetInstance targetInstance = projectModel.createTargetInstance(id, template, properties,
                new ProjectLogger(projectModel.getAntProject()));

        try {
            targetInstance.getTarget().run();
        } finally {
            targetInstance.cleanUp();
        }
    }
}
