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

package de.vanita5.twittnuker.api.twitter.model.impl;

import java.util.ArrayList;

import de.vanita5.twittnuker.api.twitter.model.QueryResult;
import de.vanita5.twittnuker.api.twitter.model.Status;

/**
 * Created by mariotaku on 15/5/7.
 */
public class QueryResultImpl extends ResponseListImpl<Status> implements QueryResult {

	private final QueryResultWrapper.SearchMetadata metadata;

	@Override
	public double getCompletedIn() {
		return metadata.completedIn;
	}

	@Override
	public long getMaxId() {
		return metadata.maxId;
	}

	@Override
	public String getQuery() {
		return metadata.query;
	}

	@Override
	public int getResultsPerPage() {
		return metadata.count;
	}

	@Override
	public long getSinceId() {
		return metadata.sinceId;
	}

	@Override
	public String getWarning() {
		return metadata.warning;
	}

	public QueryResultImpl(ArrayList<Status> statuses, QueryResultWrapper.SearchMetadata metadata) {
		addAll(statuses);
		this.metadata = metadata;
	}
}