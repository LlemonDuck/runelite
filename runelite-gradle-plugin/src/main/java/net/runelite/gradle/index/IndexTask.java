package net.runelite.gradle.index;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import static java.lang.Integer.parseInt;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

@CacheableTask
public abstract class IndexTask extends DefaultTask
{

	@InputDirectory
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract DirectoryProperty getArchiveOverlayDirectory();

	@OutputFile
	public abstract RegularFileProperty getIndexFile();

	@TaskAction
	public void buildRs2Index() throws IOException
	{
		File archiveOverlayDirectory = getArchiveOverlayDirectory().getAsFile().get();
		File indexFile = getIndexFile().getAsFile().get();

		try (DataOutputStream fout = new DataOutputStream(new FileOutputStream(indexFile)))
		{
			for (File indexFolder : archiveOverlayDirectory.listFiles())
			{
				if (indexFolder.isDirectory())
				{
					int indexId = parseInt(indexFolder.getName());
					for (File archiveFile : indexFolder.listFiles())
					{
						int archiveId;
						try
						{
							archiveId = parseInt(archiveFile.getName());
						}
						catch (NumberFormatException ex)
						{
							continue;
						}

						fout.writeInt(indexId << 16 | archiveId);
					}
				}
			}

			fout.writeInt(-1);
		}
	}

}
