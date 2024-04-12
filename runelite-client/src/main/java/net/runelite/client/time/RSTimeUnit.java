/*
 * Copyright (c) 2020, Jordan Atwood <jordan.atwood423@gmail.com>
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
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import lombok.Getter;
import net.runelite.api.Constants;

@Getter
public enum RSTimeUnit implements TemporalUnit, TemporalField
{
	CLIENT_TICKS("Client tick", Duration.ofMillis(Constants.CLIENT_TICK_LENGTH)),
	GAME_TICKS("Game tick", Duration.ofMillis(Constants.GAME_TICK_LENGTH)),
	;

	private final String name;
	private final Duration wallDuration;

	RSTimeUnit(String name, Duration estimatedDuration)
	{
		this.name = name;
		wallDuration = estimatedDuration;
	}

	@Override
	public Duration getDuration()
	{
		return wallDuration;
	}

	@Override
	public boolean isDurationEstimated()
	{
		return false;
	}

	@Override
	public TemporalUnit getBaseUnit()
	{
		return this;
	}

	@Override
	public TemporalUnit getRangeUnit()
	{
		return ChronoUnit.FOREVER;
	}

	@Override
	public ValueRange range()
	{
		return ValueRange.of(0, Integer.MAX_VALUE);
	}

	@Override
	public boolean isDateBased()
	{
		return false;
	}

	@Override
	public boolean isTimeBased()
	{
		return true;
	}

	@Override
	public boolean isSupportedBy(TemporalAccessor temporal)
	{
		return temporal instanceof RSInstant && ((RSInstant) temporal).isSupported((TemporalUnit) this);
	}

	@Override
	public ValueRange rangeRefinedBy(TemporalAccessor temporal)
	{
		return this.range();
	}

	@Override
	public long getFrom(TemporalAccessor temporal)
	{
		return temporal.getLong(this);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R extends Temporal> R adjustInto(R temporal, long newValue)
	{
		if (temporal instanceof RSInstant && ((RSInstant) temporal).getUnit() == this)
		{
			return (R) temporal.with(this, newValue);
		}
		throw new UnsupportedTemporalTypeException(temporal.toString());
	}

	@Override
	public boolean isSupportedBy(Temporal temporal)
	{
		return (temporal instanceof RSInstant && ((RSInstant) temporal).getUnit() == this) ||
			temporal.isSupported(ChronoUnit.MILLIS);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R extends Temporal> R addTo(R temporal, long amount)
	{
		// if it's an RSInstant it must be the same unit, and we'll defer to the impl in RSInstant
		if (temporal instanceof RSInstant)
		{
			if (((RSInstant) temporal).getUnit() == this)
			{
				return (R) temporal.plus(amount, this);
			}

			// don't convert to millis when adding game ticks and client ticks,
			// otherwise the type constraint on R is violated by returning a java.time.Instant
			throw new UnsupportedTemporalTypeException(temporal.toString());
		}

		// otherwise if we can convert to millis we'll do that
		if (temporal.isSupported(ChronoUnit.MILLIS))
		{
			return (R) temporal.plus(wallDuration.multipliedBy(amount));
		}
		throw new UnsupportedTemporalTypeException(temporal.toString());
	}

	@Override
	public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive)
	{
		if (temporal1Inclusive instanceof RSInstant)
		{
			return temporal1Inclusive.until(temporal2Exclusive, this);
		}
		if (temporal1Inclusive.isSupported(ChronoUnit.MILLIS) && temporal2Exclusive.isSupported(ChronoUnit.MILLIS))
		{
			return temporal1Inclusive.until(temporal2Exclusive, ChronoUnit.MILLIS);
		}
		throw new UnsupportedTemporalTypeException(temporal1Inclusive + " / " + temporal2Exclusive);
	}

	@Override
	public String toString()
	{
		return name + "(" + wallDuration.toMillis() + "ms)";
	}
}
