package net.runelite.gradle.jarsign;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

public interface JarsignExtension
{

	RegularFileProperty getKeystore();

	Property<String> getStorePass();

	Property<String> getKeyPass();

	Property<String> getAlias();

}
