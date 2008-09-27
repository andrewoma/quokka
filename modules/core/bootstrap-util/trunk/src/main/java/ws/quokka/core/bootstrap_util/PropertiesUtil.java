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


package ws.quokka.core.bootstrap_util;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;


/**
 * PropertiesUtil uses reflection to obtain a reference to Ant's underlying properties store.
 */
public class PropertiesUtil {
    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Uses reflection to gain direct access to the projects properties and returns them in an unmodifiable map.
     * This should be used in place of Project.getProperties() which returns a copy. Copying is wasteful
     * and also means any changes to properties set will not be reflected after the copy is made.
     */
    public static Map getProperties(Project project) {
        return Collections.unmodifiableMap(getProjectProperties(project, "properties"));
    }

    /**
     * Uses reflection to gain direct access to the projects properties.
     * Useful in rare cases to allow removal of properties
     */
    private static Map getProjectProperties(Project project, String field) {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(project);
        Reflect reflect = new Reflect();

        return (Hashtable)reflect.get(reflect.getField(PropertyHelper.class, field), ph);
    }

    /**
     * Clears a project property from the underlying Ant project maps
     */
    public static void clearProperty(Project project, String property) {
        Map properties = getProjectProperties(project, "properties");
        properties.remove(property);
        properties = getProjectProperties(project, "userProperties");
        properties.remove(property);
    }
}
