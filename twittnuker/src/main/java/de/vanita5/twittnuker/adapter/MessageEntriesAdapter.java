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

package de.vanita5.twittnuker.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.iface.IContentCardAdapter;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.model.StringLongPair;
import de.vanita5.twittnuker.provider.TwidereDataStore.DirectMessages.ConversationEntries;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.MediaLoaderWrapper;
import de.vanita5.twittnuker.util.MultiSelectManager;
import de.vanita5.twittnuker.util.ReadStateManager;
import de.vanita5.twittnuker.util.ReadStateManager.OnReadStateChangeListener;
import de.vanita5.twittnuker.util.SharedPreferencesWrapper;
import de.vanita5.twittnuker.util.UserColorNameManager;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.holder.LoadIndicatorViewHolder;
import de.vanita5.twittnuker.view.holder.MessageEntryViewHolder;

public class MessageEntriesAdapter extends Adapter<ViewHolder> implements Constants,
        IContentCardAdapter, OnClickListener, OnReadStateChangeListener {

    public static final int ITEM_VIEW_TYPE_MESSAGE = 0;
    public static final int ITEM_VIEW_TYPE_LOAD_INDICATOR = 1;

	private final Context mContext;
	private final LayoutInflater mInflater;
	private final MediaLoaderWrapper mImageLoader;
	private final MultiSelectManager mMultiSelectManager;
    private final int mTextSize;
    private final int mProfileImageStyle;
    private final int mMediaPreviewStyle;
    private final ReadStateManager mReadStateManager;
    private final OnSharedPreferenceChangeListener mReadStateChangeListener;
    private UserColorNameManager mUserColorNameManager;
    private final AsyncTwitterWrapper mTwitterWrapper;

    private final boolean mDisplayProfileImage;
    private boolean mLoadMoreSupported;
    private boolean mLoadMoreIndicatorVisible;
    private boolean mShowAccountsColor;
	private Cursor mCursor;
	private MessageEntriesAdapterListener mListener;
    private StringLongPair[] mPositionPairs;

    public MessageEntriesAdapter(final Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        final TwittnukerApplication app = TwittnukerApplication.getInstance(context);
        mMultiSelectManager = app.getMultiSelectManager();
        mImageLoader = app.getMediaLoaderWrapper();
        mTwitterWrapper = app.getTwitterWrapper();
        final SharedPreferencesWrapper preferences = SharedPreferencesWrapper.getInstance(context,
                SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mProfileImageStyle = Utils.getProfileImageStyle(preferences.getString(KEY_PROFILE_IMAGE_STYLE, null));
        mMediaPreviewStyle = Utils.getMediaPreviewStyle(preferences.getString(KEY_MEDIA_PREVIEW_STYLE, null));
        mDisplayProfileImage = preferences.getBoolean(KEY_DISPLAY_PROFILE_IMAGE, true);
        mTextSize = preferences.getInt(KEY_TEXT_SIZE, context.getResources().getInteger(R.integer.default_text_size));
        mReadStateManager = app.getReadStateManager();
        mUserColorNameManager = app.getUserColorNameManager();
        mReadStateChangeListener = new OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                updateReadState();
            }
        };
    }

    @Override
	public Context getContext() {
		return mContext;
	}

    @Override
    public int getProfileImageStyle() {
        return mProfileImageStyle;
    }

    @Override
    public float getTextSize() {
        return mTextSize;
    }

    @NonNull
    @Override
    public AsyncTwitterWrapper getTwitterWrapper() {
        return mTwitterWrapper;
    }

    @Override
    public boolean isProfileImageEnabled() {
        return mDisplayProfileImage;
    }

    public DirectMessageEntry getEntry(final int position) {
        final Cursor c = mCursor;
        if (c == null || c.isClosed() || !c.moveToPosition(position)) return null;
        return new DirectMessageEntry(c);
    }

    @Override
    public MediaLoaderWrapper getMediaLoader() {
        return mImageLoader;
    }

    @Override
    public UserColorNameManager getUserColorNameManager() {
        return mUserColorNameManager;
    }

    @Override
    public boolean isLoadMoreIndicatorVisible() {
        return mLoadMoreIndicatorVisible;
    }

    @Override
    public void setLoadMoreIndicatorVisible(boolean enabled) {
        if (mLoadMoreIndicatorVisible == enabled) return;
        mLoadMoreIndicatorVisible = enabled && mLoadMoreSupported;
        notifyDataSetChanged();
    }

    @Override
    public boolean isLoadMoreSupported() {
        return mLoadMoreSupported;
    }

    @Override
    public void setLoadMoreSupported(boolean supported) {
        mLoadMoreSupported = supported;
        if (!supported) {
            mLoadMoreIndicatorVisible = false;
        }
        notifyDataSetChanged();
    }

    @Override
    public void onClick(final View view) {
//        if (mMultiSelectManager.isActive()) return;
//        final Object tag = view.getTag();
//        final int position = tag instanceof Integer ? (Integer) tag : -1;
//        if (position == -1) return;
//        switch (view.getId()) {
//            case R.id.profile_image: {
//                if (mContext instanceof Activity) {
//                    final long account_id = getAccountId(position);
//                    final long user_id = getConversationId(position);
//                    final String screen_name = getScreenName(position);
//                    openUserProfile(mContext, account_id, user_id, screen_name, null);
//                }
//                break;
//            }
//        }
    }

    @Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case ITEM_VIEW_TYPE_MESSAGE: {
				final View view = mInflater.inflate(R.layout.list_item_message_entry, parent, false);
				return new MessageEntryViewHolder(this, view);
			}
            case ITEM_VIEW_TYPE_LOAD_INDICATOR: {
                final View view = mInflater.inflate(R.layout.card_item_load_indicator, parent, false);
                return new LoadIndicatorViewHolder(view);
            }
        }
        throw new IllegalStateException("Unknown view type " + viewType);
    }

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case ITEM_VIEW_TYPE_MESSAGE: {
				final Cursor c = mCursor;
				c.moveToPosition(position);
                ((MessageEntryViewHolder) holder).displayMessage(c, isUnread(c));
                break;
            }
        }
	}

    @Override
    public int getItemViewType(int position) {
        if (position == getMessagesCount()) {
            return ITEM_VIEW_TYPE_LOAD_INDICATOR;
        }
        return ITEM_VIEW_TYPE_MESSAGE;
    }

    @Override
    public final int getItemCount() {
        return getMessagesCount() + (mLoadMoreIndicatorVisible ? 1 : 0);
    }

	public void onMessageClick(int position) {
		if (mListener == null) return;
		mListener.onEntryClick(position, getEntry(position));
	}

    @Override
    public void onReadStateChanged() {

    }

    public void onUserProfileClick(int position) {
        mListener.onUserClick(position, getEntry(position));
    }

	public void setCursor(Cursor cursor) {
		mCursor = cursor;
        mReadStateManager.unregisterOnSharedPreferenceChangeListener(mReadStateChangeListener);
        if (cursor != null) {
            updateReadState();
            mReadStateManager.registerOnSharedPreferenceChangeListener(mReadStateChangeListener);
        }
		notifyDataSetChanged();
	}

    public void setListener(MessageEntriesAdapterListener listener) {
        mListener = listener;
	}

    public void updateReadState() {
        mPositionPairs = mReadStateManager.getPositionPairs(TAB_TYPE_DIRECT_MESSAGES);
        notifyDataSetChanged();
    }

    private int getMessagesCount() {
        final Cursor c = mCursor;
        if (c == null || c.isClosed()) return 0;
        return c.getCount();
    }

    private boolean isUnread(Cursor c) {
        if (mPositionPairs == null) return true;
        final long accountId = c.getLong(ConversationEntries.IDX_ACCOUNT_ID);
        final long conversationId = c.getLong(ConversationEntries.IDX_CONVERSATION_ID);
        final long messageId = c.getLong(ConversationEntries.IDX_MESSAGE_ID);
        final String key = accountId + "-" + conversationId;
        for (StringLongPair pair : mPositionPairs) {
            if (key.equals(pair.getKey())) return messageId > pair.getValue();
        }
        return true;
    }

    public void setShowAccountsColor(boolean showAccountsColor) {
        if (mShowAccountsColor == showAccountsColor) return;
        mShowAccountsColor = showAccountsColor;
        notifyDataSetChanged();
    }

    public boolean shouldShowAccountsColor() {
        return mShowAccountsColor;
    }

    public interface MessageEntriesAdapterListener {
        void onEntryClick(int position, DirectMessageEntry entry);

        void onUserClick(int position, DirectMessageEntry entry);
	}

	public static class DirectMessageEntry {

		public final long account_id, conversation_id;
		public final String screen_name, name;

		DirectMessageEntry(Cursor cursor) {
			account_id = cursor.getLong(ConversationEntries.IDX_ACCOUNT_ID);
			conversation_id = cursor.getLong(ConversationEntries.IDX_CONVERSATION_ID);
			screen_name = cursor.getString(ConversationEntries.IDX_SCREEN_NAME);
			name = cursor.getString(ConversationEntries.IDX_NAME);
		}

	}

}