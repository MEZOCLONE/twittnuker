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
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import de.vanita5.twittnuker.util.ThemeUtils;

public class RightDrawerFrameLayout extends FrameLayout {

	private final Paint mClipPaint = new Paint();
	private float mScrollScale, mPercentOpen;
	private boolean mClipEnabled;

	public RightDrawerFrameLayout(final Context context) {
		this(context, null);
	}

	public RightDrawerFrameLayout(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public RightDrawerFrameLayout(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
        ThemeUtils.setupDrawerBackground(context, this);
		setWillNotDraw(false);
		mClipPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
	}

	@Override
	public boolean hasOverlappingRendering() {
		return mClipEnabled;
	}

	public void setClipEnabled(final boolean clipEnabled) {
		mClipEnabled = clipEnabled;
		if (!clipEnabled) {
			setAlpha(1);
		}
	}

	public void setPercentOpen(final float percentOpen) {
		if (mPercentOpen == percentOpen) return;
		mPercentOpen = percentOpen;
		if (mClipEnabled) {
			setAlpha(1 - (1 - mPercentOpen) * (1.0f / 0xff));
			invalidate();
		}
	}

	public void setScrollScale(final float scrollScale) {
		mScrollScale = scrollScale;
	}

	@Override
    protected void dispatchDraw(@NonNull final Canvas canvas) {
		super.dispatchDraw(canvas);
		if (mClipEnabled && mPercentOpen > 0 && mPercentOpen < 1) {
			final int right = Math.round(getWidth() * (1 - mPercentOpen) * (1 - mScrollScale));
			canvas.drawRect(getLeft(), getTop(), right, getBottom(), mClipPaint);
		}
	}
}