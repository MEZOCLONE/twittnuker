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

package de.vanita5.twittnuker.view;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import de.vanita5.twittnuker.view.iface.IColorLabelView;

public class ColorLabelRelativeLayout extends RelativeLayout implements IColorLabelView {

	private final Helper mHelper;

	public ColorLabelRelativeLayout(final Context context) {
		this(context, null);
	}

	public ColorLabelRelativeLayout(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ColorLabelRelativeLayout(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		mHelper = new Helper(this, context, attrs, defStyle);
	}

	@Override
	public void drawBackground(final int color) {
		mHelper.drawBackground(color);
	}

	@Override
	public void drawBottom(final int... colors) {
		mHelper.drawBottom(colors);
	}

	@Override
	public void drawEnd(final int... colors) {
		mHelper.drawEnd(colors);
	}

	@Override
	public void drawLabel(final int[] start, final int[] end, final int[] top, final int[] bottom, final int background) {
		mHelper.drawLabel(start, end, top, bottom, background);
	}

	@Override
	public void drawStart(final int... colors) {
		mHelper.drawStart(colors);
	}

	@Override
	public void drawTop(final int... colors) {
		mHelper.drawTop(colors);
	}

	@Override
    public boolean isPaddingIgnored() {
        return mHelper.isPaddingIgnored();
	}

	@Override
    public void setIgnorePadding(final boolean ignorePadding) {
        mHelper.setIgnorePadding(ignorePadding);
	}

	@Override
    protected void dispatchDraw(@NonNull final Canvas canvas) {
		mHelper.dispatchDrawBackground(canvas);
		super.dispatchDraw(canvas);
		mHelper.dispatchDrawLabels(canvas);
	}

}