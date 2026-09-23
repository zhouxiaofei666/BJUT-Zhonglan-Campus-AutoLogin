package com.campus.autologin;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public final class PortalClient {
    public static final String HOST = "10.21.221.98";
    public static final int PORT = 801;
    public static final String PATH = "/eportal/portal/login";

    private PortalClient() {}

    public static final class Result {
        public final boolean success;
        public final String message;
        public final String clientIp;

        Result(boolean success, String message, String clientIp) {
            this.success = success;
            this.message = message;
            this.clientIp = clientIp;
        }
    }

    public static String getClientIpv4(Context context, Network network) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        LinkProperties lp = cm.getLinkProperties(network);
        if (lp == null) return "";

        for (LinkAddress la : lp.getLinkAddresses()) {
            if (la.getAddress() instanceof Inet4Address) {
                String ip = la.getAddress().getHostAddress();
                if (ip != null && !ip.startsWith("127.")) return ip;
            }
        }
        return "";
    }

    public static Result login(Context context, Network network) {
        String account = SecureStore.get(context, "account").trim();
        String password = SecureStore.get(context, "password");
        if (account.isEmpty() || password.isEmpty()) {
            return new Result(false, "请先在 App 中保存账号和密码", "");
        }

        if (!account.contains("@")) account = account + "@campus";

        String clientIp = getClientIpv4(context, network);
        if (clientIp.isEmpty()) {
            return new Result(false, "当前 Wi-Fi 尚未获得 IPv4 地址", "");
        }

        if (!clientIp.startsWith("10.")) {
            return new Result(false, "当前不是目标校园内网（IP：" + clientIp + "）", clientIp);
        }

        int v = 1000 + new Random().nextInt(9000);

        Uri.Builder b = new Uri.Builder()
                .scheme("http")
                .encodedAuthority(HOST + ":" + PORT)
                .encodedPath(PATH)
                .appendQueryParameter("callback", "dr1003")
                .appendQueryParameter("login_method", "1")
                .appendQueryParameter("user_account", account)
                .appendQueryParameter("user_password", password)
                .appendQueryParameter("wlan_user_ip", clientIp)
                .appendQueryParameter("wlan_user_ipv6", "")
                .appendQueryParameter("wlan_user_mac", "000000000000")
                .appendQueryParameter("wlan_ac_ip", "")
                .appendQueryParameter("wlan_ac_name", "")
                .appendQueryParameter("jsVersion", "4.2.1")
                .appendQueryParameter("terminal_type", "2")
                .appendQueryParameter("lang", "zh-cn")
                .appendQueryParameter("v", String.valueOf(v))
                .appendQueryParameter("lang", "zh");

        HttpURLConnection conn = null;
        try {
            URL url = new URL(b.build().toString());
            conn = (HttpURLConnection) network.openConnection(url);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36");

            int code = conn.getResponseCode();
            InputStream stream = code >= 200 && code < 400
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            String body = readAll(stream);
            boolean ok = code == 200 &&
                    (body.contains("\"result\":1")
                            || body.contains("Portal协议认证成功")
                            || body.contains("认证成功"));

            if (ok) {
                return new Result(true, "认证成功", clientIp);
            }

            String safeMsg = extractMessage(body);
            if (safeMsg.isEmpty()) safeMsg = "认证失败，HTTP " + code;
            return new Result(false, safeMsg, clientIp);
        } catch (Exception e) {
            return new Result(false,
                    "连接认证服务器失败：" + e.getClass().getSimpleName(), clientIp);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        BufferedReader br = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static String extractMessage(String body) {
        if (body == null) return "";
        int k = body.indexOf("\"msg\"");
        if (k < 0) return "";
        int colon = body.indexOf(':', k);
        int q1 = body.indexOf('"', colon + 1);
        int q2 = q1 >= 0 ? body.indexOf('"', q1 + 1) : -1;
        if (q1 >= 0 && q2 > q1) {
            String m = body.substring(q1 + 1, q2).trim();
            if (m.length() > 80) m = m.substring(0, 80);
            return m;
        }
        return "";
    }
}
