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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.util.LinkBrowser;
import okhttp3.*;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class DpsLauncher
{
	private static final String UI_ENDPOINT = "https://tools.runescape.wiki/osrs-dps/";
	private static final String SHORTLINK_ENDPOINT = "https://tools.runescape.wiki/osrs-dps/shortlink";

	@Inject
	private Client client;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	@Nullable
	private JsonObject createEquipmentObject(ItemContainer itemContainer, EquipmentInventorySlot slot)
	{
		if (itemContainer == null)
		{
			return null;
		}

		Item item = itemContainer.getItem(slot.getSlotIdx());
		if (item != null)
		{
			JsonObject o = new JsonObject();
			o.addProperty("id", item.getId());
			return o;
		}
		return null;
	}

	private JsonObject buildShortlinkData()
	{
		JsonObject j = new JsonObject();

		// Build the player's loadout data
		JsonArray loadouts = new JsonArray();
		ItemContainer eqContainer = client.getItemContainer(InventoryID.EQUIPMENT);

		JsonObject l = new JsonObject();
		JsonObject eq = new JsonObject();

		eq.add("ammo", createEquipmentObject(eqContainer, EquipmentInventorySlot.AMMO);
		eq.add("body", createEquipmentObject(eqContainer, EquipmentInventorySlot.BODY);
		eq.add("cape", createEquipmentObject(eqContainer, EquipmentInventorySlot.CAPE);
		eq.add("feet", createEquipmentObject(eqContainer, EquipmentInventorySlot.BOOTS);
		eq.add("hands", createEquipmentObject(eqContainer, EquipmentInventorySlot.GLOVES);
		eq.add("head", createEquipmentObject(eqContainer, EquipmentInventorySlot.HEAD);
		eq.add("legs", createEquipmentObject(eqContainer, EquipmentInventorySlot.LEGS);
		eq.add("neck", createEquipmentObject(eqContainer, EquipmentInventorySlot.AMULET);
		eq.add("ring", createEquipmentObject(eqContainer, EquipmentInventorySlot.RING);
		eq.add("shield", createEquipmentObject(eqContainer, EquipmentInventorySlot.SHIELD);
		eq.add("weapon", createEquipmentObject(eqContainer, EquipmentInventorySlot.WEAPON);
		l.add("equipment", eq);

		JsonObject skills = new JsonObject();
		skills.addProperty("atk", client.getRealSkillLevel(Skill.ATTACK));
		skills.addProperty("def", client.getRealSkillLevel(Skill.DEFENCE));
		skills.addProperty("hp", client.getRealSkillLevel(Skill.HITPOINTS));
		skills.addProperty("magic", client.getRealSkillLevel(Skill.MAGIC));
		skills.addProperty("mining", client.getRealSkillLevel(Skill.MINING));
		skills.addProperty("prayer", client.getRealSkillLevel(Skill.PRAYER));
		skills.addProperty("ranged", client.getRealSkillLevel(Skill.RANGED));
		skills.addProperty("str", client.getRealSkillLevel(Skill.STRENGTH));
		l.add("skills", skills);

		JsonObject buffs = new JsonObject();
		buffs.addProperty("inWilderness", client.getVarbitValue(Varbits.IN_WILDERNESS) == 1);
		buffs.addProperty("kandarinDiary", client.getVarbitValue(Varbits.DIARY_KANDARIN_HARD) == 1);
		buffs.addProperty("onSlayerTask", client.getVarpValue(VarPlayer.SLAYER_TASK_SIZE) > 0);
		buffs.addProperty("chargeSpell", client.getVarpValue(VarPlayer.CHARGE_GOD_SPELL) > 0);
		l.add("buffs", buffs);

		l.addProperty("name", client.getLocalPlayer().getName());

		loadouts.add(l);
		j.add("loadouts", loadouts);

		return j;
	}

	public void launch()
	{
		JsonObject jsonBody = buildShortlinkData();
		Request request = new Request.Builder()
			.url(SHORTLINK_ENDPOINT)
			.post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody.toString()))
			.build();

		OkHttpClient client = okHttpClient.newBuilder()
			.callTimeout(5, TimeUnit.SECONDS)
			.build();
		try (Response response = client.newCall(request).execute())
		{
			if (response.isSuccessful() && response.body() != null)
			{
				ShortlinkResponse resp = gson.fromJson(response.body().charStream(), ShortlinkResponse.class);
				LinkBrowser.browse(UI_ENDPOINT + "?id=" + resp.data);
			}
			else
			{
				log.error("Failed to create shortlink for DPS calculator: " + response.code());
			}
		}
		catch (IOException ioException)
		{
			log.error("Failed to create shortlink for DPS calculator");
		}
	}

}
