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

import static de.vanita5.twittnuker.util.Utils.configBaseCardAdapter;
import static de.vanita5.twittnuker.util.Utils.getDisplayName;
import static de.vanita5.twittnuker.util.Utils.getLocalizedNumber;
import static de.vanita5.twittnuker.util.Utils.openUserProfile;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.iface.IBaseCardAdapter;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.model.ParcelableUserList;
import de.vanita5.twittnuker.util.ImageLoaderWrapper;
import de.vanita5.twittnuker.util.MultiSelectManager;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.holder.UserListViewHolder;
import de.vanita5.twittnuker.view.iface.ICardItemView.OnOverflowIconClickListener;

import java.util.List;
import java.util.Locale;

public class ParcelableUserListsAdapter extends BaseArrayAdapter<ParcelableUserList> implements IBaseCardAdapter,
		OnClickListener, OnOverflowIconClickListener {

	private final Context mContext;
    private final ImageLoaderWrapper mImageLoader;
	private final MultiSelectManager mMultiSelectManager;
	private final Locale mLocale;

	private MenuButtonClickListener mListener;

	private boolean mAnimationEnabled;
	private int mMaxAnimationPosition;
	private final boolean mPlainList;

	public ParcelableUserListsAdapter(final Context context) {
		this(context, Utils.isCompactCards(context), Utils.isPlainListStyle(context));
	}

	public ParcelableUserListsAdapter(final Context context, final boolean compactCards, final boolean plainList) {
		super(context, getItemResource(compactCards));
		mPlainList = plainList;
		mContext = context;
		mLocale = context.getResources().getConfiguration().locale;
		final TwittnukerApplication app = TwittnukerApplication.getInstance(context);
        mImageLoader = app.getImageLoaderWrapper();
		mMultiSelectManager = app.getMultiSelectManager();
		configBaseCardAdapter(context, this);
	}

	public void appendData(final List<ParcelableUserList> data) {
		setData(data, false);
	}

	@Override
	public long getItemId(final int position) {
		return getItem(position) != null ? getItem(position).id : -1;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		final View view = super.getView(position, convertView, parent);
		final Object tag = view.getTag();
		final UserListViewHolder holder;
		if (tag instanceof UserListViewHolder) {
			holder = (UserListViewHolder) tag;
		} else {
			holder = new UserListViewHolder(view);
			holder.profile_image.setOnClickListener(this);
			holder.content.setOnOverflowIconClickListener(this);
			if (mPlainList) {
				((View) holder.content).setPadding(0, 0, 0, 0);
				holder.content.setItemBackground(null);
			}
			view.setTag(holder);
		}

		holder.position = position;

		final ParcelableUserList user_list = getItem(position);
		final String display_name = getDisplayName(user_list.user_name, user_list.user_screen_name, isDisplayNameFirst());
		holder.setTextSize(getTextSize());
		holder.name.setText(user_list.name);
		holder.created_by.setText(mContext.getString(R.string.created_by, display_name));
		holder.description.setVisibility(TextUtils.isEmpty(user_list.description) ? View.GONE : View.VISIBLE);
		holder.description.setText(user_list.description);
		holder.members_count.setText(getLocalizedNumber(mLocale, user_list.members_count));
		holder.subscribers_count.setText(getLocalizedNumber(mLocale, user_list.subscribers_count));
		holder.profile_image.setVisibility(isDisplayProfileImage() ? View.VISIBLE : View.GONE);
		if (isDisplayProfileImage()) {
            mImageLoader.displayProfileImage(holder.profile_image, user_list.user_profile_image_url);
        } else {
            mImageLoader.cancelDisplayTask(holder.profile_image);
		}
		holder.profile_image.setTag(position);
		if (position > mMaxAnimationPosition) {
			if (mAnimationEnabled) {
				view.startAnimation(holder.item_animation);
			}
			mMaxAnimationPosition = position;
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
					final ParcelableUserList item = getItem(position);
					openUserProfile((Activity) mContext, item.account_id, item.user_id, item.user_screen_name);
				}
				break;
			}
		}
	}

	@Override
	public void onOverflowIconClick(final View view) {
		if (mMultiSelectManager.isActive()) return;
		final Object tag = view.getTag();
		if (tag instanceof UserListViewHolder) {
			final UserListViewHolder holder = (UserListViewHolder) tag;
			final int position = holder.position;
			if (position == -1 || mListener == null) return;
			mListener.onMenuButtonClick(view, position, getItemId(position));
		}
	}

	@Override
	public void setAnimationEnabled(final boolean anim) {
		if (mAnimationEnabled == anim) return;
		mAnimationEnabled = anim;
	}

	public void setData(final List<ParcelableUserList> data, final boolean clear_old) {
		if (clear_old) {
			clear();
		}
		if (data == null) return;
		for (final ParcelableUserList user : data) {
            if (clear_old || findItemPosition(user.id) < 0) {
				add(user);
			}
		}
	}

	@Override
	public void setMaxAnimationPosition(final int position) {
		mMaxAnimationPosition = position;
	}

	@Override
	public void setMenuButtonClickListener(final MenuButtonClickListener listener) {
		mListener = listener;
	}

	private static int getItemResource(final boolean compactCards) {
		return compactCards ? R.layout.card_item_user_list_compact : R.layout.card_item_user_list;
	}
}
