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


package ws.quokka.core.plugin_spi.support;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.resources.Resources;

import ws.quokka.core.bootstrap_util.Reflect;

import java.lang.reflect.Method;

import java.util.List;


/**
 * ResourcesList exposes the underlying list of resources
 */
public class ResourcesList extends Resources {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final Object[] NESTED_PARAMETERS = new Object[] {  };
    private static final Method NESTED = new Reflect().getMethod(Resources.class, "getNested", new Class[] {  });

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public ResourcesList(Project project) {
        setProject(project);
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public List getNested() {
        return (List)new Reflect().invoke(NESTED, this, NESTED_PARAMETERS);
    }
}
