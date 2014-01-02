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

package de.vanita5.twittnuker.util;

import static de.vanita5.twittnuker.util.ServiceUtils.bindToService;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import de.vanita5.twittnuker.Constants;

import de.vanita5.twittnuker.ITweetShortener;

public final class TweetShortenerInterface implements Constants, ITweetShortener {

	private ITweetShortener mShortener;

	private final ServiceConnection mConntecion = new ServiceConnection() {

		@Override
		public void onServiceConnected(final ComponentName service, final IBinder obj) {
			mShortener = ITweetShortener.Stub.asInterface(obj);
		}

		@Override
		public void onServiceDisconnected(final ComponentName service) {
			mShortener = null;
		}
	};

	private TweetShortenerInterface(final Context context, final String shortener_name) {
		final Intent intent = new Intent(INTENT_ACTION_EXTENSION_SHORTEN_TWEET);
		final ComponentName component = ComponentName.unflattenFromString(shortener_name);
		intent.setComponent(component);
		bindToService(context, intent, mConntecion);
	}

	@Override
	public IBinder asBinder() {
		// Useless here
		return mShortener.asBinder();
	}

	@Override
	public String shorten(final String text, final String screen_name, final long in_reply_to_status_id) {
		if (mShortener == null) return null;
		try {
			return mShortener.shorten(text, screen_name, in_reply_to_status_id);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void waitForService() {
		while (mShortener == null) {
			try {
				Thread.sleep(100L);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static TweetShortenerInterface getInstance(final Application application, final String shortener_name) {
		if (shortener_name == null) return null;
		final Intent intent = new Intent(INTENT_ACTION_EXTENSION_SHORTEN_TWEET);
		final ComponentName component = ComponentName.unflattenFromString(shortener_name);
		intent.setComponent(component);
		if (application.getPackageManager().queryIntentServices(intent, 0).size() != 1) return null;
		return new TweetShortenerInterface(application, shortener_name);
	}

	public static class ServiceToken {

		ContextWrapper wrapped_context;

		ServiceToken(final ContextWrapper context) {

			wrapped_context = context;
		}
	}
}
