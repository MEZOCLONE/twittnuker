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

import static de.vanita5.twittnuker.util.Utils.clearListViewChoices;
import static de.vanita5.twittnuker.util.Utils.configBaseCardAdapter;
import static de.vanita5.twittnuker.util.Utils.getActivatedAccountIds;
import static de.vanita5.twittnuker.util.Utils.openUserProfile;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import de.vanita5.twittnuker.adapter.ParcelableUsersAdapter;
import de.vanita5.twittnuker.adapter.iface.IBaseCardAdapter.MenuButtonClickListener;
import de.vanita5.twittnuker.loader.support.DummyParcelableUsersLoader;
import de.vanita5.twittnuker.model.Panes;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.util.MultiSelectManager;
import de.vanita5.twittnuker.util.NoDuplicatesArrayList;
import de.vanita5.twittnuker.util.Utils;

import java.util.Collections;
import java.util.List;

abstract class BaseUsersListFragment extends BasePullToRefreshListFragment implements
		LoaderCallbacks<List<ParcelableUser>>, OnItemLongClickListener, Panes.Left, OnMenuItemClickListener,
		MultiSelectManager.Callback, MenuButtonClickListener {

	private SharedPreferences mPreferences;
	private MultiSelectManager mMultiSelectManager;

	private ParcelableUsersAdapter mAdapter;

	private boolean mLoadMoreAutomatically;
	private ListView mListView;
	private long mAccountId;

	private final List<ParcelableUser> mData = Collections
			.synchronizedList(new NoDuplicatesArrayList<ParcelableUser>());
	private ParcelableUser mSelectedUser;

	public long getAccountId() {
		return mAccountId;
	}

	public final List<ParcelableUser> getData() {
		return mData;
	}

	@Override
	public ParcelableUsersAdapter getListAdapter() {
		return mAdapter;
	}

	public SharedPreferences getSharedPreferences() {
		return mPreferences;
	}

	public void loadMoreUsers() {
		if (isRefreshing()) return;
		final int count = mAdapter.getCount();
		if (count - 1 > 0) {
			final Bundle args = getArguments();
			if (args != null) {
				args.putLong(EXTRA_MAX_ID, mAdapter.getItem(count - 1).id);
			}
			if (!getLoaderManager().hasRunningLoaders()) {
				getLoaderManager().restartLoader(0, args, this);
			}
		}
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		mAdapter = new ParcelableUsersAdapter(getActivity());
		mAdapter.setMenuButtonClickListener(this);
		mMultiSelectManager = getMultiSelectManager();
		mListView = getListView();
		mListView.setFastScrollEnabled(mPreferences.getBoolean(KEY_FAST_SCROLL_THUMB, false));
		final Bundle args = getArguments() != null ? getArguments() : new Bundle();
		final long accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
		if (mAccountId != accountId) {
			mAdapter.clear();
			mData.clear();
		}
		mAccountId = accountId;
		if (!mPreferences.getBoolean(KEY_PLAIN_LIST_STYLE, false)) {
			mListView.setDivider(null);
		}
		mListView.setSelector(android.R.color.transparent);
		mListView.setOnItemLongClickListener(this);
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, getArguments(), this);
		setListShown(false);
	}

	@Override
	public Loader<List<ParcelableUser>> onCreateLoader(final int id, final Bundle args) {
		setProgressBarIndeterminateVisibility(true);
		final Loader<List<ParcelableUser>> loader = newLoaderInstance(getActivity(), args);
		return loader != null ? loader : new DummyParcelableUsersLoader(getActivity());
	}

	@Override
	public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		final ParcelableUsersAdapter adapter = getListAdapter();
        final int userPosition = adapter.findItemPosition(id);
        if (userPosition < 0) return false;
        final ParcelableUser user = adapter.getItem(userPosition);
		setItemSelected(user, position, !mMultiSelectManager.isSelected(user));
		return true;
	}

	@Override
	public void onItemsCleared() {
		clearListViewChoices(mListView);
	}

	@Override
	public void onItemSelected(final Object item) {
		mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	}

	@Override
	public void onItemUnselected(final Object item) {
	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int position, final long id) {
        final ParcelableUsersAdapter adapter = getListAdapter();
        final int userPosition = adapter.findItemPosition(id);
        if (userPosition < 0) return;
        final ParcelableUser user = adapter.getItem(userPosition);
		if (mMultiSelectManager.isActive()) {
			setItemSelected(user, position, !mMultiSelectManager.isSelected(user));
			return;
		}
		openUserProfile(getActivity(), user);
	}

	@Override
	public void onLoaderReset(final Loader<List<ParcelableUser>> loader) {
		setProgressBarIndeterminateVisibility(false);
	}

	@Override
	public void onLoadFinished(final Loader<List<ParcelableUser>> loader, final List<ParcelableUser> data) {
		setProgressBarIndeterminateVisibility(false);
		mAdapter.setData(data);
		mAdapter.setShowAccountColor(shouldShowAccountColor());
		setRefreshComplete();
		setListShown(true);
	}

	@Override
	public void onMenuButtonClick(final View button, final int position, final long id) {
		final ParcelableUser user = mAdapter.getItem(position - mListView.getHeaderViewsCount());
		if (user == null) return;
		showMenu(button, user);
	}

	@Override
	public boolean onMenuItemClick(final MenuItem item) {
		if (mSelectedUser == null) return false;
		switch (item.getItemId()) {
			default: {
				if (item.getIntent() != null) {
					try {
						startActivity(item.getIntent());
					} catch (final ActivityNotFoundException e) {
						if (Utils.isDebugBuild()) Log.w(LOGTAG, e);
						return false;
					}
				}
				break;
			}
		}
		return true;
	}

	@Override
	public void onReachedBottom() {
		if (!mLoadMoreAutomatically) return;
		loadMoreUsers();
	}

	@Override
	public void onRefreshFromEnd() {
		if (mLoadMoreAutomatically) return;
		loadMoreUsers();
	}

	@Override
	public void onRefreshFromStart() {
		if (isRefreshing()) return;
		getLoaderManager().restartLoader(0, getArguments(), this);
	}

	@Override
	public void onResume() {
		super.onResume();
		mLoadMoreAutomatically = mPreferences.getBoolean(KEY_LOAD_MORE_AUTOMATICALLY, false);
		configBaseCardAdapter(getActivity(), mAdapter);
	}

    @Override
    public void onStart() {
        super.onStart();
        mMultiSelectManager.registerCallback(this);
        final int choiceMode = mListView.getChoiceMode();
        if (mMultiSelectManager.isActive()) {
            if (choiceMode != ListView.CHOICE_MODE_MULTIPLE) {
                mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            }
        } else {
            if (choiceMode != ListView.CHOICE_MODE_NONE) {
                Utils.clearListViewChoices(mListView);
            }
        }
	}

	@Override
	public void onStop() {
		mMultiSelectManager.unregisterCallback(this);
		super.onStop();
	}

    protected UserMenuDialogFragment createMenuDialog() {
        return new UserMenuDialogFragment();
    }

	protected ParcelableUser getSelectedUser() {
		return mSelectedUser;
	}

	protected int getUserMenuResource() {
		return 0;
	}

	protected abstract Loader<List<ParcelableUser>> newLoaderInstance(Context context, Bundle args);

    protected final void removeUsers(final long... userIds) {
        if (userIds == null || userIds.length == 0) return;
        for (final long userId : userIds) {
            mData.remove(mAdapter.findItemPosition(userId));
		}
		mAdapter.setData(mData, true);
	}

	protected void setItemSelected(final ParcelableUser user, final int position, final boolean selected) {
		if (selected) {
			mMultiSelectManager.selectItem(user);
		} else {
			mMultiSelectManager.unselectItem(user);
		}
		mListView.setItemChecked(position, selected);
	}

	protected boolean shouldShowAccountColor() {
		return getActivatedAccountIds(getActivity()).length > 1;
	}

	private void showMenu(final View view, final ParcelableUser user) {
		mSelectedUser = user;
		final FragmentActivity activity = getActivity();
		if (activity == null || activity.isFinishing() || view == null || user == null) return;
        final UserMenuDialogFragment df = createMenuDialog();
        final Bundle args = new Bundle();
        args.putParcelable(EXTRA_USER, user);
        df.setArguments(args);
        df.show(getChildFragmentManager(), "user_menu");
	}
}
