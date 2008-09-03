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
    private List dependencyOf = new ArrayList();

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
        return (template == null) || (prefix != null);
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

    public List getDependencyOf() {
        return Collections.unmodifiableList(dependencyOf);
    }

    public void addDependencyOf(String dependency) {
        dependencyOf.add(dependency);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        PluginDependencyTarget that = (PluginDependencyTarget)o;

        if ((alias != null) ? (!alias.equals(that.alias)) : (that.alias != null)) {
            return false;
        }

        if ((dependencies != null) ? (!dependencies.equals(that.dependencies)) : (that.dependencies != null)) {
            return false;
        }

        if ((name != null) ? (!name.equals(that.name)) : (that.name != null)) {
            return false;
        }

        if ((prefix != null) ? (!prefix.equals(that.prefix)) : (that.prefix != null)) {
            return false;
        }

        if ((template != null) ? (!template.equals(that.template)) : (that.template != null)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = ((name != null) ? name.hashCode() : 0);
        result = (31 * result) + ((prefix != null) ? prefix.hashCode() : 0);
        result = (31 * result) + ((template != null) ? template.hashCode() : 0);
        result = (31 * result) + ((alias != null) ? alias.hashCode() : 0);
        result = (31 * result) + ((dependencies != null) ? dependencies.hashCode() : 0);

        return result;
    }
}
