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

import org.apache.tools.ant.Project;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.Logger;
import ws.quokka.core.bootstrap_util.PropertiesUtil;
import ws.quokka.core.bootstrap_util.Reflect;
import ws.quokka.core.bootstrap_util.VoidExceptionHandler;
import ws.quokka.core.plugin_spi.Plugin;
import ws.quokka.core.plugin_spi.Resources;
import ws.quokka.core.plugin_spi.ResourcesAware;

import java.io.File;

import java.util.HashMap;
import java.util.Map;


/**
 * AbstractPlugin provides a useful base class for creating plugins. By default, it uses reflection
 * to execute a method matching the name of the target. It also provides access to logs, properties
 * resources and utilities.
 */
public abstract class AbstractPlugin implements Plugin, ResourcesAware {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final Map RESERVED_WORDS = new HashMap();

    static {
        RESERVED_WORDS.put("package", "packageIt");
        RESERVED_WORDS.put("import", "importIt");
    }

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Resources resources;
    private TypedProperties properties;
    private TypedProperties globalProperties;
    private Logger logger;
    private AntUtils utils;
    private Map projectProperties;
    private TypedProperties typedProjectProperties;

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Allows resources to be injected into the plugin
     */
    public void setResources(Resources resources) {
        this.resources = resources;
    }

    /**
     * Initialises various properties and utilities. Subclasses must invoke this method.
     */
    public void initialise() {
        projectProperties = PropertiesUtil.getProperties(resources.getProject());

        if (resources.getPrefix() != null) {
            properties = createProperties(resources.getPrefix() + ".");
        }

        globalProperties = createProperties("q.project.");

        typedProjectProperties = new TypedProperties("", projectProperties, resources.getProject());

        logger = resources.getLogger();
        utils = new AntUtils(getProject());
    }

    /**
     * Returns the project wide target dir as defined by q.project.targetDir
     */
    public File getTargetDir() {
        return globalProperties.getFile("targetDir");
    }

    /**
     * Returns the project wide source dir as defined by q.project.sourceDir
     */
    public File getSourceDir() {
        return globalProperties.getFile("sourceDir");
    }

    /**
     * Returns the project wide resources dir as defined by q.project.resourcesDir
     */
    public File getResourcesDir() {
        return globalProperties.getFile("resourcesDir");
    }

    /**
     * Returns an instance of Ant utilities containing various helper methods
     */
    public AntUtils utils() {
        return utils;
    }

    /**
     * Convenience method for creating TypedProperties for a given prefix agains the project properties
     */
    public TypedProperties createProperties(String prefix) {
        return new TypedProperties(prefix, projectProperties, resources.getProject());
    }

    /**
     * Provides access to the resources injected when the plugin was instantiated
     */
    public Resources getResources() {
        return resources;
    }

    /**
     * Convenience method to return the current Ant project
     */
    public Project getProject() {
        return resources.getProject();
    }

    /**
     * Provides access to the properties defined for this current target instance.
     */
    public TypedProperties properties() {
        return properties;
    }

    /**
     * Convenience method to provide access to the logger
     */
    public Logger log() {
        return logger;
    }

    /**
     * Convenience method that returns true if the specified target is enabled
     */
    public boolean isTargetEnabled(String name) {
        return getResources().isTargetEnabled(name);
    }

    /**
     * Looks for a method matching the unqualified name of the target and wraps it in an
     * {@link Runnable} instance. If name is import or package is looks for importIt and packageIt
     * respectively as the names are reserved words.
     */
    public Runnable getTarget(final String name) {
        return new Runnable() {
                public void run() {
                    new VoidExceptionHandler() {
                            public void run() throws Exception {
                                if (logger.isDebugEnabled()) {
                                    logger.debug(properties.toString());
                                }

                                new Reflect().invoke(AbstractPlugin.this, toMethodName(name), new Object[] {  });
                            }
                        };
                }
            };
    }

    private static String toMethodName(String targetName) {
        targetName = targetName.substring(targetName.indexOf(":") + 1);

        StringBuffer methodName = new StringBuffer();

        for (int i = 0; i < targetName.length(); i++) {
            char ch = targetName.charAt(i);

            if (ch != '-') {
                methodName.append(((i != 0) && (targetName.charAt(i - 1) == '-')) ? Character.toUpperCase(ch) : ch);
            }
        }

        String name = methodName.toString();

        if (RESERVED_WORDS.containsKey(name)) {
            name = (String)RESERVED_WORDS.get(name);
        }

        return name;
    }

    /**
     * Convenience method that throws a BuildException with the message provided if the condition
     * is not true. Saves the plugin from adding a dependency on bootstrap-util just for using asserts.
     */
    public void assertTrue(boolean condition, String message) {
        Assert.isTrue(condition, message);
    }

    /**
     * Convenience method to return the current unqualified target name. e.g. jar for quokka.plugin.jar:jar
     */
    public String getShortTargetName() {
        String name = getResources().getTargetName();

        return name.substring(name.indexOf(":") + 1);
    }

    /**
     * Returns the project properties accessible as TypedProperties
     */
    public TypedProperties getProjectProperties() {
        return typedProjectProperties;
    }

    /**
     * Clears a property from the underlying Ant properties instances. In general this method should not be used
     * However it is necessary is rare instances to clear a property that an underlying Ant task would otherwise
     * refuse to set as it already exists.
     */
    public void clearProjectProperty(String property) {
        PropertiesUtil.clearProperty(resources.getProject(), property);
    }

    /**
     * Returns the string prefixed by the target prefix
     */
    public String prefix(String string) {
        return getResources().getPrefix() + "." + string;
    }
}
