/*
 * Copyright (c) 2024, LlemonDuck <napkinorton@gmail.com>
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
package net.runelite.client.time;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Singleton
public class RSClock
{

	@VisibleForTesting
	static final AtomicReference<TickTimestamp> gameTime = new AtomicReference<>();

	@VisibleForTesting
	static final AtomicReference<TickTimestamp> clientTime = new AtomicReference<>();

	@VisibleForTesting
	static final AtomicInteger partition = new AtomicInteger();

	private final Client client;

	@Subscribe(priority = Integer.MAX_VALUE)
	public void onGameTick(GameTick e)
	{
		gameTime.set(new TickTimestamp(partition.getPlain(), client.getTickCount(), System.currentTimeMillis()));
	}

	@Subscribe(priority = Integer.MAX_VALUE)
	public void onClientTick(ClientTick e)
	{
		clientTime.set(new TickTimestamp(partition.getPlain(), client.getGameCycle(), System.currentTimeMillis()));
	}

	@Subscribe(priority = Integer.MAX_VALUE)
	public void onGameStateChanged(GameStateChanged e)
	{
		switch (e.getGameState())
		{
			case LOGGING_IN:
			case HOPPING:
				gameTime.set(null);
				clientTime.set(null);
				partition.incrementAndGet();
				break;
		}
	}

	static TickTimestamp getTime(RSTimeUnit unit)
	{
		TickTimestamp ts;
		switch (unit)
		{
			case GAME_TICKS:
				ts = gameTime.getPlain();
				break;

			case CLIENT_TICKS:
				ts = clientTime.getPlain();
				break;

			default:
				throw new UnsupportedOperationException();
		}

		if (ts == null)
		{
			throw new IllegalStateException("Cannot take a timestamp before any ticks are recorded");
		}
		return ts;
	}

}
