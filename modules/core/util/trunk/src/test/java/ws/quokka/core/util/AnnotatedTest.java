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

import java.util.Map;


/**
 *
 */
public class AnnotatedTest extends TestCase {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    Annotations annotations = new Annotations();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testClone() {
        annotations.put("key1", "value1");

        Annotations clone = (Annotations)annotations.clone();
        clone.put("key1", "value2");
        assertEquals("value1", annotations.get("key1"));
    }

    public void testEntrySet() {
        annotations.put("key1", "value1");

        Map.Entry entry = (Map.Entry)annotations.entrySet().iterator().next();
        assertEquals("key1", entry.getKey());
        assertEquals("value1", entry.getValue());
    }
}
