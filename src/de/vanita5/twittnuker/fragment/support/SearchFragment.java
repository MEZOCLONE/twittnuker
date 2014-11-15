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

package de.vanita5.twittnuker.fragment.support;

import android.app.ActionBar;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.support.LinkHandlerActivity;
import de.vanita5.twittnuker.adapter.support.SupportTabsAdapter;
import de.vanita5.twittnuker.fragment.iface.RefreshScrollTopInterface;
import de.vanita5.twittnuker.fragment.iface.SupportFragmentCallback;
import de.vanita5.twittnuker.model.Panes;
import de.vanita5.twittnuker.provider.RecentSearchProvider;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.view.ExtendedViewPager;
import de.vanita5.twittnuker.view.LinePageIndicator;

public class SearchFragment extends BaseSupportFragment implements Panes.Left, OnPageChangeListener,
		RefreshScrollTopInterface, SupportFragmentCallback {

	private ExtendedViewPager mViewPager;

	private SupportTabsAdapter mAdapter;
    private LinePageIndicator mPagerIndicator;

	private Fragment mCurrentVisibleFragment;

	@Override
	public Fragment getCurrentVisibleFragment() {
		return mCurrentVisibleFragment;
	}

	public void hideIndicator() {
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		final Bundle args = getArguments();
		final FragmentActivity activity = getActivity();
		mAdapter = new SupportTabsAdapter(activity, getChildFragmentManager(), null, 1);
		mAdapter.addTab(SearchStatusesFragment.class, args, getString(R.string.statuses),
                R.drawable.ic_action_twitter, 0);
        mAdapter.addTab(SearchUsersFragment.class, args, getString(R.string.users), R.drawable.ic_action_user, 1);
		mViewPager.setAdapter(mAdapter);
		mViewPager.setOnPageChangeListener(this);
		mViewPager.setOffscreenPageLimit(2);
        mPagerIndicator.setSelectedColor(ThemeUtils.getThemeColor(activity));
		mPagerIndicator.setViewPager(mViewPager);
		if (savedInstanceState == null && args != null && args.containsKey(EXTRA_QUERY)) {
			final String query = args.getString(EXTRA_QUERY);
			final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
					RecentSearchProvider.AUTHORITY, RecentSearchProvider.MODE);
			suggestions.saveRecentQuery(query, null);
			if (activity instanceof LinkHandlerActivity) {
				final ActionBar ab = activity.getActionBar();
				if (ab != null) {
					ab.setSubtitle(query);
				}
			}
		}
	}

	@Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		inflater.inflate(R.menu.menu_search, menu);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
	}
	
	@Override
	public void onDetachFragment(final Fragment fragment) {

	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SAVE: {
			final AsyncTwitterWrapper twitter = getTwitterWrapper();
			final Bundle args = getArguments();
			if (twitter != null && args != null) {
				final long accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
				final String query = args.getString(EXTRA_QUERY);
				twitter.createSavedSearchAsync(accountId, query);
			}
			return true;
		}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
	}

	@Override
	public void onPageScrollStateChanged(final int state) {
	}

	@Override
	public void onPageSelected(final int position) {
	}

	@Override
	public void onSetUserVisibleHint(final Fragment fragment, final boolean isVisibleToUser) {
		if (isVisibleToUser) {
			mCurrentVisibleFragment = fragment;
		}
	}

	@Override
	public void onViewCreated(final View view, final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mViewPager = (ExtendedViewPager) view.findViewById(R.id.search_pager);
        mPagerIndicator = (LinePageIndicator) view.findViewById(R.id.search_pager_indicator);
	}

	@Override
	public boolean scrollToStart() {
		if (!(mCurrentVisibleFragment instanceof RefreshScrollTopInterface)) return false;
		((RefreshScrollTopInterface) mCurrentVisibleFragment).scrollToStart();
		return true;
	}

	public void showIndicator() {
	}

	@Override
	public boolean triggerRefresh() {
		if (!(mCurrentVisibleFragment instanceof RefreshScrollTopInterface)) return false;
		((RefreshScrollTopInterface) mCurrentVisibleFragment).triggerRefresh();
		return true;
	}

	@Override
	public boolean triggerRefresh(final int position) {
		return false;
	}

}
