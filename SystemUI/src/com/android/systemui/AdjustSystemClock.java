package com.android.systemui;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.app.AlarmManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;

public class AdjustSystemClock{
    private static final String TAG = "AdjustSystemClock";
    private Context mContext;
    private boolean stThreadRunning = false;

    public AdjustSystemClock(Context context){
	this.mContext = context;	
    }

    public void onSystemReady(){
        final IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("running.antutu.settimes");
        mContext.registerReceiver(mAdjustClockReceiver,mIntentFilter);
    }

    private BroadcastReceiver mAdjustClockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("running.antutu.settimes")){
                Bundle bundle = intent.getExtras();
                boolean runstatus = bundle.getBoolean("runstatus");
                if(runstatus){
                    if(!stThreadRunning){
                        stThreadRunning = true;
                        new Thread(mAntutuSettimesRunnable).start();
                    }
                }else{
                    stThreadRunning = false;
		    if(isAutoTime()){//set time from network
			Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME,0);
			Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME,1);
		    }else{
			//mAlarmManager.setTime((times+180000));
		    }
			
                }
            }
	}
    };

    private boolean isAutoTime(){
         return Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME, 0) != 0;
	}

    private Runnable mAntutuSettimesRunnable = new Runnable() {
        @Override
        public void run() {
            AlarmManager mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            while(stThreadRunning){
                try {
                    Thread.sleep(2000);
                    long times = SystemClock.currentTimeMicro();
                    System.out.println("times = "+times);
                    //mAlarmManager.setTime((times-75000)/1000);
                    mAlarmManager.setTime((times-1000000)/1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

}

