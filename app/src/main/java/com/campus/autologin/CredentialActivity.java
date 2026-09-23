package com.campus.autologin;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class CredentialActivity extends Activity {
    private EditText accountEdit;
    private EditText passwordEdit;
    private TextView savedText;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        refreshSaved();
    }

    private ScrollView buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(247, 249, 253));
        LinearLayout root = vertical();
        root.setPadding(dp(22), dp(24), dp(22), dp(28));
        scroll.addView(root);

        TextView title = text("账号设置", 27, Color.rgb(30, 38, 51), true);
        root.addView(title);
        TextView sub = text("账号密码不会显示在主界面", 13, Color.rgb(102, 112, 133), false);
        sub.setPadding(0, dp(3), 0, dp(18));
        root.addView(sub);

        LinearLayout card = vertical();
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        card.setBackground(round(Color.WHITE, 20, Color.rgb(220, 226, 238)));
        root.addView(card, matchWrap());

        card.addView(text("校园网账号", 15, Color.rgb(31, 38, 51), true));
        accountEdit = input("账号，不用输入 @campus");
        LinearLayout.LayoutParams inputLp = matchWrap();
        inputLp.setMargins(0, dp(8), 0, 0);
        card.addView(accountEdit, inputLp);

        TextView passTitle = text("校园网密码", 15, Color.rgb(31, 38, 51), true);
        passTitle.setPadding(0, dp(16), 0, 0);
        card.addView(passTitle);
        passwordEdit = input("请输入密码");
        passwordEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        card.addView(passwordEdit, inputLp);

        savedText = text("", 13, Color.rgb(91, 103, 128), false);
        savedText.setPadding(0, dp(14), 0, 0);
        card.addView(savedText);

        Button save = button("保存账号密码", Color.rgb(61, 106, 242), Color.WHITE, true);
        root.addView(save, buttonLp(54, 16));
        save.setOnClickListener(v -> saveCredentials());

        Button clear = button("清除已保存信息", Color.rgb(233, 239, 255), Color.rgb(61, 106, 242), true);
        clear.setBackground(round(Color.rgb(233, 239, 255), 17, Color.rgb(213, 224, 251)));
        root.addView(clear, buttonLp(52, 12));
        clear.setOnClickListener(v -> {
            SecureStore.clear(this);
            AppState.setAutoEnabled(this, false);
            AppState.setStatus(this, "账号信息已清除");
            accountEdit.setText("");
            passwordEdit.setText("");
            refreshSaved();
            Toast.makeText(this, "已清除", Toast.LENGTH_SHORT).show();
        });

        Button done = button("完成并返回", Color.WHITE, Color.rgb(42, 50, 64), false);
        done.setBackground(round(Color.WHITE, 17, Color.rgb(220, 226, 238)));
        root.addView(done, buttonLp(50, 12));
        done.setOnClickListener(v -> finish());

        TextView note = text("账号和密码使用 Android Keystore 加密保存在本机。学校认证接口本身使用 HTTP，因此登录请求仍遵循校园网现有协议。", 13, Color.rgb(102, 112, 133), false);
        note.setPadding(0, dp(18), 0, 0);
        note.setLineSpacing(dp(3), 1f);
        root.addView(note);
        return scroll;
    }

    private void saveCredentials() {
        String account = accountEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString();
        String oldAccount = SecureStore.get(this, "account");
        String oldPassword = SecureStore.get(this, "password");
        if (account.isEmpty()) account = oldAccount;
        if (password.isEmpty()) password = oldPassword;
        if (account.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请填写账号和密码", Toast.LENGTH_SHORT).show();
            return;
        }
        if (account.endsWith("@campus")) account = account.substring(0, account.length() - 7);
        SecureStore.put(this, "account", account);
        SecureStore.put(this, "password", password);
        AppState.setStatus(this, "账号信息已保存");
        accountEdit.setText(account);
        passwordEdit.setText("");
        refreshSaved();
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
    }

    private void refreshSaved() {
        String account = SecureStore.get(this, "account");
        String password = SecureStore.get(this, "password");
        if (!account.isEmpty()) accountEdit.setText(account);
        if (!password.isEmpty()) {
            passwordEdit.setHint("密码已保存；不修改可留空");
            savedText.setText("当前账号：" + account + "@campus\n密码：已加密保存");
        } else {
            passwordEdit.setHint("请输入密码");
            savedText.setText("尚未保存账号密码");
        }
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextColor(Color.rgb(31, 38, 51));
        e.setHintTextColor(Color.rgb(134, 144, 163));
        e.setTextSize(15);
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setBackground(round(Color.WHITE, 14, Color.rgb(216, 224, 240)));
        e.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));
        return e;
    }

    private Button button(String s, int bg, int fg, boolean bold) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(fg);
        b.setGravity(Gravity.CENTER);
        if (bold) b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(round(bg, 17, bg));
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
}
