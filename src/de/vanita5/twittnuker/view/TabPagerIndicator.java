package de.vanita5.twittnuker.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.view.iface.PagerIndicator;

public class TabPagerIndicator extends RecyclerView implements PagerIndicator {

    private final int mStripHeight;

	private ViewPager mViewPager;

    private final TabPagerIndicatorAdapter mIndicatorAdapter;
    private PagerAdapter mPagerProvider;

	private OnPageChangeListener mPageChangeListener;

	public TabPagerIndicator(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
        ViewCompat.setOverScrollMode(this, ViewCompat.OVER_SCROLL_NEVER);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        setLayoutManager(new TabLayoutManager(this));
        mIndicatorAdapter = new TabPagerIndicatorAdapter(this);
        setAdapter(mIndicatorAdapter);
        mStripHeight = getResources().getDimensionPixelSize(R.dimen.element_spacing_small);
	}

    public void setStripColor(int color) {
        mIndicatorAdapter.setStripColor(color);
    }

	public TabPagerIndicator(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TabPagerIndicator(Context context) {
		this(context, null);
	}

	@Override
	public void notifyDataSetChanged() {
        mIndicatorAdapter.notifyDataSetChanged();
	}

	@Override
	public void setCurrentItem(int item) {
        mViewPager.setCurrentItem(item);
	}

	@Override
	public void setOnPageChangeListener(OnPageChangeListener listener) {
		mPageChangeListener = listener;
	}

	@Override
	public void setViewPager(ViewPager view) {
		setViewPager(view, view.getCurrentItem());
	}

	@Override
	public void setViewPager(ViewPager view, int initialPosition) {
		final PagerAdapter adapter = view.getAdapter();
		if (!(adapter instanceof TabProvider)) {
			throw new IllegalArgumentException();
		}
		mViewPager = view;
        mPagerProvider = adapter;
		view.setOnPageChangeListener(this);
        mIndicatorAdapter.setTabProvider((TabProvider) adapter);
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		if (mPageChangeListener == null) return;
		mPageChangeListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
	}

	@Override
	public void onPageSelected(int position) {
        mIndicatorAdapter.notifyDataSetChanged();
		if (mPageChangeListener == null) return;
		mPageChangeListener.onPageSelected(position);
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		if (mPageChangeListener == null) return;
		mPageChangeListener.onPageScrollStateChanged(state);
	}

	public int getCount() {
        return mIndicatorAdapter.getItemCount();
	}

	public void setBadge(int position, int count) {
        mIndicatorAdapter.setBadge(position, count);
	}

	public void setDisplayLabel(boolean display) {

	}

	public void setDisplayIcon(boolean display) {

	}

    public int getStripHeight() {
        return mStripHeight;
    }

    public void setIconColor(int color) {
        mIndicatorAdapter.setIconColor(color);
    }

    public Context getItemContext() {
        return mIndicatorAdapter.getItemContext();
    }

    public void setItemContext(Context context) {
        mIndicatorAdapter.setItemContext(context);
    }

    public void setDisplayBadge(boolean display) {
        mIndicatorAdapter.setDisplayBadge(display);
    }

    private static class TabPagerIndicatorAdapter extends Adapter<TabItemHolder> implements OnClickListener, OnLongClickListener {

		private final TabPagerIndicator mIndicator;
        private final SparseIntArray mUnreadCounts;
        private Context mItemContext;
        private LayoutInflater mInflater;

		private TabProvider mTabProvider;
        private int mStripColor, mIconColor;
        private boolean mDisplayBadge;

		public TabPagerIndicatorAdapter(TabPagerIndicator indicator) {
			mIndicator = indicator;
            mUnreadCounts = new SparseIntArray();
		}

		@Override
		public TabItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = mInflater.inflate(R.layout.tab_item_home, parent, false);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
            final View selectedIndicator = view.findViewById(R.id.selected_indicator);
            final ViewGroup.LayoutParams lp = selectedIndicator.getLayoutParams();
            lp.height = mIndicator.getStripHeight();
            selectedIndicator.setLayoutParams(lp);
            return new TabItemHolder(view);
		}

		@Override
		public void onBindViewHolder(TabItemHolder holder, int position) {
            final Drawable icon = mTabProvider.getPageIcon(position);
            final CharSequence title = mTabProvider.getPageTitle(position);
            holder.setTabData(position, icon, title, mIndicator.getCurrentItem() == position);
            holder.setStripColor(mStripColor);
            holder.setIconColor(mIconColor);
            holder.setBadge(mUnreadCounts.get(position, 0), mDisplayBadge);
		}

		@Override
		public int getItemCount() {
			if (mTabProvider == null) return 0;
			return mTabProvider.getCount();
		}

		public void setTabProvider(TabProvider tabProvider) {
			mTabProvider = tabProvider;
			notifyDataSetChanged();
		}

        @Override
        public void onClick(View v) {
            final Object tag = v.getTag();
            if (!(tag instanceof Integer)) return;
            mIndicator.dispatchTabClick((Integer) tag);
	    }

        @Override
        public boolean onLongClick(View v) {
            final Object tag = v.getTag();
            if (!(tag instanceof Integer)) return false;
            return mIndicator.dispatchTabLongClick((Integer) tag);
        }

        public void setStripColor(int color) {
            mStripColor = color;
            notifyDataSetChanged();
        }

        public void setIconColor(int color) {
            mIconColor = color;
            notifyDataSetChanged();
        }

        public void setItemContext(Context itemContext) {
            mItemContext = itemContext;
            mInflater = LayoutInflater.from(itemContext);
        }

        public Context getItemContext() {
            return mItemContext;
        }

        public void setBadge(int position, int count) {
            mUnreadCounts.put(position, count);
            notifyDataSetChanged();
        }

        public void setDisplayBadge(boolean display) {
            mDisplayBadge = display;
            notifyDataSetChanged();
        }
    }

    private void dispatchTabClick(int position) {
        final int currentItem = getCurrentItem();
        setCurrentItem(position);
        if (mPagerProvider instanceof TabListener) {
            if (currentItem != position) {
                ((TabListener) mPagerProvider).onPageSelected(position);
            } else {
                ((TabListener) mPagerProvider).onPageReselected(position);
            }
        }
    }

    private boolean dispatchTabLongClick(int position) {
        if (mPagerProvider instanceof TabListener) {
            return ((TabListener) mPagerProvider).onTabLongClick(position);
        }
        return false;
    }

    private int getCurrentItem() {
        return mViewPager.getCurrentItem();
    }

	private static class TabItemHolder extends ViewHolder {

        private final View itemView;
		private final ImageView iconView;
        private final View selectedIndicator;
        private final BadgeView badgeView;

		public TabItemHolder(View itemView) {
			super(itemView);
            this.itemView = itemView;
            selectedIndicator = itemView.findViewById(R.id.selected_indicator);
            iconView = (ImageView) itemView.findViewById(R.id.tab_icon);
            badgeView = (BadgeView) itemView.findViewById(R.id.unread_indicator);
		}


        public void setTabData(int position, Drawable icon, CharSequence title, boolean activated) {
            itemView.setTag(position);
            itemView.setContentDescription(title);
			iconView.setImageDrawable(icon);
			iconView.setContentDescription(title);
            selectedIndicator.setVisibility(activated ? VISIBLE : INVISIBLE);
        }

        public void setStripColor(int color) {
            selectedIndicator.setBackgroundColor(color);
        }

        public void setIconColor(int color) {
            iconView.setColorFilter(color);
        }

        public void setBadge(int count, boolean display) {
            badgeView.setText(String.valueOf(count));
            badgeView.setVisibility(display && count > 0 ? VISIBLE : GONE);
        }
    }

    public static class TabLayoutManager extends LinearLayoutManager {

        private final TabPagerIndicator mIndicator;

        public TabLayoutManager(TabPagerIndicator indicator) {
            super(indicator.getContext(), HORIZONTAL, false);
            mIndicator = indicator;
        }

        @Override
        public void measureChildWithMargins(View child, int widthUsed, int heightUsed) {
            final int count = mIndicator.getCount();
            if (count == 0) return;
            final int parentHeight = mIndicator.getHeight(), parentWidth = mIndicator.getWidth();
            final int width = Math.max(parentWidth / count, parentHeight);
            final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(parentHeight, MeasureSpec.EXACTLY);
            final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            child.measure(widthMeasureSpec, heightMeasureSpec);
		}
	}
}