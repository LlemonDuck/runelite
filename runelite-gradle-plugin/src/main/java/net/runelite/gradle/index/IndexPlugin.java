package net.runelite.gradle.index;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

public abstract class IndexPlugin implements Plugin<Project>
{

	@Override
	public void apply(Project project)
	{
		TaskProvider<IndexTask> buildRs2asmIndex = project.getTasks()
			.register("buildRs2asmIndex", IndexTask.class, (task) -> task.setGroup("build"));

		project.getTasks()
			.getByName("processResources")
			.dependsOn(buildRs2asmIndex);
	}
}
