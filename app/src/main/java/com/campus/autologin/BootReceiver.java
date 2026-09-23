package com.campus.autologin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!AppState.isAutoEnabled(context)) return;

        Intent service = new Intent(context, AutoLoginService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } catch (Exception e) {
            AppState.setStatus(
                    context,
                    "开机自启动被系统限制，请打开 App 启动一次");
        }
    }
}
