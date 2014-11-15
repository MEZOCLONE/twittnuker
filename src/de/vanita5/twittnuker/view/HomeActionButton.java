package de.vanita5.twittnuker.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.accessor.ViewAccessor;
import de.vanita5.twittnuker.view.iface.IHomeActionButton;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class HomeActionButton extends FrameLayout implements IHomeActionButton {

	private final ImageView mIconView;
	private final ProgressBar mProgressBar;

	public HomeActionButton(final Context context) {
		this(context, null);
	}

	public HomeActionButton(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HomeActionButton(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
        inflate(ThemeUtils.getActionBarContext(context), R.layout.action_item_home_actions, this);
		mIconView = (ImageView) findViewById(android.R.id.icon);
		mProgressBar = (ProgressBar) findViewById(android.R.id.progress);
		setOutlineProvider(new HomeActionButtonOutlineProvider());
		setClipToOutline(true);
        setButtonColor(Color.WHITE);
	}

	@Override
    public void setButtonColor(int color) {
        ViewAccessor.setBackground(this, new ColorDrawable(color));
	}

    @Override
    public void setIconColor(int color, Mode mode) {
        mIconView.setColorFilter(color, mode);
        mProgressBar.setIndeterminateTintList(ColorStateList.valueOf(color));
	}

	@Override
	public void setIcon(final Bitmap bm) {
		mIconView.setImageBitmap(bm);
	}

	@Override
	public void setIcon(final Drawable drawable) {
		mIconView.setImageDrawable(drawable);
	}

	@Override
	public void setIcon(final int resId) {
		mIconView.setImageResource(resId);
	}

	@Override
	public void setShowProgress(final boolean showProgress) {
		mProgressBar.setVisibility(showProgress ? View.VISIBLE : View.GONE);
		mIconView.setVisibility(showProgress ? View.GONE : View.VISIBLE);
	}

	@Override
	public void setTitle(final CharSequence title) {
		setContentDescription(title);
	}

	@Override
	public void setTitle(final int title) {
		setTitle(getResources().getText(title));
	}

	private static class HomeActionButtonOutlineProvider extends ViewOutlineProvider {
		@Override
		public void getOutline(View view, Outline outline) {
			final int width = view.getWidth(), height = view.getHeight();
			final int size = Math.min(width, height);
			final int left = (width - size) / 2, top = (height - size) / 2;
			outline.setOval(left, top, left + size, top + size);
		}
	}
}