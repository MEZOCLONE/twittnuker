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

package de.vanita5.twittnuker.view.holder;

import android.content.Context;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.iface.ContentCardClickListener;
import de.vanita5.twittnuker.adapter.iface.IUserListsAdapter;
import de.vanita5.twittnuker.model.ParcelableUserList;
import de.vanita5.twittnuker.util.MediaLoaderWrapper;
import de.vanita5.twittnuker.util.UserColorNameManager;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.ShapedImageView;
import de.vanita5.twittnuker.view.iface.IColorLabelView;

import java.util.Locale;

public class UserListViewHolder extends ViewHolder implements View.OnClickListener, View.OnLongClickListener {

	private final IUserListsAdapter<?> adapter;

	private final IColorLabelView itemContent;
	private final ShapedImageView profileImageView;
	private final TextView nameView;
	private final TextView createdByView;
    private final TextView descriptionView;
    private final TextView membersCountView;
    private final TextView subscribersCountView;

	private UserListClickListener userListClickListener;

	public UserListViewHolder(IUserListsAdapter<?> adapter, View itemView) {
		super(itemView);
		itemContent = (IColorLabelView) itemView.findViewById(R.id.item_content);
		this.adapter = adapter;
		profileImageView = (ShapedImageView) itemView.findViewById(R.id.profile_image);
		nameView = (TextView) itemView.findViewById(R.id.name);
		createdByView = (TextView) itemView.findViewById(R.id.created_by);
        descriptionView = (TextView) itemView.findViewById(R.id.description);
        membersCountView = (TextView) itemView.findViewById(R.id.members_count);
        subscribersCountView = (TextView) itemView.findViewById(R.id.subscribers_count);
	}

	public void displayUserList(ParcelableUserList userList) {

		final Context context = adapter.getContext();
		final MediaLoaderWrapper loader = adapter.getMediaLoader();
		final UserColorNameManager manager = adapter.getUserColorNameManager();

		itemContent.drawStart(manager.getUserColor(userList.user_id, false));
		nameView.setText(userList.name);
		final boolean nameFirst = adapter.isNameFirst();
		final String createdByDisplayName = manager.getDisplayName(userList, nameFirst, false);
		createdByView.setText(context.getString(R.string.created_by, createdByDisplayName));

		if (adapter.isProfileImageEnabled()) {
			profileImageView.setVisibility(View.VISIBLE);
			loader.displayProfileImage(profileImageView, userList.user_profile_image_url);
		} else {
			profileImageView.setVisibility(View.GONE);
			loader.cancelDisplayTask(profileImageView);
		}
        descriptionView.setVisibility(TextUtils.isEmpty(userList.description) ? View.GONE : View.VISIBLE);
        descriptionView.setText(userList.description);
        membersCountView.setText(Utils.getLocalizedNumber(Locale.getDefault(), userList.members_count));
        subscribersCountView.setText(Utils.getLocalizedNumber(Locale.getDefault(), userList.subscribers_count));
	}

	public void setOnClickListeners() {
		setUserListClickListener(adapter);
	}

	@Override
	public void onClick(View v) {
		if (userListClickListener == null) return;
		switch (v.getId()) {
			case R.id.item_content: {
				userListClickListener.onUserListClick(this, getLayoutPosition());
				break;
			}
		}
	}

	@Override
	public boolean onLongClick(View v) {
		if (userListClickListener == null) return false;
		switch (v.getId()) {
			case R.id.item_content: {
				return userListClickListener.onUserListLongClick(this, getLayoutPosition());
			}
		}
		return false;
	}

	public void setUserListClickListener(UserListClickListener listener) {
		userListClickListener = listener;
		((View) itemContent).setOnClickListener(this);
		((View) itemContent).setOnLongClickListener(this);
	}

	public void setupViewOptions() {
		setTextSize(adapter.getTextSize());
		profileImageView.setStyle(adapter.getProfileImageStyle());
	}

	public void setTextSize(final float textSize) {
		nameView.setTextSize(textSize);
		createdByView.setTextSize(textSize * 0.75f);
	}


    public interface UserListClickListener extends ContentCardClickListener {

		void onUserListClick(UserListViewHolder holder, int position);

		boolean onUserListLongClick(UserListViewHolder holder, int position);

	}
}