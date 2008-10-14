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


package ws.quokka.core.util;

import junit.framework.TestCase;

import org.xml.sax.Locator;

import ws.quokka.core.util.xml.LocatorImpl;


/**
 *
 */
public class AnnotatedObjectTest extends TestCase {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    AnnotatedObject annotatedObject = new AnnotatedObject();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testToString() {
        // Check annotations field is excluded
        assertEquals("AnnotatedObject[]", annotatedObject.toString());
    }

    public void testToShortString() {
        // Check short string default
        assertTrue(annotatedObject.toShortString().startsWith("ws.quokka.core.util.AnnotatedObject@"));
    }

    public void testAnnotations() {
        Locator locator = new LocatorImpl("public", "system", 1, 2);
        annotatedObject.setLocator(locator);

        Locator locatorOut = (Locator)annotatedObject.getAnnotations().get(AnnotatedObject.LOCATOR);
        assertSame(locator, locatorOut);
        locatorOut = annotatedObject.getLocator();
        assertSame(locator, locatorOut);
        annotatedObject.getAnnotations().put(AnnotatedObject.LOCATOR, null);
        assertNull(annotatedObject.getLocator());

        annotatedObject.setAnnotations(null);
        assertNull(annotatedObject.getAnnotations());
    }
}
