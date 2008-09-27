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

import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;

import java.lang.reflect.Field;

import java.util.Iterator;


/**
 * AbstractLogger logs messages using ant's project-based logging mechanism. It extends ant's default
 * mechanism with "isEnabled" methods to check the current level if the logging implementation is
 * an instance of DefaultLogger.
 */
public abstract class AbstractLogger implements Logger {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Project project;
    private int level = Project.MSG_DEBUG;
    private DefaultLogger defaultLogger;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public AbstractLogger(Project project) {
        this.project = project;
        setLevel();
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setLevel() {
        for (Iterator i = project.getBuildListeners().iterator(); i.hasNext();) {
            BuildListener listener = (BuildListener)i.next();

            if (listener instanceof DefaultLogger) {
                level = getLevel(listener);
                defaultLogger = (DefaultLogger)listener;

                break;
            }
        }
    }

    /**
     * Gets the level from the "msgOutputLevel" via reflection as it has been declared "protected"
     */
    private int getLevel(final BuildListener listener) {
        Integer level = (Integer)new ExceptionHandler() {
                    public Object run() throws Exception {
                        Field field = DefaultLogger.class.getDeclaredField("msgOutputLevel");
                        field.setAccessible(true);

                        return field.get(listener);
                    }
                }.soften();

        return level.intValue();
    }

    public boolean isDebugEnabled() {
        return isEnabled(Project.MSG_DEBUG);
    }

    public boolean isInfoEnabled() {
        return isEnabled(Project.MSG_INFO);
    }

    public boolean isWarnEnabled() {
        return isEnabled(Project.MSG_WARN);
    }

    public boolean isErrorEnabled() {
        return isEnabled(Project.MSG_ERR);
    }

    public boolean isVerboseEnabled() {
        return isEnabled(Project.MSG_VERBOSE);
    }

    public boolean isEnabled(int level) {
        return level <= this.level;
    }

    protected void log(String message, int level) {
        project.log(message, level);
    }

    protected void log(Task task, String message, int level) {
        project.log(task, message, level);
    }

    protected void log(Target target, String message, int level) {
        project.log(target, message, level);
    }

    public int getLevel() {
        return level;
    }

    public DefaultLogger getDefaultLogger() {
        return defaultLogger;
    }
}
