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

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.SubBuildListener;

import java.util.Stack;


/**
 *
 */
public class ParentProjectBuildListener implements SubBuildListener {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Stack parents = null;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void subBuildStarted(BuildEvent event) {
        event.getProject().log("<----- Building " + event.getProject().getName() + " ----->");

        Project parent = (Project)parents.peek();
        event.getProject().addReference("q.parentProject", parent);
        parents.push(event.getProject());
    }

    public void subBuildFinished(BuildEvent event) {
        if (parents != null) {
            parents.pop();
        }
    }

    public void buildStarted(BuildEvent event) {
        // Note: never called
    }

    public void buildFinished(BuildEvent event) {
        parents = null;
    }

    public void targetStarted(BuildEvent event) {
        // Initialise parents here as buildStarted will fire before this listener is added
        if (parents == null) {
            parents = new Stack();
            parents.push(event.getProject());
        }
    }

    public void targetFinished(BuildEvent event) {
    }

    public void taskStarted(BuildEvent event) {
    }

    public void taskFinished(BuildEvent event) {
    }

    public void messageLogged(BuildEvent event) {
    }

    public int getStackSize() {
        return parents.size();
    }

    private void message(String message, BuildEvent event) {
        //            System.out.println("build event: listener=" + System.identityHashCode(this) + ": " + message);
        //            System.out.println("  parents: " + parents);
        //            System.out.println("  event project: " + (event.getProject() == null ? "null" : event.getProject().getName()));
        //            System.out.println("  target project: " + (event.getTarget() == null ? "null" : event.getTarget().getProject().getName()));
        //            System.out.println("  task project: " + (event.getTask() == null ? "null" : event.getTask().getProject().getName()));
        //            Thread.dumpStack();
    }
}
