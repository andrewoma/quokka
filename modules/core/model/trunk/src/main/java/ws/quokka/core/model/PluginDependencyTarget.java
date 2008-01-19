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

import ws.quokka.core.util.AnnotatedObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 *
 */
public class PluginDependencyTarget extends AnnotatedObject {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String name;
    private String prefix;
    private String template;
    private String alias;
    private List dependencies = new ArrayList();

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public PluginDependencyTarget(String name) {
        this.name = name;
    }

    public PluginDependencyTarget() {
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isValid() {
        return (name != null) && (
            ((prefix == null) && (template == null)) || ((prefix != null) && (template != null))
        );
    }

    public String toShortString() {
        return name + ((template == null) ? "" : ("(" + template + ", " + prefix + ")"));
    }

    public List getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public void addDependency(String dependency) {
        dependencies.add(dependency);
    }
}
