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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;

import ws.quokka.core.test.AbstractTest;

import java.io.ByteArrayOutputStream;


/**
 *
 */
public class ExecTest extends AbstractTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void test() {
        CommandlineJava command = new CommandlineJava();
        command.setClassname(JavaEcho.class.getName());

        Path classPath = command.createClasspath(new Project());
        classPath.add(new Path(new Project(), System.getProperty("java.class.path")));

        Exec exec = new Exec();
        exec.exec(command.toString()); // Test default streams doesn't throw an exception

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exec.exec(command.toString(), System.in, out, System.err);
        assertEquals("Hello from JavaEcho", out.toString());
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public static class JavaEcho {
        public static void main(String[] args) {
            System.out.print("Hello from JavaEcho");
        }
    }
}
