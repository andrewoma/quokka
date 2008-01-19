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

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.NoBannerLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.input.InputRequest;

import ws.quokka.core.main.ant.ProjectHelper;
import ws.quokka.core.plugin_spi.PluginState;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;


/**
 *
 */
public class AntRunner {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    protected AntRunner.AntListener antListener;

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Runs the given targets for the test project specified
     */
    public Map run(File buildFile, List targets, Properties projectProperties, Properties pluginState) {
        Project project = new Project();
        project.setCoreLoader(getClass().getClassLoader());
        antListener = new AntRunner.AntListener();

        project.addBuildListener(createLogger(projectProperties.getProperty("quokka.debugger.logLevel")));
        project.addBuildListener(antListener);

        //        project.setInputHandler(new DefaultInputHandler());
        project.setInputHandler(new InputHandler() {
                public void handleInput(InputRequest request)
                        throws BuildException {
                    System.out.println(request.getPrompt());

                    try {
                        String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
                        request.setInput(line);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        project.setKeepGoingMode(false);

        //        PrintStream err = System.err;
        //        PrintStream out = System.out;
        //        InputStream in = System.in;
        project.setDefaultInputStream(System.in);

        //        System.setIn(new DemuxInputStream(project));
        //        System.setOut(new PrintStream(new DemuxOutputStream(project, false)));
        //        System.setErr(new PrintStream(new DemuxOutputStream(project, true)));
        for (Iterator i = projectProperties.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            project.setUserProperty((String)entry.getKey(), (String)entry.getValue());
        }

        RuntimeException exception = null;

        try {
            project.fireBuildStarted();

            project.init();
            project.setUserProperty("ant.version", "1.7.0");
            project.setUserProperty("ant.file", buildFile.getAbsolutePath());

            org.apache.tools.ant.ProjectHelper helper = new ProjectHelper();
            project.addReference("ant.projectHelper", helper);
            helper.parse(project, buildFile);

            // Add any plugin state
            PluginState state = (PluginState)project.getReference("quokka.pluginstate");

            for (Iterator i = pluginState.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                state.put((String)entry.getKey(), entry.getValue());
            }

            Vector targetsVector = new Vector();
            targetsVector.addAll(targets);

            // make sure that we have a target to execute
            if (targets.size() == 0) {
                if (project.getDefaultTarget() != null) {
                    targetsVector.add(project.getDefaultTarget());
                }
            }

            project.executeTargets(targetsVector);

            Map results = antListener.toMap();
            results.put("antProperties", new HashMap(project.getProperties()));

            return results;
        } catch (RuntimeException e) {
            exception = e;
            throw e;
        } finally {
            //            System.setOut(out);
            //            System.setErr(err);
            //            System.setIn(in);
            project.fireBuildFinished(exception);
        }
    }

    private DefaultLogger createLogger(String level) {
        NoBannerLogger logger = new NoBannerLogger();

        //        DefaultLogger logger = new DefaultLogger();
        logger.setMessageOutputLevel((level == null) ? Project.MSG_INFO : Integer.parseInt(level));
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);
        logger.setEmacsMode(false);

        return logger;
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public static class AntListener implements BuildListener {
        private List targetsStarted;
        private List targetsFinished;
        private List tasksStarted;
        private List tasksFinished;
        private List messagesLogged;

        public AntListener() {
            initialise();
        }

        private void initialise() {
            targetsStarted = new ArrayList();
            targetsFinished = new ArrayList();
            tasksStarted = new ArrayList();
            tasksFinished = new ArrayList();
            messagesLogged = new ArrayList();
        }

        public void clear() {
            initialise();
        }

        public List getTargetsStarted() {
            return targetsStarted;
        }

        public List getTargetsFinished() {
            return targetsFinished;
        }

        public List getTasksStarted() {
            return tasksStarted;
        }

        public List getTasksFinished() {
            return tasksFinished;
        }

        public List getMessagesLogged() {
            return messagesLogged;
        }

        public void buildStarted(BuildEvent event) {
        }

        public void buildFinished(BuildEvent event) {
        }

        public void targetStarted(BuildEvent event) {
            targetsStarted.add(event.getTarget().getName());
        }

        public void targetFinished(BuildEvent event) {
            targetsFinished.add(event.getTarget().getName());
        }

        public void taskStarted(BuildEvent event) {
            tasksStarted.add(event.getTask().getTaskName());
        }

        public void taskFinished(BuildEvent event) {
            tasksFinished.add(event.getTask().getTaskName());
        }

        public void messageLogged(BuildEvent event) {
            messagesLogged.add(event.getMessage());
        }

        public Map toMap() {
            Map map = new HashMap();
            map.put("targetsStarted", targetsStarted);

            return map;
        }
    }
}
