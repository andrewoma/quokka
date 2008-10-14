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


/**
 * MockLogger defines a mock implementation of logger for testing purposes
 */
public class MockLogger implements Logger {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private boolean debugEnabled;
    private boolean verboseEnabled;
    private boolean infoEnabled;
    private boolean warnEnabled;
    private boolean errorEnabled;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public MockLogger(boolean debugEnabled, boolean verboseEnabled, boolean infoEnabled, boolean warnEnabled,
        boolean errorEnabled) {
        this.debugEnabled = debugEnabled;
        this.verboseEnabled = verboseEnabled;
        this.infoEnabled = infoEnabled;
        this.warnEnabled = warnEnabled;
        this.errorEnabled = errorEnabled;
    }

    public MockLogger() {
        this(false, false, true, true, true);
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public boolean isInfoEnabled() {
        return infoEnabled;
    }

    public boolean isWarnEnabled() {
        return warnEnabled;
    }

    public boolean isErrorEnabled() {
        return errorEnabled;
    }

    public boolean isVerboseEnabled() {
        return verboseEnabled;
    }

    public boolean isEnabled(int level) {
        return true;
    }

    public void debug(String message) {
        if (debugEnabled) {
            System.out.println(message);
        }
    }

    public void verbose(String message) {
        if (verboseEnabled) {
            System.out.println(message);
        }
    }

    public void warn(String message) {
        if (warnEnabled) {
            System.out.println(message);
        }
    }

    public void error(String message) {
        if (errorEnabled) {
            System.out.println(message);
        }
    }

    public void info(String message) {
        if (infoEnabled) {
            System.out.println(message);
        }
    }
}
