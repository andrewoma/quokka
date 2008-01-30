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

import java.util.Map;


/**
 *
 */
public abstract class AbstractPlugin implements Plugin, ResourcesAware {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Resources resources;
    private TypedProperties properties;
    private TypedProperties globalProperties;
    private Logger logger;
    private AntUtils utils;
    private Map projectProperties;

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
        logger = resources.getLogger();
        utils = new AntUtils(getProject());
    }

    public File getTargetDir() {
        return globalProperties.getFile("targetDir");
    }

    public AntUtils getUtils() {
        return utils;
    }

    public TypedProperties createProperties(String prefix) {
        TypedProperties properties = new TypedProperties(prefix);
        properties.setProject(resources.getProject());
        properties.setProperties(projectProperties);

        return properties;
    }

    public Resources getResources() {
        return resources;
    }

    public Project getProject() {
        return resources.getProject();
    }

    public TypedProperties getProperties() {
        return properties;
    }

    public Logger logger() {
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

        return methodName.toString().equals("package") ? "packageIt" : methodName.toString();
    }

    public void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new BuildException(message);
        }
    }
}
