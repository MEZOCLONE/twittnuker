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

package de.vanita5.twittnuker.loader.support;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import org.mariotaku.querybuilder.Expression;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.model.ParcelableUser.CachedIndices;
import de.vanita5.twittnuker.model.SingleResponse;
import de.vanita5.twittnuker.provider.TwidereDataStore.Accounts;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedUsers;
import de.vanita5.twittnuker.util.TwitterAPIFactory;
import de.vanita5.twittnuker.util.TwitterWrapper;

import de.vanita5.twittnuker.api.twitter.Twitter;
import de.vanita5.twittnuker.api.twitter.TwitterException;
import de.vanita5.twittnuker.api.twitter.model.User;

import static de.vanita5.twittnuker.util.ContentValuesCreator.createCachedUser;
import static de.vanita5.twittnuker.util.Utils.isMyAccount;

public final class ParcelableUserLoader extends AsyncTaskLoader<SingleResponse<ParcelableUser>> implements Constants {

    private final boolean mOmitIntentExtra, mLoadFromCache;
    private final Bundle mExtras;
    private final long mAccountId, mUserId;
    private final String mScreenName;

    public ParcelableUserLoader(final Context context, final long accountId, final long userId,
                                final String screenName, final Bundle extras, final boolean omitIntentExtra,
                                final boolean loadFromCache) {
		super(context);
        this.mOmitIntentExtra = omitIntentExtra;
        this.mLoadFromCache = loadFromCache;
        this.mExtras = extras;
        this.mAccountId = accountId;
        this.mUserId = userId;
        this.mScreenName = screenName;
	}

	@Override
	public SingleResponse<ParcelableUser> loadInBackground() {
        final Context context = getContext();
        final ContentResolver resolver = context.getContentResolver();
        if (!mOmitIntentExtra && mExtras != null) {
            final ParcelableUser user = mExtras.getParcelable(EXTRA_USER);
			if (user != null) {
				final ContentValues values = ParcelableUser.makeCachedUserContentValues(user);
				resolver.insert(CachedUsers.CONTENT_URI, values);
                return SingleResponse.getInstance(user);
			}
		}
        final Twitter twitter = TwitterAPIFactory.getTwitterInstance(context, mAccountId, true);
        if (twitter == null) return SingleResponse.getInstance();
        if (mLoadFromCache) {
            final Expression where;
            final String[] whereArgs;
            if (mUserId > 0) {
                where = Expression.equals(CachedUsers.USER_ID, mUserId);
                whereArgs = null;
            } else {
                where = Expression.equalsArgs(CachedUsers.SCREEN_NAME);
                whereArgs = new String[]{mScreenName};
            }
            final Cursor cur = resolver.query(CachedUsers.CONTENT_URI, CachedUsers.COLUMNS,
                    where.getSQL(), whereArgs, null);
			final int count = cur.getCount();
			try {
				if (count > 0) {
                    final CachedIndices indices = new CachedIndices(cur);
					cur.moveToFirst();
                    return SingleResponse.getInstance(new ParcelableUser(cur, indices, mAccountId));
				}
			} finally {
				cur.close();
			}
		}
		try {
            final User user = TwitterWrapper.tryShowUser(twitter, mUserId, mScreenName);
            final ContentValues cachedUserValues = createCachedUser(user);
            final long userId = user.getId();
            resolver.insert(CachedUsers.CONTENT_URI, cachedUserValues);
            final ParcelableUser result = new ParcelableUser(user, mAccountId);
            if (isMyAccount(context, userId)) {
                final ContentValues accountValues = new ContentValues();
                accountValues.put(Accounts.NAME, result.name);
                accountValues.put(Accounts.SCREEN_NAME, result.screen_name);
                accountValues.put(Accounts.PROFILE_IMAGE_URL, result.profile_image_url);
                accountValues.put(Accounts.PROFILE_BANNER_URL, result.profile_banner_url);
                final String accountWhere = Expression.equals(Accounts.ACCOUNT_ID, userId).getSQL();
                resolver.update(Accounts.CONTENT_URI, accountValues, accountWhere, null);
            }
            return SingleResponse.getInstance(result);
		} catch (final TwitterException e) {
            Log.w(LOGTAG, e);
            return SingleResponse.getInstance(e);
		}
	}

	@Override
	protected void onStartLoading() {
		forceLoad();
	}

}