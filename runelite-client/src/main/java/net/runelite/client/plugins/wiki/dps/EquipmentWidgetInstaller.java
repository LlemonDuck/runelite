/*
 * Copyright (c) 2024 LlemonDuck
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
package net.runelite.client.plugins.wiki.dps;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.api.annotations.Component;
import net.runelite.api.annotations.Interface;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

@UtilityClass
class EquipmentWidgetInstaller
{
	private static final int[] SPRITE_IDS_INACTIVE = new int[]{
		SpriteID.DIALOG_BACKGROUND_BRIGHTER,
		SpriteID.WORLD_MAP_BUTTON_METAL_CORNER_TOP_LEFT,
		SpriteID.WORLD_MAP_BUTTON_METAL_CORNER_TOP_RIGHT,
		SpriteID.WORLD_MAP_BUTTON_METAL_CORNER_BOTTOM_LEFT,
		SpriteID.WORLD_MAP_BUTTON_METAL_CORNER_BOTTOM_RIGHT,
		SpriteID.WORLD_MAP_BUTTON_EDGE_LEFT,
		SpriteID.WORLD_MAP_BUTTON_EDGE_TOP,
		SpriteID.WORLD_MAP_BUTTON_EDGE_RIGHT,
		SpriteID.WORLD_MAP_BUTTON_EDGE_BOTTOM,
	};

	private static final int[] SPRITE_IDS_ACTIVE = new int[]{
		SpriteID.RESIZEABLE_MODE_SIDE_PANEL_BACKGROUND,
		SpriteID.EQUIPMENT_BUTTON_METAL_CORNER_TOP_LEFT_HOVERED,
		SpriteID.EQUIPMENT_BUTTON_METAL_CORNER_TOP_RIGHT_HOVERED,
		SpriteID.EQUIPMENT_BUTTON_METAL_CORNER_BOTTOM_LEFT_HOVERED,
		SpriteID.EQUIPMENT_BUTTON_METAL_CORNER_BOTTOM_RIGHT_HOVERED,
		SpriteID.EQUIPMENT_BUTTON_EDGE_LEFT_HOVERED,
		SpriteID.EQUIPMENT_BUTTON_EDGE_TOP_HOVERED,
		SpriteID.EQUIPMENT_BUTTON_EDGE_RIGHT_HOVERED,
		SpriteID.EQUIPMENT_BUTTON_EDGE_BOTTOM_HOVERED,
	};

	private static final int FONT_COLOUR_INACTIVE = 0xff981f;
	private static final int FONT_COLOUR_ACTIVE = 0xffffff;

	@Getter
	@RequiredArgsConstructor
	enum Screen
	{
		EQUIPMENT_BONUSES(InterfaceID.EQUIPMENT_BONUSES, ComponentID.EQUIPMENT_BONUSES_PARENT, ComponentID.EQUIPMENT_BONUSES_SET_BONUS, ComponentID.EQUIPMENT_BONUSES_STAT_BONUS, 55),
		BANK_EQUIPMENT(InterfaceID.BANK_EQUIPMENT, ComponentID.BANK_EQUIPMENT_PARENT, ComponentID.BANK_EQUIPMENT_SET_BONUS, ComponentID.BANK_EQUIPMENT_STAT_BONUS, 49),
		;

		/** interface containing all the relevant widgets */
		@Getter(onMethod_ = @Interface)
		private final int interfaceId;

		/** parent widget of the interface, install target */
		@Getter(onMethod_ = @Component)
		private final int parentId;

		/** the "Set Bonus" button widget layer */
		@Getter(onMethod_ = @Component)
		private final int setBonusId;

		/** the "Stat Bonus" button widget layer, which replaces "Set Bonus" after it is clicked */
		@Getter(onMethod_ = @Component)
		private final int statBonusId;

		/** OriginalX for Set Bonus and Stat Bonus, prior to us moving them around (for shutdown) **/
		private final int originalX;

	}

	void tryAddButton(Client client, Runnable onClick)
	{
		for (EquipmentWidgetInstaller.Screen screen : EquipmentWidgetInstaller.Screen.values())
		{
			EquipmentWidgetInstaller.addButton(client, screen, onClick);
		}
	}

	/**
	 * Shifts over the Set Bonus / Stat Bonus buttons
	 * and adds new widgets to make a visually equal button with a different name.
	 */
	void addButton(Client client, Screen screen, Runnable onClick)
	{
		Widget parent = client.getWidget(screen.getParentId());
		Widget setBonus = client.getWidget(screen.getSetBonusId());
		Widget statBonus = client.getWidget(screen.getStatBonusId());
		Widget[] refComponents;
		if (parent == null || setBonus == null || statBonus == null || (refComponents = setBonus.getChildren()) == null)
		{
			return;
		}

		// Since the Set Bonus button uses absolute positioning,
		// we must also use absolute for all the children below,
		// which means it's necessary to offset the values by simulating corresponding pos/size modes.
		int padding = 8;
		int w = setBonus.getOriginalWidth();
		int h = setBonus.getOriginalHeight();
		int x = setBonus.getOriginalX() + (w / 2) + (padding / 2);
		int y = setBonus.getOriginalY();
		if (screen == Screen.BANK_EQUIPMENT) // uses ABSOLUTE_CENTER
		{
			y += parent.getHeight() / 2 - setBonus.getHeight() / 2;
		}

		// now shift the Set Bonus and Stat Bonus buttons over a bit to make room
		setBonus.setOriginalX(setBonus.getOriginalX() - (w / 2) - (padding / 2))
			.revalidate();
		statBonus.setOriginalX(statBonus.getOriginalX() - (w / 2) - (padding / 2))
			.revalidate();

		final Widget[] spriteWidgets = new Widget[9];

		// the background uses ABSOLUTE_CENTER and MINUS sizing
		int bgWidth = w - refComponents[0].getOriginalWidth();
		int bgHeight = h - refComponents[0].getOriginalHeight();
		int bgX = (x + refComponents[0].getOriginalX()) + (w - bgWidth) / 2;
		int bgY = (y + refComponents[0].getOriginalY()) + (h - bgHeight) / 2;
		spriteWidgets[0] = parent.createChild(-1, WidgetType.GRAPHIC)
			.setSpriteId(refComponents[0].getSpriteId())
			.setPos(bgX, bgY)
			.setSize(bgWidth, bgHeight);
		spriteWidgets[0].revalidate();

		// borders and corners all use absolute positioning which is easy
		for (int i = 1; i < 9; i++)
		{
			spriteWidgets[i] = parent.createChild(-1, WidgetType.GRAPHIC)
				.setSpriteId(refComponents[i].getSpriteId())
				.setPos(x + refComponents[i].getOriginalX(), y + refComponents[i].getOriginalY())
				.setSize(refComponents[i].getOriginalWidth(), refComponents[i].getOriginalHeight());
			spriteWidgets[i].revalidate();
		}

		// text label uses ABSOLUTE_CENTER positioning and MINUS sizing,
		// but matches size of parent so effectively no-op
		final Widget text = parent.createChild(-1, WidgetType.TEXT)
			.setText("View DPS")
			.setTextColor(FONT_COLOUR_INACTIVE)
			.setFontId(refComponents[9].getFontId())
			.setTextShadowed(refComponents[9].getTextShadowed())
			.setXTextAlignment(refComponents[9].getXTextAlignment())
			.setYTextAlignment(refComponents[9].getYTextAlignment())
			.setPos(x, y)
			.setSize(w, h);
		text.revalidate();

		// we'll give the text layer the listeners since it covers the whole area
		text.setHasListener(true);
		text.setOnMouseOverListener((JavaScriptCallback) ev ->
		{
			for (int i = 0; i <= 8; i++)
			{
				spriteWidgets[i].setSpriteId(SPRITE_IDS_ACTIVE[i]);
			}
			text.setTextColor(FONT_COLOUR_ACTIVE);
		});
		text.setOnMouseLeaveListener((JavaScriptCallback) ev ->
		{
			for (int i = 0; i <= 8; i++)
			{
				spriteWidgets[i].setSpriteId(SPRITE_IDS_INACTIVE[i]);
			}
			text.setTextColor(FONT_COLOUR_INACTIVE);
		});

		// register a click listener
		text.setAction(0, "View DPS on OSRS Wiki");
		text.setOnOpListener((JavaScriptCallback) ev -> onClick.run());

		// recompute locations / sizes on parent
		parent.revalidate();
	}

	void removeButton(Client client)
	{
		for (Screen screen : Screen.values())
		{
			Widget parent = client.getWidget(screen.getParentId());
			if (parent != null)
			{
				parent.deleteAllChildren();
				parent.revalidate();
			}

			Widget setBonus = client.getWidget(screen.getSetBonusId());
			if (setBonus != null)
			{
				setBonus.setOriginalX(screen.getOriginalX())
					.revalidate();
			}

			Widget statBonus = client.getWidget(screen.getStatBonusId());
			if (statBonus != null)
			{
				statBonus.setOriginalX(screen.getOriginalX())
					.revalidate();
			}
		}
	}
}
