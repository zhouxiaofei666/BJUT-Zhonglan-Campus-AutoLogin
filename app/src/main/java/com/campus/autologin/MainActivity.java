package com.campus.autologin;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView modeText;
    private TextView statusText;
    private LinearLayout statusCard;
    private final Handler handler = new Handler();

    private final Runnable updater = new Runnable() {
        @Override public void run() {
            refreshStatus();
            handler.postDelayed(this, 1500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        requestNotificationPermission();
        refreshStatus();
    }

    private ScrollView buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247, 249, 253));

        LinearLayout root = vertical();
        root.setPadding(dp(22), dp(24), dp(22), dp(28));
        scroll.addView(root);

        LinearLayout top = horizontal();
        top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top, matchWrap());

        LinearLayout titleBox = vertical();
        top.addView(titleBox, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = text("校园网自动登录", 27, Color.rgb(30, 38, 51), true);
        titleBox.addView(title);
        TextView subtitle = text("连接校园 Wi‑Fi 后自动完成认证", 13, Color.rgb(102, 112, 133), false);
        subtitle.setPadding(0, dp(3), 0, 0);
        titleBox.addView(subtitle);

        Button settings = button("⚙", Color.WHITE, Color.rgb(91, 103, 128), 20, false);
        settings.setContentDescription("账号设置");
        settings.setBackground(round(Color.WHITE, 16, Color.rgb(220, 226, 238)));
        top.addView(settings, new LinearLayout.LayoutParams(dp(48), dp(48)));
        settings.setOnClickListener(v -> startActivity(new Intent(this, CredentialActivity.class)));

        statusCard = vertical();
        statusCard.setPadding(dp(18), dp(17), dp(18), dp(17));
        LinearLayout.LayoutParams statusLp = matchWrap();
        statusLp.setMargins(0, dp(18), 0, 0);
        root.addView(statusCard, statusLp);

        statusCard.addView(text("当前状态", 13, Color.rgb(102, 112, 133), false));
        modeText = text("自动登录已停止", 22, Color.rgb(31, 38, 51), true);
        modeText.setPadding(0, dp(5), 0, 0);
        statusCard.addView(modeText);
        statusText = text("尚未运行", 14, Color.rgb(91, 103, 128), false);
        statusText.setPadding(0, dp(9), 0, 0);
        statusText.setLineSpacing(dp(3), 1f);
        statusCard.addView(statusText);

        TextView section = text("自动登录", 17, Color.rgb(31, 38, 51), true);
        section.setPadding(0, dp(23), 0, dp(8));
        root.addView(section);

        Button start = button("开始自动登录", Color.rgb(61, 106, 242), Color.WHITE, 17, true);
        root.addView(start, buttonLp(56, 8));
        start.setOnClickListener(v -> {
            if (!hasCredentials()) {
                Toast.makeText(this, "请先点击右上角设置保存账号和密码", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, CredentialActivity.class));
                return;
            }
            AppState.setAutoEnabled(this, true);
            startAutoService(null);
            AppState.setStatus(this, "自动登录已开启，等待校园 Wi‑Fi");
            refreshStatus();
        });

        Button stop = button("停止自动登录", Color.rgb(255, 241, 241), Color.rgb(195, 68, 68), 16, true);
        stop.setBackground(round(Color.rgb(255, 241, 241), 18, Color.rgb(241, 198, 198)));
        root.addView(stop, buttonLp(56, 12));
        stop.setOnClickListener(v -> {
            AppState.setAutoEnabled(this, false);
            stopService(new Intent(this, AutoLoginService.class));
            AppState.setStatus(this, "自动登录已停止");
            refreshStatus();
        });

        Button once = button("立即登录一次", Color.rgb(233, 239, 255), Color.rgb(61, 106, 242), 16, true);
        once.setBackground(round(Color.rgb(233, 239, 255), 18, Color.rgb(213, 224, 251)));
        root.addView(once, buttonLp(52, 12));
        once.setOnClickListener(v -> {
            if (!hasCredentials()) {
                Toast.makeText(this, "请先点击右上角设置保存账号和密码", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, CredentialActivity.class));
                return;
            }
            startAutoService(AutoLoginService.ACTION_LOGIN_NOW);
            AppState.setStatus(this, "正在执行手动登录…");
            refreshStatus();
        });

        LinearLayout bgCard = card();
        LinearLayout.LayoutParams bgLp = matchWrap();
        bgLp.setMargins(0, dp(19), 0, 0);
        root.addView(bgCard, bgLp);
        bgCard.addView(text("后台运行", 16, Color.rgb(31, 38, 51), true));
        TextView bgInfo = text("如果偶尔被系统停止，可进入系统设置，将自启动打开，并把电池策略设为“不限制”。", 13, Color.rgb(102, 112, 133), false);
        bgInfo.setPadding(0, dp(7), 0, dp(11));
        bgCard.addView(bgInfo);
        Button battery = button("打开后台 / 电池设置", Color.WHITE, Color.rgb(42, 50, 64), 14, false);
        battery.setBackground(round(Color.WHITE, 14, Color.rgb(220, 226, 238)));
        bgCard.addView(battery, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));
        battery.setOnClickListener(v -> openAppSettings());

        Space spacer = new Space(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, dp(28)));

        LinearLayout guide = card();
        root.addView(guide, matchWrap());
        guide.addView(text("使用方法", 16, Color.rgb(31, 38, 51), true));
        TextView guideText = text(
                "1. 首次使用点击右上角设置，保存校园网账号和密码。\n" +
                "2. 返回主界面，点击“开始自动登录”。\n" +
                "3. 以后连接校园 Wi‑Fi 时，App 会自动尝试认证。\n" +
                "4. 临时不需要时，点击“停止自动登录”。",
                14, Color.rgb(102, 112, 133), false);
        guideText.setPadding(0, dp(9), 0, 0);
        guideText.setLineSpacing(dp(5), 1f);
        guide.addView(guideText);

        return scroll;
    }

    private boolean hasCredentials() {
        return !SecureStore.get(this, "account").isEmpty()
                && !SecureStore.get(this, "password").isEmpty();
    }

    private void refreshStatus() {
        if (modeText == null || statusText == null || statusCard == null) return;
        boolean enabled = AppState.isAutoEnabled(this);
        long t = AppState.getStatusTime(this);
        String time = t == 0 ? "" : new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(t));
        modeText.setText(enabled ? "自动登录已开启" : "自动登录已停止");
        statusText.setText(AppState.getStatus(this) + (time.isEmpty() ? "" : "\n更新时间：" + time));
        statusCard.setBackground(enabled
                ? round(Color.rgb(236, 248, 241), 20, Color.rgb(184, 225, 199))
                : round(Color.rgb(244, 247, 255), 20, Color.rgb(216, 224, 240)));
    }

    private void startAutoService(String action) {
        Intent i = new Intent(this, AutoLoginService.class);
        if (action != null) i.setAction(action);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
            else startService(i);
        } catch (Exception e) {
            AppState.setStatus(this, "系统阻止后台服务启动，请检查后台/电池设置");
        }
    }

    private void openAppSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2001);
        }
    }

    private LinearLayout card() {
        LinearLayout l = vertical();
        l.setPadding(dp(17), dp(16), dp(17), dp(16));
        l.setBackground(round(Color.WHITE, 20, Color.rgb(220, 226, 238)));
        return l;
    }

    private Button button(String s, int bg, int fg, int size, boolean bold) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(fg);
        b.setTextSize(size);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        if (bold) b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(round(bg, 18, bg));
        return b;
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable round(int fill, int radiusDp, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (stroke != fill) d.setStroke(dp(1), stroke);
        return d;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams buttonLp(int heightDp, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp));
        p.setMargins(0, dp(topDp), 0, 0);
        return p;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(updater);
        handler.post(updater);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(updater);
        super.onPause();
    }
}
