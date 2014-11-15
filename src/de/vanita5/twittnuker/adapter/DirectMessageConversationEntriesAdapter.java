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

package de.vanita5.twittnuker.adapter;

import static de.vanita5.twittnuker.provider.TweetStore.DirectMessages.ConversationEntries.IDX_ACCOUNT_ID;
import static de.vanita5.twittnuker.provider.TweetStore.DirectMessages.ConversationEntries.IDX_CONVERSATION_ID;
import static de.vanita5.twittnuker.provider.TweetStore.DirectMessages.ConversationEntries.IDX_NAME;
import static de.vanita5.twittnuker.provider.TweetStore.DirectMessages.ConversationEntries.IDX_PROFILE_IMAGE_URL;
import static de.vanita5.twittnuker.provider.TweetStore.DirectMessages.ConversationEntries.IDX_SCREEN_NAME;
import static de.vanita5.twittnuker.provider.TweetStore.DirectMessages.ConversationEntries.IDX_TEXT;
import static de.vanita5.twittnuker.util.HtmlEscapeHelper.toPlainText;
import static de.vanita5.twittnuker.util.UserColorUtils.getUserColor;
import static de.vanita5.twittnuker.util.Utils.configBaseCardAdapter;
import static de.vanita5.twittnuker.util.Utils.getAccountColor;
import static de.vanita5.twittnuker.util.Utils.openUserProfile;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.iface.IBaseCardAdapter;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.provider.TweetStore.DirectMessages.ConversationEntries;
import de.vanita5.twittnuker.util.ImageLoaderWrapper;
import de.vanita5.twittnuker.util.MultiSelectManager;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.holder.DirectMessageEntryViewHolder;

public class DirectMessageConversationEntriesAdapter extends BaseCursorAdapter implements IBaseCardAdapter,
		OnClickListener {

	private final ImageLoaderWrapper mImageLoader;
	private final MultiSelectManager mMultiSelectManager;

	private boolean mAnimationEnabled;
	private int mMaxAnimationPosition;

	private final boolean mPlainList;

	public DirectMessageConversationEntriesAdapter(final Context context) {
		this(context, Utils.isCompactCards(context), Utils.isPlainListStyle(context));
	}

	public DirectMessageConversationEntriesAdapter(final Context context, final boolean compactCards,
												   final boolean plainList) {
		super(context, getItemResource(compactCards), null, new String[0], new int[0], 0);
		mPlainList = plainList;
		final TwittnukerApplication app = TwittnukerApplication.getInstance(context);
		mMultiSelectManager = app.getMultiSelectManager();
		mImageLoader = app.getImageLoaderWrapper();
		configBaseCardAdapter(context, this);
	}

	@Override
	public void bindView(final View view, final Context context, final Cursor cursor) {
		final DirectMessageEntryViewHolder holder = (DirectMessageEntryViewHolder) view.getTag();
		final int position = cursor.getPosition();
		final long accountId = cursor.getLong(ConversationEntries.IDX_ACCOUNT_ID);
		final long conversationId = cursor.getLong(ConversationEntries.IDX_CONVERSATION_ID);
		final long timestamp = cursor.getLong(ConversationEntries.IDX_MESSAGE_TIMESTAMP);
		final boolean isOutgoing = cursor.getInt(ConversationEntries.IDX_IS_OUTGOING) == 1;

		final String name = cursor.getString(IDX_NAME);
		final String screenName = cursor.getString(IDX_SCREEN_NAME);

		final boolean showAccountColor = isShowAccountColor();

		holder.setAccountColorEnabled(showAccountColor);

		if (showAccountColor) {
			holder.setAccountColor(getAccountColor(mContext, accountId));
		}

		holder.setUserColor(getUserColor(mContext, conversationId));

		holder.setTextSize(getTextSize());
		holder.name.setText(name);
		holder.screen_name.setText("@" + screenName);
		holder.screen_name.setVisibility(View.VISIBLE);
		holder.text.setText(toPlainText(cursor.getString(IDX_TEXT)));
		holder.time.setTime(timestamp);
		holder.setIsOutgoing(isOutgoing);
		final boolean displayProfileImage = isDisplayProfileImage();
		holder.profile_image.setVisibility(displayProfileImage ? View.VISIBLE : View.GONE);
		if (displayProfileImage) {
			holder.profile_image.setTag(position);
			final String profile_image_url_string = cursor.getString(IDX_PROFILE_IMAGE_URL);
			mImageLoader.displayProfileImage(holder.profile_image, profile_image_url_string);
		} else {
            mImageLoader.cancelDisplayTask(holder.profile_image);
		}
		if (position > mMaxAnimationPosition) {
			if (mAnimationEnabled) {
				view.startAnimation(holder.item_animation);
			}
			mMaxAnimationPosition = position;
		}
		super.bindView(view, context, cursor);
	}

	public long getAccountId(final int position) {
		final Cursor c = getCursor();
		if (c == null || c.isClosed() || !c.moveToPosition(position))
			return -1;
		return c.getLong(IDX_ACCOUNT_ID);
	}

	public long getConversationId(final int position) {
		final Cursor c = getCursor();
		if (c == null || c.isClosed() || !c.moveToPosition(position))
			return -1;
		return c.getLong(IDX_CONVERSATION_ID);
	}

	public String getScreenName(final int position) {
		final Cursor c = getCursor();
		if (c == null || c.isClosed() || !c.moveToPosition(position))
			return null;
		return c.getString(IDX_SCREEN_NAME);
	}

	@Override
	public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
		final View view = super.newView(context, cursor, parent);
		final Object tag = view.getTag();
		if (!(tag instanceof DirectMessageEntryViewHolder)) {
			final DirectMessageEntryViewHolder holder = new DirectMessageEntryViewHolder(view);
			holder.profile_image.setOnClickListener(this);
			if (mPlainList) {
				((View) holder.content).setPadding(0, 0, 0, 0);
				holder.content.setItemBackground(null);
			}
			view.setTag(holder);
		}
		return view;
	}

	@Override
	public void onClick(final View view) {
		if (mMultiSelectManager.isActive()) return;
		final Object tag = view.getTag();
		final int position = tag instanceof Integer ? (Integer) tag : -1;
		if (position == -1) return;
		switch (view.getId()) {
			case R.id.profile_image: {
				if (mContext instanceof Activity) {
					final long account_id = getAccountId(position);
					final long user_id = getConversationId(position);
					final String screen_name = getScreenName(position);
					openUserProfile((Activity) mContext, account_id, user_id, screen_name);
				}
				break;
			}
//			case R.id.item_menu: {
//				if (position == -1 || mListener == null) return;
//				mListener.onMenuButtonClick(view, position, getItemId(position));
//				break;
//			}
		}
	}

	@Override
	public void setAnimationEnabled(final boolean anim) {
		if (mAnimationEnabled == anim) return;
		mAnimationEnabled = anim;
	}

	@Override
	public void setMaxAnimationPosition(final int position) {
		mMaxAnimationPosition = position;
	}

	@Override
	public void setMenuButtonClickListener(final MenuButtonClickListener listener) {
		//
	}

	private static int getItemResource(final boolean compactCards) {
		return compactCards ? R.layout.card_item_message_entry_compact : R.layout.card_item_message_entry;
	}
}
