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

package de.vanita5.twittnuker.provider;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.InboxStyle;
import android.support.v4.util.LongSparseArray;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;

import com.squareup.okhttp.internal.Network;
import com.squareup.otto.Bus;

import org.apache.commons.lang3.ArrayUtils;
import org.mariotaku.querybuilder.Columns.Column;
import org.mariotaku.querybuilder.Expression;
import org.mariotaku.querybuilder.RawItemArray;
import org.mariotaku.querybuilder.query.SQLSelectQuery;

import de.vanita5.twittnuker.BuildConfig;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.support.HomeActivity;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.model.AccountPreferences;
import de.vanita5.twittnuker.model.NotificationContent;
import de.vanita5.twittnuker.model.ParcelableDirectMessage;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.StringLongPair;
import de.vanita5.twittnuker.model.UnreadItem;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedRelationships;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedUsers;
import de.vanita5.twittnuker.provider.TwidereDataStore.DirectMessages;
import de.vanita5.twittnuker.provider.TwidereDataStore.Drafts;
import de.vanita5.twittnuker.provider.TwidereDataStore.Mentions;
import de.vanita5.twittnuker.provider.TwidereDataStore.Preferences;
import de.vanita5.twittnuker.provider.TwidereDataStore.SearchHistory;
import de.vanita5.twittnuker.provider.TwidereDataStore.Statuses;
import de.vanita5.twittnuker.provider.TwidereDataStore.UnreadCounts;
import de.vanita5.twittnuker.receiver.NotificationReceiver;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.ImagePreloader;
import de.vanita5.twittnuker.util.MediaPreviewUtils;
import de.vanita5.twittnuker.util.ParseUtils;
import de.vanita5.twittnuker.util.NotificationHelper;
import de.vanita5.twittnuker.util.ReadStateManager;
import de.vanita5.twittnuker.util.SQLiteDatabaseWrapper;
import de.vanita5.twittnuker.util.SQLiteDatabaseWrapper.LazyLoadCallback;
import de.vanita5.twittnuker.util.SharedPreferencesWrapper;
import de.vanita5.twittnuker.util.TwidereQueryBuilder.CachedUsersQueryBuilder;
import de.vanita5.twittnuker.util.TwidereQueryBuilder.ConversationQueryBuilder;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.util.collection.CompactHashSet;
import de.vanita5.twittnuker.util.message.UnreadCountUpdatedEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.vanita5.twittnuker.util.Utils.clearAccountColor;
import static de.vanita5.twittnuker.util.Utils.clearAccountName;
import static de.vanita5.twittnuker.util.Utils.getAccountIds;
import static de.vanita5.twittnuker.util.Utils.getNotificationUri;
import static de.vanita5.twittnuker.util.Utils.getTableId;
import static de.vanita5.twittnuker.util.Utils.getTableNameById;
import static de.vanita5.twittnuker.util.Utils.isNotificationsSilent;

public final class TwidereDataProvider extends ContentProvider implements Constants, OnSharedPreferenceChangeListener,
		LazyLoadCallback {

    public static final String TAG_OLDEST_MESSAGES = "oldest_messages";
	private ContentResolver mContentResolver;
	private SQLiteDatabaseWrapper mDatabaseWrapper;
	private NotificationManager mNotificationManager;
    private ReadStateManager mReadStateManager;
	private SharedPreferencesWrapper mPreferences;
	private ImagePreloader mImagePreloader;
	private Network mNetwork;
	private NotificationHelper mNotificationHelper;
	private Handler mHandler;

	private boolean mNameFirst;

	@Override
	public int bulkInsert(final Uri uri, @NonNull final ContentValues[] valuesArray) {
		try {
			final int tableId = getTableId(uri);
			final String table = getTableNameById(tableId);
			switch (tableId) {
				case TABLE_ID_DIRECT_MESSAGES_CONVERSATION:
				case TABLE_ID_DIRECT_MESSAGES:
				case TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES:
					return 0;
			}
			int result = 0;
            final long[] newIds = new long[valuesArray.length];
            if (table != null) {
				mDatabaseWrapper.beginTransaction();
                if (tableId == TABLE_ID_CACHED_USERS) {
                    for (final ContentValues values : valuesArray) {
                        final Expression where = Expression.equals(CachedUsers.USER_ID,
                                values.getAsLong(CachedUsers.USER_ID));
                        mDatabaseWrapper.update(table, values, where.getSQL(), null);
                        newIds[result++] = mDatabaseWrapper.insertWithOnConflict(table, null,
                                values, SQLiteDatabase.CONFLICT_REPLACE);
                    }
                } else if (tableId == TABLE_ID_SEARCH_HISTORY) {
                    for (final ContentValues values : valuesArray) {
                        values.put(SearchHistory.RECENT_QUERY, System.currentTimeMillis());
                        final Expression where = Expression.equalsArgs(SearchHistory.QUERY);
                        final String[] args = {values.getAsString(SearchHistory.QUERY)};
                        mDatabaseWrapper.update(table, values, where.getSQL(), args);
                        newIds[result++] = mDatabaseWrapper.insertWithOnConflict(table, null,
                                values, SQLiteDatabase.CONFLICT_IGNORE);
                    }
                } else if (shouldReplaceOnConflict(tableId)) {
                    for (final ContentValues values : valuesArray) {
                        newIds[result++] = mDatabaseWrapper.insertWithOnConflict(table, null,
                                values, SQLiteDatabase.CONFLICT_REPLACE);
                    }
                } else {
                    for (final ContentValues values : valuesArray) {
                        newIds[result++] = mDatabaseWrapper.insert(table, null, values);
				    }
                }
				mDatabaseWrapper.setTransactionSuccessful();
				mDatabaseWrapper.endTransaction();
			}
			if (result > 0) {
				onDatabaseUpdated(tableId, uri);
			}
            onNewItemsInserted(uri, tableId, valuesArray, newIds);
			return result;
		} catch (final SQLException e) {
			throw new IllegalStateException(e);
		}
	}


	@Override
	public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
		try {
			final int tableId = getTableId(uri);
			final String table = getTableNameById(tableId);
			switch (tableId) {
				case TABLE_ID_DIRECT_MESSAGES_CONVERSATION:
				case TABLE_ID_DIRECT_MESSAGES:
				case TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES:
					return 0;
				case VIRTUAL_TABLE_ID_NOTIFICATIONS: {
					final List<String> segments = uri.getPathSegments();
					if (segments.size() == 1) {
						clearNotification();
					} else if (segments.size() == 2) {
						final int notificationType = ParseUtils.parseInt(segments.get(1));
						clearNotification(notificationType, 0);
					} else if (segments.size() == 3) {
						final int notificationType = ParseUtils.parseInt(segments.get(1));
						final long accountId = ParseUtils.parseLong(segments.get(2));
						clearNotification(notificationType, accountId);
					}
					return 1;
				}
				case VIRTUAL_TABLE_ID_UNREAD_COUNTS: {
					return 0;
				}
			}
			if (table == null) return 0;
			final int result = mDatabaseWrapper.delete(table, selection, selectionArgs);
			if (result > 0) {
				onDatabaseUpdated(tableId, uri);
			}
			return result;
		} catch (final SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String getType(final Uri uri) {
		return null;
	}

	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		try {
			final int tableId = getTableId(uri);
			final String table = getTableNameById(tableId);
			switch (tableId) {
				case TABLE_ID_DIRECT_MESSAGES_CONVERSATION:
				case TABLE_ID_DIRECT_MESSAGES:
				case TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES:
					return null;
			}
			if (table == null) return null;
			final long rowId;
            if (tableId == TABLE_ID_CACHED_USERS) {
                final Expression where = Expression.equals(CachedUsers.USER_ID,
                        values.getAsLong(CachedUsers.USER_ID));
                mDatabaseWrapper.update(table, values, where.getSQL(), null);
                rowId = mDatabaseWrapper.insertWithOnConflict(table, null, values,
                        SQLiteDatabase.CONFLICT_IGNORE);
            } else if (tableId == TABLE_ID_SEARCH_HISTORY) {
                values.put(SearchHistory.RECENT_QUERY, System.currentTimeMillis());
                final Expression where = Expression.equalsArgs(SearchHistory.QUERY);
                final String[] args = {values.getAsString(SearchHistory.QUERY)};
                mDatabaseWrapper.update(table, values, where.getSQL(), args);
                rowId = mDatabaseWrapper.insertWithOnConflict(table, null, values,
                        SQLiteDatabase.CONFLICT_IGNORE);
            } else if (tableId == TABLE_ID_CACHED_RELATIONSHIPS) {
                final long accountId = values.getAsLong(CachedRelationships.ACCOUNT_ID);
                final long userId = values.getAsLong(CachedRelationships.USER_ID);
                final Expression where = Expression.and(
                        Expression.equals(CachedRelationships.ACCOUNT_ID, accountId),
                        Expression.equals(CachedRelationships.USER_ID, userId)
                );
                if (mDatabaseWrapper.update(table, values, where.getSQL(), null) > 0) {
                    final String[] projection = {CachedRelationships._ID};
                    final Cursor c = mDatabaseWrapper.query(table, projection, where.getSQL(), null,
                            null, null, null);
                    if (c.moveToFirst()) {
                        rowId = c.getLong(0);
                    } else {
                        rowId = 0;
                    }
                    c.close();
                } else {
                    rowId = mDatabaseWrapper.insertWithOnConflict(table, null, values,
                            SQLiteDatabase.CONFLICT_IGNORE);
                }
            } else if (shouldReplaceOnConflict(tableId)) {
                rowId = mDatabaseWrapper.insertWithOnConflict(table, null, values,
                        SQLiteDatabase.CONFLICT_REPLACE);
			} else {
				rowId = mDatabaseWrapper.insert(table, null, values);
			}
			onDatabaseUpdated(tableId, uri);
            onNewItemsInserted(uri, tableId, values, rowId);
			return Uri.withAppendedPath(uri, String.valueOf(rowId));
		} catch (final SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public boolean onCreate() {
		final Context context = getContext();
		final TwittnukerApplication app = TwittnukerApplication.getInstance(context);
        mHandler = new Handler(Looper.getMainLooper());
        mDatabaseWrapper = new SQLiteDatabaseWrapper(this);
        mNetwork = app.getNetwork();
        mPreferences = SharedPreferencesWrapper.getInstance(context, SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        updatePreferences();
		mReadStateManager = app.getReadStateManager();
		mImagePreloader = new ImagePreloader(context, app.getImageLoader());
		mNotificationHelper = new NotificationHelper(context);
		// final GetWritableDatabaseTask task = new
		// GetWritableDatabaseTask(context, helper, mDatabaseWrapper);
        // task.executeTask();
		return true;
	}

    @Override
    public SQLiteDatabase onCreateSQLiteDatabase() {
        final TwittnukerApplication app = TwittnukerApplication.getInstance(getContext());
        final SQLiteOpenHelper helper = app.getSQLiteOpenHelper();
        return helper.getWritableDatabase();
    }

	@Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        updatePreferences();
    }

    @Override
	public ParcelFileDescriptor openFile(final Uri uri, final String mode) throws FileNotFoundException {
		if (uri == null || mode == null) throw new IllegalArgumentException();
		final int table_id = getTableId(uri);
		switch (table_id) {
			case VIRTUAL_TABLE_ID_CACHED_IMAGES: {
				return getCachedImageFd(uri.getQueryParameter(QUERY_PARAM_URL));
			}
			case VIRTUAL_TABLE_ID_CACHE_FILES: {
				return getCacheFileFd(uri.getLastPathSegment());
			}
		}
		return null;
	}

	@Override
	public Cursor query(final Uri uri, final String[] projection, final String selection, final String[] selectionArgs,
			final String sortOrder) {
		try {
			final int tableId = getTableId(uri);
			final String table = getTableNameById(tableId);
			switch (tableId) {
				case VIRTUAL_TABLE_ID_DATABASE_READY: {
					if (mDatabaseWrapper.isReady())
						return new MatrixCursor(projection != null ? projection : new String[0]);
					return null;
				}
				case VIRTUAL_TABLE_ID_ALL_PREFERENCES: {
					return getPreferencesCursor(mPreferences, null);
				}
				case VIRTUAL_TABLE_ID_PREFERENCES: {
					return getPreferencesCursor(mPreferences, uri.getLastPathSegment());
				}
				case VIRTUAL_TABLE_ID_DNS: {
					return getDNSCursor(uri.getLastPathSegment());
				}
				case VIRTUAL_TABLE_ID_CACHED_IMAGES: {
					return getCachedImageCursor(uri.getQueryParameter(QUERY_PARAM_URL));
				}
				case VIRTUAL_TABLE_ID_NOTIFICATIONS: {
					final List<String> segments = uri.getPathSegments();
					if (segments.size() == 2)
						return getNotificationsCursor(ParseUtils.parseInt(segments.get(1), -1));
					else
						return getNotificationsCursor();
				}
				case VIRTUAL_TABLE_ID_UNREAD_COUNTS: {
					final List<String> segments = uri.getPathSegments();
					if (segments.size() == 2)
						return getUnreadCountsCursor(ParseUtils.parseInt(segments.get(1), -1));
					else
						return getUnreadCountsCursor();
				}
				case VIRTUAL_TABLE_ID_UNREAD_COUNTS_BY_TYPE: {
					final List<String> segments = uri.getPathSegments();
					if (segments.size() != 3) return null;
					return getUnreadCountsCursorByType(segments.get(2));
				}
				case TABLE_ID_DIRECT_MESSAGES_CONVERSATION: {
					final List<String> segments = uri.getPathSegments();
					if (segments.size() != 4) return null;
					final long accountId = ParseUtils.parseLong(segments.get(2));
					final long conversationId = ParseUtils.parseLong(segments.get(3));
                    final SQLSelectQuery query = ConversationQueryBuilder.buildByConversationId(projection,
							accountId, conversationId, selection, sortOrder);
                    final Cursor c = mDatabaseWrapper.rawQuery(query.getSQL(), selectionArgs);
					setNotificationUri(c, DirectMessages.CONTENT_URI);
					return c;
				}
				case TABLE_ID_DIRECT_MESSAGES_CONVERSATION_SCREEN_NAME: {
					final List<String> segments = uri.getPathSegments();
					if (segments.size() != 4) return null;
					final long accountId = ParseUtils.parseLong(segments.get(2));
					final String screenName = segments.get(3);
                    final SQLSelectQuery query = ConversationQueryBuilder.buildByScreenName(projection,
							accountId, screenName, selection, sortOrder);
                    final Cursor c = mDatabaseWrapper.rawQuery(query.getSQL(), selectionArgs);
					setNotificationUri(c, DirectMessages.CONTENT_URI);
					return c;
				}
                case VIRTUAL_TABLE_ID_CACHED_USERS_WITH_RELATIONSHIP: {
                    final long accountId = ParseUtils.parseLong(uri.getLastPathSegment(), -1);
                    final SQLSelectQuery query = CachedUsersQueryBuilder.withRelationship(projection,
							selection, sortOrder, accountId);
                    final Cursor c = mDatabaseWrapper.rawQuery(query.getSQL(), selectionArgs);
                    setNotificationUri(c, CachedUsers.CONTENT_URI);
                    return c;
			    }
                case VIRTUAL_TABLE_ID_CACHED_USERS_WITH_SCORE: {
                    final long accountId = ParseUtils.parseLong(uri.getLastPathSegment(), -1);
                    final SQLSelectQuery query = CachedUsersQueryBuilder.withScore(projection,
                            selection, sortOrder, accountId);
                    final Cursor c = mDatabaseWrapper.rawQuery(query.getSQL(), selectionArgs);
                    setNotificationUri(c, CachedUsers.CONTENT_URI);
                    return c;
                }
                case VIRTUAL_TABLE_ID_DRAFTS_UNSENT: {
                    final TwittnukerApplication app = TwittnukerApplication.getInstance(getContext());
                    final AsyncTwitterWrapper twitter = app.getTwitterWrapper();
                    final RawItemArray sendingIds = new RawItemArray(twitter.getSendingDraftIds());
                    final Expression where;
                    if (selection != null) {
                        where = Expression.and(new Expression(selection),
                                Expression.notIn(new Column(Drafts._ID), sendingIds));
                    } else {
                        where = Expression.and(Expression.notIn(new Column(Drafts._ID), sendingIds));
                    }
                    final Cursor c = mDatabaseWrapper.query(Drafts.TABLE_NAME, projection,
                            where.getSQL(), selectionArgs, null, null, sortOrder);
                    setNotificationUri(c, getNotificationUri(tableId, uri));
                    return c;
                }
            }
			if (table == null) return null;
			final Cursor c = mDatabaseWrapper.query(table, projection, selection, selectionArgs, null, null, sortOrder);
			setNotificationUri(c, getNotificationUri(tableId, uri));
			return c;
		} catch (final SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public int update(final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {
		try {
			final int tableId = getTableId(uri);
			final String table = getTableNameById(tableId);
			int result = 0;
			if (table != null) {
				switch (tableId) {
					case TABLE_ID_DIRECT_MESSAGES_CONVERSATION:
					case TABLE_ID_DIRECT_MESSAGES:
					case TABLE_ID_DIRECT_MESSAGES_CONVERSATIONS_ENTRIES:
						return 0;
				}
				result = mDatabaseWrapper.update(table, values, selection, selectionArgs);
			}
			if (result > 0) {
				onDatabaseUpdated(tableId, uri);
			}
			return result;
		} catch (final SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	private void clearNotification() {
        getNotificationManager().cancelAll();
	}

	private void clearNotification(final int notificationType, final long accountId) {

	}

	/**
	 * //TODO
	 *
	 * Creates notifications for mentions and DMs
	 * @param pref
	 * @param type
	 * @param build
	 */
	private void createNotifications(final AccountPreferences pref, final String type,
									 final Object o_status, final boolean build) {
		if (mPreferences.getBoolean(KEY_ENABLE_PUSH, false)) return;
		NotificationContent notification = null;

		if (o_status instanceof ParcelableStatus) {
			ParcelableStatus status = (ParcelableStatus) o_status;
			notification = new NotificationContent();
			notification.setAccountId(status.account_id);
			notification.setFromUser(status.user_screen_name);
			notification.setType(type);
			notification.setMessage(status.text_unescaped);
			notification.setTimestamp(status.timestamp);
			notification.setProfileImageUrl(status.user_profile_image_url);
			notification.setOriginalStatus(status);
		} else if (o_status instanceof ParcelableDirectMessage) {
			ParcelableDirectMessage dm = (ParcelableDirectMessage) o_status;
			notification = new NotificationContent();
			notification.setAccountId(dm.account_id);
			notification.setFromUser(dm.sender_screen_name);
			notification.setType(type);
			notification.setMessage(dm.text_unescaped);
			notification.setTimestamp(dm.timestamp);
			notification.setProfileImageUrl(dm.sender_profile_image_url);
			notification.setOriginalMessage(dm);
		}
		if (notification != null) {
			mNotificationHelper.cachePushNotification(notification);
		}
		if (build) mNotificationHelper.buildNotificationByType(notification, pref, false);
	}

	private Cursor getCachedImageCursor(final String url) {
		if (BuildConfig.DEBUG) {
			Log.d(LOGTAG, String.format("getCachedImageCursor(%s)", url));
		}
		final MatrixCursor c = new MatrixCursor(TwidereDataStore.CachedImages.MATRIX_COLUMNS);
		final File file = mImagePreloader.getCachedImageFile(url);
		if (url != null && file != null) {
			c.addRow(new String[] { url, file.getPath() });
		}
		return c;
	}

	private ParcelFileDescriptor getCachedImageFd(final String url) throws FileNotFoundException {
		if (BuildConfig.DEBUG) {
			Log.d(LOGTAG, String.format("getCachedImageFd(%s)", url));
		}
		final File file = mImagePreloader.getCachedImageFile(url);
		if (file == null) return null;
		return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
	}

	private ParcelFileDescriptor getCacheFileFd(final String name) throws FileNotFoundException {
		if (name == null) return null;
		final Context mContext = getContext();
		final File cacheDir = mContext.getCacheDir();
		final File file = new File(cacheDir, name);
		if (!file.exists()) return null;
		return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
	}

    private ContentResolver getContentResolver() {
        if (mContentResolver != null) return mContentResolver;
        final Context context = getContext();
        return mContentResolver = context.getContentResolver();
    }

	private Cursor getDNSCursor(final String host) {
		final MatrixCursor c = new MatrixCursor(TwidereDataStore.DNS.MATRIX_COLUMNS);
		try {
            final InetAddress[] addresses = mNetwork.resolveInetAddresses(host);
            for (InetAddress address : addresses) {
                c.addRow(new String[]{host, address.getHostAddress()});
			}
        } catch (final IOException ignore) {
            if (BuildConfig.DEBUG) {
                Log.w(LOGTAG, ignore);
            }
		}
		return c;
	}

    private NotificationManager getNotificationManager() {
        if (mNotificationManager != null) return mNotificationManager;
        final Context context = getContext();
        return mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

	private Cursor getNotificationsCursor() {
		final MatrixCursor c = new MatrixCursor(TwidereDataStore.Notifications.MATRIX_COLUMNS);
		return c;
	}

	private Cursor getNotificationsCursor(final int id) {
		final MatrixCursor c = new MatrixCursor(TwidereDataStore.Notifications.MATRIX_COLUMNS);
		return c;
	}

	private Cursor getUnreadCountsCursor() {
		final MatrixCursor c = new MatrixCursor(TwidereDataStore.UnreadCounts.MATRIX_COLUMNS);
		return c;
	}

	private Cursor getUnreadCountsCursor(final int position) {
		final MatrixCursor c = new MatrixCursor(TwidereDataStore.UnreadCounts.MATRIX_COLUMNS);

		return c;
	}

	private Cursor getUnreadCountsCursorByType(final String type) {
		final MatrixCursor c = new MatrixCursor(TwidereDataStore.UnreadCounts.MATRIX_COLUMNS);
		return c;
	}

	private void notifyContentObserver(final Uri uri) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
				final ContentResolver cr = getContentResolver();
				if (uri == null || cr == null) return;
				cr.notifyChange(uri, null);
			}
        });
    }


	private void notifyUnreadCountChanged(final int position) {
		final Context context = getContext();
        final Bus bus = TwittnukerApplication.getInstance(context).getMessageBus();
        if (bus != null) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					bus.post(new UnreadCountUpdatedEvent(position));
				}
			});
        }
		notifyContentObserver(UnreadCounts.CONTENT_URI);
	}

	private void onDatabaseUpdated(final int tableId, final Uri uri) {
		if (uri == null) return;
		switch (tableId) {
			case TABLE_ID_ACCOUNTS: {
				clearAccountColor();
				clearAccountName();
				break;
			}
		}
		notifyContentObserver(getNotificationUri(tableId, uri));
	}


    private void onNewItemsInserted(final Uri uri, final int tableId, final ContentValues values, final long newId) {
        onNewItemsInserted(uri, tableId, new ContentValues[]{values}, new long[]{newId});

    }

    private void onNewItemsInserted(final Uri uri, final int tableId, final ContentValues[] valuesArray, final long[] newIds) {
        final Context context = getContext();
        if (uri == null || valuesArray == null || valuesArray.length == 0 || context == null)
            return;
        preloadImages(valuesArray);
		if (!uri.getBooleanQueryParameter(QUERY_PARAM_NOTIFY, true)) return;
        switch (tableId) {
			case TABLE_ID_STATUSES: {
//                final AccountPreferences[] prefs = AccountPreferences.getNotificationEnabledPreferences(context,
//                        getAccountIds(context));
//                assert prefs != null;
//                for (final AccountPreferences pref : prefs) {
//                    if (!pref.isHomeTimelineNotificationEnabled()) continue;
//                    showTimelineNotification(pref, getPositionTag(TAB_TYPE_HOME_TIMELINE, pref.getAccountId()));
//                }
				notifyUnreadCountChanged(NOTIFICATION_ID_HOME_TIMELINE);
				break;
			}
			case TABLE_ID_MENTIONS: {
                final AccountPreferences[] prefs = AccountPreferences.getNotificationEnabledPreferences(context,
                        getAccountIds(context));
                assert prefs != null;
                for (final AccountPreferences pref : prefs) {
                    if (!pref.isMentionsNotificationEnabled()) continue;
                    showMentionsNotification(pref, getPositionTag(TAB_TYPE_MENTIONS_TIMELINE, pref.getAccountId()));
                }
				notifyUnreadCountChanged(NOTIFICATION_ID_MENTIONS_TIMELINE);
				break;
			}
			case TABLE_ID_DIRECT_MESSAGES_INBOX: {
                final AccountPreferences[] prefs = AccountPreferences.getNotificationEnabledPreferences(context,
                        getAccountIds(context));
                assert prefs != null;
                for (final AccountPreferences pref : prefs) {
                    if (!pref.isDirectMessagesNotificationEnabled()) continue;
                    final StringLongPair[] pairs = mReadStateManager.getPositionPairs(TAB_TYPE_DIRECT_MESSAGES);
                    showMessagesNotification(pref, pairs, valuesArray);
                }
				notifyUnreadCountChanged(NOTIFICATION_ID_DIRECT_MESSAGES);
				break;
			}
            case TABLE_ID_DRAFTS: {
                break;
	        }
        }
    }

    private long getPositionTag(String tag, long accountId) {
        final long position = mReadStateManager.getPosition(Utils.getReadPositionTagWithAccounts(tag,
				accountId));
        if (position != -1) return position;
        return mReadStateManager.getPosition(tag);
    }

    private void showTimelineNotification(AccountPreferences pref, long position) {
        final long accountId = pref.getAccountId();
        final Context context = getContext();
        final Resources resources = context.getResources();
        final NotificationManager nm = getNotificationManager();
        final Expression selection = Expression.and(Expression.equals(Statuses.ACCOUNT_ID, accountId),
                Expression.greaterThan(Statuses.STATUS_ID, position));
        final String filteredSelection = Utils.buildStatusFilterWhereClause(Statuses.TABLE_NAME,
                selection).getSQL();
        final String[] userProjection = {Statuses.USER_ID, Statuses.USER_NAME, Statuses.USER_SCREEN_NAME};
        final String[] statusProjection = {Statuses.STATUS_ID};
        final Cursor statusCursor = mDatabaseWrapper.query(Statuses.TABLE_NAME, statusProjection,
                filteredSelection, null, null, null, Statuses.SORT_ORDER_TIMESTAMP_DESC);
        final Cursor userCursor = mDatabaseWrapper.query(Statuses.TABLE_NAME, userProjection,
                filteredSelection, null, Statuses.USER_ID, null, Statuses.SORT_ORDER_TIMESTAMP_DESC);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            final int usersCount = userCursor.getCount();
            final int statusesCount = statusCursor.getCount();
            if (statusesCount == 0 || usersCount == 0) return;
            final int idxStatusId = statusCursor.getColumnIndex(Statuses.STATUS_ID),
                    idxUserName = userCursor.getColumnIndex(Statuses.USER_NAME),
                    idxUserScreenName = userCursor.getColumnIndex(Statuses.USER_NAME),
                    idxUserId = userCursor.getColumnIndex(Statuses.USER_NAME);
            final long statusId = statusCursor.moveToFirst() ? statusCursor.getLong(idxStatusId) : -1;
            final String notificationTitle = resources.getQuantityString(R.plurals.N_new_statuses,
					statusesCount, statusesCount);
            final String notificationContent;
            userCursor.moveToFirst();
            final String displayName = mNameFirst ? userCursor.getString(idxUserName) : "@" + userCursor.getString(idxUserScreenName);
            if (usersCount == 1) {
                notificationContent = context.getString(R.string.from_name, displayName);
            } else if (usersCount == 2) {
                userCursor.moveToPosition(1);
                final String othersName = mNameFirst ? userCursor.getString(idxUserName) : "@" + userCursor.getString(idxUserScreenName);
                notificationContent = resources.getQuantityString(R.plurals.from_name_and_N_others,
                        usersCount - 1, othersName, usersCount - 1);
            } else {
                userCursor.moveToPosition(1);
                final String othersName = mNameFirst ? userCursor.getString(idxUserName) : "@" + userCursor.getString(idxUserScreenName);
                notificationContent = resources.getString(R.string.from_name_and_N_others, othersName, usersCount - 1);
            }

            // Setup notification
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            builder.setAutoCancel(true);
            builder.setSmallIcon(R.drawable.ic_stat_twitter);
            builder.setTicker(notificationTitle);
            builder.setContentTitle(notificationTitle);
            builder.setContentText(notificationContent);
            builder.setCategory(NotificationCompat.CATEGORY_SOCIAL);
            builder.setContentIntent(getContentIntent(context, AUTHORITY_HOME, accountId));
            builder.setDeleteIntent(getDeleteIntent(context, AUTHORITY_HOME, accountId, statusId));
            builder.setNumber(statusesCount);
            builder.setColor(pref.getNotificationLightColor());
            setNotificationPreferences(builder, pref, pref.getHomeTimelineNotificationType());
            nm.notify("home_" + accountId, NOTIFICATION_ID_HOME_TIMELINE, builder.build());
        } finally {
            statusCursor.close();
            userCursor.close();
        }
    }

    private void showMentionsNotification(AccountPreferences pref, long position) {
        final long accountId = pref.getAccountId();
        final Context context = getContext();
        final Resources resources = context.getResources();
        final NotificationManager nm = getNotificationManager();
        final Expression selection;
        if (pref.isNotificationFollowingOnly()) {
            selection = Expression.and(Expression.equals(Statuses.ACCOUNT_ID, accountId),
                    Expression.greaterThan(Statuses.STATUS_ID, position),
                    Expression.equals(Statuses.IS_FOLLOWING, 1));
        } else {
            selection = Expression.and(Expression.equals(Statuses.ACCOUNT_ID, accountId),
                	Expression.greaterThan(Statuses.STATUS_ID, position));
        }
        final String filteredSelection = Utils.buildStatusFilterWhereClause(Mentions.TABLE_NAME,
                selection).getSQL();
        final String[] userProjection = {Statuses.USER_ID, Statuses.USER_NAME, Statuses.USER_SCREEN_NAME};
        final String[] statusProjection = {Statuses.STATUS_ID, Statuses.USER_ID, Statuses.USER_NAME, Statuses.USER_SCREEN_NAME,
                Statuses.TEXT_UNESCAPED, Statuses.STATUS_TIMESTAMP};
        final Cursor statusCursor = mDatabaseWrapper.query(Mentions.TABLE_NAME, statusProjection,
                filteredSelection, null, null, null, Statuses.SORT_ORDER_TIMESTAMP_DESC);
        final Cursor userCursor = mDatabaseWrapper.query(Mentions.TABLE_NAME, userProjection,
                filteredSelection, null, Statuses.USER_ID, null, Statuses.SORT_ORDER_TIMESTAMP_DESC);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            final int usersCount = userCursor.getCount();
            final int statusesCount = statusCursor.getCount();
            if (statusesCount == 0 || usersCount == 0) return;
			final String accountName = Utils.getAccountName(context, accountId);
            final String accountScreenName = Utils.getAccountScreenName(context, accountId);
            final int idxStatusText = statusCursor.getColumnIndex(Statuses.TEXT_UNESCAPED),
                    idxStatusId = statusCursor.getColumnIndex(Statuses.STATUS_ID),
                    idxStatusTimestamp = statusCursor.getColumnIndex(Statuses.STATUS_TIMESTAMP),
                    idxStatusUserName = statusCursor.getColumnIndex(Statuses.USER_NAME),
                    idxStatusUserScreenName = statusCursor.getColumnIndex(Statuses.USER_SCREEN_NAME),
                    idxUserName = userCursor.getColumnIndex(Statuses.USER_NAME),
                    idxUserScreenName = userCursor.getColumnIndex(Statuses.USER_NAME),
                    idxUserId = userCursor.getColumnIndex(Statuses.USER_NAME);

            final CharSequence notificationTitle = resources.getQuantityString(R.plurals.N_new_mentions,
                    statusesCount, statusesCount);
            final String notificationContent;
            userCursor.moveToFirst();
            final String displayName = mNameFirst ? userCursor.getString(idxUserName) : "@" + userCursor.getString(idxUserScreenName);
            if (usersCount == 1) {
                notificationContent = context.getString(R.string.notification_mention, displayName);
            } else {
                notificationContent = context.getString(R.string.notification_mention_multiple,
                        displayName, usersCount - 1);
            }

            // Add rich notification and get latest tweet timestamp
            long when = -1, statusId = -1;
            final InboxStyle style = new InboxStyle();
            for (int i = 0, j = Math.min(statusesCount, 5); statusCursor.moveToPosition(i) && i < j; i++) {
                if (when == -1) {
                    when = statusCursor.getLong(idxStatusTimestamp);
                }
                if (statusId == -1) {
                    statusId = statusCursor.getLong(idxStatusId);
                }
                final SpannableStringBuilder sb = new SpannableStringBuilder();
                sb.append(mNameFirst ? statusCursor.getString(idxStatusUserName) : "@" + statusCursor.getString(idxStatusUserScreenName));
                sb.setSpan(new StyleSpan(Typeface.BOLD), 0, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.append(' ');
                sb.append(statusCursor.getString(idxStatusText));
                style.addLine(sb);
            }
            if (mNameFirst) {
                style.setSummaryText(accountName);
            } else {
				style.setSummaryText("@" + accountScreenName);
            }

            // Setup notification
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            builder.setAutoCancel(true);
            builder.setSmallIcon(R.drawable.ic_stat_mention);
            builder.setTicker(notificationTitle);
			builder.setContentTitle(notificationTitle);
			builder.setContentText(notificationContent);
			builder.setCategory(NotificationCompat.CATEGORY_SOCIAL);
            builder.setContentIntent(getContentIntent(context, AUTHORITY_MENTIONS, accountId));
            builder.setDeleteIntent(getDeleteIntent(context, AUTHORITY_MENTIONS, accountId, statusId));
            builder.setNumber(statusesCount);
            builder.setWhen(when);
            builder.setStyle(style);
            builder.setColor(pref.getNotificationLightColor());
            setNotificationPreferences(builder, pref, pref.getMentionsNotificationType());
            nm.notify("mentions_" + accountId, NOTIFICATION_ID_MENTIONS_TIMELINE,
                    builder.build());
        } finally {
            statusCursor.close();
            userCursor.close();
        }
    }

    private PendingIntent getContentIntent(Context context, String type, long accountId) {
        // Setup click intent
        final Intent homeIntent = new Intent(context, HomeActivity.class);
        final Uri.Builder homeLinkBuilder = new Uri.Builder();
        homeLinkBuilder.scheme(SCHEME_TWITTNUKER);
        homeLinkBuilder.authority(type);
        homeLinkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        homeIntent.setData(homeLinkBuilder.build());
        return PendingIntent.getActivity(context, 0, homeIntent, 0);
    }

    private static PendingIntent getDeleteIntent(Context context, String type, long accountId, long position) {
        // Setup delete intent
        final Intent recvIntent = new Intent(context, NotificationReceiver.class);
        recvIntent.setAction(BROADCAST_NOTIFICATION_DELETED);
        final Uri.Builder recvLinkBuilder = new Uri.Builder();
        recvLinkBuilder.scheme(SCHEME_TWITTNUKER);
        recvLinkBuilder.authority(AUTHORITY_NOTIFICATIONS);
        recvLinkBuilder.appendPath(type);
        recvLinkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        recvLinkBuilder.appendQueryParameter(QUERY_PARAM_READ_POSITION, String.valueOf(position));
        recvIntent.setData(recvLinkBuilder.build());
        return PendingIntent.getBroadcast(context, 0, recvIntent, 0);
    }

    private static PendingIntent getDeleteIntent(Context context, String type, long accountId, StringLongPair[] positions) {
        // Setup delete intent
        final Intent recvIntent = new Intent(context, NotificationReceiver.class);
        final Uri.Builder recvLinkBuilder = new Uri.Builder();
        recvLinkBuilder.scheme(SCHEME_TWITTNUKER);
        recvLinkBuilder.authority(AUTHORITY_NOTIFICATIONS);
        recvLinkBuilder.appendPath(type);
        recvLinkBuilder.appendQueryParameter(QUERY_PARAM_ACCOUNT_ID, String.valueOf(accountId));
        recvLinkBuilder.appendQueryParameter(QUERY_PARAM_READ_POSITIONS, StringLongPair.toString(positions));
        recvIntent.setData(recvLinkBuilder.build());
        return PendingIntent.getBroadcast(context, 0, recvIntent, 0);
    }

    private void setNotificationPreferences(NotificationCompat.Builder builder, AccountPreferences pref, int defaultFlags) {
        int notificationDefaults = 0;
        if (AccountPreferences.isNotificationHasLight(defaultFlags)) {
            notificationDefaults |= NotificationCompat.DEFAULT_LIGHTS;
        }
        if (!isNotificationsSilent(getContext())) {
			if (AccountPreferences.isNotificationHasVibration(defaultFlags)) {
				notificationDefaults |= NotificationCompat.DEFAULT_VIBRATE;
            } else {
                notificationDefaults &= ~NotificationCompat.DEFAULT_VIBRATE;
			}
			if (AccountPreferences.isNotificationHasRingtone(defaultFlags)) {
				notificationDefaults |= NotificationCompat.DEFAULT_SOUND;
				builder.setSound(pref.getNotificationRingtone(), AudioManager.STREAM_NOTIFICATION);
			}
        } else {
            notificationDefaults &= ~(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND);
        }
        builder.setDefaults(notificationDefaults);
    }

    private void showMessagesNotification(AccountPreferences pref, StringLongPair[] pairs, ContentValues[] valuesArray) {
        final long accountId = pref.getAccountId();
        final long prevOldestId = mReadStateManager.getPosition(TAG_OLDEST_MESSAGES, String.valueOf(accountId));
        long oldestId = -1;
        for (final ContentValues contentValues : valuesArray) {
            final long messageId = contentValues.getAsLong(DirectMessages.MESSAGE_ID);
            oldestId = oldestId < 0 ? messageId : Math.min(oldestId, messageId);
            if (messageId <= prevOldestId) return;
        }
        mReadStateManager.setPosition(TAG_OLDEST_MESSAGES, String.valueOf(accountId), oldestId, false);
        final Context context = getContext();
        final Resources resources = context.getResources();
        final NotificationManager nm = getNotificationManager();
        final ArrayList<Expression> orExpressions = new ArrayList<>();
        final String prefix = accountId + "-";
        final int prefixLength = prefix.length();
        final Set<Long> senderIds = new CompactHashSet<>();
        for (StringLongPair pair : pairs) {
            final String key = pair.getKey();
            if (key.startsWith(prefix)) {
                final long senderId = Long.parseLong(key.substring(prefixLength));
                senderIds.add(senderId);
                final Expression expression = Expression.and(
                        Expression.equals(DirectMessages.SENDER_ID, senderId),
                        Expression.greaterThan(DirectMessages.MESSAGE_ID, pair.getValue())
                );
                orExpressions.add(expression);
            }
        }
        orExpressions.add(Expression.notIn(new Column(DirectMessages.SENDER_ID), new RawItemArray(senderIds.toArray())));
        final Expression selection = Expression.and(
                Expression.equals(DirectMessages.ACCOUNT_ID, accountId),
				Expression.greaterThan(DirectMessages.MESSAGE_ID, prevOldestId),
				Expression.or(orExpressions.toArray(new Expression[orExpressions.size()]))
        );
		final String filteredSelection = selection.getSQL();
        final String[] userProjection = {DirectMessages.SENDER_ID, DirectMessages.SENDER_NAME,
                DirectMessages.SENDER_SCREEN_NAME};
        final String[] messageProjection = {DirectMessages.MESSAGE_ID, DirectMessages.SENDER_ID,
                DirectMessages.SENDER_NAME, DirectMessages.SENDER_SCREEN_NAME, DirectMessages.TEXT_UNESCAPED,
                DirectMessages.MESSAGE_TIMESTAMP};
        final Cursor messageCursor = mDatabaseWrapper.query(DirectMessages.Inbox.TABLE_NAME, messageProjection,
                filteredSelection, null, null, null, DirectMessages.DEFAULT_SORT_ORDER);
        final Cursor userCursor = mDatabaseWrapper.query(DirectMessages.Inbox.TABLE_NAME, userProjection,
                filteredSelection, null, DirectMessages.SENDER_ID, null, DirectMessages.DEFAULT_SORT_ORDER);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            final int usersCount = userCursor.getCount();
            final int messagesCount = messageCursor.getCount();
            if (messagesCount == 0 || usersCount == 0) return;
            final String accountName = Utils.getAccountName(context, accountId);
            final String accountScreenName = Utils.getAccountScreenName(context, accountId);
            final int idxMessageText = messageCursor.getColumnIndex(DirectMessages.TEXT_UNESCAPED),
                    idxMessageTimestamp = messageCursor.getColumnIndex(DirectMessages.MESSAGE_TIMESTAMP),
                    idxMessageId = messageCursor.getColumnIndex(DirectMessages.MESSAGE_ID),
                    idxMessageUserId = messageCursor.getColumnIndex(DirectMessages.SENDER_ID),
                    idxMessageUserName = messageCursor.getColumnIndex(DirectMessages.SENDER_NAME),
                    idxMessageUserScreenName = messageCursor.getColumnIndex(DirectMessages.SENDER_SCREEN_NAME),
                    idxUserName = userCursor.getColumnIndex(DirectMessages.SENDER_NAME),
                    idxUserScreenName = userCursor.getColumnIndex(DirectMessages.SENDER_NAME),
                    idxUserId = userCursor.getColumnIndex(DirectMessages.SENDER_NAME);

            final CharSequence notificationTitle = resources.getQuantityString(R.plurals.N_new_messages,
                    messagesCount, messagesCount);
            final String notificationContent;
            userCursor.moveToFirst();
            final String displayName =  mNameFirst ? userCursor.getString(idxUserName) : "@" + userCursor.getString(idxUserScreenName);
            if (usersCount == 1) {
                if (messagesCount == 1) {
                    notificationContent = context.getString(R.string.notification_direct_message, displayName);
                } else {
                    notificationContent = context.getString(R.string.notification_direct_message_multiple_messages,
                            displayName, messagesCount);
                }
            } else {
                notificationContent = context.getString(R.string.notification_direct_message_multiple_users,
                        displayName, usersCount - 1, messagesCount);
            }

            final LongSparseArray<Long> idsMap = new LongSparseArray<>();
            // Add rich notification and get latest tweet timestamp
            long when = -1;
            final InboxStyle style = new InboxStyle();
            for (int i = 0; messageCursor.moveToPosition(i) && i < messagesCount; i++) {
                if (when < 0) {
                    when = messageCursor.getLong(idxMessageTimestamp);
                }
                if (i < 5) {
					final SpannableStringBuilder sb = new SpannableStringBuilder();
					sb.append(mNameFirst ? messageCursor.getString(idxMessageUserName) : "@" + messageCursor.getString(idxMessageUserScreenName));
					sb.setSpan(new StyleSpan(Typeface.BOLD), 0, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					sb.append(' ');
					sb.append(messageCursor.getString(idxMessageText));
					style.addLine(sb);
				}
                final long userId = messageCursor.getLong(idxMessageUserId);
                final long messageId = messageCursor.getLong(idxMessageId);
                idsMap.put(userId, Math.max(idsMap.get(userId, -1L), messageId));
            }
            if (mNameFirst) {
                style.setSummaryText(accountName);
            } else {
				style.setSummaryText("@" + accountScreenName);
            }
            final StringLongPair[] positions = new StringLongPair[idsMap.size()];
            for (int i = 0, j = idsMap.size(); i < j; i++) {
                positions[i] = new StringLongPair(String.valueOf(idsMap.keyAt(i)), idsMap.valueAt(i));
            }

            // Setup notification
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            builder.setAutoCancel(true);
            builder.setSmallIcon(R.drawable.ic_stat_message);
            builder.setTicker(notificationTitle);
            builder.setContentTitle(notificationTitle);
            builder.setContentText(notificationContent);
            builder.setCategory(NotificationCompat.CATEGORY_SOCIAL);
            builder.setContentIntent(getContentIntent(context, AUTHORITY_DIRECT_MESSAGES, accountId));
            builder.setDeleteIntent(getDeleteIntent(context, AUTHORITY_DIRECT_MESSAGES, accountId, positions));
            builder.setNumber(messagesCount);
            builder.setWhen(when);
            builder.setStyle(style);
            builder.setColor(pref.getNotificationLightColor());
            setNotificationPreferences(builder, pref, pref.getDirectMessagesNotificationType());
            nm.notify("messages_" + accountId, NOTIFICATION_ID_DIRECT_MESSAGES, builder.build());
        } finally {
            messageCursor.close();
            userCursor.close();
        }
    }

	private void preloadImages(final ContentValues... values) {
		if (values == null) return;
		for (final ContentValues v : values) {
			if (mPreferences.getBoolean(KEY_PRELOAD_PROFILE_IMAGES, false)) {
				mImagePreloader.preloadImage(v.getAsString(Statuses.USER_PROFILE_IMAGE_URL));
				mImagePreloader.preloadImage(v.getAsString(DirectMessages.SENDER_PROFILE_IMAGE_URL));
				mImagePreloader.preloadImage(v.getAsString(DirectMessages.RECIPIENT_PROFILE_IMAGE_URL));
			}
			if (mPreferences.getBoolean(KEY_PRELOAD_PREVIEW_IMAGES, false)) {
				final String textHtml = v.getAsString(Statuses.TEXT_HTML);
				for (final String link : MediaPreviewUtils.getSupportedLinksInStatus(textHtml)) {
					mImagePreloader.preloadImage(link);
				}
			}
		}
	}

	private void setNotificationUri(final Cursor c, final Uri uri) {
        final ContentResolver cr = getContentResolver();
        if (cr == null || c == null || uri == null) return;
        c.setNotificationUri(cr, uri);
	}

    private void updatePreferences() {
        mNameFirst = mPreferences.getBoolean(KEY_NAME_FIRST, false);
    }

    private static Cursor getPreferencesCursor(final SharedPreferencesWrapper preferences, final String key) {
		final MatrixCursor c = new MatrixCursor(TwidereDataStore.Preferences.MATRIX_COLUMNS);
        final Map<String, Object> map = new HashMap<>();
		final Map<String, ?> all = preferences.getAll();
		if (key == null) {
			map.putAll(all);
		} else {
			map.put(key, all.get(key));
		}
		for (final Map.Entry<String, ?> item : map.entrySet()) {
			final Object value = item.getValue();
			final int type = getPreferenceType(value);
			c.addRow(new Object[] { item.getKey(), ParseUtils.parseString(value), type });
		}
		return c;
	}

	private static int getPreferenceType(final Object object) {
		if (object == null)
			return Preferences.TYPE_NULL;
		else if (object instanceof Boolean)
			return Preferences.TYPE_BOOLEAN;
		else if (object instanceof Integer)
			return Preferences.TYPE_INTEGER;
		else if (object instanceof Long)
			return Preferences.TYPE_LONG;
		else if (object instanceof Float)
			return Preferences.TYPE_FLOAT;
		else if (object instanceof String) return Preferences.TYPE_STRING;
		return Preferences.TYPE_INVALID;
	}


	private static int getUnreadCount(final List<UnreadItem> set, final long... accountIds) {
		if (set == null || set.isEmpty()) return 0;
		int count = 0;
		for (final UnreadItem item : set.toArray(new UnreadItem[set.size()])) {
            if (item != null && ArrayUtils.contains(accountIds, item.account_id)) {
				count++;
			}
		}
		return count;
	}

	private static boolean shouldReplaceOnConflict(final int table_id) {
		switch (table_id) {
			case TABLE_ID_CACHED_HASHTAGS:
			case TABLE_ID_CACHED_STATUSES:
			case TABLE_ID_CACHED_USERS:
            case TABLE_ID_CACHED_RELATIONSHIPS:
            case TABLE_ID_SEARCH_HISTORY:
			case TABLE_ID_FILTERED_USERS:
			case TABLE_ID_FILTERED_KEYWORDS:
			case TABLE_ID_FILTERED_SOURCES:
			case TABLE_ID_FILTERED_LINKS:
				return true;
		}
		return false;
	}

	@SuppressWarnings("unused")
    private static class GetWritableDatabaseTask extends AsyncTask<Object, Object, SQLiteDatabase> {
		private final Context mContext;
		private final SQLiteOpenHelper mHelper;
		private final SQLiteDatabaseWrapper mWrapper;

		GetWritableDatabaseTask(final Context context, final SQLiteOpenHelper helper,
				final SQLiteDatabaseWrapper wrapper) {
			mContext = context;
			mHelper = helper;
			mWrapper = wrapper;
		}

		@Override
		protected SQLiteDatabase doInBackground(final Object... params) {
			return mHelper.getWritableDatabase();
		}

		@Override
		protected void onPostExecute(final SQLiteDatabase result) {
			mWrapper.setSQLiteDatabase(result);
			if (result != null) {
				mContext.sendBroadcast(new Intent(BROADCAST_DATABASE_READY));
			}
		}
	}

}