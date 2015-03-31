/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.email;

import android.app.Application;
import android.content.res.Configuration;

import com.android.email.preferences.EmailPreferenceMigrator;
import com.android.mail.preferences.BasePreferenceMigrator;
import com.android.mail.preferences.PreferenceMigratorHolder;
import com.android.mail.preferences.PreferenceMigratorHolder.PreferenceMigratorCreator;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.StorageLowState;

import com.mediatek.email.extension.OPExtensionFactory;
import com.mediatek.email.util.EmailLowStorageHandler;
import com.mediatek.mail.vip.VipMemberCache;

public class EmailApplication extends Application {
    private static final String LOG_TAG = "Email";

    static {
        LogTag.setLogTag(LOG_TAG);

        PreferenceMigratorHolder.setPreferenceMigratorCreator(new PreferenceMigratorCreator() {
            @Override
            public BasePreferenceMigrator createPreferenceMigrator() {
                return new EmailPreferenceMigrator();
            }
        });
    }

    /**
     * M: Monitor the configuration change, and update the plugin's context.
     * @see android.app.Application#onConfigurationChanged(android.content.res.Configuration)
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        OPExtensionFactory.resetAllPluginObject(getApplicationContext());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // M: Init the Vip member cache
        VipMemberCache.init(this);
        /// M: Set low storage handler for email.
        StorageLowState.registerHandler(new EmailLowStorageHandler(this));
        /// M: Should active to check the storage state when we register handler to
        //  avoid email launched behind the low storage broadcast.
        StorageLowState.checkStorageLowMode(this);
    }
}
