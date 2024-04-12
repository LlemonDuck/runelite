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

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RSClockTest
{

	@Mock
	private Client client;

	@InjectMocks
	private RSClock clock;

	@Test
	public void testTickerGameTime()
	{
		when(client.getTickCount()).thenReturn(1, 5, 10);

		clock.onGameTick(new GameTick());
		assertEquals(RSClock.getTime(RSTimeUnit.GAME_TICKS).getTick(), 1);

		clock.onGameTick(new GameTick());
		assertEquals(RSClock.getTime(RSTimeUnit.GAME_TICKS).getTick(), 5);

		clock.onGameTick(new GameTick());
		assertEquals(RSClock.getTime(RSTimeUnit.GAME_TICKS).getTick(), 10);
	}

	@Test
	public void testTickerClientTime()
	{
		when(client.getGameCycle()).thenReturn(1, 5, 10);

		clock.onClientTick(new ClientTick());
		assertEquals(RSClock.getTime(RSTimeUnit.CLIENT_TICKS).getTick(), 1);

		clock.onClientTick(new ClientTick());
		assertEquals(RSClock.getTime(RSTimeUnit.CLIENT_TICKS).getTick(), 5);

		clock.onClientTick(new ClientTick());
		assertEquals(RSClock.getTime(RSTimeUnit.CLIENT_TICKS).getTick(), 10);
	}

	@Test
	public void testTickerIncrementsPartitionOnWorldLogin()
	{
		clock.onGameTick(new GameTick());
		clock.onClientTick(new ClientTick());
		assertEquals(RSClock.getTime(RSTimeUnit.GAME_TICKS).getPartitionId(), 0);
		assertEquals(RSClock.getTime(RSTimeUnit.GAME_TICKS).getPartitionId(), 0);

		// LOGGING_IN should increment
		GameStateChanged ev = new GameStateChanged();
		ev.setGameState(GameState.LOGGING_IN);
		clock.onGameStateChanged(ev);
		clock.onGameTick(new GameTick());
		clock.onClientTick(new ClientTick());
		assertEquals(RSClock.getTime(RSTimeUnit.GAME_TICKS).getPartitionId(), 1);
		assertEquals(RSClock.getTime(RSTimeUnit.GAME_TICKS).getPartitionId(), 1);

		// HOPPING should increment
		ev.setGameState(GameState.HOPPING);
		clock.onGameStateChanged(ev);
		clock.onGameTick(new GameTick());
		clock.onClientTick(new ClientTick());
		assertEquals(RSClock.getTime(RSTimeUnit.GAME_TICKS).getPartitionId(), 2);
		assertEquals(RSClock.getTime(RSTimeUnit.GAME_TICKS).getPartitionId(), 2);

		// other events should not increment
		ev.setGameState(GameState.LOGGED_IN);
		clock.onGameStateChanged(ev);
		clock.onGameTick(new GameTick());
		clock.onClientTick(new ClientTick());
		assertEquals(RSClock.getTime(RSTimeUnit.GAME_TICKS).getPartitionId(), 2);
		assertEquals(RSClock.getTime(RSTimeUnit.GAME_TICKS).getPartitionId(), 2);

		ev.setGameState(GameState.LOADING);
		clock.onGameStateChanged(ev);
		clock.onGameTick(new GameTick());
		clock.onClientTick(new ClientTick());
		assertEquals(RSClock.getTime(RSTimeUnit.GAME_TICKS).getPartitionId(), 2);
		assertEquals(RSClock.getTime(RSTimeUnit.GAME_TICKS).getPartitionId(), 2);

		ev.setGameState(GameState.CONNECTION_LOST);
		clock.onGameStateChanged(ev);
		clock.onGameTick(new GameTick());
		clock.onClientTick(new ClientTick());
		assertEquals(RSClock.getTime(RSTimeUnit.GAME_TICKS).getPartitionId(), 2);
		assertEquals(RSClock.getTime(RSTimeUnit.GAME_TICKS).getPartitionId(), 2);
	}

}
