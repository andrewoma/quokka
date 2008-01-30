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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 *
 */
public class PluginState {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Map state;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public PluginState(Map state) {
        this.state = state;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public synchronized Object get(String key, Object defaultValue) {
        Object value = state.get(key);

        if (value == null) {
            state.put(key, defaultValue);

            return defaultValue;
        }

        return value;
    }

    public synchronized Object put(String key, Object value) {
        if (state.containsKey(key)) {
            return state.get(key);
        } else {
            state.put(key, value);

            return value;
        }
    }

    public synchronized List getList(String key, boolean create) {
        List list = (List)state.get(key);

        if ((list == null) && create) {
            list = new ArrayList();
            state.put(key, list);
        }

        return list;
    }

    public synchronized Path getPath(String key, Project project) {
        Path path = (Path)state.get(key);

        if ((path == null) && (project != null)) {
            path = new Path(project);
            state.put(key, path);
        }

        return path;
    }

    public Map getState() {
        return state;
    }
}
