package com.android.systemui.applock;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.app.ActionBar;
import android.view.MenuItem;

import java.io.IOException;
import android.os.RemoteException;
import android.util.Log;

import android.os.Message;
import android.os.Handler;
import android.content.Intent;
import android.app.ActivityManager;

import android.view.Window;
import android.view.WindowManager;

import com.android.systemui.R;

import android.os.Vibrator;
import android.content.res.Resources;
import android.widget.ImageView;
import android.content.Context;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.VerifyCredentialResponse;
import android.provider.Settings.SettingNotFoundException;
//import android.app.qcfingerprint.IFc909ResultListener;
//import android.app.qcfingerprint.QcFingerprintManager;
import android.os.PowerManager;
import com.android.internal.widget.LockPatternChecker;
import android.os.UserHandle;
import com.android.keyguard.KeyguardUpdateMonitor;
import android.os.CancellationSignal;
import android.app.ActivityManager;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.os.CountDownTimer;
import android.os.UserManager;
import android.os.SystemClock;

import android.content.ComponentName;
import android.os.SystemProperties;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.os.UEventObserver;

public class ConfirmAppLockPasswordActivity extends Activity implements OnClickListener{
    private static final String FINGERPRINT_APPLOCK_PASSWORD = "fingerprint.applock.password";
    private static final String FINGERPRINT_APPLOCK_PASSWORD_STRING = "fingerprint.applock.password.string";
    private static final String SETTINGS_LAST_PACKAGE_NAME = "setting.lastpackage.name";
    private static final String SETTINGS_NEEDLOCK_APP_PACKAGENAMES = "needlock_app_packagenames";
    private static final String FINGERPRINT_KEYGUARD_USE = "com.fingerprint.enable";
    private static final String FINGERPRINT_UNLOCK_APP = "persist.sys.fp_unlock_app";
    private static final String GF_MODE = "/sys/bus/spi/devices/spi0.0/gf_mode/gf_mode";
    private static final String PERSIST_NAVIGATION_BAR = "persist.sys.navg_bar_visible";
    private static String DEV_PATH = "DEVPATH=/devices/soc.0/78b7000.spi/spi_master/spi0/spi0.0/goodix_fp/goodix_fp";

    private static boolean DEBUG = true;

    private String mGlobalPackageNameString = null;
    private String mExtraClassName = null;
    private String mrunmode = null;
    /* resume from unlock screen */
    private boolean resumefromUS = false;

    private static final int MSG_FP_ONSUCCESS = 1;
    private static final int MSG_FP_ONFAIL = 2;
    private static final int MSG_FP_CANCEL = 3;
    private static final int MSG_FP_CONNECTED = 4;
    private boolean mConnectionStatus = false;
    private final static String TAG = "cdfinger";

    private int MinPassword = 4;
    private int MaxPassword = 6;
    private Button mkey1;
    private Button mkey2;
    private Button mkey3;
    private Button mkey4;
    private Button mkey5;
    private Button mkey6;
    private Button mkey7;
    private Button mkey8;
    private Button mkey9;
    private Button mkey0;
    private EditText mEditText;
    private Button cancel;
    private TextView header;
    private TextView noteheader;
    private Button clear;
    private String edtiString = null;
    private String FirstInputString = null;
    private boolean firstinput = false;
    private String headerString = null;
    private String noteHeaderStr = null;
    private boolean activityStatus = false;
    private LockPatternUtils mLockPatternUtils;
    private int passwordLength = 0;
	//private QcFingerprintManager mQcFingerprintManager;
    private PowerManager mPowerManager;
    private boolean fsavailable = false;
    private KeyguardUpdateMonitor updateMonitor;
    private CancellationSignal mFingerprintCancelSignal;
    private FingerprintManager mFpm;
    private VerifyCredentialResponse response;
    //private boolean passwordIsNotMatch = false;
    private int EffectiveUserId;
    private Context mContext;
    private ActivityManager am;
    private boolean AuthenticationRunning = false;
    private boolean isObserving = false;
    private int mFailedAttempts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);         
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //        | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);  
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN  
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);  
                        //| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION  
                        //| View.SYSTEM_UI_FLAG_LAYOUT_STABLE);  
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS | WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);  
        
        window.setStatusBarColor(Color.TRANSPARENT);  
        //window.setNavigationBarColor(Color.TRANSPARENT);    

        setContentView(R.layout.applock_confirm_password);

	mContext = this;

        setImmerseLayout(findViewById(R.id.common_back));
        initBackButton();
        setTitleBar(R.string.confirm_password_title);

        mLockPatternUtils = new LockPatternUtils(this);
        passwordLength = mLockPatternUtils.getAppLockPasswordLength();
        /*int reval = Settings.System.getInt(getContentResolver(),"persist.sys.fsavailable", 0);
        if(reval == 1){
            fsavailable = true;
        }else{
            fsavailable = false;
        }*/
	
	updateMonitor = KeyguardUpdateMonitor.getInstance(this);
	mFpm = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
	
        if(updateMonitor.isUnlockWithFingerprintPossible(ActivityManager.getCurrentUser()) && isUnlockAppEnable() && (!mFpm.isFpUnlockAppLock())){
            headerString = getResources().getString(R.string.confirm_password_header_title2);
        }else{
            headerString = getResources().getString(R.string.confirm_password_header_title1);
        }
	if(mFpm.isFpUnlockAppLock()){
	    noteHeaderStr = getResources().getString(R.string.confirm_password_noteheader_title);
	}else{
	    noteHeaderStr = "";
	}

        if(mPowerManager == null){
            mPowerManager = (PowerManager)getSystemService( Context.POWER_SERVICE);
        }

	EffectiveUserId = getEffectiveUserId(this);

        View v1 = findViewById(R.id.first);
        View v2 = findViewById(R.id.second);
        View v3 = findViewById(R.id.third);
        View v4 = findViewById(R.id.fourth);

        mkey1 = (Button)v1.findViewById(R.id.key_left);
        mkey2 = (Button)v1.findViewById(R.id.key_middle);
        mkey3 = (Button)v1.findViewById(R.id.key_right);

        mkey4 = (Button)v2.findViewById(R.id.key_left);
        mkey5 = (Button)v2.findViewById(R.id.key_middle);
        mkey6 = (Button)v2.findViewById(R.id.key_right);

        mkey7 = (Button)v3.findViewById(R.id.key_left);
        mkey8 = (Button)v3.findViewById(R.id.key_middle);
        mkey9 = (Button)v3.findViewById(R.id.key_right);

        mkey0 = (Button)v4.findViewById(R.id.key_middle);
        v4.findViewById(R.id.key_left).setEnabled(false);
        v4.findViewById(R.id.key_right).setEnabled(false);

        mEditText = (EditText) findViewById(R.id.edit1);
        cancel = (Button) findViewById(R.id.cancel);
        header = (TextView) findViewById(R.id.header);
        noteheader = (TextView) findViewById(R.id.note_header);
	noteheader.setText(noteHeaderStr);
        clear = (Button) findViewById(R.id.clear);
        clear.setVisibility(View.GONE);
       
        mkey1.setText("1");
        mkey2.setText("2");
        mkey3.setText("3");
        mkey4.setText("4");
        mkey5.setText("5");
        mkey6.setText("6");
        mkey7.setText("7");
        mkey8.setText("8");
        mkey9.setText("9");
        mkey0.setText("0");

        header.setText(headerString);
        cancel.setText(getResources().getString(R.string.confirm_password_cancel));

        mkey1.setOnClickListener(this);
        mkey2.setOnClickListener(this);
        mkey3.setOnClickListener(this);
        mkey4.setOnClickListener(this);
        mkey5.setOnClickListener(this);
        mkey6.setOnClickListener(this);
        mkey7.setOnClickListener(this);
        mkey8.setOnClickListener(this);
        mkey9.setOnClickListener(this);
        mkey0.setOnClickListener(this);

	am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

	resumefromUS = getIntent().getBooleanExtra("resumefromUS",false);

        clear.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                edtiString = null;
                mEditText.setText("");
                clear.setVisibility(View.GONE);
            }
        });

        cancel.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
		if(resumefromUS){
                    startLauncher();
		}else {
		    removeActivity();
		}
                finish();
            }
        });

    }
    private int getEffectiveUserId(Context context) {
        UserManager um = UserManager.get(context);
        if (um != null) {
            return um.getCredentialOwnerProfile(UserHandle.myUserId());
        } else {
            Log.e(TAG, "Unable to acquire UserManager");
            return UserHandle.myUserId();
        }
    }

    private boolean isUnlockAppEnable(){
        return SystemProperties.getBoolean(FINGERPRINT_UNLOCK_APP, false) ? true : false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityStatus = true;
        System.out.println("onResume...");
	int userId = ActivityManager.getCurrentUser();
	long deadline = mLockPatternUtils.getAppLockoutAttemptDeadline(EffectiveUserId); 
	if(deadline >0){
	    countDownTimes(deadline);	
	}else{
        if (updateMonitor.isUnlockWithFingerprintPossible(userId) && isUnlockAppEnable() && (!mFpm.isFpUnlockAppLock())) {
	    if(!updateMonitor.isFingerUp){
		mgfObserver.startObserving(DEV_PATH);
		isObserving = true;
		return;
	    }
            if (mFingerprintCancelSignal != null) {
                mFingerprintCancelSignal.cancel();
            }
            mFingerprintCancelSignal = new CancellationSignal();
            mFpm.authenticate(null, mFingerprintCancelSignal, 0, mAuthenticationCallback, null, userId);
	    updateMonitor.setFingerprintRunningState(updateMonitor.FINGERPRINT_STATE_RUNNING);
	    AuthenticationRunning = true;
	    updateMonitor.applock_verify=true;
	    mFpm.setFpUnlockApp(true);
	    if(!SystemProperties.getBoolean(PERSIST_NAVIGATION_BAR, false)){
	    	writeFile(GF_MODE,"1");
	    }
        }
	}
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(DEBUG) Log.d("cdfinger","onPause");
        activityStatus = false;
	if(isObserving){
	    mgfObserver.stopObserving();
	    isObserving = false;
	}
        //if (updateMonitor.mFingerprintRunningState == updateMonitor.FINGERPRINT_STATE_RUNNING && mFingerprintCancelSignal != null) {
        if (AuthenticationRunning && mFingerprintCancelSignal != null) {
            if(DEBUG) Log.d("cdfinger","onPause, mFingerprintCancelSignal.cancel");
	    updateMonitor.applock_cancle_verify=true;
            mFingerprintCancelSignal.cancel();
            mFingerprintCancelSignal = null;
	    AuthenticationRunning = false;
	    updateMonitor.setFingerprintRunningState(updateMonitor.FINGERPRINT_STATE_CANCELLING);
	    updateMonitor.applock_verify=false;
	    mFpm.setFpUnlockApp(false);
        }
	if(!SystemProperties.getBoolean(PERSIST_NAVIGATION_BAR, false)){
	    writeFile(GF_MODE,"0");
	}
	mFailedAttempts = 0;
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(DEBUG) Log.d("cdfinger","onStop");
    }

    @Override
    public void onDestroy (){
        super.onDestroy();
        if(DEBUG) Log.d("cdfinger","onDestroy");
    }
   
    @Override
    public void onBackPressed() {
	if(resumefromUS){
            startLauncher();
	}else{
	    removeActivity();
	}
        super.onBackPressed();
    }
 
    @Override
    public void onClick(View view) {
        String inpuString = null;
           
	if(mLockPatternUtils.getAppLockoutAttemptDeadline(EffectiveUserId) > 0)return; 
        if(clear.getVisibility() == View.GONE){
            clear.setVisibility(View.VISIBLE);
        }
            
        if(view == mkey1){
            inpuString = mkey1.getText().toString();
        }else if(view == mkey2){
            inpuString = mkey2.getText().toString();
        }else if(view == mkey3){
            inpuString = mkey3.getText().toString();
        }else if(view == mkey4){
            inpuString = mkey4.getText().toString();
        }else if(view == mkey5){
            inpuString = mkey5.getText().toString();
        }else if(view == mkey6){
            inpuString = mkey6.getText().toString();
        }else if(view == mkey7){
            inpuString = mkey7.getText().toString();
        }else if(view == mkey8){
            inpuString = mkey8.getText().toString();
        }else if(view == mkey9){
            inpuString = mkey9.getText().toString();
        }else if(view == mkey0){
            inpuString = mkey0.getText().toString();
        }
            
        if(edtiString == null){
            edtiString = inpuString;
	    header.setText(headerString);
	    /*
	    if(passwordIsNotMatch){
		header.setText("Confirm AppLock Password");
	    }
	    */
        }else{
            if(edtiString.length() < MaxPassword){
                edtiString = edtiString+inpuString;
            }
        }
            
        mEditText.setText(edtiString);
        mEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());      
           
            if(edtiString.length() == passwordLength){
                //if (mLockPatternUtils.checkAppLockPassword(edtiString)) {
		//response = mLockPatternUtils.checkAppLockPassword(edtiString,UserHandle.myUserId());

                LockPatternChecker.checkApplockPassword(
                        mLockPatternUtils,
                        edtiString,
                        EffectiveUserId,
                        new LockPatternChecker.OnCheckCallback() {
                            @Override
                            public void onChecked(boolean matched, int timeoutMs) {
				if(matched){
   		                    DeletePackageName();
                		    StartProtectedActivity();
				    mFpm.setFpUnlockAppLock(false);
                    		    finish();
				}else{
				    if(timeoutMs > 0){
		                        mEditText.setText("");
                    			edtiString = null;
                    			if(clear.getVisibility() == View.VISIBLE){
                        		    clear.setVisibility(View.GONE);
                    			}
		                        long deadline = mLockPatternUtils.setAppLockoutAttemptDeadline(EffectiveUserId, timeoutMs);
                		        countDownTimes(deadline);
				    }else{
                    			header.setText(getResources().getString(R.string.confirm_password_header_errorstring1));
		                        mEditText.setText("");
                    			edtiString = null;
                    			if(clear.getVisibility() == View.VISIBLE){
                        		    clear.setVisibility(View.GONE);
                    			}
				    }
				}
                            }
                        });
		/*
		if (LockPatternChecker.checkAppLockPassword(mLockPatternUtils,edtiString,UserHandle.myUserId())) {
                    DeletePackageName();
                    StartProtectedActivity();
                    finish();
                }else{
                    header.setText(getResources().getString(R.string.confirm_password_header_errorstring1));
		    //passwordIsNotMatch = true;
                    mEditText.setText("");
                    edtiString = null;
                    if(clear.getVisibility() == View.VISIBLE){
                        clear.setVisibility(View.GONE);
                    }

                }*/
            }
    }
    private void countDownTimes(long times){
	long elapsedRealtime = SystemClock.elapsedRealtime();
	new CountDownTimer(times-elapsedRealtime, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
		int str_id = R.string.confirm_password_header_errorstring4;
		final String msg = mContext.getString(str_id,millisUntilFinished / 1000);
                header.setText(msg);
		if (updateMonitor.mFingerprintRunningState == updateMonitor.FINGERPRINT_STATE_RUNNING && mFingerprintCancelSignal != null) {
            		mFingerprintCancelSignal.cancel();
            		mFingerprintCancelSignal = null;
            		updateMonitor.setFingerprintRunningState(updateMonitor.FINGERPRINT_STATE_CANCELLING);
        	}
            }

            @Override
            public void onFinish() {
        	header.setText(headerString);
	        if (updateMonitor.isUnlockWithFingerprintPossible(UserHandle.myUserId()) && isUnlockAppEnable() && (!mFpm.isFpUnlockAppLock())) {
        	    if (mFingerprintCancelSignal != null) {
                	mFingerprintCancelSignal.cancel();
            	    }
            	    mFingerprintCancelSignal = new CancellationSignal();
            	    mFpm.authenticate(null, mFingerprintCancelSignal, 0, mAuthenticationCallback, null, UserHandle.myUserId());
            	    updateMonitor.setFingerprintRunningState(updateMonitor.FINGERPRINT_STATE_RUNNING);
		    AuthenticationRunning = true;
	    	    if(!SystemProperties.getBoolean(PERSIST_NAVIGATION_BAR, false)){
	    		writeFile(GF_MODE,"1");
		    }
        	}
            }
	}.start();

    }

    private void DeletePackageName(){
        mGlobalPackageNameString = getIntent().getStringExtra("packagename");
        mExtraClassName = getIntent().getStringExtra("classname");
        mrunmode = getIntent().getStringExtra("runmode");
        if(DEBUG) Log.d("cdfinger","runmode"+mrunmode);

        String appString = Settings.System.getString(getContentResolver(), SETTINGS_NEEDLOCK_APP_PACKAGENAMES);

        if(DEBUG) Log.d("cdfinger","old string="+appString);

        if (appString != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(appString);
            if (mGlobalPackageNameString != null) {
                int index = stringBuilder.indexOf(mGlobalPackageNameString);
                int length = mGlobalPackageNameString.length()+1;
                if(DEBUG) Log.d("cdfinger","index="+index+",length="+length);
                if(index >= 0 && length > 0){
                    stringBuilder.delete(index,index+length);
                }
                if(stringBuilder.toString() != null){
                    if(DEBUG) Log.d("cdfinger","string="+stringBuilder.toString());
                    Settings.System.putString(getContentResolver(), SETTINGS_NEEDLOCK_APP_PACKAGENAMES,null);
                    Settings.System.putString(getContentResolver(), SETTINGS_NEEDLOCK_APP_PACKAGENAMES,stringBuilder.toString());
                    if(DEBUG) Log.d("cdfinger","new string="+Settings.System.getString(getContentResolver(), SETTINGS_NEEDLOCK_APP_PACKAGENAMES));
                }
            }
        }

    }

    private void StartProtectedActivity(){
        if(mrunmode != null){
            if(mrunmode.equals("create")){
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                intent.setClassName(mGlobalPackageNameString, mExtraClassName);
                startActivity(intent);
            }
        }
    }
/*
    private boolean isFingerPrintEnable() {
        int reval = Settings.System.getInt(getContentResolver(),FINGERPRINT_KEYGUARD_USE, 0);
        if(reval == 1){
            return true;
        }else{
            return false;
        }
    }
*/
    private Handler mFingerprintVerifyHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FP_ONSUCCESS:
                    DeletePackageName();
                    StartProtectedActivity();
                    finish();
                    break;
                case MSG_FP_ONFAIL:
	            mFailedAttempts++;
		    if(mFailedAttempts <5){
                        header.setText(getResources().getString(R.string.confirm_password_header_errorstring2));
		    }else{
			mFailedAttempts = 0;
	                if (updateMonitor.mFingerprintRunningState == updateMonitor.FINGERPRINT_STATE_RUNNING && mFingerprintCancelSignal != null) {
	                    header.setText(getResources().getString(R.string.confirm_password_header_errorstring3));
               	            headerString = getResources().getString(R.string.confirm_password_header_title1);

                            mFingerprintCancelSignal.cancel();
                            mFingerprintCancelSignal = null;
                            updateMonitor.setFingerprintRunningState(updateMonitor.FINGERPRINT_STATE_CANCELLING);
			    mFpm.setFpUnlockAppLock(true);
                	}
		    }
                    break;
                default:
                    break;
            }
        };
    };

/*
    private IFc909ResultListener mIFc909Result = new IFc909ResultListener.Stub() {
        @Override
        public void success() throws RemoteException {
            if(DEBUG) Log.i(TAG, "qucii fp verfy success");
	    mQcFingerprintManager.SetCancelFlag();
	    mQcFingerprintManager.ReleaseExcuteLock();
            Message msg = new Message();
            msg.what = MSG_FP_ONSUCCESS;
            mFingerprintVerifyHandler.sendMessage(msg);
        }

        @Override
        public void fail() throws RemoteException {
            if(DEBUG) Log.i(TAG, "qucii fp verfy fail");
	    mQcFingerprintManager.SetCancelFlag();
	    mQcFingerprintManager.ReleaseExcuteLock();
                Message msg = new Message();
                msg.what = MSG_FP_ONFAIL;
                mFingerprintVerifyHandler.sendMessage(msg);
                return;
        }
        @Override
        public void cancel() throws RemoteException {
            if(DEBUG) Log.i(TAG, "qucii fp cancel");
	    mQcFingerprintManager.SetCancelFlag();
	    mQcFingerprintManager.ReleaseExcuteLock();
            Message msg = new Message();
            msg.what = MSG_FP_CANCEL;
            mFingerprintVerifyHandler.sendMessage(msg);
        }
    };



    private final Runnable mCheckFingerDetectRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized(this){
                while(true){
                    if(mQcFingerprintManager.isFingerDetect() && mQcFingerprintManager.isKeyguardDone()){
                        if(DEBUG) Log.i(TAG,"fingerprint verify........");
                        FingerprintVerify();
                        return;
                    }else{
                        if(DEBUG) Log.i(TAG,"fingerprint not detect...");
                    }
    
                    try {
                        Thread.currentThread().sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    if(!mQcFingerprintManager.isFingerDetect() && !mQcFingerprintManager.isKeyguardDone() && !activityStatus){
                        if(DEBUG) Log.i(TAG,"fingerprint not detect,and not detect,return...");
                        return;
                    }
                }
            }
        }
    };

    private void FingerprintVerify(){
        if(mIFc909Result != null){
            if(DEBUG) Log.i(TAG,"SetDevicelistener...");
            mQcFingerprintManager.SetResultlistener(mIFc909Result);
        }

        if(mQcFingerprintManager != null){
            if(mQcFingerprintManager.GetOprStatus()){
                if(DEBUG) Log.d(TAG, "CancelAction... ");
                mQcFingerprintManager.CancelAction();
            }
            if(DEBUG) Log.d(TAG, "verify... ");
            mQcFingerprintManager.Verify();
        }
    }

    private final Runnable mFingerprintTouchDetectRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized(this){
                while(true){
                    if(mQcFingerprintManager != null){
                        if(mQcFingerprintManager.TouchDetect()){
                            if(DEBUG) Log.d(TAG, "touch detect and mQcFingerprintManager.Verify ...");
                            mFingerprintVerifyHandler.removeCallbacks(mFpVfRunnable);
                            mFingerprintVerifyHandler.postDelayed(mFpVfRunnable, 250);
                            return;
                        }else{
                            if(DEBUG) Log.d(TAG, "keyguardDone finger not detected...");
                            if(mQcFingerprintManager.GetOprStatus()){
                                if(DEBUG) Log.d(TAG, "CancelAction... ");
                                mQcFingerprintManager.CancelAction();
                            }
                        }
                        if(!mQcFingerprintManager.isKeyguardDone()){
                            return;
                        }

                    }else{
                        return;
                    }
                } 
            }
        }
    };
    private final Runnable mFpVfRunnable = new Runnable(){
        @Override
        public void run() {
            if(mQcFingerprintManager.GetOprStatus()){
                if(DEBUG) Log.d(TAG, "mFpVfRunnable CancelAction... ");
                mQcFingerprintManager.CancelAction();
            }
            mQcFingerprintManager.Verify();
        }
    };
*/
    private void startLauncher(){
        Intent mIntent = new Intent();
        mIntent.addCategory("android.intent.category.HOME");
        mIntent.setAction("android.intent.action.MAIN");
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mIntent);
    }

    private void removeActivity(){
    	String pkg = getIntent().getStringExtra("packagename");
        String cls = getIntent().getStringExtra("classname");
        ComponentName mComponentName = new ComponentName(pkg, cls);
        if(am != null){
       	    am.removeActivityRecord(mComponentName);
        }	
    }

    public void initBackButton() {
        ImageView backButton = (ImageView) this.findViewById(R.id.durian_back_image);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishActivity();
            }
        });
    }   
    
    protected void setImmerseLayout(View view) {
        int statusBarHeight = getStatusBarHeight(this);
        view.setPadding(0, statusBarHeight, 0, 0);
    }   
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen",
                "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }   
    public void finishActivity() {
	if(resumefromUS){
            startLauncher();
	}else{
	    removeActivity();
	}
        finish();
        //overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }    
    public void setTitleBar(int id) {
        TextView tvName = (TextView) findViewById(R.id.durian_title_text);
        tvName.setText(id);
    }

/*
    private void vibrate(int millisecond) {
        Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(millisecond);
    }
*/

    private FingerprintManager.AuthenticationCallback mAuthenticationCallback
            = new AuthenticationCallback() {

        @Override
        public void onAuthenticationFailed() {
            Log.d(TAG,"onAuthenticationFailed");
            Message msg = new Message();
            msg.what = MSG_FP_ONFAIL;
            mFingerprintVerifyHandler.sendMessage(msg);
        };

        @Override
        public void onAuthenticationSucceeded(AuthenticationResult result) {
            Log.d(TAG,"onAuthenticationSucceeded");
	    try{
		AuthenticationRunning = false;
	        updateMonitor.applock_verify=false;
                Message msg = new Message();
                msg.what = MSG_FP_ONSUCCESS;
                mFingerprintVerifyHandler.sendMessage(msg);
	    }finally{
	    	updateMonitor.setFingerprintRunningState(updateMonitor.FINGERPRINT_STATE_STOPPED);
	    }
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            Log.d(TAG,"onAuthenticationHelp");
        }

        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            Log.d(TAG,"onAuthenticationError");
	    if (updateMonitor.mFingerprintRunningState == updateMonitor.FINGERPRINT_STATE_RUNNING){
                header.setText(getResources().getString(R.string.confirm_password_header_errorstring3));
		headerString = getResources().getString(R.string.confirm_password_header_title1);		
	    }
	    updateMonitor.setFingerprintRunningState(updateMonitor.FINGERPRINT_STATE_STOPPED);
	    updateMonitor.applock_cancle_verify=false;
	    updateMonitor.applock_verify=false;
	    AuthenticationRunning = false;
	    if(!SystemProperties.getBoolean(PERSIST_NAVIGATION_BAR, false)){
	    	writeFile(GF_MODE,"0");
	    }
        }

        @Override
        public void onAuthenticationAcquired(int acquireInfo) {
            Log.d(TAG,"onAuthenticationAcquired");
        }
    };


    private void writeFile(String fileName, String write_str){
        File file = new File(fileName);
        try{
            FileOutputStream fos = new FileOutputStream(file);
            byte [] bytes = write_str.getBytes();
            fos.write(bytes);
            fos.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private UEventObserver mgfObserver = new UEventObserver() {
    	@Override
        public void onUEvent(UEvent event) {
	    String status = event.get("STATUS");
	    if(status.equals("up")){ 
                if (updateMonitor.isUnlockWithFingerprintPossible(ActivityManager.getCurrentUser()) && isUnlockAppEnable()) {
            	    if (mFingerprintCancelSignal != null) {
                    	mFingerprintCancelSignal.cancel();
            	    }
            	    mFingerprintCancelSignal = new CancellationSignal();
            	    mFpm.authenticate(null, mFingerprintCancelSignal, 0, mAuthenticationCallback, null, ActivityManager.getCurrentUser());
            	    updateMonitor.setFingerprintRunningState(updateMonitor.FINGERPRINT_STATE_RUNNING);
            	    AuthenticationRunning = true;
            	    updateMonitor.applock_verify=true;
            	    if(!SystemProperties.getBoolean(PERSIST_NAVIGATION_BAR, false)){
                    	writeFile(GF_MODE,"1");
            	    }
                }
	    }
	}
    };
}

