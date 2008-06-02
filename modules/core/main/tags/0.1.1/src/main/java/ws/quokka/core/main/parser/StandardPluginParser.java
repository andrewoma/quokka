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


package ws.quokka.core.main.parser;

import org.apache.tools.ant.BuildException;

import org.xml.sax.Locator;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.QuokkaEntityResolver;
import ws.quokka.core.main.ant.DefaultBuildResources;
import ws.quokka.core.model.Path;
import ws.quokka.core.model.PathGroup;
import ws.quokka.core.model.Plugin;
import ws.quokka.core.model.Target;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoPath;
import ws.quokka.core.repo_spi.RepoType;
import ws.quokka.core.repo_spi.RepoXmlConverter;
import ws.quokka.core.util.AnnotatedObject;
import ws.quokka.core.util.Annotations;
import ws.quokka.core.util.Strings;
import ws.quokka.core.util.URLs;
import ws.quokka.core.util.xml.Converter;
import ws.quokka.core.util.xml.Document;
import ws.quokka.core.util.xml.Element;
import ws.quokka.core.util.xml.LocatorDomParser;
import ws.quokka.core.util.xml.ReflectionConverter;
import ws.quokka.core.util.xml.XmlConverter;

import java.net.URL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 *
 */
public class StandardPluginParser implements PluginParser {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private XmlConverter xmlConverter = new XmlConverter();

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public StandardPluginParser() {
        xmlConverter.add(new PluginConverter(Plugin.class));
        xmlConverter.add(new ReflectionConverter(RepoType.class));
        xmlConverter.add(new TargetConverter(Target.class));
        xmlConverter.add(new RepoXmlConverter.RepoPathConverter(Path.class));
        xmlConverter.add(new PathGroupConverter(PathGroup.class));
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public Plugin getPluginInstance(RepoArtifact artifact) {
        Plugin plugin = parsePluginXml(artifact);
        plugin.setArtifact(artifact);
        addBuildResources(plugin);
        addLocalResources(plugin);

        return plugin;
    }

    private void addBuildResources(Plugin plugin) {
        //        String resourcesPath = "META-INF/quokka/resources/";
        String resourcesPath = getResourcesPrefix(plugin.getArtifact().getId()) + "resources/build/";
        plugin.addBuildResources(URLs.toURLEntries(plugin.getArtifact().getLocalCopy(), resourcesPath));
    }

    private void addLocalResources(Plugin plugin) {
        String resourcesPath = getResourcesPrefix(plugin.getArtifact().getId()) + "resources/local/";
        DefaultBuildResources resources = new DefaultBuildResources();
        resources.putAll(URLs.toURLEntries(plugin.getArtifact().getLocalCopy(), resourcesPath));

        //        resources.setTempDir(); TODO
        plugin.setLocalResources(resources);
    }

    private String getResourcesPrefix(RepoArtifactId id) {
        return "META-INF/quokka/" + id.toPathString() + "/";
    }

    private Plugin parsePluginXml(RepoArtifact artifact) {
        String pluginPath = getResourcesPrefix(artifact.getId()) + "plugin.xml";
        URL url = URLs.toURL(artifact.getLocalCopy(), pluginPath);

        if (url == null) {
            throw new BuildException(pluginPath + " cannot be found at location " + artifact.getLocalCopy().getPath());
        }

        xmlConverter.addContext("artifact", artifact);

        QuokkaEntityResolver resolver = new QuokkaEntityResolver();

//        resolver.addVersion("plugin", new String[] {"0.1"});
        resolver.addVersion("plugin", "0.1");

        return (Plugin)xmlConverter.fromXml(Plugin.class, Document.parse(url, resolver).getRoot());
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public static class PluginConverter extends ReflectionConverter {
        public PluginConverter(Class clazz) {
            super(clazz);
        }

        public Object fromXml(Element pluginEl) {
            Plugin plugin = (Plugin)super.fromXml(pluginEl);
            plugin.setClassName(pluginEl.getAttribute("class"));

            //            System.out.println(plugin.getClassName());
            //            System.out.println(plugin.getDescription());
            // Targets
            Converter converter;
            Element targetsEl = pluginEl.getChild("targets");

            if (targetsEl != null) {
                List targetEls = targetsEl.getChildren("target");

                for (Iterator i = targetEls.iterator(); i.hasNext();) {
                    Element targetEl = (Element)i.next();
                    converter = getConverter(Target.class);
                    getXmlConverter().addContext("plugin", plugin);
                    plugin.addTarget((Target)converter.fromXml(targetEl));
                }
            }

            // Types
            Element typesEl = pluginEl.getChild("types");

            if (typesEl != null) {
                List typeEls = typesEl.getChildren("type");

                for (Iterator i = typeEls.iterator(); i.hasNext();) {
                    Element typeEl = (Element)i.next();
                    converter = getConverter(RepoType.class);
                    plugin.addType((RepoType)converter.fromXml(typeEl));
                }
            }

            // Properties
            Element propertiesEl = pluginEl.getChild("properties");

            if (propertiesEl != null) {
                List propertyEls = propertiesEl.getChildren("property");

                for (Iterator i = propertyEls.iterator(); i.hasNext();) {
                    Element propertyEl = (Element)i.next();
                    addProperty(propertyEl, plugin);
                }
            }

            return plugin;
        }

        private void addProperty(Element propertyEl, Plugin plugin) {
            Locator locator = LocatorDomParser.getLocator(propertyEl.getElement());
            Annotations annotations = new Annotations();
            annotations.put(AnnotatedObject.LOCATOR, locator);

            String value = propertyEl.getAttribute("value");
            annotations.put("initial", value);

            List targetNames = Strings.commaSepList(propertyEl.getAttribute("targets"));
            List targets = (targetNames.size() == 0) ? plugin.getTargets() : toTargets(plugin, locator, targetNames);

            for (Iterator i = targets.iterator(); i.hasNext();) {
                Target target = (Target)i.next();
                target.getDefaultProperties().setProperty(propertyEl.getAttribute("name"), value, annotations);
            }
        }

        private List toTargets(Plugin plugin, Locator locator, List targetNames) {
            List targets = new ArrayList();

            for (Iterator i = targetNames.iterator(); i.hasNext();) {
                String targetName = (String)i.next();
                targetName = plugin.getNameSpace() + ":" + targetName;

                Target target = plugin.getTarget(targetName);
                Assert.isTrue(target != null, locator, "Target specified is not defined for plugin: " + targetName);
                targets.add(target);
            }

            return targets;
        }
    }

    public static class TargetConverter extends ReflectionConverter {
        public TargetConverter(Class clazz) {
            super(clazz);
        }

        public Object fromXml(Element targetEl) {
            Plugin plugin = (Plugin)getXmlConverter().getContext("plugin");
            Target target = (Target)super.fromXml(targetEl);

            target.setImplementsPlugin(targetEl.getAttribute("implements"));

            String abstractValue = targetEl.getAttribute("abstract");

            target.setAbstract((abstractValue == null) ? (plugin.getClassName() == null)
                                                       : Boolean.valueOf(abstractValue).booleanValue());

            target.setName(plugin.getNameSpace() + ":" + target.getName());

            if (target.isTemplate()) {
                target.setEnabledByDefault(false);
            }

            // Path groups
            Converter converter;
            List pathGroupEls = targetEl.getChildren("path-group");

            for (Iterator j = pathGroupEls.iterator(); j.hasNext();) {
                Element pathGroupEl = (Element)j.next();
                converter = getConverter(PathGroup.class);
                target.addPathGroup((PathGroup)converter.fromXml(pathGroupEl));
            }

            // Add a default path group for the classpath 
            if (target.getPathGroup("classpath") == null) {
                RepoArtifact artifact = (RepoArtifact)getContext("artifact");
                int size = artifact.getPaths().size();
                Assert.isTrue(size <= 1, target.getLocator(),
                    "A 'classpath' path group must be defined for the target as there are multiple possible paths defined in the repository");

                List paths = new ArrayList();
                paths.add("plugin");

                if (size != 0) {
                    paths.add("plugin." + ((RepoPath)artifact.getPaths().iterator().next()).getId());
                }

                target.addPathGroup(new PathGroup("classpath", paths, Boolean.TRUE));
            }

            // Project paths
            List pathEls = targetEl.getChildren("project-path");

            for (Iterator j = pathEls.iterator(); j.hasNext();) {
                Element pathEl = (Element)j.next();
                converter = getConverter(Path.class);
                target.addProjectPath((Path)converter.fromXml(pathEl));
            }

            // Dependencies
            target.setPlugin((Plugin)getXmlConverter().getContext("plugin"));

            List dependencies = Strings.commaSepList(targetEl.getAttribute("depends"));

            for (Iterator i = dependencies.iterator(); i.hasNext();) {
                String dependency = (String)i.next();
                dependency = (dependency.indexOf(":") != -1) ? dependency : (plugin.getNameSpace() + ":" + dependency);

                //                if (target.isAbstract()) {
                target.addOriginalDependency(dependency);

                //                } else {
                target.addDependency(dependency);

                //                }
            }

            // Properties
            List propertyEls = targetEl.getChildren("property");

            for (Iterator i = propertyEls.iterator(); i.hasNext();) {
                Element propertyEl = (Element)i.next();
                Locator locator = LocatorDomParser.getLocator(propertyEl.getElement());
                Annotations annotations = new Annotations();
                annotations.put(AnnotatedObject.LOCATOR, locator);

                String value = propertyEl.getAttribute("value");
                annotations.put("initial", value);
                target.getDefaultProperties().setProperty(propertyEl.getAttribute("name"), value, annotations);
            }

            return target;
        }
    }

    public static class PathGroupConverter extends ReflectionConverter {
        public PathGroupConverter(Class clazz) {
            super(clazz);
        }

        public Object fromXml(Element pathGroupEl) {
            PathGroup pathGroup = (PathGroup)super.fromXml(pathGroupEl);

            return new PathGroup(pathGroup.getId(), Strings.commaSepList(pathGroupEl.getAttribute("paths")),
                pathGroup.getMergeWithCore());
        }
    }
}
