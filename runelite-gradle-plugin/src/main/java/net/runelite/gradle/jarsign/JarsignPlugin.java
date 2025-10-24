package net.runelite.gradle.jarsign;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

public abstract class JarsignPlugin implements Plugin<Project>
{

	@Override
	public void apply(Project project)
	{
		JarsignExtension ext = project.getExtensions()
			.create(JarsignExtension.class, "jarsign", JarsignExtension.class);
		ext.getKeystore().convention(toRegularFileProvider(project, propProvider(project, "jarsigner.keystore")));
		ext.getStorePass().convention(propProvider(project, "jarsigner.storepass"));
		ext.getKeyPass().convention(propProvider(project, "jarsigner.keypass"));
		ext.getAlias().convention(propProvider(project, "jarsigner.alias"));

		project.getTasks()
			.withType(Jar.class, jarTask -> registerSignTask(project, jarTask, ext));
	}

	private void registerSignTask(Project project, Jar jarTask, JarsignExtension ext)
	{
		TaskProvider<JarsignTask> signTask = project.getTasks().register(
			jarTask.getName() + "Sign", JarsignTask.class, (jarsignTask) ->
			{
				jarsignTask.setGroup(BasePlugin.BUILD_GROUP);

				jarsignTask.getBuildTask().convention(jarTask);
				jarsignTask.getArchive().convention(jarTask.getArchiveFile());
				jarsignTask.getKeystore().convention(ext.getKeystore());
				jarsignTask.getStorePass().convention(ext.getStorePass());
				jarsignTask.getKeyPass().convention(ext.getKeyPass());
				jarsignTask.getAlias().convention(ext.getAlias());
			}
		);
		jarTask.finalizedBy(signTask);
	}

	private static Provider<String> propProvider(Project project, String key)
	{
		return project.provider(() ->
			(String) project.findProperty(key));
	}

	private static RegularFileProperty toRegularFileProvider(Project project, Provider<String> propProvider)
	{
		return project.getObjects()
			.fileProperty()
			.fileProvider(propProvider.map(project::file));
	}
}
