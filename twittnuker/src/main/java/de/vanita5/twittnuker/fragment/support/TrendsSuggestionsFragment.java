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

package de.vanita5.twittnuker.fragment.support;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import de.vanita5.twittnuker.adapter.TrendsAdapter;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedTrends;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.MultiSelectManager;
import de.vanita5.twittnuker.util.message.TaskStateChangedEvent;

import static de.vanita5.twittnuker.util.Utils.getDefaultAccountId;
import static de.vanita5.twittnuker.util.Utils.getTableNameByUri;
import static de.vanita5.twittnuker.util.Utils.openTweetSearch;

public class TrendsSuggestionsFragment extends AbsContentListViewFragment<TrendsAdapter>
        implements LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

	private MultiSelectManager mMultiSelectManager;
	private SharedPreferences mPreferences;


	private long mAccountId;

    @Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		super.onActivityCreated(savedInstanceState);
		mMultiSelectManager = getMultiSelectManager();
		mAccountId = getDefaultAccountId(getActivity());
        getListView().setOnItemClickListener(this);
		getLoaderManager().initLoader(0, null, this);
        showProgress();
	}

    @NonNull
	@Override
    protected TrendsAdapter onCreateAdapter(Context context, boolean compact) {
        return new TrendsAdapter(getActivity());
    }

    @Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		final Uri uri = CachedTrends.Local.CONTENT_URI;
		final String table = getTableNameByUri(uri);
		final String where = table != null ? CachedTrends.TIMESTAMP + " = " + "(SELECT " + CachedTrends.TIMESTAMP
				+ " FROM " + table + " ORDER BY " + CachedTrends.TIMESTAMP + " DESC LIMIT 1)" : null;
		return new CursorLoader(getActivity(), uri, CachedTrends.COLUMNS, where, null, null);
	}

	@Override
    public void onItemClick(final AdapterView<?> view, final View child, final int position, final long id) {
		if (mMultiSelectManager.isActive()) return;
        final String trend;
        if (view instanceof ListView) {
            trend = getAdapter().getItem(position - ((ListView) view).getHeaderViewsCount());
        } else {
            trend = getAdapter().getItem(position);

        }
        if (trend == null) return;
        openTweetSearch(getActivity(), mAccountId, trend);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
        getAdapter().swapCursor(null);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor cursor) {
        getAdapter().swapCursor(cursor);
        showContent();
	}

	@Override
    public void onRefresh() {
		if (isRefreshing()) return;
		final AsyncTwitterWrapper twitter = getTwitterWrapper();
		if (twitter == null) return;
		twitter.getLocalTrendsAsync(mAccountId, mPreferences.getInt(KEY_LOCAL_TRENDS_WOEID, 1));
	}

	@Override
    public boolean isRefreshing() {
        return false;
    }

    @Override
	public void onStart() {
		super.onStart();
		getLoaderManager().restartLoader(0, null, this);
        final Bus bus = TwittnukerApplication.getInstance(getActivity()).getMessageBus();
        assert bus != null;
        bus.register(this);
	}

	@Override
	public void onStop() {
        final Bus bus = TwittnukerApplication.getInstance(getActivity()).getMessageBus();
        assert bus != null;
        bus.unregister(this);
		super.onStop();
    }

    @Subscribe
    public void notifyTaskStateChanged(TaskStateChangedEvent event) {
        updateRefreshState();
	}

	protected void updateRefreshState() {
		final AsyncTwitterWrapper twitter = getTwitterWrapper();
		if (twitter == null || !getUserVisibleHint()) return;
		setRefreshing(twitter.isLocalTrendsRefreshing());
	}

}