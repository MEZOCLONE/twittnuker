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

package de.vanita5.twittnuker.view.themed;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.widget.MultiAutoCompleteTextView;

import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.ViewUtils;
import de.vanita5.twittnuker.view.iface.IThemedView;

public class ThemedMultiAutoCompleteTextView extends MultiAutoCompleteTextView  implements IThemedView {

    public ThemedMultiAutoCompleteTextView(final Context context) {
        this(context, null);
    }

    public ThemedMultiAutoCompleteTextView(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle);
    }

    public ThemedMultiAutoCompleteTextView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        ThemeUtils.initTextView(this);
    }

    @Override
    public void setThemeTintColor(ColorStateList color) {
        ViewCompat.setBackgroundTintList(this, color);
        setLinkTextColor(color);
        setHighlightColor(color.getDefaultColor());
    }
}