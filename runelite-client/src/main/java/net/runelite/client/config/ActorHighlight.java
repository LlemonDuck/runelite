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
package net.runelite.client.config;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import java.awt.Color;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import net.runelite.client.game.npcoverlay.HighlightedNpc;

@ConfigSerializer(ActorHighlightSerializer.class)
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
@Getter
@With
public class ActorHighlight
{

	public static final ActorHighlight DISABLED = ActorHighlight.builder()
		.build();

	public static final ActorHighlight ENABLED = ActorHighlight.builder()
		.enabled(true)
		.build();

	public enum ActorHighlightMode
	{
		TILE,
		TRUE_TILE,
		SW_TILE,
		SW_TRUE_TILE,
		HULL,
		OUTLINE,
	}

	@SerializedName("e")
	boolean enabled;

	@Builder.Default
	@SerializedName("m")
	Set<ActorHighlightMode> highlightModes = Set.of(ActorHighlightMode.HULL);

	@Builder.Default
	@SerializedName("w")
	int strokeWidth = 2;

	@Builder.Default
	@SerializedName("s")
	Color strokeColor = Color.CYAN;

	@Builder.Default
	@SerializedName("f")
	Color fillColor = new Color(0, 0, 0, 50);

	@SerializedName("b")
	int feather;

	@SerializedName("n")
	boolean showName;

	@SerializedName("N")
	boolean showNameOnMinimap;

	public HighlightedNpc.HighlightedNpcBuilder toHighlightedNpcBuilder()
	{
		return HighlightedNpc.builder()
			.highlightColor(getStrokeColor())
			.fillColor(getFillColor())
			.hull(getHighlightModes().contains(ActorHighlightMode.HULL))
			.tile(getHighlightModes().contains(ActorHighlightMode.TILE))
			.trueTile(getHighlightModes().contains(ActorHighlightMode.TRUE_TILE))
			.swTile(getHighlightModes().contains(ActorHighlightMode.SW_TILE))
			.swTrueTile(getHighlightModes().contains(ActorHighlightMode.SW_TRUE_TILE))
			.outline(getHighlightModes().contains(ActorHighlightMode.OUTLINE))
			.name(isShowName())
			.nameOnMinimap(isShowNameOnMinimap())
			.borderWidth(getStrokeWidth())
			.outlineFeather(getFeather());
	}

}

class ActorHighlightSerializer implements Serializer<ActorHighlight>
{
	private final Gson gson;

	@Inject
	private ActorHighlightSerializer(Gson gson)
	{
		this.gson = gson;
	}

	@Override
	public String serialize(ActorHighlight value)
	{
		return gson.toJson(value);
	}

	@Override
	public ActorHighlight deserialize(String s)
	{
		return gson.fromJson(s, ActorHighlight.class);
	}
}
