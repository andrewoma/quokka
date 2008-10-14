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

import org.apache.tools.ant.BuildException;

import org.xml.sax.Locator;


/**
 *
 */
public class AssertTest extends TestCase {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testTrue() {
        //        new Assert();
        Assert.isTrue(true, "");
    }

    public void testFalse() {
        try {
            Assert.isTrue(false, "message");
            fail();
        } catch (BuildException e) {
            assertEquals("message", e.getMessage());
        }
    }

    public void testLocator() {
        try {
            Assert.isTrue(true, getLocator(), "message");
            Assert.isTrue(false, getLocator(), "message");
            fail();
        } catch (BuildException e) {
            assertEquals(13, e.getLocation().getLineNumber());
            assertEquals(14, e.getLocation().getColumnNumber());
            assertEquals("systemId", e.getLocation().getFileName());
            assertEquals("message", e.getMessage());
        }
    }

    private Locator getLocator() {
        return new Locator() {
                public String getPublicId() {
                    return "publicId";
                }

                public String getSystemId() {
                    return "systemId";
                }

                public int getLineNumber() {
                    return 13;
                }

                public int getColumnNumber() {
                    return 14;
                }
            };
    }
}
