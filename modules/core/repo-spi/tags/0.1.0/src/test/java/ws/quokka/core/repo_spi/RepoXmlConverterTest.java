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


package ws.quokka.core.repo_spi;

import ws.quokka.core.test.AbstractTest;
import ws.quokka.core.util.xml.XmlConverter;
import ws.quokka.core.version.Version;


/**
 *
 */
public class RepoXmlConverterTest extends AbstractTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testConvert() {
        RepoArtifact artifact = new RepoArtifact(new RepoArtifactId("group", "name", "type", new Version("version")));

        //        artifact.setLocalCopy(new File("C:\\Temp"));
        RepoDependency dependency = new RepoDependency();
        dependency.setId(new RepoArtifactId("dgroup", "dname", "dtype", new Version("dversion")));

        RepoPathSpec pathSpec = new RepoPathSpec();
        pathSpec.setTo("runtime");
        pathSpec.setFrom("runtime");
        pathSpec.setDescend(Boolean.TRUE);
        pathSpec.setMandatory(Boolean.TRUE);
        pathSpec.setOptions("some options");
        dependency.addPathSpec(pathSpec);
        artifact.addDependency(dependency);

        RepoPath path = new RepoPath();
        path.setId("runtime");
        path.setDescription("Runtime path");
        artifact.addPath(path);

        XmlConverter converter = RepoXmlConverter.getXmlConverter();
        String xml = converter.toXml(artifact, "artifact");
        System.out.println(xml);

        RepoArtifact converted = (RepoArtifact)converter.fromXml(RepoArtifact.class, xml);
        System.out.println(artifact);
        System.out.println(converted);
        assertEquals(artifact.getId(), converted.getId());
        assertEquals(artifact, converted);
    }
}
