package net.runelite.gradle.jarsign;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.tasks.Jar;

@CacheableTask
public abstract class JarsignTask extends DefaultTask
{

	public JarsignTask()
	{
		dependsOn(getBuildTask());
		onlyIf(
			"target archive must be specified",
			_t -> getArchive().getAsFile().get().exists()
		);

		onlyIf(
			"keystore properties are set",
			_t ->
				getKeystore().isPresent() &&
					getStorePass().isPresent() &&
					getKeyPass().isPresent() &&
					getAlias().isPresent()
		);
	}

	@Input
	public abstract Property<Jar> getBuildTask();

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract RegularFileProperty getArchive();

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract RegularFileProperty getKeystore();

	@Input
	public abstract Property<String> getStorePass();

	@Input
	public abstract Property<String> getKeyPass();

	@Input
	public abstract Property<String> getAlias();

	@TaskAction
	public void signArtifact()
	{
		getProject().exec(exec ->
			exec.commandLine(
				"jarsigner",
				"-keystore", getKeystore().getAsFile().get().getAbsolutePath(),
				"-storepass", getStorePass().get(),
				"-keypass", getKeyPass().get(),
				getArchive().getAsFile().get().getAbsolutePath(),
				getAlias().get()
			));
	}

}
