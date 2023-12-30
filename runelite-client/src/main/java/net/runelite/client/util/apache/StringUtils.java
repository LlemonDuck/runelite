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

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * <p>Operations on {@link java.lang.String} that are
 * {@code null} safe.</p>
 *
 * <ul>
 *  <li><b>IsEmpty/IsBlank</b>
 *      - checks if a String contains text</li>
 *  <li><b>Trim/Strip</b>
 *      - removes leading and trailing whitespace</li>
 *  <li><b>Equals/Compare</b>
 *      - compares two strings null-safe</li>
 *  <li><b>startsWith</b>
 *      - check if a String starts with a prefix null-safe</li>
 *  <li><b>endsWith</b>
 *      - check if a String ends with a suffix null-safe</li>
 *  <li><b>IndexOf/LastIndexOf/Contains</b>
 *      - null-safe index-of checks
 *  <li><b>IndexOfAny/LastIndexOfAny/IndexOfAnyBut/LastIndexOfAnyBut</b>
 *      - index-of any of a set of Strings</li>
 *  <li><b>ContainsOnly/ContainsNone/ContainsAny</b>
 *      - does String contains only/none/any of these characters</li>
 *  <li><b>Substring/Left/Right/Mid</b>
 *      - null-safe substring extractions</li>
 *  <li><b>SubstringBefore/SubstringAfter/SubstringBetween</b>
 *      - substring extraction relative to other strings</li>
 *  <li><b>Split/Join</b>
 *      - splits a String into an array of substrings and vice versa</li>
 *  <li><b>Remove/Delete</b>
 *      - removes part of a String</li>
 *  <li><b>Replace/Overlay</b>
 *      - Searches a String and replaces one String with another</li>
 *  <li><b>Chomp/Chop</b>
 *      - removes the last part of a String</li>
 *  <li><b>AppendIfMissing</b>
 *      - appends a suffix to the end of the String if not present</li>
 *  <li><b>PrependIfMissing</b>
 *      - prepends a prefix to the start of the String if not present</li>
 *  <li><b>LeftPad/RightPad/Center/Repeat</b>
 *      - pads a String</li>
 *  <li><b>UpperCase/LowerCase/SwapCase/Capitalize/Uncapitalize</b>
 *      - changes the case of a String</li>
 *  <li><b>CountMatches</b>
 *      - counts the number of occurrences of one String in another</li>
 *  <li><b>IsAlpha/IsNumeric/IsWhitespace/IsAsciiPrintable</b>
 *      - checks the characters in a String</li>
 *  <li><b>DefaultString</b>
 *      - protects against a null input String</li>
 *  <li><b>Rotate</b>
 *      - rotate (circular shift) a String</li>
 *  <li><b>Reverse/ReverseDelimited</b>
 *      - reverses a String</li>
 *  <li><b>Abbreviate</b>
 *      - abbreviates a string using ellipsis or another given String</li>
 *  <li><b>Difference</b>
 *      - compares Strings and reports on their differences</li>
 *  <li><b>LevenshteinDistance</b>
 *      - the number of changes needed to change one String into another</li>
 * </ul>
 *
 * <p>The {@code StringUtils} class defines certain words related to
 * String handling.</p>
 *
 * <ul>
 *  <li>null - {@code null}</li>
 *  <li>empty - a zero-length string ({@code ""})</li>
 *  <li>space - the space character ({@code ' '}, char 32)</li>
 *  <li>whitespace - the characters defined by {@link Character#isWhitespace(char)}</li>
 *  <li>trim - the characters &lt;= 32 as in {@link String#trim()}</li>
 * </ul>
 *
 * <p>{@code StringUtils} handles {@code null} input Strings quietly.
 * That is to say that a {@code null} input will return {@code null}.
 * Where a {@code boolean} or {@code int} is being returned
 * details vary by method.</p>
 *
 * <p>A side effect of the {@code null} handling is that a
 * {@code NullPointerException} should be considered a bug in
 * {@code StringUtils}.</p>
 *
 * <p>Methods in this class give sample code to explain their operation.
 * The symbol {@code *} is used to indicate any input including {@code null}.</p>
 *
 * <p>#ThreadSafe#</p>
 *
 * <p>
 * This code has been adapted from Apache Commons Lang 3.3.
 * </p>
 *
 * @see java.lang.String
 * @since 1.0
 */
//@Immutable
public class StringUtils
{

	/**
	 * A String for a space character.
	 *
	 * @since 3.2
	 */
	public static final String SPACE = " ";

	/**
	 * The empty String {@code ""}.
	 *
	 * @since 2.0
	 */
	public static final String EMPTY = "";

	/**
	 * <p>The maximum size to which the padding constant(s) can expand.</p>
	 */
	private static final int PAD_LIMIT = 8192;

	/**
	 * Represents a failed index search.
	 *
	 * @since 2.1
	 */
	public static final int INDEX_NOT_FOUND = -1;

	/**
	 * <p>Returns padding using the specified delimiter repeated
	 * to a given length.</p>
	 *
	 * <pre>
	 * StringUtils.repeat('e', 0)  = ""
	 * StringUtils.repeat('e', 3)  = "eee"
	 * StringUtils.repeat('e', -2) = ""
	 * </pre>
	 *
	 * <p>Note: this method does not support padding with
	 * <a href="http://www.unicode.org/glossary/#supplementary_character">Unicode Supplementary Characters</a>
	 * as they require a pair of {@code char}s to be represented.
	 * </p>
	 *
	 * @param ch     character to repeat
	 * @param repeat number of times to repeat char, negative treated as zero
	 * @return String with repeated character
	 */
	public static String repeat(final char ch, final int repeat)
	{
		if (repeat <= 0)
		{
			return EMPTY;
		}
		final char[] buf = new char[repeat];
		for (int i = repeat - 1; i >= 0; i--)
		{
			buf[i] = ch;
		}
		return new String(buf);
	}

	/**
	 * <p>Repeat a String {@code repeat} times to form a
	 * new String.</p>
	 *
	 * <pre>
	 * StringUtils.repeat(null, 2) = null
	 * StringUtils.repeat("", 0)   = ""
	 * StringUtils.repeat("", 2)   = ""
	 * StringUtils.repeat("a", 3)  = "aaa"
	 * StringUtils.repeat("ab", 2) = "abab"
	 * StringUtils.repeat("a", -2) = ""
	 * </pre>
	 *
	 * @param str    the String to repeat, may be null
	 * @param repeat number of times to repeat str, negative treated as zero
	 * @return a new String consisting of the original String repeated,
	 * {@code null} if null String input
	 */
	public static String repeat(final String str, final int repeat)
	{
		// Performance tuned for 2.0 (JDK1.4)

		if (str == null)
		{
			return null;
		}
		if (repeat <= 0)
		{
			return EMPTY;
		}
		final int inputLength = str.length();
		if (repeat == 1 || inputLength == 0)
		{
			return str;
		}
		if (inputLength == 1 && repeat <= PAD_LIMIT)
		{
			return repeat(str.charAt(0), repeat);
		}

		final int outputLength = inputLength * repeat;
		switch (inputLength)
		{
			case 1:
				return repeat(str.charAt(0), repeat);
			case 2:
				final char ch0 = str.charAt(0);
				final char ch1 = str.charAt(1);
				final char[] output2 = new char[outputLength];
				for (int i = repeat * 2 - 2; i >= 0; i--, i--)
				{
					output2[i] = ch0;
					output2[i + 1] = ch1;
				}
				return new String(output2);
			default:
				final StringBuilder buf = new StringBuilder(outputLength);
				for (int i = 0; i < repeat; i++)
				{
					buf.append(str);
				}
				return buf.toString();
		}
	}

	/**
	 * <p>Removes diacritics (~= accents) from a string. The case will not be altered.</p>
	 * <p>For instance, '&agrave;' will be replaced by 'a'.</p>
	 * <p>Note that ligatures will be left as is.</p>
	 *
	 * <pre>
	 * StringUtils.stripAccents(null)                = null
	 * StringUtils.stripAccents("")                  = ""
	 * StringUtils.stripAccents("control")           = "control"
	 * StringUtils.stripAccents("&eacute;clair")     = "eclair"
	 * </pre>
	 *
	 * @param input String to be stripped
	 * @return input text with diacritics removed
	 * @since 3.0
	 */
	// See also Lucene's ASCIIFoldingFilter (Lucene 2.9) that replaces accented characters by their unaccented equivalent (and uncommitted bug fix: https://issues.apache.org/jira/browse/LUCENE-1343?focusedCommentId=12858907&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#action_12858907).
	public static String stripAccents(final String input)
	{
		if (input == null)
		{
			return null;
		}
		final Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");//$NON-NLS-1$
		final StringBuilder decomposed = new StringBuilder(Normalizer.normalize(input, Normalizer.Form.NFD));
		convertRemainingAccentCharacters(decomposed);
		// Note that this doesn't correctly remove ligatures...
		return pattern.matcher(decomposed).replaceAll(EMPTY);
	}

	private static void convertRemainingAccentCharacters(final StringBuilder decomposed)
	{
		for (int i = 0; i < decomposed.length(); i++)
		{
			if (decomposed.charAt(i) == '\u0141')
			{
				decomposed.deleteCharAt(i);
				decomposed.insert(i, 'L');
			}
			else if (decomposed.charAt(i) == '\u0142')
			{
				decomposed.deleteCharAt(i);
				decomposed.insert(i, 'l');
			}
		}
	}

	/**
	 * <p>Case insensitive check if a CharSequence starts with a specified prefix.</p>
	 *
	 * <p>{@code null}s are handled without exceptions. Two {@code null}
	 * references are considered to be equal. The comparison is case insensitive.</p>
	 *
	 * <pre>
	 * StringUtils.startsWithIgnoreCase(null, null)      = true
	 * StringUtils.startsWithIgnoreCase(null, "abc")     = false
	 * StringUtils.startsWithIgnoreCase("abcdef", null)  = false
	 * StringUtils.startsWithIgnoreCase("abcdef", "abc") = true
	 * StringUtils.startsWithIgnoreCase("ABCDEF", "abc") = true
	 * </pre>
	 *
	 * @param str    the CharSequence to check, may be null
	 * @param prefix the prefix to find, may be null
	 * @return {@code true} if the CharSequence starts with the prefix, case insensitive, or
	 * both {@code null}
	 * @see java.lang.String#startsWith(String)
	 * @since 2.4
	 * @since 3.0 Changed signature from startsWithIgnoreCase(String, String) to startsWithIgnoreCase(CharSequence, CharSequence)
	 */
	public static boolean startsWithIgnoreCase(final CharSequence str, final CharSequence prefix)
	{
		return startsWith(str, prefix, true);
	}

	/**
	 * <p>Check if a CharSequence starts with a specified prefix (optionally case insensitive).</p>
	 *
	 * @param str        the CharSequence to check, may be null
	 * @param prefix     the prefix to find, may be null
	 * @param ignoreCase indicates whether the compare should ignore case
	 *                   (case insensitive) or not.
	 * @return {@code true} if the CharSequence starts with the prefix or
	 * both {@code null}
	 * @see java.lang.String#startsWith(String)
	 */
	private static boolean startsWith(final CharSequence str, final CharSequence prefix, final boolean ignoreCase)
	{
		if (str == null || prefix == null)
		{
			return str == null && prefix == null;
		}
		if (prefix.length() > str.length())
		{
			return false;
		}
		return regionMatches(str, ignoreCase, 0, prefix, 0, prefix.length());
	}

	/**
	 * Green implementation of regionMatches.
	 *
	 * @param cs         the {@code CharSequence} to be processed
	 * @param ignoreCase whether or not to be case insensitive
	 * @param thisStart  the index to start on the {@code cs} CharSequence
	 * @param substring  the {@code CharSequence} to be looked for
	 * @param start      the index to start on the {@code substring} CharSequence
	 * @param length     character length of the region
	 * @return whether the region matched
	 */
	static boolean regionMatches(final CharSequence cs, final boolean ignoreCase, final int thisStart, final CharSequence substring, final int start, final int length)
	{
		if (cs instanceof String && substring instanceof String)
		{
			return ((String) cs).regionMatches(ignoreCase, thisStart, (String) substring, start, length);
		}
		int index1 = thisStart;
		int index2 = start;
		int tmpLen = length;

		// Extract these first so we detect NPEs the same as the java.lang.String version
		final int srcLen = cs.length() - thisStart;
		final int otherLen = substring.length() - start;

		// Check for invalid parameters
		if (thisStart < 0 || start < 0 || length < 0)
		{
			return false;
		}

		// Check that the regions are long enough
		if (srcLen < length || otherLen < length)
		{
			return false;
		}

		while (tmpLen-- > 0)
		{
			final char c1 = cs.charAt(index1++);
			final char c2 = substring.charAt(index2++);

			if (c1 == c2)
			{
				continue;
			}

			if (!ignoreCase)
			{
				return false;
			}

			// The same check as in String.regionMatches():
			if (Character.toUpperCase(c1) != Character.toUpperCase(c2) && Character.toLowerCase(c1) != Character.toLowerCase(c2))
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * <p>Left pad a String with spaces (' ').</p>
	 *
	 * <p>The String is padded to the size of {@code size}.</p>
	 *
	 * <pre>
	 * StringUtils.leftPad(null, *)   = null
	 * StringUtils.leftPad("", 3)     = "   "
	 * StringUtils.leftPad("bat", 3)  = "bat"
	 * StringUtils.leftPad("bat", 5)  = "  bat"
	 * StringUtils.leftPad("bat", 1)  = "bat"
	 * StringUtils.leftPad("bat", -1) = "bat"
	 * </pre>
	 *
	 * @param str  the String to pad out, may be null
	 * @param size the size to pad to
	 * @return left padded String or original String if no padding is necessary,
	 * {@code null} if null String input
	 */
	public static String leftPad(final String str, final int size)
	{
		return leftPad(str, size, ' ');
	}

	/**
	 * <p>Left pad a String with a specified character.</p>
	 *
	 * <p>Pad to a size of {@code size}.</p>
	 *
	 * <pre>
	 * StringUtils.leftPad(null, *, *)     = null
	 * StringUtils.leftPad("", 3, 'z')     = "zzz"
	 * StringUtils.leftPad("bat", 3, 'z')  = "bat"
	 * StringUtils.leftPad("bat", 5, 'z')  = "zzbat"
	 * StringUtils.leftPad("bat", 1, 'z')  = "bat"
	 * StringUtils.leftPad("bat", -1, 'z') = "bat"
	 * </pre>
	 *
	 * @param str     the String to pad out, may be null
	 * @param size    the size to pad to
	 * @param padChar the character to pad with
	 * @return left padded String or original String if no padding is necessary,
	 * {@code null} if null String input
	 * @since 2.0
	 */
	public static String leftPad(final String str, final int size, final char padChar)
	{
		if (str == null)
		{
			return null;
		}
		final int pads = size - str.length();
		if (pads <= 0)
		{
			return str; // returns original String when possible
		}
		if (pads > PAD_LIMIT)
		{
			return leftPad(str, size, String.valueOf(padChar));
		}
		return repeat(padChar, pads).concat(str);
	}

	/**
	 * <p>Left pad a String with a specified String.</p>
	 *
	 * <p>Pad to a size of {@code size}.</p>
	 *
	 * <pre>
	 * StringUtils.leftPad(null, *, *)      = null
	 * StringUtils.leftPad("", 3, "z")      = "zzz"
	 * StringUtils.leftPad("bat", 3, "yz")  = "bat"
	 * StringUtils.leftPad("bat", 5, "yz")  = "yzbat"
	 * StringUtils.leftPad("bat", 8, "yz")  = "yzyzybat"
	 * StringUtils.leftPad("bat", 1, "yz")  = "bat"
	 * StringUtils.leftPad("bat", -1, "yz") = "bat"
	 * StringUtils.leftPad("bat", 5, null)  = "  bat"
	 * StringUtils.leftPad("bat", 5, "")    = "  bat"
	 * </pre>
	 *
	 * @param str    the String to pad out, may be null
	 * @param size   the size to pad to
	 * @param padStr the String to pad with, null or empty treated as single space
	 * @return left padded String or original String if no padding is necessary,
	 * {@code null} if null String input
	 */
	public static String leftPad(final String str, final int size, String padStr)
	{
		if (str == null)
		{
			return null;
		}
		if (padStr.isEmpty())
		{
			padStr = SPACE;
		}
		final int padLen = padStr.length();
		final int strLen = str.length();
		final int pads = size - strLen;
		if (pads <= 0)
		{
			return str; // returns original String when possible
		}
		if (padLen == 1 && pads <= PAD_LIMIT)
		{
			return leftPad(str, size, padStr.charAt(0));
		}

		if (pads == padLen)
		{
			return padStr.concat(str);
		}
		else if (pads < padLen)
		{
			return padStr.substring(0, pads).concat(str);
		}
		else
		{
			final char[] padding = new char[pads];
			final char[] padChars = padStr.toCharArray();
			for (int i = 0; i < pads; i++)
			{
				padding[i] = padChars[i % padLen];
			}
			return new String(padding).concat(str);
		}
	}

	/**
	 * <p>Capitalizes a String changing the first character to title case as
	 * per {@link Character#toTitleCase(int)}. No other characters are changed.</p>
	 *
	 * <p>For a word based algorithm, see {@link WordUtils#capitalize(String)}.
	 * A {@code null} input String returns {@code null}.</p>
	 *
	 * <pre>
	 * StringUtils.capitalize(null)  = null
	 * StringUtils.capitalize("")    = ""
	 * StringUtils.capitalize("cat") = "Cat"
	 * StringUtils.capitalize("cAt") = "CAt"
	 * StringUtils.capitalize("'cat'") = "'cat'"
	 * </pre>
	 *
	 * @param str the String to capitalize, may be null
	 * @return the capitalized String, {@code null} if null String input
	 * @see WordUtils#capitalize(String)
	 * @since 2.0
	 */
	public static String capitalize(final String str)
	{
		int strLen;
		if (str == null || (strLen = str.length()) == 0)
		{
			return str;
		}

		final int firstCodepoint = str.codePointAt(0);
		final int newCodePoint = Character.toTitleCase(firstCodepoint);
		if (firstCodepoint == newCodePoint)
		{
			// already capitalized
			return str;
		}

		final int newCodePoints[] = new int[strLen]; // cannot be longer than the char array
		int outOffset = 0;
		newCodePoints[outOffset++] = newCodePoint; // copy the first codepoint
		for (int inOffset = Character.charCount(firstCodepoint); inOffset < strLen; )
		{
			final int codepoint = str.codePointAt(inOffset);
			newCodePoints[outOffset++] = codepoint; // copy the remaining ones
			inOffset += Character.charCount(codepoint);
		}
		return new String(newCodePoints, 0, outOffset);
	}

	/**
	 * <p>Replaces a String with another String inside a larger String,
	 * for the first {@code max} values of the search String.</p>
	 *
	 * <p>A {@code null} reference passed to this method is a no-op.</p>
	 *
	 * <pre>
	 * StringUtils.replace(null, *, *, *)         = null
	 * StringUtils.replace("", *, *, *)           = ""
	 * StringUtils.replace("any", null, *, *)     = "any"
	 * StringUtils.replace("any", *, null, *)     = "any"
	 * StringUtils.replace("any", "", *, *)       = "any"
	 * StringUtils.replace("any", *, *, 0)        = "any"
	 * StringUtils.replace("abaa", "a", null, -1) = "abaa"
	 * StringUtils.replace("abaa", "a", "", -1)   = "b"
	 * StringUtils.replace("abaa", "a", "z", 0)   = "abaa"
	 * StringUtils.replace("abaa", "a", "z", 1)   = "zbaa"
	 * StringUtils.replace("abaa", "a", "z", 2)   = "zbza"
	 * StringUtils.replace("abaa", "a", "z", -1)  = "zbzz"
	 * </pre>
	 *
	 * @param text         text to search and replace in, may be null
	 * @param searchString the String to search for, may be null
	 * @param replacement  the String to replace it with, may be null
	 * @param max          maximum number of values to replace, or {@code -1} if no maximum
	 * @return the text with any replacements processed,
	 * {@code null} if null String input
	 */
	public static String replace(final String text, final String searchString, final String replacement, final int max)
	{
		return replace(text, searchString, replacement, max, false);
	}

	/**
	 * <p>Replaces a String with another String inside a larger String,
	 * for the first {@code max} values of the search String,
	 * case sensitively/insensisitively based on {@code ignoreCase} value.</p>
	 *
	 * <p>A {@code null} reference passed to this method is a no-op.</p>
	 *
	 * <pre>
	 * StringUtils.replace(null, *, *, *, false)         = null
	 * StringUtils.replace("", *, *, *, false)           = ""
	 * StringUtils.replace("any", null, *, *, false)     = "any"
	 * StringUtils.replace("any", *, null, *, false)     = "any"
	 * StringUtils.replace("any", "", *, *, false)       = "any"
	 * StringUtils.replace("any", *, *, 0, false)        = "any"
	 * StringUtils.replace("abaa", "a", null, -1, false) = "abaa"
	 * StringUtils.replace("abaa", "a", "", -1, false)   = "b"
	 * StringUtils.replace("abaa", "a", "z", 0, false)   = "abaa"
	 * StringUtils.replace("abaa", "A", "z", 1, false)   = "abaa"
	 * StringUtils.replace("abaa", "A", "z", 1, true)   = "zbaa"
	 * StringUtils.replace("abAa", "a", "z", 2, true)   = "zbza"
	 * StringUtils.replace("abAa", "a", "z", -1, true)  = "zbzz"
	 * </pre>
	 *
	 * @param text         text to search and replace in, may be null
	 * @param searchString the String to search for (case insensitive), may be null
	 * @param replacement  the String to replace it with, may be null
	 * @param max          maximum number of values to replace, or {@code -1} if no maximum
	 * @param ignoreCase   if true replace is case insensitive, otherwise case sensitive
	 * @return the text with any replacements processed,
	 * {@code null} if null String input
	 */
	private static String replace(final String text, String searchString, final String replacement, int max, final boolean ignoreCase)
	{
		if (isEmpty(text) || isEmpty(searchString) || replacement == null || max == 0)
		{
			return text;
		}
		String searchText = text;
		if (ignoreCase)
		{
			searchText = text.toLowerCase();
			searchString = searchString.toLowerCase();
		}
		int start = 0;
		int end = searchText.indexOf(searchString, start);
		if (end == INDEX_NOT_FOUND)
		{
			return text;
		}
		final int replLength = searchString.length();
		int increase = replacement.length() - replLength;
		increase = increase < 0 ? 0 : increase;
		increase *= max < 0 ? 16 : max > 64 ? 64 : max;
		final StringBuilder buf = new StringBuilder(text.length() + increase);
		while (end != INDEX_NOT_FOUND)
		{
			buf.append(text, start, end).append(replacement);
			start = end + replLength;
			if (--max == 0)
			{
				break;
			}
			end = searchText.indexOf(searchString, start);
		}
		buf.append(text, start, text.length());
		return buf.toString();
	}

	/**
	 * <p>Checks if a CharSequence is empty ("") or null.</p>
	 *
	 * <pre>
	 * StringUtils.isEmpty(null)      = true
	 * StringUtils.isEmpty("")        = true
	 * StringUtils.isEmpty(" ")       = false
	 * StringUtils.isEmpty("bob")     = false
	 * StringUtils.isEmpty("  bob  ") = false
	 * </pre>
	 *
	 * <p>NOTE: This method changed in Lang version 2.0.
	 * It no longer trims the CharSequence.
	 * That functionality is available in isBlank().</p>
	 *
	 * @param cs the CharSequence to check, may be null
	 * @return {@code true} if the CharSequence is empty or null
	 * @since 3.0 Changed signature from isEmpty(String) to isEmpty(CharSequence)
	 */
	public static boolean isEmpty(final CharSequence cs)
	{
		return cs == null || cs.length() == 0;
	}

}
