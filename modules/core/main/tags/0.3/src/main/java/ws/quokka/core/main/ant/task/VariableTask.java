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


package ws.quokka.core.main.ant.task;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.PropertiesUtil;
import ws.quokka.core.util.Strings;


/*
 * Original Copyright: Taken from Ant-Contrib 1.0b3
 *
 * Copyright (c) 2001-2004 Ant-Contrib project.  All rights reserved.
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

/**
 * Similar to Property, but this property is mutable. In fact, much of the code
 * in this class is copy and paste from Property. In general, the standard Ant
 * property should be used, but occasionally it is useful to use a mutable
 * property.
 * <p/>
 * This used to be a nice little task that took advantage of what is probably
 * a flaw in the Ant Project API -- setting a "user" property programatically
 * causes the project to overwrite a previously set property. Now this task
 * has become more violent and employs a technique known as "object rape" to
 * directly access the Project's private property hashtable.
 * <p>Developed for use with Antelope, migrated to ant-contrib Oct 2003.
 *
 * @author Dale Anson, danson@germane-software.com
 * @version $Revision: 1.6 $
 * @since Ant 1.5
 */
public class VariableTask extends Task {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String value = "";
    private String name = null;
    private boolean unset = false;

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Set the name of the property. Required unless 'file' is used.
     * @param name the name of the property.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the value of the property. Optional, defaults to "".
     * @param value the value of the property.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Determines whether the property should be removed from the project.
     * Default is false. Once  removed, conditions that check for property
     * existence will find this property does not exist.
     * @param b set to true to remove the property from the project.
     */
    public void setUnset(boolean b) {
        unset = b;
    }

    /**
     * Execute this task.
     * @throws BuildException Description of the Exception
     */
    public void execute() throws BuildException {
        Assert.isTrue(!Strings.isBlank(name)
            && ((unset && Strings.isBlank(value)) || (!unset && !Strings.isBlank(value))),
            "The 'name' attribute and either 'unset' or 'value' are required");

        if (unset) {
            removeProperty(name);

            return;
        }

        // adjust the property value if necessary -- is this necessary?
        // Doesn't Ant do this automatically?
        value = getProject().replaceProperties(value);
        removeProperty(name);
        getProject().setUserProperty(name, value);
    }

    /**
     * Remove a property from the project's property table and the userProperty table.
     * Note that Ant 1.6 uses a helper for this.
     */
    private void removeProperty(String name) {
        PropertiesUtil.clearProperty(getProject(), name);
    }
}
