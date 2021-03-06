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

package de.vanita5.twittnuker.fragment.support;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.fragment.iface.IMapFragment;
import de.vanita5.twittnuker.util.webkit.DefaultWebViewClient;

public class WebMapFragment extends BaseSupportWebViewFragment implements IMapFragment {

	private static final String MAPVIEW_URI = "file:///android_asset/mapview.html";

	private double latitude, longitude;

	@Override
	public void center() {
		final WebView webview = getWebView();
		webview.loadUrl("javascript:center();");
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case MENU_CENTER: {
				center();
				break;
			}
		}
		return true;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_google_maps_viewer, menu);
	}


	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		getLocation();
		setupWebView();
	}

	/**
	 * The Location Manager manages location providers. This code searches for
	 * the best provider of data (GPS, WiFi/cell phone tower lookup, some other
	 * mechanism) and finds the last known location.
	 */
	private void getLocation() {
		final Bundle bundle = getArguments();
		if (bundle != null) {
			latitude = bundle.getDouble(EXTRA_LATITUDE, 0.0);
			longitude = bundle.getDouble(EXTRA_LONGITUDE, 0.0);
		}
	}

	/**
	 * Sets up the WebView object and loads the URL of the page *
	 */
	private void setupWebView() {

		final WebView webview = getWebView();
		webview.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		webview.setWebViewClient(new MapWebViewClient(getActivity()));
		webview.loadUrl(MAPVIEW_URI);

		final WebSettings settings = webview.getSettings();
		settings.setBuiltInZoomControls(false);

		/** Allows JavaScript calls to access application resources **/
		webview.addJavascriptInterface(new MapJavaScriptInterface(), "android");

	}

	/**
	 * Sets up the interface for getting access to Latitude and Longitude data
	 * from device
	 */
	class MapJavaScriptInterface {

		@JavascriptInterface
		public double getLatitude() {
			return latitude;
		}

		@JavascriptInterface
		public double getLongitude() {
			return longitude;
		}

	}

	class MapWebViewClient extends DefaultWebViewClient {

		public MapWebViewClient(final Activity activity) {
			super(activity);
		}

		@Override
		public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
			final Uri uri = Uri.parse(url);
			if (uri.getScheme().equals(Uri.parse(MAPVIEW_URI).getScheme())) return false;
			final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(intent);
			return true;
		}
	}

}