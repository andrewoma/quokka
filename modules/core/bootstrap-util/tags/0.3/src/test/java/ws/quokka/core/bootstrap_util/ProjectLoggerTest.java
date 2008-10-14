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

import junit.framework.TestCase;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;


/**
 *
 */
public class ProjectLoggerTest extends TestCase {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    protected ResultLogger resultLogger = new ResultLogger();
    protected Project project;

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        project = new Project();
        project.addBuildListener(resultLogger);
        resultLogger.setErrorPrintStream(System.err);
        resultLogger.setOutputPrintStream(System.out);
        project.init();
    }

    public void testDebug() {
        assertMessages(Project.MSG_DEBUG);
    }

    public void testVerbose() {
        assertMessages(Project.MSG_VERBOSE);
    }

    public void testInfo() {
        assertMessages(Project.MSG_INFO);
    }

    public void testWarn() {
        assertMessages(Project.MSG_WARN);
    }

    public void testError() {
        assertMessages(Project.MSG_ERR);
    }

    public Logger createLogger() {
        return new ProjectLogger(project);
    }

    public void assertMessages(int level) {
        resultLogger.clear();
        resultLogger.setMessageOutputLevel(level);

        Logger logger = createLogger();

        switch (level) {
        case Project.MSG_DEBUG:
            assertTrue(logger.isDebugEnabled());

            if (logger.isDebugEnabled()) {
                logger.debug("debug");
            }

            assertEquals("debug", getMessage());

        case Project.MSG_VERBOSE:

            if (level <= Project.MSG_VERBOSE) {
                assertTrue(!logger.isDebugEnabled());
            }

            assertTrue(logger.isVerboseEnabled());
            logger.verbose("verbose");
            assertEquals("verbose", getMessage());

        case Project.MSG_INFO:

            if (level <= Project.MSG_INFO) {
                assertTrue(!logger.isDebugEnabled());
                assertTrue(!logger.isVerboseEnabled());
            }

            assertTrue(logger.isInfoEnabled());
            logger.info("info");
            assertEquals("info", getMessage());

        case Project.MSG_WARN:

            if (level <= Project.MSG_WARN) {
                assertTrue(!logger.isDebugEnabled());
                assertTrue(!logger.isVerboseEnabled());
                assertTrue(!logger.isInfoEnabled());
            }

            assertTrue(logger.isWarnEnabled());
            logger.warn("warn");
            assertEquals("warn", getMessage());

        case Project.MSG_ERR:

            if (level <= Project.MSG_ERR) {
                assertTrue(!logger.isDebugEnabled());
                assertTrue(!logger.isVerboseEnabled());
                assertTrue(!logger.isInfoEnabled());
                assertTrue(!logger.isWarnEnabled());
            }

            assertTrue(logger.isErrorEnabled());
            logger.error("error");
            assertEquals("error", getMessage());
        }
    }

    private String getMessage() {
        String message = resultLogger.getLastMessage();

        return message.substring(message.indexOf("]") + 1).trim();
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public static class ResultLogger extends DefaultLogger {
        String lastMessage;

        protected void log(String message) {
            lastMessage = message;
        }

        public String getLastMessage() {
            return lastMessage;
        }

        public void clear() {
            lastMessage = null;
        }
    }
}
