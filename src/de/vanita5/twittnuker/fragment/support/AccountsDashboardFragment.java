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

import static de.vanita5.twittnuker.util.Utils.getDisplayName;
import static de.vanita5.twittnuker.util.Utils.openUserFavorites;
import static de.vanita5.twittnuker.util.Utils.openUserListMemberships;
import static de.vanita5.twittnuker.util.Utils.openUserLists;
import static de.vanita5.twittnuker.util.Utils.openUserProfile;
import static de.vanita5.twittnuker.util.Utils.openUserTimeline;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;

import java.util.ArrayList;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.FiltersActivity;
import de.vanita5.twittnuker.activity.iface.IThemedActivity;
import de.vanita5.twittnuker.activity.support.AccountsManagerActivity;
import de.vanita5.twittnuker.activity.support.HomeActivity;
import de.vanita5.twittnuker.activity.SettingsActivity;
import de.vanita5.twittnuker.activity.support.DraftsActivity;
import de.vanita5.twittnuker.activity.support.UserProfileEditorActivity;
import de.vanita5.twittnuker.adapter.ArrayAdapter;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.model.Account;
import de.vanita5.twittnuker.provider.TweetStore.Accounts;
import de.vanita5.twittnuker.util.ImageLoaderWrapper;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.content.SupportFragmentReloadCursorObserver;
import de.vanita5.twittnuker.view.iface.IColorLabelView;

public class AccountsDashboardFragment extends BaseSupportListFragment implements LoaderCallbacks<Cursor>,
		OnSharedPreferenceChangeListener, OnAccountActivateStateChangeListener {

	private final SupportFragmentReloadCursorObserver mReloadContentObserver = new SupportFragmentReloadCursorObserver(
			this, 0, this);

	private ContentResolver mResolver;
	private SharedPreferences mPreferences;
	private MergeAdapter mAdapter;

	private DrawerAccountsAdapter mAccountsAdapter;
	private AccountOptionsAdapter mAccountOptionsAdapter;
	private AppMenuAdapter mAppMenuAdapter;

	private TextView mAccountsSectionView, mAccountOptionsSectionView, mAppMenuSectionView;

	private Context mThemedContext;

	@Override
	public void onAccountActivateStateChanged(final Account account, final boolean activated) {
		final ContentValues values = new ContentValues();
		values.put(Accounts.IS_ACTIVATED, activated);
		final String where = Accounts.ACCOUNT_ID + " = " + account.account_id;
		mResolver.update(Accounts.CONTENT_URI, values, where, null);
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		mResolver = getContentResolver();
		final Context context = getView().getContext();
		mAdapter = new MergeAdapter();
		mAccountsAdapter = new DrawerAccountsAdapter(context);
		mAccountOptionsAdapter = new AccountOptionsAdapter(context);
		mAppMenuAdapter = new AppMenuAdapter(context);
		mAccountsSectionView = newSectionView(context, R.string.accounts);
		mAccountOptionsSectionView = newSectionView(context, 0);
		mAppMenuSectionView = newSectionView(context, R.string.more);
		mAccountsAdapter.setOnAccountActivateStateChangeListener(this);
		mAdapter.addView(mAccountsSectionView, false);
		mAdapter.addAdapter(mAccountsAdapter);
		mAdapter.addView(mAccountOptionsSectionView, false);
		mAdapter.addAdapter(mAccountOptionsAdapter);
		mAdapter.addView(mAppMenuSectionView, false);
		mAdapter.addAdapter(mAppMenuAdapter);
		setListAdapter(mAdapter);
		mPreferences.registerOnSharedPreferenceChangeListener(this);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
    protected void fitSystemWindows(Rect insets) {
        // No-op
    }

    @Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
			case REQUEST_SETTINGS: {
				if (data == null) return;
				final FragmentActivity activity = getActivity();
				if (data.getBooleanExtra(EXTRA_RESTART_ACTIVITY, false) && activity instanceof IThemedActivity) {
					((IThemedActivity) activity).restart();
		}
				return;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        return new CursorLoader(getActivity(), Accounts.CONTENT_URI, Accounts.COLUMNS, null, null, Accounts.SORT_POSITION);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		return LayoutInflater.from(getThemedContext()).inflate(R.layout.fragment_accounts_drawer, container, false);
	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int position, final long id) {
		final ListAdapter adapter = mAdapter.getAdapter(position);
		final Object item = mAdapter.getItem(position);
		if (adapter instanceof DrawerAccountsAdapter) {
			if (!(item instanceof Account)) return;
			final Account account = (Account) item;
			mAccountsAdapter.setSelectedAccountId(account.account_id);
			updateAccountOptionsSeparatorLabel();
			updateDefaultAccountState();
		} else if (adapter instanceof AccountOptionsAdapter) {
			final Account account = mAccountsAdapter.getSelectedAccount();
			if (account == null || !(item instanceof OptionItem)) return;
			final OptionItem option = (OptionItem) item;
			switch (option.id) {
				case MENU_VIEW_PROFILE: {
					openUserProfile(getActivity(), account.account_id, account.account_id, account.screen_name);
					closeAccountsDrawer();
					break;
				}
				case MENU_SEARCH: {
					final FragmentActivity a = getActivity();
					if (a instanceof HomeActivity) {
						((HomeActivity) a).openSearchView(account);
					} else {
						getActivity().onSearchRequested();
					}
					closeAccountsDrawer();
					break;
				}
				case MENU_STATUSES: {
					openUserTimeline(getActivity(), account.account_id, account.account_id, account.screen_name);
					closeAccountsDrawer();
					break;
				}
				case MENU_FAVORITES: {
					openUserFavorites(getActivity(), account.account_id, account.account_id, account.screen_name);
					closeAccountsDrawer();
					break;
				}
				case MENU_LISTS: {
					openUserLists(getActivity(), account.account_id, account.account_id, account.screen_name);
					closeAccountsDrawer();
					break;
				}
				case MENU_LIST_MEMBERSHIPS: {
					openUserListMemberships(getActivity(), account.account_id, account.account_id, account.screen_name);
					closeAccountsDrawer();
					break;
					}
				case MENU_EDIT: {
					final Bundle bundle = new Bundle();
					bundle.putLong(EXTRA_ACCOUNT_ID, account.account_id);
					final Intent intent = new Intent(INTENT_ACTION_EDIT_USER_PROFILE);
					intent.setClass(getActivity(), UserProfileEditorActivity.class);
					intent.putExtras(bundle);
					startActivity(intent);
					closeAccountsDrawer();
					break;
				}
			}
		} else if (adapter instanceof AppMenuAdapter) {
			if (!(item instanceof OptionItem)) return;
			final OptionItem option = (OptionItem) item;
			switch (option.id) {
                case MENU_ACCOUNTS: {
                    final Intent intent = new Intent(getActivity(), AccountsManagerActivity.class);
					startActivity(intent);
					break;
				}
				case MENU_DRAFTS: {
					final Intent intent = new Intent(INTENT_ACTION_DRAFTS);
					intent.setClass(getActivity(), DraftsActivity.class);
					startActivity(intent);
					break;
				}
				case MENU_FILTERS: {
					final Intent intent = new Intent(getActivity(), FiltersActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(intent);
					break;
				}
				case MENU_SETTINGS: {
					final Intent intent = new Intent(getActivity(), SettingsActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivityForResult(intent, REQUEST_SETTINGS);
					break;
				}
			}
			closeAccountsDrawer();
		}
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		mAccountsAdapter.changeCursor(null);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
		if (data != null && data.getCount() > 0 && mAccountsAdapter.getSelectedAccountId() <= 0) {
			data.moveToFirst();
			mAccountsAdapter.setSelectedAccountId(data.getLong(data.getColumnIndex(Accounts.ACCOUNT_ID)));
		}
		mAccountsAdapter.changeCursor(data);
		updateAccountOptionsSeparatorLabel();
		updateDefaultAccountState();
	}

	@Override
	public void onResume() {
		super.onResume();
		mAccountsAdapter.setDefaultAccountId(mPreferences.getLong(KEY_DEFAULT_ACCOUNT_ID, -1));
		updateDefaultAccountState();
	}

	@Override
	public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
		if (KEY_DEFAULT_ACCOUNT_ID.equals(key)) {
			mAccountsAdapter.setDefaultAccountId(mPreferences.getLong(KEY_DEFAULT_ACCOUNT_ID, -1));
			updateDefaultAccountState();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		final ContentResolver resolver = getContentResolver();
		resolver.registerContentObserver(Accounts.CONTENT_URI, true, mReloadContentObserver);
		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public void onStop() {
		final ContentResolver resolver = getContentResolver();
		resolver.unregisterContentObserver(mReloadContentObserver);
		super.onStop();
	}

	private void closeAccountsDrawer() {
		final Activity activity = getActivity();
		if (activity instanceof HomeActivity) {
			((HomeActivity) activity).closeAccountsDrawer();
		}
	}

	private Context getThemedContext() {
		if (mThemedContext != null) return mThemedContext;
		final Context context = getActivity();
		final int themeResource = ThemeUtils.getDrawerThemeResource(context);
        return mThemedContext = new ContextThemeWrapper(context, themeResource);
	}

	private void updateAccountOptionsSeparatorLabel() {
		final Account account = mAccountsAdapter.getSelectedAccount();
		if (account != null) {

			final String displayName = getDisplayName(account.name, account.screen_name);
			mAccountOptionsSectionView.setText(displayName);
		} else {
			mAccountOptionsSectionView.setText(null);
		}
	}

	private void updateDefaultAccountState() {
		final long defaultAccountId = mAccountsAdapter.getDefaultAccountId();
		final long selectedAccountId = mAccountsAdapter.getSelectedAccountId();
	}

	private static TextView newSectionView(final Context context, final int titleRes) {
		final TextView textView = new TextView(context, null, android.R.attr.listSeparatorTextViewStyle);
		if (titleRes != 0) {
			textView.setText(titleRes);
		}
		return textView;
	}


	private static final class AccountOptionsAdapter extends OptionItemsAdapter {

        private static final ArrayList<OptionItem> sOptions = new ArrayList<>();

		static {
            sOptions.add(new OptionItem(R.string.view_user_profile, R.drawable.ic_action_profile, MENU_VIEW_PROFILE));
            sOptions.add(new OptionItem(android.R.string.search_go, R.drawable.ic_action_search, MENU_SEARCH));
            sOptions.add(new OptionItem(R.string.statuses, R.drawable.ic_action_quote, MENU_STATUSES));
            sOptions.add(new OptionItem(R.string.favorites, R.drawable.ic_action_star, MENU_FAVORITES));
            sOptions.add(new OptionItem(R.string.users_lists, R.drawable.ic_action_list, MENU_LISTS));
            sOptions.add(new OptionItem(R.string.lists_following_me, R.drawable.ic_action_list,
					MENU_LIST_MEMBERSHIPS));
		}

		public AccountOptionsAdapter(final Context context) {
			super(context);
			clear();
			addAll(sOptions);
		}

	}

	private static final class AppMenuAdapter extends OptionItemsAdapter {

		public AppMenuAdapter(final Context context) {
			super(context);
            add(new OptionItem(R.string.accounts, R.drawable.ic_action_accounts, MENU_ACCOUNTS));
            add(new OptionItem(R.string.drafts, R.drawable.ic_action_draft, MENU_DRAFTS));
            add(new OptionItem(R.string.filters, R.drawable.ic_action_speaker_muted, MENU_FILTERS));
            add(new OptionItem(R.string.settings, R.drawable.ic_action_settings, MENU_SETTINGS));
		}

	}

	private static class DrawerAccountsAdapter extends SimpleCursorAdapter implements Constants,
			OnCheckedChangeListener {

		private final ImageLoaderWrapper mImageLoader;
        private final int mActivatedColor;

		private Account.Indices mIndices;
		private long mSelectedAccountId, mDefaultAccountId;

		private OnAccountActivateStateChangeListener mOnAccountActivateStateChangeListener;

		public DrawerAccountsAdapter(final Context context) {
            super(context, R.layout.list_item_drawer_account, null, new String[0], new int[0], 0);
			final TwittnukerApplication app = TwittnukerApplication.getInstance(context);
			mImageLoader = app.getImageLoaderWrapper();
            mActivatedColor = ThemeUtils.getUserAccentColor(context);
		}

		@Override
        public void bindView(@NonNull final View view, final Context context, @NonNull final Cursor cursor) {
			super.bindView(view, context, cursor);
			final CompoundButton toggle = (CompoundButton) view.findViewById(R.id.toggle);
			final TextView name = (TextView) view.findViewById(R.id.name);
            final TextView screenNameView = (TextView) view.findViewById(R.id.screen_name);
            final TextView defaultIndicatorView = (TextView) view.findViewById(R.id.default_indicator);
            final ImageView profileImageView = (ImageView) view.findViewById(R.id.profile_image);
			final Account account = new Account(cursor, mIndices);
			name.setText(account.name);
            screenNameView.setText(String.format("@%s", account.screen_name));
            defaultIndicatorView.setVisibility(account.account_id == mDefaultAccountId ? View.VISIBLE : View.GONE);
            mImageLoader.displayProfileImage(profileImageView, account.profile_image_url);
			toggle.setChecked(account.is_activated);
			toggle.setTag(account);
			toggle.setOnCheckedChangeListener(this);
			view.setActivated(account.account_id == mSelectedAccountId);
            final IColorLabelView colorLabelView = (IColorLabelView) view;
            colorLabelView.drawStart(account.account_id == mSelectedAccountId ? mActivatedColor : 0);
            colorLabelView.drawEnd(account.color);
		}

		public long getDefaultAccountId() {
			return mDefaultAccountId;
		}

		@Override
		public Account getItem(final int position) {
			final Cursor c = getCursor();
			if (c == null || c.isClosed() || !c.moveToPosition(position)) return null;
			return new Account(c, mIndices);
		}

		public Account getSelectedAccount() {
			final Cursor c = getCursor();
			if (c == null || c.isClosed() || !c.moveToFirst() || mIndices == null) return null;
			while (!c.isAfterLast()) {
				if (mSelectedAccountId == c.getLong(mIndices.account_id)) return new Account(c, mIndices);
				c.moveToNext();
			}
			return null;
		}

		public long getSelectedAccountId() {
			return mSelectedAccountId;
		}

		@Override
		public boolean isEnabled(final int position) {
			final Cursor c = getCursor();
			if (c == null || c.isClosed() || !c.moveToPosition(position)) return false;
			return c.getLong(mIndices.account_id) != mSelectedAccountId;
		}

		@Override
		public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
			final Account account = (Account) buttonView.getTag();
			if (mOnAccountActivateStateChangeListener != null) {
				mOnAccountActivateStateChangeListener.onAccountActivateStateChanged(account, isChecked);
			}
		}

		public void setDefaultAccountId(final long account_id) {
			if (mDefaultAccountId == account_id) return;
			mDefaultAccountId = account_id;
			notifyDataSetChanged();
		}

		public void setOnAccountActivateStateChangeListener(final OnAccountActivateStateChangeListener listener) {
			mOnAccountActivateStateChangeListener = listener;
		}

		public void setSelectedAccountId(final long account_id) {
			if (mSelectedAccountId == account_id) return;
			mSelectedAccountId = account_id;
			notifyDataSetChanged();
		}

		@Override
		public Cursor swapCursor(final Cursor c) {
			final Cursor old = super.swapCursor(c);
			mIndices = c != null ? new Account.Indices(c) : null;
			return old;
		}

	}

	private static class OptionItem {

		private final int name, icon, id;

		OptionItem(final int name, final int icon, final int id) {
			this.name = name;
			this.icon = icon;
			this.id = id;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (!(obj instanceof OptionItem)) return false;
			final OptionItem other = (OptionItem) obj;
			if (icon != other.icon) return false;
			if (id != other.id) return false;
			if (name != other.name) return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + icon;
			result = prime * result + id;
			result = prime * result + name;
			return result;
		}

		@Override
		public String toString() {
			return "AccountOption{name=" + name + ", icon=" + icon + ", id=" + id + "}";
		}

	}

	private static abstract class OptionItemsAdapter extends ArrayAdapter<OptionItem> {

        private final int mActionIconColor;

		public OptionItemsAdapter(final Context context) {
			super(context, R.layout.list_item_menu);
            mActionIconColor = ThemeUtils.getThemeForegroundColor(context);
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			final View view = super.getView(position, convertView, parent);
			final OptionItem option = getItem(position);
			final TextView text1 = (TextView) view.findViewById(android.R.id.text1);
			final ImageView icon = (ImageView) view.findViewById(android.R.id.icon);
			text1.setText(option.name);
            icon.setImageDrawable(icon.getResources().getDrawable(option.icon));
            icon.setColorFilter(mActionIconColor, Mode.SRC_ATOP);
			return view;
		}

	}
}

interface OnAccountActivateStateChangeListener {
	void onAccountActivateStateChanged(Account account, boolean activated);
}
