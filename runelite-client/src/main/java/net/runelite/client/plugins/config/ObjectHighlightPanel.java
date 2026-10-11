/*
 * Copyright (c) 2026, LlemonDuck <napkinorton@gmail.com>
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

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.ObjectHighlight;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;

@Slf4j
class ObjectHighlightPanel extends ConfigSubPanel<ObjectHighlight>
{

	@Inject
	private ObjectHighlightPanel(
		ConfigManager configManager,
		ColorPickerManager colorPickerManager,
		PluginListPanel pluginList
	)
	{
		super(configManager, colorPickerManager, pluginList);
	}

	@Override
	protected void rebuild(ObjectHighlight highlight)
	{
		item(
			"Highlight mode",
			"How to highlight the object.",
			createList(
				ObjectHighlight.ObjectHighlightMode.values(),
				highlight.getHighlightModes(),
				ObjectHighlight::withHighlightModes
			)
		);

		item(
			"Outline width",
			"The width of the outline.",
			createIntSpinner(0, 100, highlight.getStrokeWidth(), "px", ObjectHighlight::withStrokeWidth)
		);

		item(
			"Outline feather",
			"Specify between 0-4 how much of the outline should be faded.",
			createIntSpinner(0, 4, highlight.getFeather(), "px", ObjectHighlight::withFeather)
		);

		item(
			"Outline color",
			"The color of the outline.",
			createColorPicker("Outline color", highlight.getStrokeColor(), ObjectHighlight::withStrokeColor)
		);

		item(
			"Fill color",
			"The color of the fill.",
			createColorPicker("Fill color", highlight.getFillColor(), ObjectHighlight::withFillColor)
		);

		addResetButton();
	}
}
