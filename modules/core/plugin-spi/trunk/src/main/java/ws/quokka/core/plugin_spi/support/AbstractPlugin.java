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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

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
 * AbstractPlugin provides a useful base class for creating plugins. By default, it expectes a
 * targets to method of the same name as the target. It also provides access to logs, properties
 * and other resources.
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

    public void setResources(Resources resources) {
        this.resources = resources;
    }

    public void initialise() {
        projectProperties = PropertiesUtil.getProperties(resources.getProject());

        if (resources.getPrefix() != null) {
            properties = createProperties(resources.getPrefix() + ".");
        }

        globalProperties = createProperties("quokka.project.");

        typedProjectProperties = new TypedProperties("", projectProperties, resources.getProject());

        logger = resources.getLogger();
        utils = new AntUtils(getProject());
    }

    public File getTargetDir() {
        return globalProperties.getFile("targetDir");
    }

    public AntUtils utils() {
        return utils;
    }

    public TypedProperties createProperties(String prefix) {
        return new TypedProperties(prefix, projectProperties, resources.getProject());
    }

    public Resources getResources() {
        return resources;
    }

    public Project getProject() {
        return resources.getProject();
    }

    public TypedProperties properties() {
        return properties;
    }

    public Logger log() {
        return logger;
    }

    public boolean isTargetEnabled(String name) {
        return getResources().isTargetEnabled(name);
    }

    public Runnable getTarget(final String name) {
        return new Runnable() {
                public void run() {
                    new VoidExceptionHandler() {
                            public void run() throws Exception {
                                new Reflect().invoke(AbstractPlugin.this, toMethodName(name), new Object[] {  });
                            }
                        };
                }
            };
    }

    public static String toMethodName(String targetName) {
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

    public void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new BuildException(message);
        }
    }

    public String getShortTargetName() {
        String name = getResources().getTargetName();

        return name.substring(name.indexOf(":") + 1);
    }

    public TypedProperties getProjectProperties() {
        return typedProjectProperties;
    }

    public Map getAntProjectProperties() {
        return projectProperties;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
