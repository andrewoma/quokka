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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.input.InputRequest;


/**
 *
 */
public class InterruptableInputHandlerTest extends TestCase {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void test() {
        final InterruptableInputHandler handler = new InterruptableInputHandler(System.in);

        final Thread mainThread = Thread.currentThread();
        Thread thread = new Thread() {
                public void run() {
                    System.out.println("Sleeping prior to interrupt");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    System.out.println("Interrupting main thread");

//                mainThread.interrupt();
                    handler.interrupt();
                }
            };

        thread.start();

        System.out.println("Main thread waiting for input");

        try {
            while (true) {
                handler.handleInput(new InputRequest("Prompt"));
            }

//            fail("Expected interrupted exception");
        } catch (BuildException e) {
            assertEquals(InterruptedException.class, e.getCause().getClass());
        }
    }

    public static void main(String[] args) {
        new InterruptableInputHandlerTest().test();
    }
}
