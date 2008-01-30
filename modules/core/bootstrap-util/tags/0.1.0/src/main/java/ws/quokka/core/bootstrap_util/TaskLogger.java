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
import org.apache.tools.ant.Task;


/**
 * ProjectLogger logs messages using ant's project-based logging mechanism. If extends ant's default
 * mechanism with "isEnabled" methods to check the current level if the logging implementation is
 * an instance of DefaultLogger.
 */
public class TaskLogger extends AbstractLogger {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Task task;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public TaskLogger(Task task) {
        super(task.getProject());
        this.task = task;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void debug(String message) {
        log(task, message, Project.MSG_DEBUG);
    }

    public void verbose(String message) {
        log(task, message, Project.MSG_VERBOSE);
    }

    public void warn(String message) {
        log(task, message, Project.MSG_WARN);
    }

    public void error(String message) {
        log(task, message, Project.MSG_ERR);
    }

    public void info(String message) {
        log(task, message, Project.MSG_INFO);
    }
}
