package net.runelite.gradle.component;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.lang.model.element.Modifier;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.tomlj.Toml;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

@CacheableTask
public abstract class ComponentTask extends DefaultTask
{

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract RegularFileProperty getInputFile();

	@OutputDirectory
	public abstract DirectoryProperty getOutputDirectory();

	private final Logger log = getLogger();
	private final Set<Integer> seenInterfaces = new HashSet<>();
	private final Set<Integer> seenComponents = new HashSet<>();

	@TaskAction
	public void packComponents() throws IOException
	{
		File inputFile = getInputFile().getAsFile().get();
		File outputDirectory = getOutputDirectory().getAsFile().get();

		TypeSpec.Builder interfaceType = TypeSpec.classBuilder("InterfaceID")
			.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
			.addAnnotation(Deprecated.class)
			.addJavadoc("@deprecated Use {@link net.runelite.api.gameval.InterfaceID} instead");;

		TypeSpec.Builder componentType = TypeSpec.classBuilder("ComponentID")
			.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
			.addAnnotation(Deprecated.class)
			.addJavadoc("@deprecated Use nested classes of {@link net.runelite.api.gameval.InterfaceID} instead");

		executeOne(inputFile, interfaceType, componentType);

		writeClass(outputDirectory, "net.runelite.api.widgets", interfaceType.build());
		writeClass(outputDirectory, "net.runelite.api.widgets", componentType.build());
	}

	private void executeOne(File file, TypeSpec.Builder interfaceType, TypeSpec.Builder componentType) throws IOException
	{
		TomlParseResult result = Toml.parse(file.toPath());

		if (result.hasErrors())
		{
			for (TomlParseError err : result.errors())
			{
				log.error(err.toString());
			}
			throw new RuntimeException("unable to parse component file " + file.getName());
		}

		for (var entry : result.entrySet())
		{
			var interfaceName = entry.getKey();
			TomlTable tbl = (TomlTable) entry.getValue();

			if (!tbl.contains("id"))
			{
				throw new RuntimeException("interface " + interfaceName + " has no id");
			}

			int interfaceId = (int) (long) tbl.getLong("id");
			if (interfaceId < 0 || interfaceId > 0xffff)
			{
				throw new RuntimeException("interface id out of range for " + interfaceName);
			}

			if (seenInterfaces.contains(interfaceId))
			{
				throw new RuntimeException("duplicate interface id " + interfaceId);
			}
			seenInterfaces.add(interfaceId);

			addField(interfaceType, interfaceName.toUpperCase(Locale.ENGLISH), interfaceId, null);

			for (var entry2 : tbl.entrySet())
			{
				var componentName = entry2.getKey();
				if (componentName.equals("id"))
				{
					continue;
				}

				int id = (int) (long) entry2.getValue();
				if (id < 0 || id > 0xffff)
				{
					throw new RuntimeException("component id out of range for " + componentName);
				}

				var fullName = interfaceName.toUpperCase(Locale.ENGLISH) + "_" + componentName.toUpperCase(Locale.ENGLISH);
				var comment = interfaceId + ":" + id;
				int componentId = (interfaceId << 16) | id;

				if (seenComponents.contains(componentId))
				{
					throw new RuntimeException("duplicate component id " + comment);
				}
				seenComponents.add(componentId);

				addField(componentType, fullName, componentId, comment);
			}
		}
	}

	private static void addField(TypeSpec.Builder type, String name, int value, String comment)
	{
		FieldSpec.Builder field = FieldSpec.builder(int.class, name)
			.addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
			.initializer("$L", value);
		if (comment != null)
		{
			field.addJavadoc(comment);
		}
		type.addField(field.build());
	}

	private void writeClass(File outputDirectory, String pkg, TypeSpec type) throws IOException
	{
		JavaFile.builder(pkg, type)
			.build()
			.writeToFile(outputDirectory);
	}
}
