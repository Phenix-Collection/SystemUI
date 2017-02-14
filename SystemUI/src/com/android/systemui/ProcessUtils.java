/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.systemui;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import java.lang.Override;
import java.lang.Runnable;
import java.lang.Thread;
import java.util.List;

// hsp 2016-06-24 : Kill third part app's processes when memory is low

public class ProcessUtils {
    private static final String TAG = "ProcessUtils";

    public static long killProcess(Context context, boolean restarted) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        long availMem = getAvaiMemory(am);

        kill(am, ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND, restarted);

        Log.d(TAG, "zolen tag : Has release " + (getAvaiMemory(am) - availMem) + "M memory");
        return getAvaiMemory(am) - availMem;
    }

    private static void kill(ActivityManager am, int important, boolean restarted) {
        List<ActivityManager.RunningAppProcessInfo> infoList = am.getRunningAppProcesses();
        if (infoList != null && infoList.size() > 0) {
            for (int i = 0; i < infoList.size(); i++) {
                ActivityManager.RunningAppProcessInfo processInfo = infoList.get(i);
                if (processInfo.importance >= important) {
                    String[] pkgList = processInfo.pkgList;
                    for (int j = 0; j < pkgList.length; j++) {
                        String packageName = pkgList[j];
                        // Don't kill system process
                        if (!packageName.contains("android") && !packageName.contains("launcher") && am != null) {
                            Log.d(TAG, "zolen tag : kill :" + packageName + ", restarted : " + restarted);
                            if (restarted) {
                                //Kill process but can restart
                                am.killBackgroundProcesses(packageName);
                            } else {
                                //Kill process but can not restart
                                am.forceStopPackage(packageName);
                            }
                        }
                    }
                }
            }
        }
    }

    public static long getAvaiMemory(ActivityManager am) {
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(info);
        return info.availMem / (1024 * 1024);
    }
}