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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@ConfigSerializer(TileHighlightSerializer.class)
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
@Getter
@With
public class TileHighlight
{

	public static final TileHighlight DISABLED = TileHighlight.builder()
		.build();

	public static final TileHighlight ENABLED = TileHighlight.builder()
		.enabled(true)
		.build();

	@Builder.Default
	@SerializedName("e")
	boolean enabled = false;

	@Builder.Default
	@SerializedName("w")
	int strokeWidth = 2;

	@Builder.Default
	@SerializedName("s")
	Color strokeColor = Color.YELLOW;

	@Builder.Default
	@SerializedName("f")
	Color fillColor = new Color(0, 0, 0, 50);

	@Builder.Default
	@SerializedName("b")
	int feather = 0;

}

class TileHighlightSerializer implements Serializer<TileHighlight>
{
	private final Gson gson;

	@Inject
	private TileHighlightSerializer(Gson gson)
	{
		this.gson = gson;
	}

	@Override
	public String serialize(TileHighlight value)
	{
		return gson.toJson(value);
	}

	@Override
	public TileHighlight deserialize(String s)
	{
		return gson.fromJson(s, TileHighlight.class);
	}
}
