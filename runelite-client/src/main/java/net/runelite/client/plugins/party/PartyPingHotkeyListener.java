/*
 * Copyright (c) 2022, Adam <Adam@sigterm.info>
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
package net.runelite.client.plugins.party;

import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Tile;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.party.PartyService;
import net.runelite.client.plugins.party.messages.TilePing;
import net.runelite.client.util.HotkeyListener;

@Singleton
@Slf4j
public class PartyPingHotkeyListener extends HotkeyListener
{
	
	private final Client client;
	private final ClientThread clientThread;
	private final PartyConfig config;
	private final PartyService partyService;
	
	@Inject
	public PartyPingHotkeyListener(Client client, ClientThread clientThread, PartyConfig config, PartyService partyService)
	{
		super(config::pingHotkey);
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.partyService = partyService;
	}

	@Override
	public void hotkeyPressed()
	{
		clientThread.invokeLater(() ->
		{
			if (client.isMenuOpen() || partyService.getMembers().isEmpty() || !config.pings())
			{
				return;
			}

			Tile selectedSceneTile = client.getSelectedSceneTile();
			if (selectedSceneTile == null)
			{
				return;
			}

			boolean isOnCanvas = false;
			for (MenuEntry me : client.getMenuEntries())
			{
				if (me != null)
				{
					if (me.getType() == MenuAction.WALK)
					{
						isOnCanvas = true;
						break;
					}
				}
			}
			if (!isOnCanvas)
			{
				return;
			}

			final TilePing tilePing = new TilePing(selectedSceneTile.getWorldLocation());
			tilePing.setMemberId(partyService.getLocalMember().getMemberId());
			partyService.send(tilePing);
		});
	}
}
