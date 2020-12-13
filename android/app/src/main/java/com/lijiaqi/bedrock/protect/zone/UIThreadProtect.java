package com.lijiaqi.bedrock.protect.zone;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.lijiaqi.bedrock.protect.ActivityStackManager;
import com.lijiaqi.bedrock.protect.IProtect;

/**
 * @author LiJiaqi
 * @date 2020/12/13
 * Description:
 */
public class UIThreadProtect implements IProtect {
    @Override
    public void protect(Application application) {
        ActivityStackManager.getInstance().init(application);
        protectActivityStart(null);

    }


    private CrashException lastException;

    private void protectActivityStart(CrashException exception){
        lastException = exception;
        new Handler(Looper.getMainLooper()).post(mainRun);
    }

    private final Runnable mainRun = new Runnable() {
        @Override
        public void run() {
            try {
                Looper.loop();
            }catch (Exception e){
                CrashException crashException = new CrashException(e);
                if(crashException.analysisException(lastException)){
                    ///进行恢复
                    Activity activity = ActivityStackManager.getInstance().exceptionBirthplaceActivity(e);
                    if(activity != null){
                        protectActivityStart(crashException);
                        activity.finish();
                        return;
                    }
                }
                throw  e;
            }

        }
    };
}

class CrashException extends Exception {

    private final Throwable cause;
    private final long createTime = System.currentTimeMillis();

    CrashException(Throwable cause) {
        super(cause);
        this.cause = cause;
    }

    public boolean analysisException(CrashException e){
        if(isSystemException()){
            ///系统异常 放弃恢复
            return false;
        }
        if(e == null){
            ///进行恢复
            return true;
        }
        if(createTime - e.createTime < 100){
            ///连续异常下， 放弃恢复
            return false;
        }
        return true;
    }


    private boolean isSystemException(){
        if(cause != null){
            if(cause.getStackTrace() != null){
                if(cause.getStackTrace()[0] != null){
                    StackTraceElement element = cause.getStackTrace()[0];
                    ClassLoader classLoader ;
                    try {
                        classLoader = Class.forName(element.getClassName()).getClassLoader();
                        return classLoader == null || classLoader == Exception.class.getClassLoader();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        return true;
                    }
                }else{
                    return false;
                }
            }
        }
        return false;
    }
}

