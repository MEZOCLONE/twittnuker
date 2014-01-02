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

import static de.vanita5.twittnuker.util.Utils.openStatus;
import static de.vanita5.twittnuker.util.Utils.openUserProfile;
import static de.vanita5.twittnuker.util.Utils.openUsers;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import de.vanita5.twittnuker.adapter.BaseParcelableActivitiesAdapter;
import de.vanita5.twittnuker.adapter.ParcelableActivitiesAboutMeAdapter;
import de.vanita5.twittnuker.loader.ActivitiesAboutMeLoader;
import de.vanita5.twittnuker.model.ParcelableActivity;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableUser;


import java.util.Arrays;
import java.util.List;

public class ActivitiesAboutMeFragment extends BaseActivitiesListFragment {

	@Override
	public BaseParcelableActivitiesAdapter createListAdapter(final Context context) {
		return new ParcelableActivitiesAboutMeAdapter(context);
	}

	@Override
	public Loader<List<ParcelableActivity>> onCreateLoader(final int id, final Bundle args) {
		setProgressBarIndeterminateVisibility(true);
		final long account_id = args != null ? args.getLong(EXTRA_ACCOUNT_ID, -1) : -1;
		return new ActivitiesAboutMeLoader(getActivity(), account_id, getData(), getSavedActivitiesFileArgs(),
				getTabPosition());
	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int position, final long id) {
		final int adapter_pos = position - l.getHeaderViewsCount();
		final ParcelableActivity item = getListAdapter().getItem(adapter_pos);
		if (item == null) return;
		final ParcelableUser[] sources = item.sources;
		final ParcelableStatus[] target_statuses = item.target_statuses;
		final int sources_length = sources != null ? sources.length : 0;
		if (sources_length > 0) {
			final ParcelableStatus[] target_objects = item.target_object_statuses;
			switch (item.action) {
				case ParcelableActivity.ACTION_FAVORITE: {
					if (sources_length == 1) {
						openUserProfile(getActivity(), sources[0]);
					} else {
						final List<ParcelableUser> users = Arrays.asList(sources);
						openUsers(getActivity(), users);
					}
					break;
				}
				case ParcelableActivity.ACTION_FOLLOW: {
					if (sources_length == 1) {
						openUserProfile(getActivity(), sources[0]);
					} else {
						final List<ParcelableUser> users = Arrays.asList(sources);
						openUsers(getActivity(), users);
					}
					break;
				}
				case ParcelableActivity.ACTION_MENTION: {
					if (target_objects != null && target_objects.length > 0) {
						openStatus(getActivity(), target_objects[0]);
					}
					break;
				}
				case ParcelableActivity.ACTION_REPLY: {
					if (target_statuses != null && target_statuses.length > 0) {
						openStatus(getActivity(), target_statuses[0]);
					}
					break;
				}
				case ParcelableActivity.ACTION_RETWEET: {
					if (sources_length == 1) {
						openUserProfile(getActivity(), sources[0]);
					} else {
						final List<ParcelableUser> users = Arrays.asList(sources);
						openUsers(getActivity(), users);
					}
					break;
				}
			}
		}
	}

	@Override
	protected String[] getSavedActivitiesFileArgs() {
		final Bundle args = getArguments();
		if (args == null) return null;
		final long account_id = args.getLong(EXTRA_ACCOUNT_ID, -1);
		return new String[] { AUTHORITY_ACTIVITIES_ABOUT_ME, "account" + account_id };
	}

}
