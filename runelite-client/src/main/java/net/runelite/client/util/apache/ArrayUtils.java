/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.runelite.client.util.apache;

import java.lang.reflect.Array;

/**
 * <p>
 * This code has been adapted from Apache Commons Lang 3.3.
 * </p>
 */
public class ArrayUtils
{

	/**
	 * The index value when an element is not found in a list or array: {@code -1}.
	 * This value is returned by methods in this class and can also be used in comparisons with values returned by
	 * various method from {@link java.util.List}.
	 */
	public static final int INDEX_NOT_FOUND = -1;

	// Object IndexOf
	//-----------------------------------------------------------------------

	/**
	 * <p>Finds the index of the given object in the array.
	 *
	 * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
	 *
	 * @param array        the array to search through for the object, may be {@code null}
	 * @param objectToFind the object to find, may be {@code null}
	 * @return the index of the object within the array,
	 * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
	 */
	public static int indexOf(final Object[] array, final Object objectToFind)
	{
		return indexOf(array, objectToFind, 0);
	}

	/**
	 * <p>Finds the index of the given object in the array starting at the given index.
	 *
	 * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
	 *
	 * <p>A negative startIndex is treated as zero. A startIndex larger than the array
	 * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).
	 *
	 * @param array        the array to search through for the object, may be {@code null}
	 * @param objectToFind the object to find, may be {@code null}
	 * @param startIndex   the index to start searching at
	 * @return the index of the object within the array starting at the index,
	 * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
	 */
	public static int indexOf(final Object[] array, final Object objectToFind, int startIndex)
	{
		if (array == null)
		{
			return INDEX_NOT_FOUND;
		}
		if (startIndex < 0)
		{
			startIndex = 0;
		}
		if (objectToFind == null)
		{
			for (int i = startIndex; i < array.length; i++)
			{
				if (array[i] == null)
				{
					return i;
				}
			}
		}
		else
		{
			for (int i = startIndex; i < array.length; i++)
			{
				if (objectToFind.equals(array[i]))
				{
					return i;
				}
			}
		}
		return INDEX_NOT_FOUND;
	}

	/**
	 * <p>Checks if the object is in the given array.
	 *
	 * <p>The method returns {@code false} if a {@code null} array is passed in.
	 *
	 * @param array        the array to search through
	 * @param objectToFind the object to find
	 * @return {@code true} if the array contains the object
	 */
	public static boolean contains(final Object[] array, final Object objectToFind)
	{
		return indexOf(array, objectToFind) != INDEX_NOT_FOUND;
	}


	/**
	 * <p>Shallow clones an array returning a typecast result and handling
	 * {@code null}.
	 *
	 * <p>The objects in the array are not cloned, thus there is no special
	 * handling for multi-dimensional arrays.
	 *
	 * <p>This method returns {@code null} for a {@code null} input array.
	 *
	 * @param <T>   the component type of the array
	 * @param array the array to shallow clone, may be {@code null}
	 * @return the cloned array, {@code null} if {@code null} input
	 */
	public static <T> T[] clone(final T[] array)
	{
		if (array == null)
		{
			return null;
		}
		return array.clone();
	}

	/**
	 * <p>Inserts elements into an array at the given index (starting from zero).</p>
	 *
	 * <p>When an array is returned, it is always a new array.</p>
	 *
	 * <pre>
	 * ArrayUtils.insert(index, null, null)      = null
	 * ArrayUtils.insert(index, array, null)     = cloned copy of 'array'
	 * ArrayUtils.insert(index, null, values)    = null
	 * </pre>
	 *
	 * @param <T>    The type of elements in {@code array} and {@code values}
	 * @param index  the position within {@code array} to insert the new values
	 * @param array  the array to insert the values into, may be {@code null}
	 * @param values the new values to insert, may be {@code null}
	 * @return The new array.
	 * @throws IndexOutOfBoundsException if {@code array} is provided
	 *                                   and either {@code index < 0} or {@code index > array.length}
	 * @since 3.6
	 */
	@SafeVarargs
	public static <T> T[] insert(final int index, final T[] array, final T... values)
	{
		/*
		 * Note on use of @SafeVarargs:
		 *
		 * By returning null when 'array' is null, we avoid returning the vararg
		 * array to the caller. We also avoid relying on the type of the vararg
		 * array, by inspecting the component type of 'array'.
		 */

		if (array == null)
		{
			return null;
		}
		if (values == null || values.length == 0)
		{
			return clone(array);
		}
		if (index < 0 || index > array.length)
		{
			throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + array.length);
		}

		final Class<?> type = array.getClass().getComponentType();
		@SuppressWarnings("unchecked") // OK, because array and values are of type T
		final T[] result = (T[]) Array.newInstance(type, array.length + values.length);

		System.arraycopy(values, 0, result, index, values.length);
		if (index > 0)
		{
			System.arraycopy(array, 0, result, 0, index);
		}
		if (index < array.length)
		{
			System.arraycopy(array, index, result, index + values.length, array.length - index);
		}
		return result;
	}

	// int IndexOf
	//-----------------------------------------------------------------------

	/**
	 * <p>Finds the index of the given value in the array.
	 *
	 * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
	 *
	 * @param array       the array to search through for the object, may be {@code null}
	 * @param valueToFind the value to find
	 * @return the index of the value within the array,
	 * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
	 */
	public static int indexOf(final int[] array, final int valueToFind)
	{
		return indexOf(array, valueToFind, 0);
	}

	/**
	 * <p>Finds the index of the given value in the array starting at the given index.
	 *
	 * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
	 *
	 * <p>A negative startIndex is treated as zero. A startIndex larger than the array
	 * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).
	 *
	 * @param array       the array to search through for the object, may be {@code null}
	 * @param valueToFind the value to find
	 * @param startIndex  the index to start searching at
	 * @return the index of the value within the array,
	 * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
	 */
	public static int indexOf(final int[] array, final int valueToFind, int startIndex)
	{
		if (array == null)
		{
			return INDEX_NOT_FOUND;
		}
		if (startIndex < 0)
		{
			startIndex = 0;
		}
		for (int i = startIndex; i < array.length; i++)
		{
			if (valueToFind == array[i])
			{
				return i;
			}
		}
		return INDEX_NOT_FOUND;
	}

	/**
	 * <p>Checks if the value is in the given array.
	 *
	 * <p>The method returns {@code false} if a {@code null} array is passed in.
	 *
	 * @param array       the array to search through
	 * @param valueToFind the value to find
	 * @return {@code true} if the array contains the object
	 */
	public static boolean contains(final int[] array, final int valueToFind)
	{
		return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
	}

	/**
	 * <p>Returns the length of the specified array.
	 * This method can deal with {@code Object} arrays and with primitive arrays.
	 *
	 * <p>If the input array is {@code null}, {@code 0} is returned.
	 *
	 * <pre>
	 * ArrayUtils.getLength(null)            = 0
	 * ArrayUtils.getLength([])              = 0
	 * ArrayUtils.getLength([null])          = 1
	 * ArrayUtils.getLength([true, false])   = 2
	 * ArrayUtils.getLength([1, 2, 3])       = 3
	 * ArrayUtils.getLength(["a", "b", "c"]) = 3
	 * </pre>
	 *
	 * @param array the array to retrieve the length from, may be null
	 * @return The length of the array, or {@code 0} if the array is {@code null}
	 * @throws IllegalArgumentException if the object argument is not an array.
	 * @since 2.1
	 */
	public static int getLength(final Object array)
	{
		if (array == null)
		{
			return 0;
		}
		return Array.getLength(array);
	}

	/**
	 * <p>Checks if an array of Objects is empty or {@code null}.
	 *
	 * @param array the array to test
	 * @return {@code true} if the array is empty or {@code null}
	 * @since 2.1
	 */
	public static boolean isEmpty(final Object[] array)
	{
		return getLength(array) == 0;
	}

	/**
	 * <p>Adds all the elements of the given arrays into a new array.
	 * <p>The new array contains all of the element of {@code array1} followed
	 * by all of the elements {@code array2}. When an array is returned, it is always
	 * a new array.
	 *
	 * <pre>
	 * ArrayUtils.addAll(null, null)     = null
	 * ArrayUtils.addAll(array1, null)   = cloned copy of array1
	 * ArrayUtils.addAll(null, array2)   = cloned copy of array2
	 * ArrayUtils.addAll([], [])         = []
	 * ArrayUtils.addAll([null], [null]) = [null, null]
	 * ArrayUtils.addAll(["a", "b", "c"], ["1", "2", "3"]) = ["a", "b", "c", "1", "2", "3"]
	 * </pre>
	 *
	 * @param <T>    the component type of the array
	 * @param array1 the first array whose elements are added to the new array, may be {@code null}
	 * @param array2 the second array whose elements are added to the new array, may be {@code null}
	 * @return The new array, {@code null} if both arrays are {@code null}.
	 * The type of the new array is the type of the first array,
	 * unless the first array is null, in which case the type is the same as the second array.
	 * @throws IllegalArgumentException if the array types are incompatible
	 * @since 2.1
	 */
	public static <T> T[] addAll(final T[] array1, final T... array2)
	{
		if (array1 == null)
		{
			return clone(array2);
		}
		else if (array2 == null)
		{
			return clone(array1);
		}
		final Class<?> type1 = array1.getClass().getComponentType();
		@SuppressWarnings("unchecked") // OK, because array is of type T
		final T[] joinedArray = (T[]) Array.newInstance(type1, array1.length + array2.length);
		System.arraycopy(array1, 0, joinedArray, 0, array1.length);
		try
		{
			System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
		}
		catch (final ArrayStoreException ase)
		{
			// Check if problem was due to incompatible types
			/*
			 * We do this here, rather than before the copy because:
			 * - it would be a wasted check most of the time
			 * - safer, in case check turns out to be too strict
			 */
			final Class<?> type2 = array2.getClass().getComponentType();
			if (!type1.isAssignableFrom(type2))
			{
				throw new IllegalArgumentException("Cannot store " + type2.getName() + " in an array of " + type1.getName(), ase);
			}
			throw ase; // No, so rethrow original
		}
		return joinedArray;
	}

}
