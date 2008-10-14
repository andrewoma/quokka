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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * InterruptableInputHandler provides an input handler that doesn't block and can
 * be interrupted by {@link Thread#interrupt}. It also provides a safer mechanism of interrupting
 * via it's own interrupt method which will exit after waking from the next sleep.
 * This is safer as there is no risk of interrupting the thread when it is not actually waiting
 * for input.
 * <p/>
 * This is based on Ant's {@link DefaultInputHandler}, but modified to read only when the buffer is ready
 * <p/>
 * Note: Ant's default input stream ({@link org.apache.tools.ant.DemuxInputStream}) won't work with this as it does
 * not support ready. You should pass in {@link org.apache.tools.ant.Project#defaultInputStream} instead.
 */
public class InterruptableInputHandler extends DefaultInputHandler {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private volatile boolean interrupted = false;
    private InputStream in;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public InterruptableInputHandler(InputStream in) {
        this.in = in;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void interrupt() {
        interrupted = true;
    }

    public void handleInput(InputRequest request) throws BuildException {
        String prompt = getPrompt(request);
        BufferedReader r = new BufferedReader(new InputStreamReader(in));

        do {
            System.err.println(prompt);
            System.err.flush();

            try {
                while (!r.ready() && !interrupted) {
                    Thread.sleep(100);
                }

                if (interrupted) {
                    throw new InterruptedException();
                }

                String input = r.readLine();
                request.setInput(input);
            } catch (IOException e) {
                throw new BuildException("Failed to read input from Console.", e);
            } catch (InterruptedException e) {
                throw new BuildException("Thread interrupted", e);
            }
        } while (!request.isInputValid());

        // Closing the input stream (usually System.in) prevents r.ready from ever returning
        // true after the first line is read. Is it valid to close System.in anyway after
        // reading a line? I wouldn't have thought so ...
//            if (r != null) {
//                try {
//                    r.close();
//                } catch (IOException e) {
//                    throw new BuildException("Failed to close input.", e);
//                }
//            }
    }

    protected InputStream getInputStream() {
        return in;
    }
}
