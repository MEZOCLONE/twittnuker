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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import org.mariotaku.menucomponent.internal.menu.MenuAdapter;
import org.mariotaku.menucomponent.internal.menu.MenuUtils;

import de.vanita5.twittnuker.fragment.support.BaseSupportDialogFragment;
import de.vanita5.twittnuker.util.ThemeUtils;

public abstract class MenuDialogFragment extends BaseSupportDialogFragment implements OnItemClickListener {

    private MenuAdapter mAdapter;

    @NonNull
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final Context context = getThemedContext();
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        mAdapter = new MenuAdapter(context);
		final ListView listView = new ListView(context);
        listView.setAdapter(mAdapter);
		listView.setOnItemClickListener(this);
		builder.setView(listView);
		final Menu menu = MenuUtils.createMenu(context);
        onCreateMenu(new MenuInflater(context), menu);
        final int itemColor = ThemeUtils.getThemeForegroundColor(context);
        final int highlightColor = ThemeUtils.getUserAccentColor(context);
        ThemeUtils.applyColorFilterToMenuIcon(menu, itemColor, highlightColor, Mode.SRC_ATOP);
        mAdapter.setMenu(menu);
		return builder.create();
    }

    public Context getThemedContext() {
        return getActivity();
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		final Fragment parentFragment = getParentFragment();
		final MenuItem item = (MenuItem) parent.getItemAtPosition(position);
		if (item.hasSubMenu()) {
            mAdapter.setMenu(item.getSubMenu());
		} else if (parentFragment instanceof OnMenuItemClickListener) {
			((OnMenuItemClickListener) parentFragment).onMenuItemClick(item);
			dismiss();
		}
	}

    protected abstract void onCreateMenu(MenuInflater inflater, Menu menu);

}
