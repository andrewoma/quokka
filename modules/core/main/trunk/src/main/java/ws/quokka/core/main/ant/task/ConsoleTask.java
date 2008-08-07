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

import org.apache.tools.ant.Task;
import org.apache.tools.ant.input.InputRequest;

import ws.quokka.core.util.Strings;

import java.util.Arrays;
import java.util.Vector;


/**
 *ÊConsoleTask
 */
public class ConsoleTask extends Task {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void execute() {
        getProject().log("Enter one or more comma separated targets, 'exit' to exit, or <enter> to accept the default");

        String defaultTarget = getProject().getDefaultTarget();

        while (true) {
            InputRequest request = new InputRequest("Enter targets");
            request.setDefaultValue(defaultTarget);
            getProject().getInputHandler().handleInput(request);

            String value = request.getInput();
            value = value.equals("") ? defaultTarget : value;
            defaultTarget = value;

            if ("exit".equals(value.toLowerCase())) {
                break;
            }

            Vector targets = new Vector(Arrays.asList(Strings.trim(Strings.split(value, ","))));
            getProject().executeTargets(targets);
        }
    }
}
