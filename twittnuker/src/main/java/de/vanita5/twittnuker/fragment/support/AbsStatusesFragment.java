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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.AbsStatusesAdapter;
import de.vanita5.twittnuker.adapter.AbsStatusesAdapter.StatusAdapterListener;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.loader.iface.IExtendedLoader;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler;
import de.vanita5.twittnuker.util.KeyboardShortcutsHandler.KeyboardShortcutCallback;
import de.vanita5.twittnuker.util.ReadStateManager;
import de.vanita5.twittnuker.util.RecyclerViewNavigationHelper;
import de.vanita5.twittnuker.util.RecyclerViewUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.util.message.StatusListChangedEvent;
import de.vanita5.twittnuker.view.holder.GapViewHolder;
import de.vanita5.twittnuker.view.holder.StatusViewHolder;

import static de.vanita5.twittnuker.util.Utils.setMenuForStatus;

public abstract class AbsStatusesFragment<Data> extends AbsContentRecyclerViewFragment<AbsStatusesAdapter<Data>>
        implements LoaderCallbacks<Data>, StatusAdapterListener, KeyboardShortcutCallback {

    private final Object mStatusesBusCallback;
    private SharedPreferences mPreferences;
    private PopupMenu mPopupMenu;
    private ReadStateManager mReadStateManager;
    private RecyclerViewNavigationHelper mNavigationHelper;

    private ParcelableStatus mSelectedStatus;

    private OnMenuItemClickListener mOnStatusMenuItemClickListener = new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            final ParcelableStatus status = mSelectedStatus;
            if (status == null) return false;
            if (item.getItemId() == MENU_SHARE) {
                final Intent shareIntent = Utils.createStatusShareIntent(getActivity(), status);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_status)));
                return true;
            }
            return Utils.handleMenuItemClick(getActivity(), AbsStatusesFragment.this,
                    getFragmentManager(), getTwitterWrapper(), status, item);
        }
    };
    private OnScrollListener mOnScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                final LinearLayoutManager layoutManager = getLayoutManager();
                saveReadPosition(layoutManager.findFirstVisibleItemPosition());
            }
        }
    };

    protected AbsStatusesFragment() {
        mStatusesBusCallback = createMessageBusCallback();
    }

    public SharedPreferences getSharedPreferences() {
        if (mPreferences != null) return mPreferences;
        return mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public abstract boolean getStatuses(long[] accountIds, long[] maxIds, long[] sinceIds);

	@Override
    public boolean handleKeyboardShortcutSingle(@NonNull KeyboardShortcutsHandler handler, int keyCode, @NonNull KeyEvent event) {
        String action = handler.getKeyAction(CONTEXT_TAG_NAVIGATION, keyCode, event);
        if (ACTION_NAVIGATION_REFRESH.equals(action)) {
            triggerRefresh();
            return true;
        }
        final RecyclerView mRecyclerView = getRecyclerView();
        final LinearLayoutManager layoutManager = getLayoutManager();
        final View focusedChild = RecyclerViewUtils.findRecyclerViewChild(mRecyclerView, layoutManager.getFocusedChild());
        final int position;
        if (focusedChild != null && focusedChild.getParent() == mRecyclerView) {
            position = mRecyclerView.getChildLayoutPosition(focusedChild);
        } else {
            return false;
        }
        if (position == -1) return false;
        final ParcelableStatus status = getAdapter().getStatus(position);
        if (status == null) return false;
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            Utils.openStatus(getActivity(), status, null);
            return true;
        }
        if (action == null) {
            action = handler.getKeyAction(CONTEXT_TAG_STATUS, keyCode, event);
        }
        if (action == null) return false;
        switch (action) {
            case ACTION_STATUS_REPLY: {
                final Intent intent = new Intent(INTENT_ACTION_REPLY);
                intent.putExtra(EXTRA_STATUS, status);
                startActivity(intent);
                return true;
            }
            case ACTION_STATUS_RETWEET: {
                RetweetQuoteDialogFragment.show(getFragmentManager(), status);
                return true;
            }
            case ACTION_STATUS_FAVORITE: {
                final AsyncTwitterWrapper twitter = getTwitterWrapper();
                if (status.is_favorite) {
                    twitter.destroyFavoriteAsync(status.account_id, status.id);
                } else {
                    twitter.createFavoriteAsync(status.account_id, status.id);
                }
                return true;
            }
        }
        return mNavigationHelper.handleKeyboardShortcutSingle(handler, keyCode, event);
    }

    @Override
    public boolean handleKeyboardShortcutRepeat(@NonNull KeyboardShortcutsHandler handler, final int keyCode, final int repeatCount,
                                                @NonNull final KeyEvent event) {
        return mNavigationHelper.handleKeyboardShortcutRepeat(handler, keyCode, repeatCount, event);
	}

	@Override
    public final Loader<Data> onCreateLoader(int id, Bundle args) {
        final boolean fromUser = args.getBoolean(EXTRA_FROM_USER);
        args.remove(EXTRA_FROM_USER);
        return onCreateStatusesLoader(getActivity(), args, fromUser);
    }

    @Override
    public final void onLoadFinished(Loader<Data> loader, Data data) {
        final AbsStatusesAdapter<Data> adapter = getAdapter();
        final SharedPreferences preferences = getSharedPreferences();
        final boolean rememberPosition = preferences.getBoolean(KEY_REMEMBER_POSITION, false);
        final boolean readFromBottom = preferences.getBoolean(KEY_READ_FROM_BOTTOM, false);
        final long lastReadId;
        final int lastVisiblePos, lastVisibleTop;
        final String tag = getCurrentReadPositionTag();
        final LinearLayoutManager layoutManager = getLayoutManager();
        if (readFromBottom) {
            lastVisiblePos = layoutManager.findLastVisibleItemPosition();
        } else {
            lastVisiblePos = layoutManager.findFirstVisibleItemPosition();
        }
        if (lastVisiblePos != RecyclerView.NO_POSITION) {
            lastReadId = adapter.getStatusId(lastVisiblePos);
            final View positionView = layoutManager.findViewByPosition(lastVisiblePos);
            lastVisibleTop = positionView != null ? positionView.getTop() : 0;
        } else if (rememberPosition && tag != null) {
            lastReadId = mReadStateManager.getPosition(tag);
            lastVisibleTop = 0;
        } else {
            lastReadId = -1;
            lastVisibleTop = 0;
        }
        adapter.setData(data);
        setRefreshEnabled(true);
        if (!(loader instanceof IExtendedLoader) || ((IExtendedLoader) loader).isFromUser()) {
            adapter.setLoadMoreSupported(hasMoreData(data));
            int pos = -1;
            for (int i = 0, j = adapter.getItemCount(); i < j; i++) {
                if (lastReadId != -1 && lastReadId == adapter.getStatusId(i)) {
                    pos = i;
                    break;
                }
            }
            if (pos != -1 && adapter.isStatus(pos) && (readFromBottom || lastVisiblePos != 0)) {
                if (layoutManager.getHeight() == 0) {
                    // RecyclerView has not currently laid out, ignore padding.
                    layoutManager.scrollToPositionWithOffset(pos, lastVisibleTop);
                } else {
                    layoutManager.scrollToPositionWithOffset(pos, lastVisibleTop - layoutManager.getPaddingTop());
                }
            }
        }
        if (loader instanceof IExtendedLoader) {
            ((IExtendedLoader) loader).setFromUser(false);
        }
        onLoadingFinished();
    }

    @Override
    public void onLoaderReset(Loader<Data> loader) {
        if (loader instanceof IExtendedLoader) {
            ((IExtendedLoader) loader).setFromUser(false);
        }
    }

    @Override
    public void onGapClick(GapViewHolder holder, int position) {
        final AbsStatusesAdapter<Data> adapter = getAdapter();
        final ParcelableStatus status = adapter.getStatus(position);
        final long sinceId = position + 1 < adapter.getStatusesCount() ? adapter.getStatus(position + 1).id : -1;
        final long[] accountIds = {status.account_id};
        final long[] maxIds = {status.id};
        final long[] sinceIds = {sinceId};
        getStatuses(accountIds, maxIds, sinceIds);
    }

	@Override
    public void onMediaClick(StatusViewHolder holder, View view, ParcelableMedia media, int position) {
        final AbsStatusesAdapter<Data> adapter = getAdapter();
        final ParcelableStatus status = adapter.getStatus(position);
        if (status == null) return;
        final Bundle options = Utils.createMediaViewerActivityOption(view);
        Utils.openMedia(getActivity(), status, media, options);
    }

    @Override
    public void onStatusActionClick(StatusViewHolder holder, int id, int position) {
        final AbsStatusesAdapter<Data> adapter = getAdapter();
        final ParcelableStatus status = adapter.getStatus(position);
        if (status == null) return;
        final FragmentActivity activity = getActivity();
        switch (id) {
            case R.id.reply_count: {
                final Intent intent = new Intent(INTENT_ACTION_REPLY);
                intent.setPackage(activity.getPackageName());
                intent.putExtra(EXTRA_STATUS, status);
                activity.startActivity(intent);
                break;
            }
            case R.id.retweet_count: {
                RetweetQuoteDialogFragment.show(getFragmentManager(), status);
                break;
            }
            case R.id.favorite_count: {
                final AsyncTwitterWrapper twitter = getTwitterWrapper();
                if (twitter == null) return;
                if (status.is_favorite) {
                    twitter.destroyFavoriteAsync(status.account_id, status.id);
                } else {
                    twitter.createFavoriteAsync(status.account_id, status.id);
                }
                break;
            }
        }
    }

    @Override
    public void onStatusClick(StatusViewHolder holder, int position) {
        final AbsStatusesAdapter<Data> adapter = getAdapter();
        Utils.openStatus(getActivity(), adapter.getStatus(position), null);
    }

    @Override
    public boolean onStatusLongClick(StatusViewHolder holder, int position) {
        //TODO handle long click event
        return true;
    }

    @Override
    public void onStatusMenuClick(StatusViewHolder holder, View menuView, int position) {
        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
        }
        final AbsStatusesAdapter<Data> adapter = getAdapter();
        final PopupMenu popupMenu = new PopupMenu(adapter.getContext(), menuView,
                Gravity.NO_GRAVITY, R.attr.actionOverflowMenuStyle, 0);
        popupMenu.setOnMenuItemClickListener(mOnStatusMenuItemClickListener);
        popupMenu.inflate(R.menu.action_status);
        final ParcelableStatus status = adapter.getStatus(position);
        setMenuForStatus(adapter.getContext(), popupMenu.getMenu(), status);
        popupMenu.show();
        mPopupMenu = popupMenu;
        mSelectedStatus = status;
    }

    @Override
    public void onUserProfileClick(StatusViewHolder holder, ParcelableStatus status, int position) {
        final FragmentActivity activity = getActivity();
        final View profileImageView = holder.getProfileImageView();
        final View profileTypeView = holder.getProfileTypeView();
        final Bundle options = Utils.makeSceneTransitionOption(activity,
                new Pair<>(profileImageView, UserFragment.TRANSITION_NAME_PROFILE_IMAGE),
                new Pair<>(profileTypeView, UserFragment.TRANSITION_NAME_PROFILE_TYPE));
        if (status.is_quote) {
            Utils.openUserProfile(activity, status.account_id, status.quoted_by_user_id,
                    status.quoted_by_user_screen_name, options);
        } else {
			Utils.openUserProfile(activity, status.account_id, status.user_id, status.user_screen_name, options);
		}
    }

    @Override
    public void onStart() {
        super.onStart();
        final RecyclerView recyclerView = getRecyclerView();
        recyclerView.addOnScrollListener(mOnScrollListener);
        final Bus bus = TwittnukerApplication.getInstance(getActivity()).getMessageBus();
        bus.register(mStatusesBusCallback);
    }

    @Override
    public void onStop() {
        final Bus bus = TwittnukerApplication.getInstance(getActivity()).getMessageBus();
        bus.unregister(mStatusesBusCallback);
        final RecyclerView recyclerView = getRecyclerView();
        recyclerView.removeOnScrollListener(mOnScrollListener);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
        }
        super.onDestroyView();
    }

    @Override
    public final boolean scrollToStart() {
        final boolean result = super.scrollToStart();
        if (result) {
            saveReadPosition(0);
        }
        return result;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mReadStateManager = getReadStateManager();
        final AbsStatusesAdapter<Data> adapter = getAdapter();
        final RecyclerView recyclerView = getRecyclerView();
        final LinearLayoutManager layoutManager = getLayoutManager();
        mNavigationHelper = new RecyclerViewNavigationHelper(recyclerView, layoutManager,
                adapter, this);

        adapter.setListener(this);

        final Bundle loaderArgs = new Bundle(getArguments());
        loaderArgs.putBoolean(EXTRA_FROM_USER, true);
        getLoaderManager().initLoader(0, loaderArgs, this);
        showProgress();
    }

    protected Object createMessageBusCallback() {
        return new StatusesBusCallback();
    }

    protected abstract long[] getAccountIds();

	protected Data getAdapterData() {
        final AbsStatusesAdapter<Data> adapter = getAdapter();
        return adapter.getData();
	}

    protected void setAdapterData(Data data) {
        final AbsStatusesAdapter<Data> adapter = getAdapter();
        adapter.setData(data);
	}

    protected String getReadPositionTag() {
        return null;
    }

    protected abstract boolean hasMoreData(Data data);

    protected abstract Loader<Data> onCreateStatusesLoader(final Context context, final Bundle args,
                                                           final boolean fromUser);

    protected abstract void onLoadingFinished();

    protected void saveReadPosition(int position) {
        final String readPositionTag = getReadPositionTagWithAccounts();
        if (readPositionTag == null) return;
        if (position == RecyclerView.NO_POSITION) return;
        final AbsStatusesAdapter<Data> adapter = getAdapter();
        final ParcelableStatus status = adapter.getStatus(position);
        if (status == null) return;
        mReadStateManager.setPosition(readPositionTag, status.id);
        mReadStateManager.setPosition(getCurrentReadPositionTag(), status.id, true);
    }

    @NonNull
    @Override
    protected Rect getExtraContentPadding() {
        final int paddingVertical = getResources().getDimensionPixelSize(R.dimen.element_spacing_small);
        return new Rect(0, paddingVertical, 0, paddingVertical);
    }

    private String getCurrentReadPositionTag() {
        final String tag = getReadPositionTagWithAccounts();
        if (tag == null) return null;
        return tag + "_current";
    }

    private String getReadPositionTagWithAccounts() {
        return Utils.getReadPositionTagWithAccounts(getReadPositionTag(), getAccountIds());
    }

    protected final class StatusesBusCallback {

        protected StatusesBusCallback() {
        }

        @Subscribe
        public void notifyStatusListChanged(StatusListChangedEvent event) {
            final AbsStatusesAdapter<Data> adapter = getAdapter();
            adapter.notifyDataSetChanged();
        }

	}
}