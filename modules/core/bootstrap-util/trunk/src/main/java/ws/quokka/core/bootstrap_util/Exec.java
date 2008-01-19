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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;

import java.io.InputStream;
import java.io.OutputStream;


/**
 *
 */
public class Exec {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public int exec(String command) {
        return exec(command, System.in, System.out, System.err);
    }

    public int exec(String command, InputStream in, OutputStream out, OutputStream error) {
        //        System.out.println("Using new exec ...");
        try {
            Process process = Runtime.getRuntime().exec(command);
            PumpStreamHandler handler = new PumpStreamHandler(out, error, in);
            handler.setProcessErrorStream(process.getErrorStream());
            handler.setProcessOutputStream(process.getInputStream());
            handler.setProcessInputStream(process.getOutputStream());
            handler.start();

            return process.waitFor();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
