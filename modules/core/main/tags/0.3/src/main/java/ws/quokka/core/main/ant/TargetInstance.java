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

import org.apache.tools.ant.AntClassLoader;

import ws.quokka.core.plugin_spi.Plugin;


/**
 *
 */
public class TargetInstance {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    Plugin plugin;
    Runnable target;
    AntClassLoader loader;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public TargetInstance(Plugin plugin, Runnable target, AntClassLoader loader) {
        this.plugin = plugin;
        this.target = target;
        this.loader = loader;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public Plugin getPlugin() {
        return plugin;
    }

    public Runnable getTarget() {
        return target;
    }

    public void cleanUp() {
        loader.resetThreadContextLoader();
        loader.cleanup();
    }
}
