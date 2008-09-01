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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;

import org.xml.sax.Locator;


/**
 * Assert provides support for simple assertions that result in BuildExceptions on failure
 */
public class Assert {
    //~ Constructors ---------------------------------------------------------------------------------------------------

    private Assert() {
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new BuildException(message);
        }
    }

    public static void isTrue(boolean condition, Location location, String message) {
        if (!condition) {
            throw new BuildException(message, (location == null) ? Location.UNKNOWN_LOCATION : location);
        }
    }

    public static void isTrue(boolean condition, Locator locator, String message) {
        if (!condition) {
            throw new BuildException(message, (locator == null) ? Location.UNKNOWN_LOCATION : new Location(locator));
        }
    }
}
