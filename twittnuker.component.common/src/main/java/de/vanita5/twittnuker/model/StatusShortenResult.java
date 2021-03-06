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

package de.vanita5.twittnuker.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

@JsonObject
public class StatusShortenResult implements Parcelable {

	public static final Parcelable.Creator<StatusShortenResult> CREATOR = new Parcelable.Creator<StatusShortenResult>() {

		@Override
		public StatusShortenResult createFromParcel(final Parcel source) {
			return new StatusShortenResult(source);
		}

		@Override
		public StatusShortenResult[] newArray(final int size) {
			return new StatusShortenResult[size];
		}
	};

	@JsonField(name = "shortened")
	public String shortened;

	@JsonField(name = "error_code")
	public int error_code;

	@JsonField(name = "error_message")
	public String error_message;

	public StatusShortenResult() {
	}

	public StatusShortenResult(final int errorCode, final String errorMessage) {
		if (errorCode == 0) throw new IllegalArgumentException("Error code must not be 0");
		shortened = null;
		error_code = errorCode;
		error_message = errorMessage;
	}

	public StatusShortenResult(final Parcel src) {
		shortened = src.readString();
		error_code = src.readInt();
		error_message = src.readString();
	}

	public StatusShortenResult(final String shortened) {
		if (shortened == null)
			throw new IllegalArgumentException("Shortened text must not be null");
		this.shortened = shortened;
		error_code = 0;
		error_message = null;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public String toString() {
		return "StatusShortenResult{shortened=" + shortened + ", error_code=" + error_code + ", error_message="
				+ error_message + "}";
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeString(shortened);
		dest.writeInt(error_code);
		dest.writeString(error_message);
	}

	public static StatusShortenResult getInstance(final int errorCode, final String errorMessage) {
		return new StatusShortenResult(errorCode, errorMessage);
	}

	public static StatusShortenResult getInstance(final String shortened) {
		return new StatusShortenResult(shortened);
	}

}