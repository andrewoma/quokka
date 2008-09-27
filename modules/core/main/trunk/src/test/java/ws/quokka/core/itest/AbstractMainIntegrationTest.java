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


package ws.quokka.core.itest;

import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * MainIntegrationTest adds the core.main instrumented classes to the front of the class path
 * so that test coverage data is collected for the integration tests
 */
public class AbstractMainIntegrationTest extends IntegrationTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    protected URL[] getCoreClassPath(String path) {
        List classPath = new ArrayList();
        String testPath = System.getProperty("q.junit.instrumentCompiledOutput");

        if (testPath != null) {
            classPath.add(toURL(testPath));
        }

        classPath.addAll(Arrays.asList(super.getCoreClassPath(path)));

        return (URL[])classPath.toArray(new URL[classPath.size()]);
    }
}
