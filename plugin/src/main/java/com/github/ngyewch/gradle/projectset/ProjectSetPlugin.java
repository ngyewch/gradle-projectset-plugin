package com.github.ngyewch.gradle.projectset;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

import java.util.HashSet;
import java.util.Set;

public class ProjectSetPlugin
    implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    // register 'maven-publish' for all subprojects (must happen before project evaluation)
    project.getSubprojects().stream()
        .filter(subproject -> !subproject.getPlugins().hasPlugin(MavenPublishPlugin.class))
        .forEach(subproject -> subproject.getPlugins().apply(MavenPublishPlugin.class));

    project.getExtensions().create("projectSets", ProjectSetExtension.class);

    project.afterEvaluate(this::afterEvaluate);
  }

  private void afterEvaluate(Project project) {
    final ProjectSetExtension customExtension = project.getExtensions().getByType(ProjectSetExtension.class);

    final Set<String> taskNames = new HashSet<>();
    customExtension.getProjectSets().get().forEach(projectSet -> {
      final String taskName = String.format("projectSet%s", StringUtils.capitalize(projectSet.getId().get()));
      project.getTasks().register(taskName, ProjectSetTask.class, projectSet);
      taskNames.add(taskName);
    });
    project.getTasks().register("projectSets", DefaultTask.class,
        task -> {
          task.setGroup("Project set");
          task.setDescription("All project sets");
          task.getDependsOn().addAll(taskNames);
        });
  }
}
