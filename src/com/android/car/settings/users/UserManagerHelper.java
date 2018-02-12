/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.car.settings.users;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import com.android.car.settings.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class for managing users, providing methods for removing, adding and switching users.
 */
public final class UserManagerHelper {
    private static final String TAG = "UserManagerHelper";
    private final Context mContext;
    private OnUsersUpdateListener mUpdateListener;

    private final UserManager mUserManager;

    private final BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mUpdateListener.onUsersUpdate();
        }
    };

    /**
     * Interface for listeners that want to register for receiving updates to changes to the users
     * on the system including removing and adding users, and changing user info.
     */
    public interface OnUsersUpdateListener {
        /**
         * Method that will get called when users list has been changed.
         */
        void onUsersUpdate();
    }

    public UserManagerHelper(Context context) {
        mContext = context;
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
    }

    /**
     * Registers a listener for updates to all users - removing, adding users or changing user info.
     *
     * @param listener Instance of {@link OnUsersUpdateListener}.
     */
    public void registerOnUsersUpdateListener(OnUsersUpdateListener listener) {
        mUpdateListener = listener;
        registerReceiver();
    }

    /**
     * Unregisters listener by unregistering {@code BroadcastReceiver}.
     */
    public void unregisterOnUsersUpdateListener() {
        unregisterReceiver();
    }

    /**
     * Gets {@link UserInfo} for the current user.
     *
     * @return {@link UserInfo} for the current user.
     */
    public UserInfo getCurrentUserInfo() {
        return mUserManager.getUserInfo(UserHandle.myUserId());
    }

    /**
     * Sets new Username for the user.
     *
     * @param user User who's name should be changed.
     * @param name New username.
     */
    public void setUserName(UserInfo user, String name) {
        mUserManager.setUserName(user.id, name);
    }

    /**
     * Gets all the other users on the system that are not the current user.
     *
     * @return List of {@code UserInfo} for each user that is not the current user.
     */
    public List<UserInfo> getOtherUsers() {
        List<UserInfo> others = mUserManager.getUsers(true);

        for (Iterator<UserInfo> iterator = others.iterator(); iterator.hasNext();) {
            UserInfo userInfo = iterator.next();
            if (userInfo.id == UserHandle.myUserId()) {
                // Remove current user from the list.
                iterator.remove();
            }
        }
        return others;
    }

    /**
     * Gets an icon for the user.
     *
     * @param userInfo User for which we want to get the icon.
     * @return a drawable for the icon
     */
    public Drawable getUserIcon(UserInfo userInfo) {
        Bitmap picture = mUserManager.getUserIcon(userInfo.id);

        if (picture == null) {
            return mContext.getDrawable(R.drawable.ic_user);
        }

        int avatarSize = mContext.getResources()
                .getDimensionPixelSize(R.dimen.car_primary_icon_size);
        picture = Bitmap.createScaledBitmap(
                picture, avatarSize, avatarSize, true /* filter */);
        return new BitmapDrawable(mContext.getResources(), picture);
    }

    /**
     * Creates a new user on the system.
     */
    public UserInfo createNewUser() {
        UserInfo user = mUserManager.createUser(
                mContext.getString(R.string.user_new_user_name), 0 /* flags */);
        if (user == null) {
            // Couldn't create user, most likely because there are too many, but we haven't
            // been able to reload the list yet.
            Log.w(TAG, "can't create user.");
            return null;
        }
        return user;
    }

    /**
     * Tries to remove the user that's passed in. System user cannot be removed.
     * If the user to be removed is current user, it switches to the system user first, and then
     * removes the user.
     *
     * @param userInfo User to be removed
     * @return {@code true} if user is successfully removed, {@code false} otherwise.
     */
    public boolean removeUser(UserInfo userInfo) {
        if (userInfo.id == UserHandle.USER_SYSTEM) {
            Log.w(TAG, "User " + userInfo.id + " is system user, could not be removed.");
            return false;
        }

        if (userInfo.id == getCurrentUserInfo().id) {
            switchToUserId(UserHandle.USER_SYSTEM);
        }

        return mUserManager.removeUser(userInfo.id);
    }

    /**
     * Checks whether the user is system user (admin).
     *
     * @param userInfo User to check against system user.
     * @return {@code true} if system user, {@code false} otherwise.
     */
    public boolean userIsSystemUser(UserInfo userInfo) {
        return userInfo.id == UserHandle.USER_SYSTEM;
    }

    /**
     * Returns whether this user can be removed from the system.
     *
     * @param userInfo User to be removed
     * @return {@code true} if they can be removed, {@code false} otherwise.
     */
    public boolean userCanBeRemoved(UserInfo userInfo) {
        return !userIsSystemUser(userInfo);
    }

    /**
     * Checks whether currently logged in user is also the system user (admin).
     *
     * @return {@code true} if current user is admin, {@code false} otherwise.
     */
    public boolean currentUserIsSystemUser() {
        return userIsSystemUser(getCurrentUserInfo());
    }

    /**
     * Checks whether passed in user is the user that's currently logged in.
     *
     * @param userInfo User to check.
     * @return {@code true} if current user, {@code false} otherwise.
     */
    public boolean userIsCurrentUser(UserInfo userInfo) {
        return getCurrentUserInfo().id == userInfo.id;
    }

    /**
     * Switches (logs in) to another user.
     *
     * @param userInfo User to switch to.
     */
    public void switchToUser(UserInfo userInfo) {
        if (userInfo.id == getCurrentUserInfo().id) {
            return;
        }

        if (userInfo.isGuest()) {
            switchToGuest();
            return;
        }

        if (UserManager.isGuestUserEphemeral()) {
            // If switching from guest, we want to bring up the guest exit dialog instead of
            // switching
            UserInfo currUserInfo = getCurrentUserInfo();
            if (currUserInfo != null && currUserInfo.isGuest()) {
                return;
            }
        }

        switchToUserId(userInfo.id);
    }

    /**
     * Creates a guest session and switches into the guest session.
     */
    public void switchToGuest() {
        UserInfo guest = mUserManager.createGuest(mContext,
                mContext.getString(R.string.user_guest));
        if (guest == null) {
            // Couldn't create user, most likely because there are too many, but we haven't
            // been able to reload the list yet.
            Log.w(TAG, "can't create user.");
            return;
        }
        switchToUserId(guest.id);
    }

    private void switchToUserId(int id) {
        try {
            ActivityManager.getService().switchUser(id);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't switch user.", e);
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_REMOVED);
        filter.addAction(Intent.ACTION_USER_ADDED);
        filter.addAction(Intent.ACTION_USER_INFO_CHANGED);
        mContext.registerReceiverAsUser(mUserChangeReceiver, UserHandle.ALL, filter, null, null);
    }

    private void unregisterReceiver() {
        mContext.unregisterReceiver(mUserChangeReceiver);
    }
}
