package com.github.ngyewch.gradle.projectset;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

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

    customExtension.getProjectSets().get().forEach(projectSet -> {
      project.getTasks().register(String.format("projectSet%s", StringUtils.capitalize(projectSet.getId().get())),
          ProjectSetTask.class, projectSet);
    });
  }
}
