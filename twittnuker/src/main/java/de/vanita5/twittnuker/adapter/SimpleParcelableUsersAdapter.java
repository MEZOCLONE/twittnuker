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
import android.view.View;
import android.view.ViewGroup;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.iface.IBaseAdapter;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.util.MediaLoaderWrapper;
import de.vanita5.twittnuker.util.UserColorNameManager;
import de.vanita5.twittnuker.view.holder.TwoLineWithIconViewHolder;

import java.util.List;

import static de.vanita5.twittnuker.util.Utils.configBaseAdapter;
import static de.vanita5.twittnuker.util.Utils.getUserTypeIconRes;

public class SimpleParcelableUsersAdapter extends BaseArrayAdapter<ParcelableUser> implements IBaseAdapter {

    private final MediaLoaderWrapper mImageLoader;
	private final Context mContext;
    private final UserColorNameManager mUserColorNameManager;

	public SimpleParcelableUsersAdapter(final Context context) {
        this(context, R.layout.list_item_user);
    }

    public SimpleParcelableUsersAdapter(final Context context, final int layoutRes) {
        super(context, layoutRes);
		mContext = context;
		final TwittnukerApplication app = TwittnukerApplication.getInstance(context);
        mImageLoader = app.getMediaLoaderWrapper();
        mUserColorNameManager = app.getUserColorNameManager();
		configBaseAdapter(context, this);
	}

	@Override
	public long getItemId(final int position) {
		return getItem(position) != null ? getItem(position).id : -1;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		final View view = super.getView(position, convertView, parent);
		final Object tag = view.getTag();
		final TwoLineWithIconViewHolder holder;
		if (tag instanceof TwoLineWithIconViewHolder) {
			holder = (TwoLineWithIconViewHolder) tag;
		} else {
			holder = new TwoLineWithIconViewHolder(view);
			view.setTag(holder);
		}

		final ParcelableUser user = getItem(position);

		holder.text1.setCompoundDrawablesWithIntrinsicBounds(0, 0,
				getUserTypeIconRes(user.is_verified, user.is_protected), 0);
		holder.text1.setText(user.name);
		holder.text2.setText("@" + user.screen_name);
        holder.icon.setVisibility(isProfileImageDisplayed() ? View.VISIBLE : View.GONE);
        if (isProfileImageDisplayed()) {
            mImageLoader.displayProfileImage(holder.icon, user.profile_image_url);
        } else {
            mImageLoader.cancelDisplayTask(holder.icon);
		}
		return view;
	}

	public void setData(final List<ParcelableUser> data) {
		setData(data, false);
	}

    public void setData(final List<ParcelableUser> data, final boolean clearOld) {
        if (clearOld) {
			clear();
		}
		if (data == null) return;
		for (final ParcelableUser user : data) {
            if (clearOld || findItemPosition(user.id) < 0) {
				add(user);
			}
		}
	}

}