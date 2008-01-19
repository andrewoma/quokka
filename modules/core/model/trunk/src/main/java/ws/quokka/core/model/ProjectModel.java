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


package ws.quokka.core.model;

import ws.quokka.core.plugin_spi.BuildResources;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.util.AnnotatedProperties;

import java.util.List;
import java.util.Map;


/**
 *
 */
public interface ProjectModel {
    //~ Methods --------------------------------------------------------------------------------------------------------

    Project getProject();

    Map getProjectPaths();

    List getProjectPath(String id, boolean mergeWithCore, boolean flatten);

    List getPluginPath(Plugin plugin, String id, boolean mergeWithCore, boolean flatten);

    Map getTargets();

    AnnotatedProperties getProperties();

    BuildResources getBuildResources();

    List getAliases();

    Repository getRepository();
}
