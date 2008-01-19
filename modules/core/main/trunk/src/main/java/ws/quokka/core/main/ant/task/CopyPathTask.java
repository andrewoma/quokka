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


package ws.quokka.core.main.ant.task;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.util.FileUtils;

import ws.quokka.core.model.ProjectModel;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoType;

import java.io.File;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;


/**
 * Copies a path, renaming it based on the pattern provided
 */
public class CopyPathTask extends Task {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String id;
    private File todir;
    private String pattern = "#{group}_#{name}_#{type}_#{version}.#{extension}";

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void setId(String id) {
        this.id = id;
    }

    public void setTodir(File todir) {
        this.todir = todir;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void execute() throws BuildException {
        ProjectModel projectModel = (ProjectModel)getProject().getReference("quokka.projectModel");
        PropertyHelper ph = PropertyHelper.getPropertyHelper(getProject());

        List path = projectModel.getProjectPath(id, false, true);

        for (Iterator i = path.iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();
            RepoArtifactId id = artifact.getId();
            Properties properties = new Properties();
            properties.put("group", id.getGroup());
            properties.put("name", id.getName());
            properties.put("type", id.getType());
            properties.put("version", id.getVersion().toString());

            RepoType type = projectModel.getRepository().getType(id.getType());
            properties.put("extension", type.getExtension());

            String name = expandName(ph, pattern, properties);
            File destination = FileUtils.getFileUtils().normalize(todir + "/" + name);
            Copy copy = (Copy)getProject().createTask("copy");
            copy.setTofile(destination);
            copy.setFile(artifact.getLocalCopy());
            copy.perform();
        }
    }

    private String expandName(PropertyHelper ph, String pattern, Properties properties) {
        pattern = pattern.replace('#', '$');

        Vector fragments = new Vector();
        Vector propertyRefs = new Vector();
        ph.parsePropertyString(pattern, fragments, propertyRefs);

        StringBuffer name = new StringBuffer();
        Iterator refs = propertyRefs.iterator();

        for (Iterator i = fragments.iterator(); i.hasNext();) {
            String fragement = (String)i.next();

            if (fragement == null) {
                String ref = (String)refs.next();
                String property = (String)properties.get(ref);

                if (property == null) {
                    throw new BuildException("", getLocation());
                }

                name.append(property);
            } else {
                name.append(fragement);
            }
        }

        return name.toString();
    }
}
