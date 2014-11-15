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

package de.vanita5.twittnuker.activity.support;

import static de.vanita5.twittnuker.util.Utils.createFragmentForIntent;
import static de.vanita5.twittnuker.util.Utils.matchLinkId;

import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.iface.IControlBarActivity;
import de.vanita5.twittnuker.fragment.iface.IBaseFragment;
import de.vanita5.twittnuker.fragment.iface.IBaseFragment.SystemWindowsInsetsCallback;
import de.vanita5.twittnuker.fragment.iface.IBasePullToRefreshFragment;
import de.vanita5.twittnuker.fragment.iface.RefreshScrollTopInterface;
import de.vanita5.twittnuker.fragment.iface.SupportFragmentCallback;
import de.vanita5.twittnuker.util.FlymeUtils;
import de.vanita5.twittnuker.util.MultiSelectEventHandler;

public class LinkHandlerActivity extends BaseSupportActivity implements OnClickListener,
        OnLongClickListener, SystemWindowsInsetsCallback, IControlBarActivity {

	private MultiSelectEventHandler mMultiSelectHandler;

	private boolean mFinishOnly;

	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
			case R.id.go_top: {
                final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_fragment);
				if (fragment instanceof RefreshScrollTopInterface) {
					((RefreshScrollTopInterface) fragment).scrollToStart();
				} else if (fragment instanceof ListFragment) {
					((ListFragment) fragment).setSelection(0);
				}
				break;
			}
		}
	}

	@Override
	public boolean onLongClick(final View v) {
		switch (v.getId()) {
			case R.id.go_top: {
                final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_fragment);
				if (fragment instanceof RefreshScrollTopInterface) {
					((RefreshScrollTopInterface) fragment).triggerRefresh();
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case MENU_HOME: {
				if (mFinishOnly) {
					finish();
				} else {
					navigateUpFromSameTask();
				}
				break;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected IBasePullToRefreshFragment getCurrentPullToRefreshFragment() {
		final Fragment fragment = getSupportFragmentManager().findFragmentById(android.R.id.content);
		if (fragment instanceof IBasePullToRefreshFragment)
			return (IBasePullToRefreshFragment) fragment;
		else if (fragment instanceof SupportFragmentCallback) {
			final Fragment curr = ((SupportFragmentCallback) fragment).getCurrentVisibleFragment();
			if (curr instanceof IBasePullToRefreshFragment) return (IBasePullToRefreshFragment) curr;
		}
		return null;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		mMultiSelectHandler = new MultiSelectEventHandler(this);
		mMultiSelectHandler.dispatchOnCreate();
		final Intent intent = getIntent();
		final Uri data = intent.getData();
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setUiOptions(getWindow(), data);
		super.onCreate(savedInstanceState);
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.activity_content_fragment);
		setProgressBarIndeterminateVisibility(false);
		if (data == null || !showFragment(data)) {
			finish();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		mMultiSelectHandler.dispatchOnStart();
	}

	@Override
	protected void onStop() {
		mMultiSelectHandler.dispatchOnStop();
		super.onStop();
	}

	@Override
    public void fitSystemWindows(Rect insets) {
        super.fitSystemWindows(insets);
        final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_fragment);
        if (fragment instanceof IBaseFragment) {
            ((IBaseFragment) fragment).requestFitSystemWindows();
        }
	}

	private void setUiOptions(final Window window, final Uri data) {
		if (FlymeUtils.hasSmartBar()) {
			window.setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
		} else {
			window.setUiOptions(0);
		}
	}

	private boolean showFragment(final Uri uri) {
		final Intent intent = getIntent();
		intent.setExtrasClassLoader(getClassLoader());
		final Fragment fragment = createFragmentForIntent(this, intent);
		if (uri == null || fragment == null) return false;
		switch (matchLinkId(uri)) {
			case LINK_ID_STATUS: {
				setTitle(R.string.status);
				break;
			}
			case LINK_ID_USER: {
				setTitle(R.string.user);
				break;
			}
			case LINK_ID_USER_TIMELINE: {
				setTitle(R.string.statuses);
				break;
			}
			case LINK_ID_USER_FAVORITES: {
				setTitle(R.string.favorites);
				break;
			}
			case LINK_ID_USER_FOLLOWERS: {
				setTitle(R.string.followers);
				break;
			}
			case LINK_ID_USER_FRIENDS: {
				setTitle(R.string.action_following);
				break;
			}
			case LINK_ID_USER_BLOCKS: {
				setTitle(R.string.blocked_users);
				break;
			}
			case LINK_ID_MUTES_USERS: {
				setTitle(R.string.twitter_muted_users);
				break;
			}
			case LINK_ID_DIRECT_MESSAGES_CONVERSATION: {
				setTitle(R.string.direct_messages);
				break;
			}
			case LINK_ID_USER_LIST: {
				setTitle(R.string.user_list);
				break;
			}
			case LINK_ID_USER_LISTS: {
				setTitle(R.string.user_lists);
				break;
			}
			case LINK_ID_USER_LIST_TIMELINE: {
				setTitle(R.string.list_timeline);
				break;
			}
			case LINK_ID_USER_LIST_MEMBERS: {
				setTitle(R.string.list_members);
				break;
			}
			case LINK_ID_USER_LIST_SUBSCRIBERS: {
				setTitle(R.string.list_subscribers);
				break;
			}
			case LINK_ID_USER_LIST_MEMBERSHIPS: {
				setTitle(R.string.lists_following_user);
				break;
			}
			case LINK_ID_SAVED_SEARCHES: {
				setTitle(R.string.saved_searches);
				break;
			}
			case LINK_ID_USER_MENTIONS: {
				setTitle(R.string.user_mentions);
				break;
			}
			case LINK_ID_INCOMING_FRIENDSHIPS: {
				setTitle(R.string.incoming_friendships);
				break;
			}
			case LINK_ID_USERS: {
				setTitle(R.string.users);
				break;
			}
			case LINK_ID_STATUSES: {
				setTitle(R.string.statuses);
				break;
			}
            case LINK_ID_USER_MEDIA_TIMELINE: {
                setTitle(R.string.medias);
                break;
            }
			case LINK_ID_STATUS_RETWEETERS: {
				setTitle(R.string.users_retweeted_this);
				break;
			}
			case LINK_ID_STATUS_FAVORITERS: {
				setTitle(R.string.users_retweeted_this);
				break;
			}
			case LINK_ID_STATUS_REPLIES: {
                setTitle(R.string.view_replies);
				break;
			}
			case LINK_ID_SEARCH: {
				setTitle(android.R.string.search_go);
//                setSubtitle(uri.getQueryParameter(QUERY_PARAM_QUERY));
				break;
			}
		}
		mFinishOnly = Boolean.parseBoolean(uri.getQueryParameter(QUERY_PARAM_FINISH_ONLY));
		final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.content_fragment, fragment);
		ft.commit();
		return true;
	}

    @Override
    public void setControlBarOffset(float offset) {

    }

    @Override
    public float getControlBarOffset() {
        return 0;
    }

    @Override
    public int getControlBarHeight() {
        final ActionBar actionBar = getActionBar();
        return actionBar != null ? actionBar.getHeight() : 0;
    }

    @Override
    public void moveControlBarBy(float delta) {

    }
}
