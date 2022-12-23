package com.github.ngyewch.gradle.projectset;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.UnknownConfigurationException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ProjectSetTask
    extends DefaultTask {

  private final ProjectSet projectSet;

  @Inject
  public ProjectSetTask(ProjectSet projectSet) {
    super();

    this.projectSet = projectSet;

    projectSet.getResolvedProjects(getProject()).forEach(project -> {
      if (project.getPlugins().hasPlugin(JavaBasePlugin.class)) {
        setupPublishing(project, "java");
      } else if (project.getPlugins().hasPlugin(JavaPlatformPlugin.class)) {
        setupPublishing(project, "javaPlatform");
      }
    });
    setupJavadoc();
  }

  private void setupJavadoc() {
    final File javadocOutputDirectory = new File(getProject().getBuildDir(),
        String.format("projectSets/%s/javadoc", projectSet.getId().get()));
    final Set<File> aggregateJavadocClasspathSet = new HashSet<>();
    final TaskProvider<Javadoc> javadocTaskProvider = getProject().getTasks()
        .register(String.format("javadocProjectSet%s", projectSet.getCapitalizedId()),
            Javadoc.class, aggregateJavadoc -> {
              aggregateJavadoc.setTitle(projectSet.getJavadoc().getTitle().getOrElse(projectSet.getId().get()));
              aggregateJavadoc.setDescription(
                  String.format("Generates Javadoc API documentation for project set '%s'.", projectSet.getId().get()));
              aggregateJavadoc.setGroup(JavaBasePlugin.DOCUMENTATION_GROUP);
              aggregateJavadoc.setDestinationDir(javadocOutputDirectory);

              final List<Project> excludeProjects = projectSet.getJavadoc().getExcludeProjects()
                  .getOrElse(new ArrayList<>());
              final boolean useJavadocIo = projectSet.getJavadoc().getUseJavadocIo().getOrElse(false);
              final List<String> excludeLinksForDependencies = projectSet.getJavadoc().getExcludeLinksForDependencies()
                  .getOrElse(new ArrayList<>());
              projectSet.getResolvedProjects(getProject()).forEach(subproject -> {
                if (excludeProjects.contains(subproject)) {
                  return;
                }
                if (!subproject.getPlugins().hasPlugin(JavaBasePlugin.class)) {
                  return;
                }

                final SourceSet mainSourceSet = subproject.getExtensions().getByType(JavaPluginExtension.class)
                    .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                final JavaCompile javaCompile = (JavaCompile) subproject.getTasks()
                    .getByName(mainSourceSet.getCompileJavaTaskName());

                aggregateJavadoc.dependsOn(String.format("%s:%s", subproject.getPath(),
                    mainSourceSet.getProcessResourcesTaskName()));
                aggregateJavadoc.dependsOn(String.format("%s:%s", subproject.getPath(),
                    mainSourceSet.getCompileJavaTaskName()));
                aggregateJavadoc.source(javaCompile.getSource());

                final Javadoc javadoc = subproject.getTasks().named(mainSourceSet.getJavadocTaskName(), Javadoc.class)
                    .get();
                aggregateJavadocClasspathSet.addAll(javadoc.getClasspath().getFiles());

                final List<Dependency> dependencies = getDependencies(subproject, "implementation", "api", "compileOnly");
                final List<String> links = new ArrayList<>();
                if (useJavadocIo) {
                  dependencies.forEach(dependency -> {
                    if (dependency instanceof ExternalModuleDependency) {
                      final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) dependency;
                      if (!isLinkExcluded(subproject, excludeLinksForDependencies, externalModuleDependency)) {
                        links.add(String.format("https://javadoc.io/doc/%s/%s/%s/",
                            externalModuleDependency.getGroup(), externalModuleDependency.getName(),
                            externalModuleDependency.getVersion()));
                      }
                    }
                  });
                }
                final StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) javadoc.getOptions();
                final StandardJavadocDocletOptions aggregateOptions = (StandardJavadocDocletOptions) aggregateJavadoc
                    .getOptions();
                addAll(aggregateOptions.getLinks(), options.getLinks());
                addAll(aggregateOptions.getLinks(), links);
                addAll(aggregateOptions.getLinksOffline(), options.getLinksOffline());
                addAll(aggregateOptions.getJFlags(), options.getJFlags());
              });
            });
    javadocTaskProvider.configure(task -> {
      final ConfigurableFileCollection aggregateJavadocClasspathFileCollection = getProject().files();
      aggregateJavadocClasspathFileCollection.setFrom(aggregateJavadocClasspathSet);
      task.setClasspath(aggregateJavadocClasspathFileCollection);
    });
    getDependsOn().add(javadocTaskProvider.getName());
  }

  private void setupPublishing(Project project, String componentName) {
    project.getExtensions().configure(PublishingExtension.class, publishingExtension -> {
      publishingExtension.publications(publicationContainer -> {
        publicationContainer.create(projectSet.getId().get(), MavenPublication.class, mavenPublication -> {
          mavenPublication.from(project.getComponents().getByName(componentName));
          mavenPublication.setGroupId(project.getGroup().toString());
          mavenPublication.setArtifactId(project.getName());
          mavenPublication.setVersion(project.getVersion().toString());
        });
      });
      final File repositoryOutputDirectory = new File(getProject().getRootProject().getBuildDir(),
          String.format("projectSets/%s/m2repo", projectSet.getId().get()));
      publishingExtension.repositories(artifactRepositories -> {
        artifactRepositories.mavenLocal(mavenArtifactRepository -> {
          mavenArtifactRepository.setName(projectSet.getId().get());
          mavenArtifactRepository.setUrl(repositoryOutputDirectory.toURI());
        });
      });
      getDependsOn().add(String.format("%s:publish%sPublicationTo%sRepository", project.getPath(),
          projectSet.getCapitalizedId(), projectSet.getCapitalizedId()));
    });
  }

  @Override
  @Internal
  public String getGroup() {
    return "Project set";
  }

  @Override
  @Internal
  public String getDescription() {
    return String.format("Project set '%s'", projectSet.getId().get());
  }

  @TaskAction
  public void action() {
    // do nothing
  }

  private static <T> void addAll(List<T> list, List<T> newElements) {
    if (newElements == null) {
      return;
    }
    newElements.forEach(element -> {
      if ((element != null) && !list.contains(element)) {
        list.add(element);
      }
    });
  }

  private static List<Dependency> getDependencies(Project project, String... configurationNames) {
    final List<Dependency> dependencies = new ArrayList<>();
    for (final String configurationName : configurationNames) {
      try {
        dependencies.addAll(project.getConfigurations().getByName(configurationName).getDependencies());
      } catch (UnknownConfigurationException e) {
        // ignore exception
      }
    }
    return dependencies;
  }

  private static boolean isLinkExcluded(Project project, List<String> excludeLinksForDependencies,
                                        ModuleDependency moduleDependency) {
    for (final String excludeLinkForDependency : excludeLinksForDependencies) {
      final Dependency dep = project.getDependencies().create(excludeLinkForDependency);
      boolean exclude = ((dep.getGroup() == null) || dep.getGroup().equals(moduleDependency.getGroup()))
          && ((dep.getName() == null) || dep.getName().equals("*") || dep.getName().equals(moduleDependency.getName()))
          && ((dep.getVersion() == null) || dep.getVersion().equals(moduleDependency.getVersion()));
      if (exclude) {
        return true;
      }
    }
    return false;
  }
}
