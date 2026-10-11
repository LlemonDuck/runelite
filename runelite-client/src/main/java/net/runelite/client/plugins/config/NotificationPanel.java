/*
 * Copyright (c) 2024, Adam <Adam@sigterm.info>
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

import java.awt.BorderLayout;
import java.awt.TrayIcon;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.FlashNotification;
import net.runelite.client.config.Notification;
import net.runelite.client.config.NotificationSound;
import net.runelite.client.config.RequestFocusType;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;

@Slf4j
class NotificationPanel extends ConfigSubPanel<Notification>
{
	private final JCheckBox enabled;

	@Inject
	private NotificationPanel(
		ConfigManager configManager,
		ColorPickerManager colorPickerManager,
		PluginListPanel pluginList,
		RuneLiteConfig runeLiteConfig
	)
	{
		super(configManager, colorPickerManager, pluginList);

		enabled = new JCheckBox();
		enabled.addActionListener(l ->
		{
			Notification notif = load();
			// If the notification has not been initialized yet, apply default config from the RuneLite settings
			if (enabled.isSelected() && !notif.isInitialized())
			{
				log.debug("Initializing notification {}.{}", configDescriptor.getGroup().value(), configItemDescriptor.getItem().name());
				notif = new Notification(true, true, true,
					runeLiteConfig.enableTrayNotifications(), TrayIcon.MessageType.NONE, runeLiteConfig.notificationRequestFocus(),
					runeLiteConfig.notificationSound(), null, runeLiteConfig.notificationVolume(), runeLiteConfig.notificationTimeout(),
					runeLiteConfig.enableGameMessageNotification(), runeLiteConfig.flashNotification(), runeLiteConfig.notificationFlashColor(),
					runeLiteConfig.sendNotificationsWhenFocused());
			}
			else
			{
				notif = notif.withOverride(enabled.isSelected());
			}

			save(notif);
			rebuild();
		});
	}

	@Override
	protected void rebuild(Notification notif)
	{
		enabled.setSelected(notif.isOverride());
		item("Customize notification", "", enabled);

		if (notif.isOverride())
		{
			item(
				"Tray notification",
				"Enables tray notifications.",
				createCheckbox(notif.isTray(), Notification::withTray)
			);

			item(
				"Request focus",
				"Configures the window focus request type on notification.",
				createCombobox(RequestFocusType.values(), notif.getRequestFocus(), Notification::withRequestFocus)
			);

			item(
				"Notification sound",
				"Enables the playing of a sound when notifications are displayed.",
				createNotificationOptionsCombobox(notif)
			);

			item(
				"Notification volume",
				"Configures the volume of custom notifications (does not control native volume).",
				createIntSpinner(0, 100, notif.getVolume(), "%", Notification::withVolume)
			);

			item(
				"Notification timeout",
				"How long notification will be shown in milliseconds. A value of 0 will make it use the system configuration. (Linux only)",
				createIntSpinner(0, Integer.MAX_VALUE, notif.getTimeout(), "ms", Notification::withTimeout)
			);

			item(
				"Game message notification",
				"Adds a notification message to the chatbox.",
				createCheckbox(notif.isGameMessage(), Notification::withGameMessage)
			);

			item(
				"Flash",
				"Flashes the game frame as a notification.",
				createCombobox(FlashNotification.values(), notif.getFlash(), Notification::withFlash)
			);

			item(
				"Flash color",
				"The color of the notification flashes.",
				createColorPicker("Flash color", notif.getFlashColor(), Notification::withFlashColor)
			);

			item(
				"Send notifications when focused",
				"Send the notification even when the client is focused.",
				createCheckbox(notif.isSendWhenFocused(), Notification::withSendWhenFocused)
			);

			addResetButton();
		}
		else
		{
			JPanel infoPanel = new JPanel();
			infoPanel.setLayout(new BorderLayout());
			mainPanel.add(infoPanel);

			infoPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
			infoPanel.add(new JLabel("<html>Notification settings can be customized for each type of notification. Notifications without custom settings use the default settings found in the 'RuneLite' configuration under 'Notification Settings'.</html>"));
		}
	}

	@Override
	protected void reset()
	{
		save(new Notification().withEnabled(true));
	}

	private JComboBox<NotificationOption> createNotificationOptionsCombobox(Notification notif)
	{
		// Native, Custom, Off
		var options = Arrays.stream(NotificationSound.values())
			.map(ns -> new NotificationOption(ns, null, ns.toString()))
			.collect(Collectors.toCollection(ArrayList::new));
		options.addAll(loadCustomNotifications());

		var existing = options.stream()
			.filter(no -> no.type == notif.getSound() && Objects.equals(no.soundName, notif.getSoundName()))
			.findAny()
			.orElse(null);

		if (existing == null)
		{
			// this should only happen for file notifications which have been deleted
			String optionName = notif.getSoundName().substring(0, notif.getSoundName().length() - ".wav".length());
			options.add(existing = new NotificationOption(
				notif.getSound(),
				notif.getSoundName(),
				"<html><font color=red>" + optionName + "</font></html>"
			));
		}

		return createCombobox(options.toArray(new NotificationOption[0]), existing, (n, selected) ->
		{
			NotificationSound sound = selected.type;
			String soundName = selected.soundName;

			log.debug("Notification changed to {} ({})", sound, soundName != null ? soundName : "no file");

			return n.withSound(sound)
				.withSoundName(soundName);
		});
	}

	private static List<NotificationOption> loadCustomNotifications()
	{
		File[] files = RuneLite.NOTIFICATIONS_DIR.listFiles();
		if (files == null)
		{
			return Collections.emptyList();
		}

		return Arrays.stream(files)
			.filter(f -> f.getName().endsWith(".wav"))
			.map(f -> new NotificationOption(
				NotificationSound.CUSTOM,
				f.getName(),
				// .wav extension is hidden from ui
				f.getName().substring(0, f.getName().length() - ".wav".length())
			))
			.collect(Collectors.toList());
	}

	@AllArgsConstructor
	private static class NotificationOption
	{
		NotificationSound type;
		String soundName;
		String optionName;

		@Override
		public String toString()
		{
			return optionName;
		}
	}
}
