/*
 * Copyright (c) 2025, Hamish <https://github.com/DustyRealm>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.config;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JSeparator;
import lombok.AllArgsConstructor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.FontType;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;

@AllArgsConstructor
final class TruncatedListCellRenderer extends DefaultListCellRenderer
{
	private final int maxLength;

	private String truncateValue(String value)
	{
		if (value.length() > maxLength)
		{
			return value.substring(0, maxLength) + "…";
		}

		return value;
	}

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		if (value.equals("---"))
		{
			return new JSeparator(JSeparator.HORIZONTAL);
		}

		String text = truncateValue(value.toString());
		return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
	}
}

class FontPanel extends ConfigSubPanel<FontType>
{

	@Inject
	private FontPanel(
		ConfigManager configManager,
		ColorPickerManager colorPickerManager,
		PluginListPanel pluginListPanel
	)
	{
		super(configManager, colorPickerManager, pluginListPanel);
	}

	@Override
	protected void rebuild(FontType fontType)
	{
		List<String> fontOptions = new ArrayList<>();
		fontOptions.addAll(FontManager.getBuiltInFonts());
		fontOptions.add("---");
		fontOptions.addAll(FontManager.getCustomFonts());
		fontOptions.add("---");
		fontOptions.addAll(FontManager.getSystemFonts());

		item(
			"Font",
			"Configures the font.",
			createCombobox(fontOptions.toArray(new String[0]), fontType.getFamily(), new TruncatedListCellRenderer(23), FontType::withFamily)
		);

		item(
			"Size",
			"Configures the font size.",
			createIntSpinner(1, Integer.MAX_VALUE, fontType.getSize(), "pt", FontType::withSize)
		);

		item(
			"Bold",
			"Toggle bold styling for the font.",
			createCheckbox(fontType.isBold(), FontType::withBold)
		);

		item(
			"Italic",
			"Toggle italic styling for the font.",
			createCheckbox(fontType.isItalic(), FontType::withItalic)
		);
	}
}
