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
import org.apache.tools.ant.taskdefs.Input;

import ws.quokka.core.plugin_spi.support.AntUtils;


/**
 *
 */
public class InputUnlessSetTask extends Task {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String property;
    private String message;
    private String validargs;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void setProperty(String property) {
        this.property = property;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setValidargs(String validargs) {
        this.validargs = validargs;
    }

    public void execute() throws BuildException {
        message = (message == null) ? ("Enter value for '" + property + "': ") : message;

        if (getProject().getProperty(property) == null) {
            Input input = (Input)new AntUtils(getProject()).init(new Input(), "input");
            input.setValidargs(validargs);
            input.setAddproperty(property);
            input.setMessage(message);
            input.perform();
        }
    }
}
