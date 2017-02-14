
package com.android.systemui.statusbar.policy;

public interface BatteryStateRegistar {
    interface BatteryStateChangeCallback {
        void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging);
        void onPowerSaveChanged();
        void onBatteryStyleChanged(int style, int percentMode);
    }

    public void addStateChangedCallback(BatteryStateChangeCallback cb);
    public void removeStateChangedCallback(BatteryStateChangeCallback cb);
}
