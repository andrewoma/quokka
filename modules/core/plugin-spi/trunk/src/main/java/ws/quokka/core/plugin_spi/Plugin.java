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


/**
 * Plugin is the minimal interface that must be implemented to be considered as a plugin.
 * It is more common to inherit from {@link ws.quokka.core.plugin_spi.support.AbstractPlugin}.
 * A plugin may also implement additonal interfaces, such as {@link ResourcesAware}. The
 * values of such interfaces with be set prior to {@link #initialise()} being called.
 * <br>
 * Note: a new instance of plugin is created for each target. In fact a new class loader is
 * created for each target. This must be considered if state is passed from one target to another.
 */
public interface Plugin {
    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Called after any resources have been injected.
     */
    void initialise();

    /**
     * This {@link Runnable#run()} method will be invoked to execute the plugin
     */
    Runnable getTarget(String name);
}
