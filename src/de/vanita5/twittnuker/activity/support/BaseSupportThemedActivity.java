/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2014 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.activity.support;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.activity.iface.IThemedActivity;
import de.vanita5.twittnuker.util.StrictModeUtils;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.Utils;

import static de.vanita5.twittnuker.util.Utils.restartActivity;

public abstract class BaseSupportThemedActivity extends FragmentActivity implements Constants, IThemedActivity {

	private int mCurrentThemeResource, mCurrentThemeColor,
			mCurrentThemeBackgroundAlpha, mCurrentActionBarColor;

	@Override
	public void finish() {
		super.finish();
		overrideCloseAnimationIfNeeded();
	}

	@Override
	public Resources getDefaultResources() {
		return super.getResources();
	}

    @Override
    public final int getCurrentThemeResourceId() {
        return mCurrentThemeResource;
	}

    @Override
    public int getThemeBackgroundAlpha() {
        return ThemeUtils.isTransparentBackground(this) ? ThemeUtils.getUserThemeBackgroundAlpha(this) : 0xff;
    }

    @Override
	public String getThemeFontFamily() {
		return ThemeUtils.getThemeFontFamily(this);
	}

	@Override
	public void navigateUpFromSameTask() {
		NavUtils.navigateUpFromSameTask(this);
		overrideCloseAnimationIfNeeded();
	}

	@Override
	public void overrideCloseAnimationIfNeeded() {
		if (shouldOverrideActivityAnimation()) {
			ThemeUtils.overrideActivityCloseAnimation(this);
		} else {
			ThemeUtils.overrideNormalActivityCloseAnimation(this);
		}
    }


	@Override
	public final void restart() {
		restartActivity(this);
	}

	@Override
	public boolean shouldOverrideActivityAnimation() {
		return true;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		if (Utils.isDebugBuild()) {
			StrictModeUtils.detectAllVmPolicy();
			StrictModeUtils.detectAllThreadPolicy();
		}
		if (shouldOverrideActivityAnimation()) {
			ThemeUtils.overrideActivityOpenAnimation(this);
		}
		setTheme();
		super.onCreate(savedInstanceState);
		setActionBarBackground();
	}

	@Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        final SpannableStringBuilder builder = new SpannableStringBuilder(title);
        super.onTitleChanged(title, color);
        final int themeResId = getCurrentThemeResourceId();
        final int themeColor = getThemeColor(), contrastColor = Utils.getContrastYIQ(themeColor, 192);
        if (!ThemeUtils.isDarkTheme(themeResId)) {
            builder.setSpan(new ForegroundColorSpan(contrastColor), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @Override
	protected void onResume() {
		super.onResume();
		}

    protected boolean shouldSetWindowBackground() {
        return true;
    }

	private final void setActionBarBackground() {
        ThemeUtils.applyActionBarBackground(getActionBar(), this, mCurrentThemeResource);
	}

	public int getActionBarColor() {
		return ThemeUtils.getActionBarColor(this);
	}

	private final void setTheme() {
		mCurrentThemeResource = getThemeResourceId();
		mCurrentThemeColor = getThemeColor();
		mCurrentActionBarColor = getActionBarColor();
        mCurrentThemeBackgroundAlpha = getThemeBackgroundAlpha();
		ThemeUtils.notifyStatusBarColorChanged(this, mCurrentThemeResource, mCurrentThemeColor,
				mCurrentThemeBackgroundAlpha);
		setTheme(mCurrentThemeResource);
        if (shouldSetWindowBackground() && ThemeUtils.isTransparentBackground(mCurrentThemeResource)) {
            getWindow().setBackgroundDrawable(ThemeUtils.getWindowBackground(this));
        }
	}
}