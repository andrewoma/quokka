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


package ws.quokka.core.plugin_spi;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

import ws.quokka.core.bootstrap_util.Logger;


/**
 * Resources is the standard mechanism for a plugin to gain access to the runtime environment.
 * A plugin should implement {@link ResourcesAware} and the Resources implementation will
 * automatically be injected at runtim
 */
public interface Resources {
    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Returns the current Ant project
     */
    Project getProject();

    /**
     * Returns the parent Ant project when currently executing a sub-project via subant. This allows
     * information to passed to the parent project. e.g. The development reports uses this mechanism
     * to pass the report information to the parent for aggregation. It will return null if this
     * is the top level project
     */
    Project getParentProject();

    /**
     * Returns the parent of the given project. Useful for finding ancestors any number of levels above
     * the current project
     */
    Project getParentProject(Project project);

    /**
     * Returns the plugin path group as an Ant path
     */
    Path getPathGroupAsPath(String name);

    /**
     * Returns the name space of the currently executing plugin
     */
    String getNameSpace();

    /**
     * Returns the prefix of the currently executing target
     */
    String getPrefix();

    /**
     * Returns the project-wide build resources
     */
    BuildResources getBuildResources();

    /**
     * Returns the build resources that are local (private) to the plugin
     */
    BuildResources getLocalResources();

    /**
     * Returns true if the named target is enabled. Enabled means that the target is not
     * abstract and has an actual implementation.
     * @param name the name of the target. It must be fully qualified with the name space. e.g. lifecycle:test
     */
    boolean isTargetEnabled(String name);

    /**
     * Returns the plugin state, a globally shared object that plugin targets can use to communicate with
     * one another. At present, each target has it's own class loader, so if a target wants to share
     * state, it must serialize it to something accessible by the core loader (either primitives, or
     * by using Serializable classes).
     */
    PluginState getPluginState();

    /**
     * Returns the plugin state for the given project. Useful if retrieving the plugin state from the
     * parent, or another ancestor.
     */
    PluginState getPluginState(Project project);

    /**
     * Returns the name of the currently executing target. If the target is an instance of a template, the
     * template name is returned.
     */
    String getTargetName();

    /**
     * Returns the named project path as an Ant path
     */
    Path getProjectPath(String name);

    /**
     * Returns the logger that should be used for logging messages to the console
     */
    Logger getLogger();
}
