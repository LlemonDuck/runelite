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

import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
public class RSInstant implements Temporal, Comparable<Temporal>
{

	// used for isSupportedBy and similar, the actual timestamp used doesn't matter
	private static final Instant JAVA_TIME_INSTANT = Instant.now();

	private final int partition;
	private final int tick;
	private final RSTimeUnit unit;

	@Override
	public boolean isSupported(TemporalUnit unit)
	{
		return unit == this.unit || unit.isSupportedBy(JAVA_TIME_INSTANT);
	}

	@Override
	public RSInstant with(TemporalField field, long newValue)
	{
		if (field == unit)
		{
			return new RSInstant(this.partition, (int) newValue, unit);
		}
		throw new UnsupportedTemporalTypeException(field.toString());
	}

	@Override
	public Temporal plus(long amountToAdd, TemporalUnit unit)
	{
		if (unit == this.unit)
		{
			return new RSInstant(this.partition, (int) (this.tick + amountToAdd), this.unit);
		}
		else if (JAVA_TIME_INSTANT.isSupported(unit))
		{
			return toWallInstant().plus(amountToAdd, unit);
		}
		throw new UnsupportedTemporalTypeException(unit.toString());
	}

	@Override
	public Temporal minus(long amountToAdd, TemporalUnit unit)
	{
		return plus(-amountToAdd, unit);
	}

	@Override
	public long until(Temporal endExclusive, TemporalUnit unit)
	{
		if (unit instanceof RSTimeUnit && endExclusive instanceof RSInstant)
		{
			if (unit != this.unit)
			{
				throw new UnsupportedTemporalTypeException(unit.toString());
			}
			if (unit != ((RSInstant) endExclusive).unit)
			{
				throw new UnsupportedTemporalTypeException("Cannot compare different tick types");
			}
			if (this.partition != ((RSInstant) endExclusive).partition)
			{
				throw new IllegalArgumentException("Cannot compare two RSInstants from different time partitions (server connection instances)");
			}
			return endExclusive.get((RSTimeUnit) unit) - this.tick;
		}
		if (JAVA_TIME_INSTANT.isSupported(unit) && endExclusive.isSupported(unit))
		{
			return toWallInstant().until(endExclusive, unit);
		}
		throw new UnsupportedTemporalTypeException(unit.toString());
	}

	@Override
	public boolean isSupported(TemporalField field)
	{
		if (field instanceof RSTimeUnit)
		{
			return field == unit;
		}
		return JAVA_TIME_INSTANT.isSupported(field);
	}

	@Override
	public long getLong(TemporalField field)
	{
		if (field == this.unit)
		{
			return tick;
		}
		if (JAVA_TIME_INSTANT.isSupported(field))
		{
			return toWallInstant().getLong(field);
		}
		throw new UnsupportedTemporalTypeException(field.toString());
	}

	public Instant toWallInstant()
	{
		TickTimestamp reference = RSClock.getTime(unit);
		int tickDelta = tick - reference.getTick();
		return Instant.ofEpochMilli(reference.getWallMs()).plus(new RSDuration(tickDelta, unit));
	}

	@Override
	public String toString()
	{
		return "RSInstant(p" + partition + ", " + unit.getName() + " " + tick + ")";
	}

	@Override
	public int compareTo(@NotNull Temporal o)
	{
		if (o instanceof RSInstant)
		{
			return (int) this.until(o, this.getUnit());
		}
		if (o instanceof Instant)
		{
			return toWallInstant().compareTo((Instant) o);
		}
		throw new UnsupportedTemporalTypeException(o.toString());
	}

	public boolean isAfter(Temporal other)
	{
		return compareTo(other) > 0;
	}

	public boolean isBefore(Instant other)
	{
		return compareTo(other) < 0;
	}

	public static RSInstant gameNow()
	{
		return of(RSTimeUnit.GAME_TICKS);
	}

	public static RSInstant clientNow()
	{
		return of(RSTimeUnit.CLIENT_TICKS);
	}

	private static RSInstant of(RSTimeUnit unit)
	{
		TickTimestamp ts = RSClock.getTime(unit);
		return new RSInstant(ts.getPartitionId(), ts.getTick(), unit);
	}
}
