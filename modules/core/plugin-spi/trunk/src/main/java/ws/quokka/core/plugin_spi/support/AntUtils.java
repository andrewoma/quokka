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


package ws.quokka.core.plugin_spi.support;

import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Mkdir;
import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * AntUtils provides some helper methods for ANT
 */
public class AntUtils {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Project project;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public AntUtils(Project project) {
        this.project = project;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public Task createTask(String type) {
        return ComponentHelper.getComponentHelper(project).createTask(type);
    }

    public List getFiles(FileSet fileSet) {
        List files = new ArrayList();
        String[] names = fileSet.getDirectoryScanner(project).getIncludedFiles();

        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            files.add(new File(fileSet.getDir(project), name));
        }

        return files;
    }

    public void mkdir(File dir) {
        Mkdir mkdir = (Mkdir)project.createTask("mkdir");
        mkdir.setDir(dir);
        mkdir.perform();
    }

    public void deleteFile(File file) {
        if (file.exists()) {
            Delete delete = (Delete)project.createTask("delete");

            if (file.isDirectory()) {
                delete.setDir(file);
            } else {
                delete.setFile(file);
            }

            delete.perform();
        }
    }

    public Task init(Task task, String name) {
        task.setProject(project);
        task.setTaskName(name);
        task.init();

        return task;
    }

    public FileSet toFileSet(File file) {
        FileSet fileSet = new FileSet();
        fileSet.setProject(project);
        fileSet.setFile(file);

        return fileSet;
    }

    public Collection filterExisting(Collection rcs) {
        for (Iterator i = rcs.iterator(); i.hasNext();) {
            ResourceCollection rc = (ResourceCollection)i.next();

            if (!containsFiles(rc, true)) {
                i.remove();
            }
        }

        return rcs;
    }

    public boolean containsFiles(ResourceCollection rc, boolean log) {
        try {
            if (!rc.iterator().hasNext()) {
                if (log) {
                    project.log("Skipping resource collection as it is empty", Project.MSG_VERBOSE);
                }

                return false;
            }
        } catch (Exception e) {
            if (log) {
                project.log("Skipping resource collection as it is invalid: " + e.getMessage(), Project.MSG_VERBOSE);
            }

            return false;
        }

        return true;
    }

    public File normalise(String file) {
        return FileUtils.getFileUtils().normalize(file);
    }

    public FileSet createFileSet() {
        return (FileSet)project.createDataType("fileset");
    }

    public DirSet createDirSet() {
        return (DirSet)project.createDataType("dirset");
    }

    public ZipFileSet createZipFileSet() {
        return (ZipFileSet)project.createDataType("zipfileset");
    }

    public Copy createCopyTask() {
        return (Copy)init(new Copy(), "copy");
    }
}
