/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.util;

import android.support.v4.util.LongSparseArray;

public class LongSparseArrayUtils {
	/**
	 * @return A copy of all keys contained in the sparse array.
	 */
	public static <E> long[] getKeys(final LongSparseArray<E> array) {
		final int length = array.size();
		final long[] result = new long[length];
		for (int i = 0, j = length; i < j; i++) {
			result[i] = array.keyAt(i);
		}
		return result;
	}

	public static <E> boolean hasKey(final LongSparseArray<E> array, final long key) {
		return array.indexOfKey(key) >= 0;
	}

	/**
	 * Sets all supplied keys to the given unique value.
	 * 
	 * @param keys Keys to set
	 * @param uniqueValue Value to set all supplied keys to
	 */
	public static <E> void setValues(final LongSparseArray<E> array, final long[] keys, final E uniqueValue) {
		final int length = keys.length;
        for (long key : keys) {
            array.put(key, uniqueValue);
		}
	}
}