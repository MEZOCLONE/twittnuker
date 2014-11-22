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

package de.vanita5.twittnuker.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.mariotaku.jsonserializer.JSONParcel;
import org.mariotaku.jsonserializer.JSONParcelable;
import org.mariotaku.jsonserializer.JSONSerializer;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import de.vanita5.twittnuker.util.MediaPreviewUtils;
import de.vanita5.twittnuker.util.ParseUtils;
import twitter4j.EntitySupport;
import twitter4j.ExtendedEntitySupport;
import twitter4j.MediaEntity;
import twitter4j.MediaEntity.Size;
import twitter4j.URLEntity;

public class ParcelableMedia implements Parcelable, JSONParcelable {

	public static final int TYPE_IMAGE = 1;

    public static final Parcelable.Creator<ParcelableMedia> CREATOR = new Parcelable.Creator<ParcelableMedia>() {
        @Override
        public ParcelableMedia createFromParcel(final Parcel in) {
            return new ParcelableMedia(in);
        }

        @Override
        public ParcelableMedia[] newArray(final int size) {
            return new ParcelableMedia[size];
        }
    };

    public static final JSONParcelable.Creator<ParcelableMedia> JSON_CREATOR = new JSONParcelable.Creator<ParcelableMedia>() {
        @Override
        public ParcelableMedia createFromParcel(final JSONParcel in) {
            return new ParcelableMedia(in);
        }

        @Override
        public ParcelableMedia[] newArray(final int size) {
            return new ParcelableMedia[size];
        }
    };

    public final String url, media_url;
	public final int start, end, type;
    public final int width, height;

    public ParcelableMedia(final JSONParcel in) {
        url = in.readString("url");
        media_url = in.readString("media_url");
        start = in.readInt("start");
        end = in.readInt("end");
		type = in.readInt("type");
        width = in.readInt("width");
        height = in.readInt("height");
    }

    public ParcelableMedia(final MediaEntity entity) {
        url = ParseUtils.parseString(entity.getMediaURL());
        media_url = ParseUtils.parseString(entity.getMediaURL());
        start = entity.getStart();
        end = entity.getEnd();
		type = TYPE_IMAGE;
        final Size size = entity.getSizes().get(Size.LARGE);
        width = size != null ? size.getWidth() : 0;
        height = size != null ? size.getHeight() : 0;
    }

    public ParcelableMedia(final Parcel in) {
        url = in.readString();
        media_url = in.readString();
        start = in.readInt();
        end = in.readInt();
		type = in.readInt();
        width = in.readInt();
        height = in.readInt();
    }

	private ParcelableMedia(final String url, final String media_url, final int start, final int end, final int type) {
        this.url = url;
        this.media_url = media_url;
        this.start = start;
        this.end = end;
		this.type = type;
        this.width = 0;
        this.height = 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final JSONParcel out) {
        out.writeString("url", url);
        out.writeString("media_url", media_url);
        out.writeInt("start", start);
        out.writeInt("end", end);
		out.writeInt("type", type);
        out.writeInt("width", width);
        out.writeInt("height", height);
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(url);
        dest.writeString(media_url);
        dest.writeInt(start);
        dest.writeInt(end);
		dest.writeInt(type);
        dest.writeInt(width);
        dest.writeInt(height);
    }

    public static ParcelableMedia[] fromEntities(final EntitySupport entities) {
        final List<ParcelableMedia> list = new ArrayList<>();
        final MediaEntity[] mediaEntities;
        if (entities instanceof ExtendedEntitySupport) {
            final ExtendedEntitySupport extendedEntities = (ExtendedEntitySupport) entities;
            final MediaEntity[] extendedMedias = extendedEntities.getExtendedMediaEntities();
            mediaEntities = extendedMedias != null ? extendedMedias : entities.getMediaEntities();
        } else {
            mediaEntities = entities.getMediaEntities();
        }
        if (mediaEntities != null) {
            for (final MediaEntity media : mediaEntities) {
                final URL mediaURL = media.getMediaURL();
                if (mediaURL != null) {
					list.add(new ParcelableMedia(media));
                }
            }
        }
        final URLEntity[] urlEntities = entities.getURLEntities();
        if (urlEntities != null) {
            for (final URLEntity url : urlEntities) {
				final String expanded = ParseUtils.parseString(url.getExpandedURL());
				final String media_url = MediaPreviewUtils.getSupportedLink(expanded);
				if (expanded != null && media_url != null) {
					list.add(new ParcelableMedia(expanded, media_url, url.getStart(), url.getEnd(), TYPE_IMAGE));
                }
            }
        }
		if (list.isEmpty()) return null;
		return list.toArray(new ParcelableMedia[list.size()]);
    }

    public static ParcelableMedia[] fromJSONString(final String json) {
        if (TextUtils.isEmpty(json)) return null;
        try {
            return JSONSerializer.createArray(JSON_CREATOR, new JSONArray(json));
        } catch (final JSONException e) {
            return null;
        }
    }

	public static ParcelableMedia newImage(final String media_url, final String url) {
		return new ParcelableMedia(url, media_url, 0, 0, TYPE_IMAGE);
    }

    public static class MediaSize {

        public static final int LARGE = 1;
        public static final int MEDIUM = 2;
        public static final int SMALL = 3;
        public static final int THUMB = 4;


	}

}