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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 *
 */
public class PluginDependency extends Dependency {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private List targets = new ArrayList();
    private List templates = new ArrayList();
    private boolean useDefaults = true;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public List getTargets() {
        return Collections.unmodifiableList(targets);
    }

    public void addTarget(PluginDependencyTarget target) {
        targets.add(target);
    }

    public List getTemplates() {
        return Collections.unmodifiableList(templates);
    }

    public void addTemplate(String template) {
        templates.add(template);
    }

    public PluginDependencyTarget getTarget(String name) {
        for (Iterator i = targets.iterator(); i.hasNext();) {
            PluginDependencyTarget target = (PluginDependencyTarget)i.next();

            if (target.getName().equals(name)) {
                return target;
            }
        }

        return null;
    }

    public boolean isUseDefaults() {
        return useDefaults;
    }

    public void setUseDefaults(boolean useDefaults) {
        this.useDefaults = useDefaults;
    }
}
