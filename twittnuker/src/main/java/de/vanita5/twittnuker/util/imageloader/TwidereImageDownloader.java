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

package de.vanita5.twittnuker.util.imageloader;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Pair;

import com.nostra13.universalimageloader.core.assist.ContentLengthInputStream;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import org.mariotaku.restfu.RestRequestInfo;
import org.mariotaku.restfu.annotation.method.GET;
import org.mariotaku.restfu.http.Authorization;
import org.mariotaku.restfu.http.Endpoint;
import org.mariotaku.restfu.http.RestHttpClient;
import org.mariotaku.restfu.http.RestHttpRequest;
import org.mariotaku.restfu.http.RestHttpResponse;
import org.mariotaku.restfu.http.mime.TypedData;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.api.twitter.auth.OAuthAuthorization;
import de.vanita5.twittnuker.api.twitter.auth.OAuthEndpoint;
import de.vanita5.twittnuker.constant.SharedPreferenceConstants;
import de.vanita5.twittnuker.model.ParcelableAccount;
import de.vanita5.twittnuker.model.ParcelableCredentials;
import de.vanita5.twittnuker.model.ParcelableMedia;
import de.vanita5.twittnuker.util.MediaPreviewUtils;
import de.vanita5.twittnuker.util.SharedPreferencesWrapper;
import de.vanita5.twittnuker.util.TwidereLinkify;
import de.vanita5.twittnuker.util.TwitterAPIFactory;
import de.vanita5.twittnuker.util.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TwidereImageDownloader extends BaseImageDownloader implements Constants {

	private final Context mContext;
    private final SharedPreferencesWrapper mPreferences;
    private RestHttpClient mClient;
	private final boolean mFullImage;
	private final String mTwitterProfileImageSize;

	public TwidereImageDownloader(final Context context, final boolean fullImage) {
		super(context);
		mContext = context;
        mPreferences = SharedPreferencesWrapper.getInstance(context, SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE, SharedPreferenceConstants.class);
		mFullImage = fullImage;
		mTwitterProfileImageSize = context.getString(R.string.profile_image_size);
		reloadConnectivitySettings();

	}

	public void reloadConnectivitySettings() {
        mClient = TwitterAPIFactory.getDefaultHttpClient(mContext);
	}

	@Override
	protected InputStream getStreamFromNetwork(final String uriString, final Object extras) throws IOException {
		if (uriString == null) return null;
        final ParcelableMedia media = MediaPreviewUtils.getAllAvailableImage(uriString, mFullImage, mClient);
		try {
			final String mediaUrl = media != null ? media.media_url : uriString;
			if (isTwitterProfileImage(uriString)) {
                final String replaced = Utils.getTwitterProfileImageOfSize(mediaUrl, mTwitterProfileImageSize);
				return getStreamFromNetworkInternal(replaced, extras);
			} else
				return getStreamFromNetworkInternal(mediaUrl, extras);
        } catch (final FileNotFoundException e) {
            if (isTwitterProfileImage(uriString) && !uriString.contains("_normal.")) {
				return getStreamFromNetworkInternal(Utils.getNormalTwitterProfileImage(uriString), extras);
			}
            throw new IOException(String.format(Locale.US, "Error downloading image %s", uriString));
		}
	}

    private Uri getReplacedUri(@NonNull final Uri uri, final String apiUrlFormat) {
        if (apiUrlFormat == null) return uri;
		if (isTwitterUri(uri)) {
			final StringBuilder sb = new StringBuilder();
            final String host = uri.getHost();
            final String domain = host.substring(0, host.lastIndexOf(".twitter.com"));
			final String path = uri.getPath();
            sb.append(TwitterAPIFactory.getApiUrl(apiUrlFormat, domain, path));
			final String query = uri.getQuery();
			if (!TextUtils.isEmpty(query)) {
				sb.append("?");
				sb.append(query);
	}
			final String fragment = uri.getFragment();
			if (!TextUtils.isEmpty(fragment)) {
				sb.append("#");
				sb.append(fragment);
	}
            return Uri.parse(sb.toString());
		}
        return uri;
	}

    private ContentLengthInputStream getStreamFromNetworkInternal(final String uriString, final Object extras) throws IOException {
		final Uri uri = Uri.parse(uriString);
		final Authorization auth;
		final ParcelableCredentials account;
		if (isTwitterAuthRequired(uri) && extras instanceof AccountExtra) {
			final AccountExtra accountExtra = (AccountExtra) extras;
			account = ParcelableAccount.getCredentials(mContext, accountExtra.account_id);
            auth = TwitterAPIFactory.getAuthorization(account);
		} else {
			account = null;
			auth = null;
		}
        Uri modifiedUri = getReplacedUri(uri, account != null ? account.api_url_format : null);

        final List<Pair<String, String>> additionalHeaders = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            additionalHeaders.add(Pair.create("Accept", "image/webp, */*"));
        }
        final String method = GET.METHOD;
        final String requestUri;
        if (auth!= null && auth.hasAuthorization()) {
            final Endpoint endpoint;
            if (auth instanceof OAuthAuthorization) {
                endpoint = new OAuthEndpoint(getEndpoint(modifiedUri), getEndpoint(uri));
            } else {
                endpoint = new Endpoint(getEndpoint(modifiedUri));
        	}
            final List<Pair<String, String>> queries = new ArrayList<>();
            for (String name : uri.getQueryParameterNames()) {
                for (String value : uri.getQueryParameters(name)) {
                    queries.add(Pair.create(name, value));
                }
            }
            final RestRequestInfo info = new RestRequestInfo(method, uri.getPath(), queries, null,
                    additionalHeaders, null, null, null, null);
            additionalHeaders.add(Pair.create("Authorization", auth.getHeader(endpoint, info)));
            requestUri = modifiedUri.toString();
        } else {
            requestUri = modifiedUri.toString();
        }
        final RestHttpResponse resp = mClient.execute(new RestHttpRequest.Builder().method(method).url(requestUri).headers(additionalHeaders).build());
        final TypedData body = resp.getBody();
        return new ContentLengthInputStream(body.stream(), (int) body.length());
	}

    private String getEndpoint(Uri uri) {
        final StringBuilder sb = new StringBuilder();
        sb.append(uri.getScheme());
        sb.append("://");
        sb.append(uri.getHost());
        if (uri.getPort() != -1) {
            sb.append(':');
            sb.append(uri.getPort());
        }
        sb.append("/");
        return sb.toString();
    }

	private boolean isTwitterAuthRequired(final Uri uri) {
        return uri != null && "ton.twitter.com".equalsIgnoreCase(uri.getHost());
	}

	private boolean isTwitterProfileImage(final String uriString) {
        return !TextUtils.isEmpty(uriString) && TwidereLinkify.PATTERN_TWITTER_PROFILE_IMAGES.matcher(uriString).matches();
	}

	private boolean isTwitterUri(final Uri uri) {
        return uri != null && "ton.twitter.com".equalsIgnoreCase(uri.getHost());
	}

}