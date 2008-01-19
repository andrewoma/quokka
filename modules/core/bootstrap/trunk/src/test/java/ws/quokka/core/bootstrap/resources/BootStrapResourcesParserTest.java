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


package ws.quokka.core.bootstrap.resources;

import ws.quokka.core.test.AbstractTest;

import java.io.File;


/**
 *
 */
public class BootStrapResourcesParserTest extends AbstractTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testParse() {
        File cacheDir = new File(normalise(getModuleHome().getAbsolutePath()
                    + "/target/test/BootStrapResourcesParserTest"));
        BootStrapResourcesParser parser = new BootStrapResourcesParser();
        BootStrapResources resources = parser.parse(getTestCaseResource("bootstrap.xml"), getTestCaseResource("libs"),
                cacheDir);
        System.out.println(resources.toString());
    }
}
