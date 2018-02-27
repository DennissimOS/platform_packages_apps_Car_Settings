/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.car.settings;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.Fs;
import org.robolectric.res.ResourcePath;

import java.util.List;

/**
 * Custom test runner for the testing of BluetoothPairingDialogs. This is needed because the
 * default behavior for robolectric is just to grab the resource directory in the target package.
 * We want to override this to add several spanning different projects.
 */
public class CarSettingsRobolectricTestRunner extends RobolectricTestRunner {

    /**
     * We don't actually want to change this behavior, so we just call super.
     */
    public CarSettingsRobolectricTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    /**
     * We are going to create our own custom manifest so that we can add multiple resource
     * paths to it. This lets us access resources in both Settings and SettingsLib in our tests.
     */
    @Override
    protected AndroidManifest getAppManifest(Config config) {
        // Using the manifest file's relative path, we can figure out the application directory.
        final String appRoot = "packages/apps/Car/Settings/";
        final String manifestPath = appRoot + "/AndroidManifest.xml";
        final String resDir = appRoot + "/res";
        final String assetsDir = appRoot + config.assetDir();

        // By adding any resources from libraries we need to the AndroidManifest, we can access
        // them from within the parallel universe's resource loader.
        return new AndroidManifest(Fs.fileFromPath(manifestPath), Fs.fileFromPath(resDir),
            Fs.fileFromPath(assetsDir), "com.android.car.settings") {
            @Override
            public List<ResourcePath> getIncludedResourcePaths() {
                List<ResourcePath> paths = super.getIncludedResourcePaths();
                paths.add(new ResourcePath(
                        null,
                        Fs.fileFromPath("./packages/apps/Car/Settings/res"),
                        null));
                paths.add(new ResourcePath(
                        null,
                        Fs.fileFromPath("./frameworks/base/packages/SettingsLib/res"),
                        null));
                paths.add(new ResourcePath(
                        null,
                        Fs.fileFromPath("./frameworks/base/core/res/res"),
                        null));
                paths.add(new ResourcePath(
                        null,
                        Fs.fileFromPath("./frameworks/support/car/res"),
                        null));
                paths.add(new ResourcePath(
                        null,
                        Fs.fileFromPath("./frameworks/support/v7/appcompat/res"),
                        null));
                paths.add(new ResourcePath(
                        null,
                        Fs.fileFromPath("./packages/apps/Car/libs/car-stream-ui-lib/res"),
                        null));
                paths.add(new ResourcePath(
                        null,
                        Fs.fileFromPath("./packages/apps/Car/libs/car-list/res"),
                        null));
                return paths;
            }
        };
    }
}
