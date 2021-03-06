package com.fuyong.main;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: democrazy
 * Date: 13-6-16
 * Time: 下午9:12
 * To change this template use File | Settings | File Templates.
 */
public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static UncaughtExceptionHandler instance;
    private Logger log = Log.getLogger(Log.CRASH);
    private final Context myContext;

    private UncaughtExceptionHandler() {
        myContext = null;
    }

    private UncaughtExceptionHandler(Context context) {
        myContext = context;
    }

    synchronized public static UncaughtExceptionHandler getInstance() {
        if (null == instance) {
            instance = new UncaughtExceptionHandler(MyApp.getInstance().getAppContext());
        }
        return instance;
    }

    public void uncaughtException(Thread thread, Throwable e) {
        StringWriter stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));
        log.error(stackTrace.toString());

        Map<String, String> infoMap = collectDeviceInfo();
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : infoMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }
        log.error(sb);
        Toast.makeText(MyApp.getInstance().getAppContext()
                , R.string.crash_msg
                , Toast.LENGTH_SHORT)
                .show();
        // 重启应用
        new MyAppRestartThread().start();
    }

    private Map<String, String> collectDeviceInfo() {
        //用来存储设备信息和异常信息
        Map<String, String> infoMap = new HashMap<String, String>();
        try {
            PackageManager pm = myContext.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(myContext.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                infoMap.put("versionName", versionName);
                infoMap.put("versionCode", versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            log.error("collect package info\n" + e.toString());
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infoMap.put(field.getName(), field.get(null).toString());
            } catch (Exception e) {
                log.error("collect crash info\n" + e.toString());
            }
        }
        return infoMap;
    }
}