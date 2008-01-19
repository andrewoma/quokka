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


package ws.quokka.core.main.ant;

import org.apache.tools.ant.util.FileUtils;

import ws.quokka.core.model.Project;
import ws.quokka.core.test.AbstractTest;
import ws.quokka.core.util.Strings;

import java.io.File;
import java.io.IOException;


/**
 *
 */
public class ProjectMetadataTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private ProjectMetadata metadata;
    private File original;
    private File backup;

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        super.setUp();

        DefaultProjectModel model = new DefaultProjectModel();
        Project project = new Project();

        original = new File(getModuleHome(), "target/test-compile/ProjectMetadataTest/sample-build.xml");
        original = new File(normalise(original.getAbsolutePath()));

        backup = new File(original.getParentFile(), "sample-build-backup.xml");
        project.setProjectFile(backup);
        model.setProject(project);
        metadata = new ProjectMetadata(model);
    }

    public void testMatches() {
        assertTrue(metadata.matches("<!--", "12<!--345".toCharArray(), 2));
        assertTrue(!metadata.matches("<!--", "12<!--345".toCharArray(), 1));
        assertTrue(!metadata.matches("<!--", "12<!--345".toCharArray(), 3));
        assertTrue(metadata.matches("<!--", "<!--".toCharArray(), 0));
        assertTrue(!metadata.matches("<!--", "<!-".toCharArray(), 0));
        assertTrue(metadata.matches("<!--", "<!--\n\t ".toCharArray(), 0));
    }

    public void testSkipTo() {
        assertEquals(4, metadata.skipTo("-->", "1-->2".toCharArray(), 0));
        assertEquals(3, metadata.skipTo("-->", "-->2".toCharArray(), 0));
        assertEquals(3, metadata.skipTo("-->", "-->".toCharArray(), 0));
    }

    public void testRolloverVersion() {
        String xml = "abc<artifacts group=\"somegroup\" version=\"1.0\">def";
        assertSameVersion(xml);

        xml = "<!-- Hello there -->\n" + "  <!-- <artifacts group=\"commented\" version=\"commented\">\n" + "  -->"
            + "  <artifacts\n" + "    group=\"someGroup\"\n" + "    version=\"someVersion\">\n" + "    <artifact/>\n"
            + "  </artifacts>";

        String expected = Strings.replace(xml, "someVersion", "1.0");
        String out = new String(metadata.rolloverVersion(xml.toCharArray(), "1.0"));
        assertEquals(expected, out);
    }

    public void testRolloverVersionFull() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE project PUBLIC \"quokka.ws/dtd/project-1.0-m01\" \"http://quokka.ws/dtd/project-1.0-m01.dtd\">\n"
            + "\n" + "<project name=\"core.main\">\n"
            + "    <artifacts group=\"quokka.core.main\" version=\"1.0-m01-ss\">\n"
            + "        <artifact paths=\"runtime, dist\"/>\n" + "    </artifacts>\n" + "\n" + "    <dependency-set>\n"
            + "        <path id=\"dist\" description=\"Libraries required for the runtime distribution\"/>\n"
            + "        <dependency group=\"apache.ant\" version=\"1.7.0\" paths=\"dist+\"/>\n"
            + "        <dependency-set group=\"quokka.internal.module-depset\" version=\"1.0-m01-ss\"/>\n"
            + "    </dependency-set>\n" + "</project>";

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE project PUBLIC \"quokka.ws/dtd/project-1.0-m01\" \"http://quokka.ws/dtd/project-1.0-m01.dtd\">\n"
            + "\n" + "<project name=\"core.main\">\n"
            + "    <artifacts group=\"quokka.core.main\" version=\"1.0-m01\">\n"
            + "        <artifact paths=\"runtime, dist\"/>\n" + "    </artifacts>\n" + "\n" + "    <dependency-set>\n"
            + "        <path id=\"dist\" description=\"Libraries required for the runtime distribution\"/>\n"
            + "        <dependency group=\"apache.ant\" version=\"1.7.0\" paths=\"dist+\"/>\n"
            + "        <dependency-set group=\"quokka.internal.module-depset\" version=\"1.0-m01-ss\"/>\n"
            + "    </dependency-set>\n" + "</project>";
        assertEquals(expected, new String(metadata.rolloverVersion(xml.toCharArray(), "1.0-m01")));
    }

    public void testReplaceVersion() {
        assertEquals("<artifacts version='3.2.1'>", metadata.replaceVersion("<artifacts version='1.0'>", "3.2.1"));
        assertEquals("<artifacts pony='club' version='3.2.1'>",
            metadata.replaceVersion("<artifacts pony='club' version='1.0'>", "3.2.1"));
        assertEquals("<artifacts pony='club'   version  =  '3.2.1'>",
            metadata.replaceVersion("<artifacts pony='club'   version  =  '1.0'>", "3.2.1"));
        assertEquals("<artifacts pony='club'   version  =  '3.2.1'>",
            metadata.replaceVersion("<artifacts pony='club'   version  =  ' 1.0 '>", "3.2.1"));
        assertEquals("<artifacts  \n pony  =  'club'  \n  version  =  '3.2.1' \n>",
            metadata.replaceVersion("<artifacts  \n pony  =  'club'  \n  version  =  '1.0' \n>", "3.2.1"));
        assertEquals("<artifacts version='3.2.1'>", metadata.replaceVersion("<artifacts version=''>", "3.2.1"));
        assertEquals("<artifacts version=\"3.2.1\">", metadata.replaceVersion("<artifacts version=\"1.0\">", "3.2.1"));
        assertEquals("<artifacts version=\"3.2.1\">", metadata.replaceVersion("<artifacts version=\"1\">", "3.2.1"));
    }

    private void assertSameVersion(String xml) {
        String out = new String(metadata.rolloverVersion(xml.toCharArray(), "1.0"));
        assertEquals(xml, out);
    }

    public void testRolloverVersionWithFile() throws IOException {
        FileUtils.getFileUtils().copyFile(original, backup, null, true, false);
        metadata.rolloverVersion("newVersion");
        assertTrue(contains(backup, new String[] { "version=\"newVersion\"" }));
    }
}
