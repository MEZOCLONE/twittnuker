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

package de.vanita5.twittnuker.preference;

import android.content.Context;
import android.util.AttributeSet;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;

public class AutoRefreshContentPreference extends MultiSelectListPreference implements Constants {

    public static final boolean DEFAULT_ENABLE_HOME_TIMELINE = false;
	public static final boolean DEFAULT_ENABLE_MENTIONS = true;
	public static final boolean DEFAULT_ENABLE_DIRECT_MESSAGES = true;
	public static final boolean DEFAULT_ENABLE_TRENDS = false;

	public AutoRefreshContentPreference(final Context context) {
		this(context, null);
	}

	public AutoRefreshContentPreference(final Context context, final AttributeSet attrs) {
		this(context, attrs, android.R.attr.preferenceStyle);
	}

	public AutoRefreshContentPreference(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected boolean[] getDefaults() {
        return new boolean[]{DEFAULT_ENABLE_HOME_TIMELINE, DEFAULT_ENABLE_MENTIONS, DEFAULT_ENABLE_DIRECT_MESSAGES,
				DEFAULT_ENABLE_TRENDS };
	}

	@Override
	protected String[] getKeys() {
		return new String[] { KEY_AUTO_REFRESH_HOME_TIMELINE, KEY_AUTO_REFRESH_MENTIONS,
				KEY_AUTO_REFRESH_DIRECT_MESSAGES, KEY_AUTO_REFRESH_TRENDS };
	}

	@Override
	protected String[] getNames() {
		return getContext().getResources().getStringArray(R.array.entries_auto_refresh_content);
	}

}