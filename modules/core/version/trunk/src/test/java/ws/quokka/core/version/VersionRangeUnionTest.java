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


package ws.quokka.core.version;

import ws.quokka.core.test.AbstractTest;


/**
 *
 */
public class VersionRangeUnionTest extends AbstractTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testInRange() {
        assertInRange("1", "1");
        assertInRange("1", "1.1");
        assertInRange("1.1", "1.1");
        assertInRange("1.1", "1.1.1");
        assertInRange("1.1", "1.2");
        assertInRange("1.1.1", "1.1.1");
        assertInRange("1.1.1", "1.1.1.1");
        assertInRange("1.1.1", "1.1.2");
        assertInRange("1.1.1.1", "1.1.1.1");
        assertInRange("1.1.1.1", "1.1.1.2");
        assertInRange("1-qual", "1");
        assertInRange("1.1-qual", "1.1");
        assertInRange("1.1.1-qual", "1.1.1");
        assertInRange("1.1.1.1-qual", "1.1.1.1");
        assertInRange("AAA", "AAA");
        assertInRange("0.1", "0.2");
        assertInRange("1.0", "1");
        assertInRange("1.0.0", "1");
        assertInRange("1.0.0.0", "1");
    }

    public void testRepositoryVersions() {
        Version version = new Version("1.2.3.4-qual~10");
        assertEquals(version.getMajor(), 1);
        assertEquals(version.getMinor(), 2);
        assertEquals(version.getMicro(), 3);
        assertEquals(version.getUpdate(), 4);
        assertEquals(version.getQualifier(), "qual");
        assertEquals(version.getRepositoryVersion(), 10);

        version = new Version("somecrapthing~10");
        assertEquals(version.getNonStandardString(), "somecrapthing");
        assertEquals(version.getRepositoryVersion(), 10);

        assertInRange("1", "1~1");
        assertInRange("[1~1,1~1]", "1~1");
        assertNotInRange("1~1", "1");
        assertNotInRange("[1~1,1~1]", "1~2");
    }

    public void testNotInRange() {
        assertNotInRange("1", "1-qual");
        assertNotInRange("1.1", "1");
        assertNotInRange("1.0.1", "1");
        assertNotInRange("1.0.0.1", "1");
        assertNotInRange("1.0.0.1-qual", "1");
        assertNotInRange("100000000", "0");
    }

    public void testBounds() {
        assertBounds("[1,1]", "1-qual", "1.0.0.1", "1");
        assertBounds("[1,3]", "1-qual", "3.0.0.1", "1", "2", "3");
        assertBounds("(1,3]", "1", "3.0.0.1", "1.0.0.1", "2", "3");
        assertBounds("(1,3)", "1", "3", "1.0.0.1", "2", "3-qual");
        assertBounds("[1,3)", "1-qual", "3", "1", "2", "3-qual");
    }

    public void testUnions() {
        assertInRange("1; 2", "3");
        assertInRange("[1,3]; [5,6]", "2");
        assertInRange("[1,3]; [5,6]", "6");
        assertNotInRange("[1,3]; [5,6]", "4");
    }

    public void test() {
        assertInRange("[1.6.0.2,1.6.0.2]", "1.6.0.02");
    }

    public boolean isInRange(String range, String version) {
        VersionRangeUnion union = VersionRangeUnion.parse(range);
        Version version1 = Version.parse(version);

        return union.isInRange(version1);
    }

    public void assertBounds(String range, String under, String over, String in1) {
        assertBounds(range, under, over, in1, null, null);
    }

    public void assertBounds(String range, String under, String over, String in1, String in2) {
        assertBounds(range, under, over, in1, in2, null);
    }

    public void assertBounds(String range, String under, String over, String in1, String in2, String in3) {
        VersionRangeUnion union = VersionRangeUnion.parse(range);
        assertTrue(union.isInRange(Version.parse(in1)));
        assertTrue((in2 == null) || union.isInRange(Version.parse(in2)));
        assertTrue((in3 == null) || union.isInRange(Version.parse(in3)));
        assertTrue(!union.isInRange(Version.parse(under)));
        assertTrue(!union.isInRange(Version.parse(over)));
    }

    public void assertNotInRange(String range, String version) {
        assertTrue(!isInRange(range, version));
    }

    public void assertInRange(String range, String version) {
        assertTrue(isInRange(range, version));
    }
}
