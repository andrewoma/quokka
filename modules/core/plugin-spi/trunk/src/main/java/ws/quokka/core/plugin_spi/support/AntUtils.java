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
import org.apache.tools.ant.types.FileList;
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
 * AntUtils provides some helper methods for common Ant-related tasks that are useful when implementing plugins
 */
public class AntUtils {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Project project;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public AntUtils(Project project) {
        this.project = project;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Returns the files represented by the given fileset
     */
    public List getFiles(FileSet fileSet) {
        List files = new ArrayList();
        String[] names = fileSet.getDirectoryScanner(project).getIncludedFiles();

        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            files.add(new File(fileSet.getDir(project), name));
        }

        return files;
    }

    /**
     * Creates the given directory if it doesn't already exist. It silently ignores the
     * request if the directory already exists
     */
    public void mkdir(File dir) {
        Mkdir mkdir = (Mkdir)project.createTask("mkdir");
        mkdir.setDir(dir);
        mkdir.perform();
    }

    /**
     * Deletes the file or directory given. It silently ignores the request if the
     * specified file does not exist
     */
    public void delete(File file) {
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

    /**
     * An alternative to {@link Project#createTask(String)} that initialises the given
     * task object. Works for all tasks, not just built-in Ant tasks
     */
    public Task init(Task task, String name) {
        task.setProject(project);
        task.setTaskName(name);
        task.init();

        return task;
    }

    /**
     * Converts a file to a file set
     */
    public FileSet toFileSet(File file) {
        FileSet fileSet = new FileSet();
        fileSet.setProject(project);
        fileSet.setFile(file);

        return fileSet;
    }

    /**
     * Filters a collection of resource collections to those that are valid and actually contain files.
     * It will log any resource collections skipped at a log level of verbose.
     * <br>
     * Note: it will modify the collection passed to it as well as returning the same reference
     */
    public Collection filterExisting(Collection rcs) {
        for (Iterator i = rcs.iterator(); i.hasNext();) {
            ResourceCollection rc = (ResourceCollection)i.next();

            if (!containsFiles(rc, true)) {
                i.remove();
            }
        }

        return rcs;
    }

    /**
     * Returns true if the resource collections given in rc is valid and contains file.
     * @param log if true, it will log that resources given will be skipped at a log level or verbose
     */
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

    /**
     * Normalises the absolute path given. You can use {@link Project#resolveFile(String)} if you wish to
     * normalise a relative path
     */
    public File normalise(String path) {
        return FileUtils.getFileUtils().normalize(path);
    }

    /**
     * Convenience method to create a FileSet task
     */
    public FileSet createFileSet() {
        return (FileSet)project.createDataType("fileset");
    }

    /**
     * Convenience method to create a FileList task
     */
    public FileList createFileList() {
        return (FileList)project.createDataType("filelist");
    }

    /**
     * Convenience method to create a DirSet task
     */
    public DirSet createDirSet() {
        return (DirSet)project.createDataType("dirset");
    }

    /**
     * Convenience method to create a ZipFileSet task
     */
    public ZipFileSet createZipFileSet() {
        return (ZipFileSet)project.createDataType("zipfileset");
    }

    /**
     * Convenience method to create a Copy task
     */
    public Copy createCopyTask() {
        return (Copy)init(new Copy(), "copy");
    }
}
