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

package de.vanita5.twittnuker.api.twitter.api;

import org.mariotaku.restfu.annotation.method.GET;
import org.mariotaku.restfu.annotation.method.POST;
import org.mariotaku.restfu.annotation.param.Body;
import org.mariotaku.restfu.annotation.param.Form;
import org.mariotaku.restfu.annotation.param.Query;
import org.mariotaku.restfu.http.BodyType;
import de.vanita5.twittnuker.api.twitter.TwitterException;
import de.vanita5.twittnuker.api.twitter.model.PageableResponseList;
import de.vanita5.twittnuker.api.twitter.model.Paging;
import de.vanita5.twittnuker.api.twitter.model.ResponseList;
import de.vanita5.twittnuker.api.twitter.model.Status;
import de.vanita5.twittnuker.api.twitter.model.User;
import de.vanita5.twittnuker.api.twitter.model.UserList;
import de.vanita5.twittnuker.api.twitter.model.UserListUpdate;

@SuppressWarnings("RedundantThrows")
public interface ListsResources {
    @POST("/lists/members/create.json")
    @Body(BodyType.FORM)
    UserList addUserListMember(@Query("list_id") long listId, @Query("user_id") long userId) throws TwitterException;

    @POST("/lists/members/create.json")
    @Body(BodyType.FORM)
    UserList addUserListMember(@Query("list_id") long listId, @Query("screen_name") String userScreenName) throws TwitterException;

    @POST("/lists/members/create_all.json")
    @Body(BodyType.FORM)
    UserList addUserListMembers(@Form("list_id") long listId, @Form("user_id") long[] userIds) throws TwitterException;

    @POST("/lists/members/create_all.json")
    @Body(BodyType.FORM)
    UserList addUserListMembers(@Form("list_id") long listId, @Form("screen_name") String[] screenNames) throws TwitterException;

    @POST("/lists/create.json")
    @Body(BodyType.FORM)
    UserList createUserList(@Form UserListUpdate update) throws TwitterException;

    @POST("/lists/subscribers/create.json")
    @Body(BodyType.FORM)
    UserList createUserListSubscription(@Form("list_id") long listId) throws TwitterException;

    @POST("/lists/members/destroy.json")
    @Body(BodyType.FORM)
    UserList deleteUserListMember(@Query("list_id") long listId, @Query("user_id") long userId) throws TwitterException;

    @POST("/lists/members/destroy.json")
    @Body(BodyType.FORM)
    UserList deleteUserListMember(@Query("list_id") long listId, @Form("screen_name") String screenName) throws TwitterException;

    @POST("/lists/members/destroy_all.json")
    @Body(BodyType.FORM)
    UserList deleteUserListMembers(@Form("list_id") long listId, @Form("user_id") long[] userIds) throws TwitterException;

    @POST("/lists/members/destroy_all.json")
    @Body(BodyType.FORM)
    UserList deleteUserListMembers(@Query("list_id") long listId, @Form("screen_name") String[] screenNames) throws TwitterException;

    @POST("/lists/destroy.json")
    @Body(BodyType.FORM)
    UserList destroyUserList(@Form("list_id") long listId) throws TwitterException;

    @POST("/lists/subscribers/destroy.json")
    @Body(BodyType.FORM)
    UserList destroyUserListSubscription(@Form("list_id") long listId) throws TwitterException;

    @GET("/lists/members.json")
    PageableResponseList<User> getUserListMembers(@Query("list_id") long listId, @Query Paging paging) throws TwitterException;

    @GET("/lists/members.json")
    PageableResponseList<User> getUserListMembers(@Query("slug") String slug, @Query("owner_id") long ownerId, @Query Paging paging)
			throws TwitterException;

    @GET("/lists/members.json")
    PageableResponseList<User> getUserListMembers(@Query("slug") String slug, @Query("owner_screen_name") String ownerScreenName, @Query Paging paging)
			throws TwitterException;

    @GET("/lists/memberships.json")
    PageableResponseList<UserList> getUserListMemberships(@Query Paging paging) throws TwitterException;

    @GET("/lists/memberships.json")
    PageableResponseList<UserList> getUserListMemberships(@Query("user_id") long listMemberId, @Query Paging paging) throws TwitterException;

    @GET("/lists/memberships.json")
    PageableResponseList<UserList> getUserListMemberships(@Query("user_id") long listMemberId, @Query Paging paging,
                                                          @Query("filter_to_owned_lists") boolean filterToOwnedLists) throws TwitterException;

    @GET("/lists/memberships.json")
    PageableResponseList<UserList> getUserListMemberships(@Query("screen_name") String listMemberScreenName, @Query Paging paging)
			throws TwitterException;

    @GET("/lists/ownerships.json")
    PageableResponseList<UserList> getUserListMemberships(@Query("screen_name") String listMemberScreenName, @Query Paging paging,
			boolean filterToOwnedLists) throws TwitterException;

    @GET("/lists/ownerships.json")
    PageableResponseList<UserList> getUserListOwnerships(@Query Paging paging) throws TwitterException;

    @GET("/lists/ownerships.json")
    PageableResponseList<UserList> getUserListOwnerships(@Query("user_id") long listMemberId, @Query Paging paging) throws TwitterException;

    @GET("/lists/ownerships.json")
    PageableResponseList<UserList> getUserListOwnerships(@Query("screen_name") String listMemberScreenName, @Query Paging paging)
            throws TwitterException;

    @GET("/lists/list.json")
    ResponseList<UserList> getUserLists(@Query("user_id") long userId, @Query("reverse") boolean reverse) throws TwitterException;

    @GET("/lists/list.json")
    ResponseList<UserList> getUserLists(@Query("screen_name") String screenName, @Query("reverse") boolean reverse) throws TwitterException;

    @GET("/lists/statuses.json")
    ResponseList<Status> getUserListStatuses(@Query("list_id") long listId, @Query Paging paging) throws TwitterException;

    @GET("/lists/statuses.json")
    ResponseList<Status> getUserListStatuses(@Query("slug") String slug, @Query("owner_id") long ownerId, @Query Paging paging) throws TwitterException;

    @GET("/lists/statuses.json")
    ResponseList<Status> getUserListStatuses(@Query("slug") String slug, @Query("owner_screen_name") String ownerScreenName, @Query Paging paging)
			throws TwitterException;

    @GET("/lists/subscribers.json")
    PageableResponseList<User> getUserListSubscribers(@Query("list_id") long listId, @Query Paging paging) throws TwitterException;

    @GET("/lists/subscribers.json")
    PageableResponseList<User> getUserListSubscribers(@Query("list_id") String slug, @Query("owner_id") long ownerId, @Query Paging paging)
			throws TwitterException;

    @GET("/lists/subscribers.json")
    PageableResponseList<User> getUserListSubscribers(@Query("list_id") String slug, @Query("owner_screen_name") String ownerScreenName, @Query Paging paging)
			throws TwitterException;


    @GET("/lists/subscriptions.json")
    PageableResponseList<UserList> getUserListSubscriptions(@Query("screen_name") String listOwnerScreenName, long cursor)
			throws TwitterException;

    @GET("/lists/subscriptions.json")
    PageableResponseList<UserList> getUserListSubscriptions(@Query("user_id") long userId, long cursor)
            throws TwitterException;

    @GET("/lists/show.json")
    UserList showUserList(@Query("list_id") long listId) throws TwitterException;

    @GET("/lists/show.json")
    UserList showUserList(@Query("slug") String slug, @Query("owner_id") long ownerId) throws TwitterException;

    @GET("/lists/show.json")
    UserList showUserList(@Query("slug") String slug, @Query("owner_screen_name") String ownerScreenName) throws TwitterException;

    @POST("/lists/update.json")
    @Body(BodyType.FORM)
    UserList updateUserList(@Form("list_id") long listId, @Form UserListUpdate update) throws TwitterException;
}