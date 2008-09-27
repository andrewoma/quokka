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

import ws.quokka.core.test.AbstractTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 *
 */
public class ProfilesMatcherTest extends AbstractTest {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final Set NONE = Collections.EMPTY_SET;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    ProfilesMatcher matcher = new ProfilesMatcher();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testProfileVsDefault() {
        assertTrue(!matcher.matches("p1", NONE));
        assertTrue(!matcher.matches("p1 | p2", NONE));
    }

    public void testProfileInActive() {
        assertTrue(matcher.matches("p1", s("p1")));
        assertTrue(matcher.matches("p1", s("p1", "p2")));
        assertTrue(matcher.matches("p2", s("p1", "p2")));
        assertTrue(matcher.matches("p1 | p2", s("p1", "p2")));
    }

    public void testNegativeVsDefault() {
        assertTrue(matcher.matches("!p1", NONE));
        assertTrue(matcher.matches("!p1 & !p2", NONE));
    }

    public void testNegativeInActive() {
        assertTrue(!matcher.matches("!p1", s("p1")));
        assertTrue(!matcher.matches("!p1", s("p1", "p2")));
        assertTrue(!matcher.matches("!p1 & !p2", s("p1", "p2")));
        assertTrue(!matcher.matches("!p1 + !p2", s("p1", "p2")));
        assertTrue(!matcher.matches("!p2", s("p1", "p2")));
    }

    public void testNegativeNotInActive() {
        assertTrue(matcher.matches("!p1", s("p3")));
        assertTrue(matcher.matches("!p1", s("p3", "p4")));
        assertTrue(matcher.matches("!p1 & !p2", s("p3")));
        assertTrue(matcher.matches("!p1 & !p2", s("p3", "p4")));
    }

    public Set s(Object o1) {
        return new HashSet(Arrays.asList(new Object[] { o1 })); // needs to be modifiable
    }

    public Set s(Object o1, Object o2) {
        return new HashSet(Arrays.asList(new Object[] { o1, o2 }));
    }
}
