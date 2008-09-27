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
 * NullLogger consumes all messages, logging nothing
 */
public class NullLogger implements Logger {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public boolean isDebugEnabled() {
        return false;
    }

    public boolean isInfoEnabled() {
        return false;
    }

    public boolean isWarnEnabled() {
        return false;
    }

    public boolean isErrorEnabled() {
        return false;
    }

    public boolean isVerboseEnabled() {
        return false;
    }

    public boolean isEnabled(int level) {
        return false;
    }

    public void debug(String message) {
    }

    public void verbose(String message) {
    }

    public void warn(String message) {
    }

    public void error(String message) {
    }

    public void info(String message) {
    }
}
