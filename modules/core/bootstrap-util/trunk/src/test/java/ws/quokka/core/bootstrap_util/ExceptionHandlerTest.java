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

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;


/**
 *
 */
public class ExceptionHandlerTest extends TestCase {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testSucceed() {
        String result = (String)new ExceptionHandler() {
                    public Object run() throws Exception {
                        return "result";
                    }
                }.soften();
        assertEquals("result", result);
    }

    public void testFail() {
        try {
            new ExceptionHandler() {
                    public Object run() throws Exception {
                        throw new Exception("failed");
                    }
                }.soften();
            fail();
        } catch (BuildException e) {
            assertEquals("failed", e.getCause().getMessage());
        }
    }
}
