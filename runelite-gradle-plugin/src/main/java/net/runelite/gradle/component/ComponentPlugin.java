package net.runelite.gradle.component;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

public class ComponentPlugin implements Plugin<Project>
{

	@Override
	public void apply(Project project)
	{
		TaskProvider<ComponentTask> packComponents = project.getTasks()
			.register("packComponents", ComponentTask.class, (task) -> task.setGroup("build"));

		project.getTasks()
			.getByName("compileJava")
			.dependsOn(packComponents);

		project.getExtensions()
			.getByType(SourceSetContainer.class)
			.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
			.getJava()
			.srcDir(packComponents.map(ComponentTask::getOutputDirectory));
	}

}
