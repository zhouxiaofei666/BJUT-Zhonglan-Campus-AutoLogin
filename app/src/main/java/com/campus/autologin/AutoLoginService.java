package com.campus.autologin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.IBinder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoLoginService extends Service {
    public static final String ACTION_LOGIN_NOW =
            "com.campus.autologin.LOGIN_NOW";
    private static final String CHANNEL_ID = "campus_login_service";
    private static final int NOTIFICATION_ID = 1001;

    private ConnectivityManager cm;
    private ConnectivityManager.NetworkCallback callback;
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean working = new AtomicBoolean(false);
    private volatile long lastAutoAttempt = 0L;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID,
                buildNotification("等待校园 Wi-Fi…"));

        cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        registerWifiCallback();
        AppState.setStatus(this, "自动登录服务已启动");
    }

    private void registerWifiCallback() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                executor.schedule(
                        () -> considerAutoLogin(network),
                        1200,
                        TimeUnit.MILLISECONDS);
            }

            @Override
            public void onCapabilitiesChanged(
                    Network network,
                    NetworkCapabilities caps) {
                if (caps.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    setStatus("Wi-Fi 已联网，无需认证");
                } else {
                    executor.schedule(
                            () -> considerAutoLogin(network),
                            800,
                            TimeUnit.MILLISECONDS);
                }
            }

            @Override
            public void onLost(Network network) {
                setStatus("Wi-Fi 已断开，等待重新连接");
            }
        };

        cm.registerNetworkCallback(request, callback);
    }

    private void considerAutoLogin(Network network) {
        if (!AppState.isAutoEnabled(this)) return;

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        if (caps == null ||
                !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return;

        if (caps.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            setStatus("Wi-Fi 已联网，无需认证");
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastAutoAttempt < 15000) return;
        lastAutoAttempt = now;
        doLogin(network);
    }

    private void doLogin(Network network) {
        if (!working.compareAndSet(false, true)) return;

        setStatus("正在向校园网认证…");
        try {
            PortalClient.Result r = PortalClient.login(this, network);
            if (r.success) {
                setStatus("✓ 校园网认证成功（" + r.clientIp + "）");
                try {
                    cm.reportNetworkConnectivity(network, true);
                } catch (Exception ignored) {}
            } else {
                setStatus("认证未完成：" + r.message);
            }
        } finally {
            working.set(false);
        }
    }

    private void loginCurrentWifiNow() {
        for (Network n : cm.getAllNetworks()) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(n);
            if (caps != null &&
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                executor.execute(() -> doLogin(n));
                return;
            }
        }
        setStatus("没有检测到 Wi-Fi 连接");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null &&
                ACTION_LOGIN_NOW.equals(intent.getAction())) {
            loginCurrentWifiNow();
        }
        return START_STICKY;
    }

    private void setStatus(String s) {
        AppState.setStatus(this, s);
        NotificationManager nm =
                (NotificationManager) getSystemService(
                        Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, buildNotification(s));
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(
                    CHANNEL_ID,
                    "校园网自动登录",
                    NotificationManager.IMPORTANCE_LOW);
            c.setDescription("用于连接校园 Wi-Fi 后自动完成 Portal 认证");
            NotificationManager nm =
                    (NotificationManager) getSystemService(
                            Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(c);
        }
    }

    private Notification buildNotification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                this,
                0,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return b.setSmallIcon(R.drawable.ic_wifi)
                .setContentTitle("校园网自动登录")
                .setContentText(text)
                .setContentIntent(pi)
                .setOngoing(true)
                .setShowWhen(false)
                .build();
    }

    @Override
    public void onDestroy() {
        try {
            if (callback != null) cm.unregisterNetworkCallback(callback);
        } catch (Exception ignored) {}
        executor.shutdownNow();
        AppState.setStatus(this, "自动登录服务已停止");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
