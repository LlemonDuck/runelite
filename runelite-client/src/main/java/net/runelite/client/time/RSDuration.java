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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@Getter
public class RSDuration implements TemporalAmount, Comparable<RSDuration>
{

	private final int ticks;
	private final RSTimeUnit unit;

	@Override
	public long get(TemporalUnit unit)
	{
		if (unit == this.unit)
		{
			return ticks;
		}
		throw new UnsupportedTemporalTypeException(unit.toString());
	}

	@Override
	public List<TemporalUnit> getUnits()
	{
		return Collections.singletonList(unit);
	}

	@Override
	public Temporal addTo(Temporal temporal)
	{
		if (temporal.isSupported((TemporalUnit) unit))
		{
			return temporal.plus(ticks, unit);
		}
		if (temporal.isSupported(ChronoUnit.MILLIS))
		{
			return temporal.plus(unit.getWallDuration().multipliedBy(ticks));
		}
		throw new UnsupportedTemporalTypeException(temporal.toString());
	}

	@Override
	public Temporal subtractFrom(Temporal temporal)
	{
		if (temporal instanceof RSInstant)
		{
			return temporal.minus(ticks, unit);
		}
		if (temporal.isSupported(ChronoUnit.MILLIS))
		{
			return temporal.minus(unit.getWallDuration().multipliedBy(ticks));
		}
		throw new UnsupportedTemporalTypeException(temporal.toString());
	}

	@Override
	public int compareTo(@NotNull RSDuration o)
	{
		return Integer.compare(ticks, o.ticks);
	}

	public Duration toWallDuration()
	{
		return unit.getWallDuration().multipliedBy(ticks);
	}

	@Override
	public String toString()
	{
		String unitStr = unit.getName().toLowerCase() + (ticks != 1 ? "s" : "");
		return "RSDuration(" + ticks + " " + unitStr + ")";
	}

	public static RSDuration ofGameTicks(int ticks)
	{
		return new RSDuration(ticks, RSTimeUnit.GAME_TICKS);
	}

	public static RSDuration ofClientTicks(int ticks)
	{
		return new RSDuration(ticks, RSTimeUnit.CLIENT_TICKS);
	}

	public static RSDuration between(RSInstant start, RSInstant end)
	{
		return new RSDuration((int) start.until(end, start.getUnit()), start.getUnit());
	}

}
