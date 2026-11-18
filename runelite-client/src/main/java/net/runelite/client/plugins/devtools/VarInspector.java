/*
 * Copyright (c) 2018 Abex
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
package net.runelite.client.plugins.devtools;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.IndexDataBase;
import net.runelite.api.VarbitComposition;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.events.VarClientStrChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.Text;

class VarInspector extends DevToolsFrame
{
	@Getter
	private enum VarFilter
	{
		VARBIT("Varbit", "varbBlacklist",
			VarbitID.CLOCK, VarbitID.DATE_MILLISECONDS_PAST_MINUTE, VarbitID.DATE_SECONDS_PAST_MINUTE),
		VARP("VarPlayer", "varpBlacklist",
			VarPlayerID.DATE_MINUTES, VarPlayerID.MAP_CLOCK),
		VARC("VarClient", "varcBlacklist",
			VarClientID.CAMERA_ZOOM_BIG, VarClientID.CAMERA_ZOOM_SMALL);

		private final String name;
		private final String configKey;
		private final int[] defaultBlacklist;

		VarFilter(String name, String configKey, int... defaultBlacklist)
		{
			this.name = name;
			this.configKey = configKey;
			this.defaultBlacklist = defaultBlacklist;
		}
	}

	@Getter
	private enum VarType
	{
		VARBIT("Varbit", VarFilter.VARBIT),
		VARP("VarPlayer", VarFilter.VARP),
		VARCINT("VarClientInt", VarFilter.VARC),
		VARCSTR("VarClientStr", VarFilter.VARC);

		private final String name;
		private final VarFilter filter;
		private final JCheckBox checkBox;

		VarType(String name, VarFilter filter)
		{
			this.name = name;
			this.filter = filter;
			checkBox = new JCheckBox(name, true);
		}
	}

	private static final String CONFIG_GROUP = "devtools";

	private final static int MAX_LOG_ENTRIES = 10_000;
	private static final int VARBITS_ARCHIVE_ID = 14;
	private static final Map<Integer, String> VARBIT_NAMES = DevToolsPlugin.loadFieldNames(VarbitID.class);
	private static final Map<Integer, String> VARC_NAMES = DevToolsPlugin.loadFieldNames(VarClientID.class);
	private static final Map<Integer, String> VARP_NAMES = DevToolsPlugin.loadFieldNames(VarPlayerID.class);
	private static final Map<String, Integer> VARBIT_IDS = invertNames(VARBIT_NAMES);
	private static final Map<String, Integer> VARC_IDS = invertNames(VARC_NAMES);
	private static final Map<String, Integer> VARP_IDS = invertNames(VARP_NAMES);

	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final ConfigManager configManager;

	private final JPanel tracker = new JPanel();
	private final JTabbedPane blacklistTabs = new JTabbedPane();
	private final Map<VarFilter, Set<Integer>> blacklists = new EnumMap<>(VarFilter.class);
	private final Map<VarFilter, JList<Integer>> blacklistLists = new EnumMap<>(VarFilter.class);
	private final Map<VarFilter, DefaultListModel<Integer>> blacklistModels = new EnumMap<>(VarFilter.class);

	private int lastTick = 0;

	private int[] oldVarps = null;
	private int[] oldVarps2 = null;

	private Multimap<Integer, Integer> varbits;
	private Map<Integer, Object> varcs = null;

	@Inject
	VarInspector(Client client, ClientThread clientThread, EventBus eventBus, ConfigManager configManager)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.configManager = configManager;

		setTitle("RuneLite Var Inspector");

		setLayout(new BorderLayout());

		tracker.setLayout(new DynamicGridLayout(0, 1, 0, 3));

		final JPanel varEventsPanel = new JPanel();
		varEventsPanel.setLayout(new BorderLayout());

		final JPanel trackerWrapper = new JPanel();
		trackerWrapper.setLayout(new BorderLayout());
		trackerWrapper.add(tracker, BorderLayout.NORTH);

		final JScrollPane trackerScroller = new JScrollPane(trackerWrapper);
		trackerScroller.setPreferredSize(new Dimension(400, 400));

		final JScrollBar vertical = trackerScroller.getVerticalScrollBar();
		vertical.addAdjustmentListener(new AdjustmentListener()
		{
			int lastMaximum = actualMax();

			private int actualMax()
			{
				return vertical.getMaximum() - vertical.getModel().getExtent();
			}

			@Override
			public void adjustmentValueChanged(AdjustmentEvent e)
			{
				if (vertical.getValue() >= lastMaximum)
				{
					vertical.setValue(actualMax());
				}
				lastMaximum = actualMax();
			}
		});

		varEventsPanel.add(trackerScroller, BorderLayout.CENTER);

		final JPanel trackerOpts = new JPanel();
		trackerOpts.setLayout(new FlowLayout());
		for (VarType cb : VarType.values())
		{
			trackerOpts.add(cb.getCheckBox());
		}

		final JButton clearBtn = new JButton("Clear");
		clearBtn.addActionListener(e ->
		{
			tracker.removeAll();
			tracker.revalidate();
		});
		trackerOpts.add(clearBtn);

		varEventsPanel.add(trackerOpts, BorderLayout.SOUTH);
		add(varEventsPanel, BorderLayout.CENTER);

		final JPanel blacklistsPanel = new JPanel();
		blacklistsPanel.setLayout(new BorderLayout());

		for (VarFilter filter : VarFilter.values())
		{
			blacklists.put(filter, loadBlacklist(filter));

			final DefaultListModel<Integer> model = new DefaultListModel<>();
			final JList<Integer> jList = new JList<>(model);
			jList.setCellRenderer(new DefaultListCellRenderer()
			{
				@Override
				public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
				{
					return super.getListCellRendererComponent(list, formatVar(filter, (Integer) value), index, isSelected, cellHasFocus);
				}
			});
			blacklistModels.put(filter, model);
			blacklistLists.put(filter, jList);
			refreshList(filter);

			final JScrollPane listScrollPane = new JScrollPane(jList);
			listScrollPane.setPreferredSize(new Dimension(250, 400));
			blacklistTabs.addTab(filter.getName(), listScrollPane);
		}

		blacklistsPanel.add(blacklistTabs, BorderLayout.CENTER);

		final JTextField varInput = new JTextField(12);
		varInput.addActionListener(e -> addToBlacklist(varInput));

		final JButton addButton = new JButton("Add");
		addButton.addActionListener(e -> addToBlacklist(varInput));

		final JButton removeButton = new JButton("Remove");
		removeButton.addActionListener(e -> removeSelectedFromBlacklist());

		final JPanel bottomButtonRow = new JPanel();
		bottomButtonRow.setLayout(new FlowLayout());
		bottomButtonRow.add(addButton);
		bottomButtonRow.add(varInput);
		bottomButtonRow.add(removeButton);

		blacklistsPanel.add(bottomButtonRow, BorderLayout.SOUTH);

		add(blacklistsPanel, BorderLayout.EAST);

		pack();
	}

	private static Map<String, Integer> invertNames(Map<Integer, String> names)
	{
		return names.entrySet()
			.stream()
			.collect(Collectors.toMap(
				e -> e.getValue().toUpperCase(Locale.ROOT),
				Map.Entry::getKey,
				(a, b) -> a
			));
	}

	private static Map<Integer, String> getNames(VarFilter filter)
	{
		switch (filter)
		{
			case VARBIT:
				return VARBIT_NAMES;
			case VARP:
				return VARP_NAMES;
			default:
				return VARC_NAMES;
		}
	}

	private static Map<String, Integer> getIds(VarFilter filter)
	{
		switch (filter)
		{
			case VARBIT:
				return VARBIT_IDS;
			case VARP:
				return VARP_IDS;
			default:
				return VARC_IDS;
		}
	}

	private static String formatVar(VarFilter filter, int id)
	{
		final String name = getNames(filter).get(id);
		if (name == null)
		{
			return Integer.toString(id);
		}

		return name + " (" + id + ")";
	}

	private Set<Integer> loadBlacklist(VarFilter filter)
	{
		final Set<Integer> set = ConcurrentHashMap.newKeySet();
		final String config = configManager.getConfiguration(CONFIG_GROUP, filter.getConfigKey());
		if (config != null)
		{
			try
			{
				for (String id : Text.fromCSV(config))
				{
					set.add(Integer.parseInt(id));
				}
				return set;
			}
			catch (NumberFormatException e)
			{
				set.clear();
			}
		}

		for (int id : filter.getDefaultBlacklist())
		{
			set.add(id);
		}
		return set;
	}

	private void saveBlacklists()
	{
		for (VarFilter filter : VarFilter.values())
		{
			configManager.setConfiguration(
				CONFIG_GROUP,
				filter.getConfigKey(),
				Text.toCSV(blacklists.get(filter)
					.stream()
					.sorted()
					.map(String::valueOf)
					.collect(Collectors.toList()))
			);
		}
	}

	private VarFilter getSelectedFilter()
	{
		return VarFilter.values()[blacklistTabs.getSelectedIndex()];
	}

	private void addToBlacklist(JTextField field)
	{
		final String input = field.getText().trim();
		if (input.isEmpty())
		{
			return;
		}

		final VarFilter filter = getSelectedFilter();
		Integer id;
		try
		{
			id = Integer.parseInt(input);
		}
		catch (NumberFormatException e)
		{
			id = getIds(filter).get(input.toUpperCase(Locale.ROOT));
		}

		if (id == null)
		{
			JOptionPane.showMessageDialog(
				this,
				"Unknown " + filter.getName() + ": " + input,
				"Var Inspector",
				JOptionPane.WARNING_MESSAGE
			);
			return;
		}

		blacklists.get(filter).add(id);
		refreshList(filter);
		field.setText("");
	}

	private void removeSelectedFromBlacklist()
	{
		final VarFilter filter = getSelectedFilter();
		blacklists.get(filter).removeAll(blacklistLists.get(filter).getSelectedValuesList());
		refreshList(filter);
	}

	private void refreshList(VarFilter filter)
	{
		final DefaultListModel<Integer> model = blacklistModels.get(filter);
		model.clear();
		blacklists.get(filter)
			.stream()
			.sorted()
			.forEach(model::addElement);
	}

	private void addVarLog(VarType type, int id, String name, int old, int neew)
	{
		addVarLog(type, id, name, Integer.toString(old), Integer.toString(neew));
	}

	private void addVarLog(VarType type, int id, String name, String old, String neew)
	{
		if (!type.getCheckBox().isSelected() || blacklists.get(type.getFilter()).contains(id))
		{
			return;
		}

		int tick = client.getTickCount();
		SwingUtilities.invokeLater(() ->
		{
			if (tick != lastTick)
			{
				lastTick = tick;
				JLabel header = new JLabel("Tick " + tick);
				header.setFont(FontManager.getRunescapeSmallFont());
				header.setBorder(new CompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.LIGHT_GRAY_COLOR),
					BorderFactory.createEmptyBorder(3, 6, 0, 0)
				));
				tracker.add(header);
			}
			tracker.add(new JLabel(String.format("%s %s changed: %s -> %s", type.getName(), name, old, neew)));

			// Cull very old stuff
			while (tracker.getComponentCount() > MAX_LOG_ENTRIES)
			{
				tracker.remove(0);
			}

			tracker.revalidate();
		});
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged varbitChanged)
	{
		int index = varbitChanged.getIndex();
		int[] varps = client.getVarps();

		// Check varbits
		for (int i : varbits.get(index))
		{
			int old = client.getVarbitValue(oldVarps, i);
			int neew = client.getVarbitValue(varps, i);
			if (old != neew)
			{
				// Set the varbit so it doesn't show in the varp changes
				// However, some varbits share common bits, so we only do it in oldVarps2
				// Example: 4101 collides with 4104-4129
				client.setVarbitValue(oldVarps2, i, neew);

				final String name = VARBIT_NAMES.getOrDefault(i, Integer.toString(i));
				addVarLog(VarType.VARBIT, i, name, old, neew);
			}
		}

		// Check varps
		int old = oldVarps2[index];
		int neew = varps[index];
		if (old != neew)
		{
			String name = VARP_NAMES.get(index);
			if (name != null)
			{
				name += "(" + index + ")";
			}
			else
			{
				name = Integer.toString(index);
			}
			addVarLog(VarType.VARP, index, name, old, neew);
		}

		System.arraycopy(client.getVarps(), 0, oldVarps, 0, oldVarps.length);
		System.arraycopy(client.getVarps(), 0, oldVarps2, 0, oldVarps2.length);
	}

	@Subscribe
	public void onVarClientIntChanged(VarClientIntChanged e)
	{
		int idx = e.getIndex();
		int neew = (Integer) client.getVarcMap().getOrDefault(idx, 0);
		int old = (Integer) varcs.getOrDefault(idx, 0);
		varcs.put(idx, neew);

		if (old != neew)
		{
			final String name = VARC_NAMES.getOrDefault(idx, Integer.toString(idx));
			addVarLog(VarType.VARCINT, idx, name, old, neew);
		}
	}

	@Subscribe
	public void onVarClientStrChanged(VarClientStrChanged e)
	{
		int idx = e.getIndex();
		String neew = (String) client.getVarcMap().getOrDefault(idx, "");
		String old = (String) varcs.getOrDefault(idx, "");
		varcs.put(idx, neew);

		if (!Objects.equals(old, neew))
		{
			final String name = VARC_NAMES.getOrDefault(idx, Integer.toString(idx));
			if (old != null)
			{
				old = "\"" + old + "\"";
			}
			else
			{
				old = "null";
			}
			if (neew != null)
			{
				neew = "\"" + neew + "\"";
			}
			else
			{
				neew = "null";
			}
			addVarLog(VarType.VARCSTR, idx, name, old, neew);
		}
	}

	@Override
	public void open()
	{
		if (oldVarps == null)
		{
			oldVarps = new int[client.getVarps().length];
			oldVarps2 = new int[client.getVarps().length];
		}

		System.arraycopy(client.getVarps(), 0, oldVarps, 0, oldVarps.length);
		System.arraycopy(client.getVarps(), 0, oldVarps2, 0, oldVarps2.length);
		varcs = new HashMap<>(client.getVarcMap());
		varbits = HashMultimap.create();

		clientThread.invoke(() ->
		{
			// Build varp index -> varbit id map
			IndexDataBase indexVarbits = client.getIndexConfig();
			final int[] varbitIds = indexVarbits.getFileIds(VARBITS_ARCHIVE_ID);
			for (int id : varbitIds)
			{
				VarbitComposition varbit = client.getVarbit(id);
				if (varbit != null)
				{
					varbits.put(varbit.getIndex(), id);
				}
			}
		});

		eventBus.register(this);
		super.open();
	}

	@Override
	public void close()
	{
		saveBlacklists();
		super.close();
		tracker.removeAll();
		eventBus.unregister(this);
		varcs = null;
		varbits = null;
	}
}
