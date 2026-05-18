package com.example.uorder.core.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkUtils {

    private static final String PREFS_NAME = "uorder_network_prefs";
    private static final String KEY_BASE_URL = "base_url";
    private static final String DEFAULT_BASE_URL = "http://192.168.1.7/uorder/";
    private static String baseUrl = DEFAULT_BASE_URL;

    public interface NetworkCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public static void getMenu(NetworkCallback callback) {
        new HttpTask("GET", baseUrl + "api/menu.php", null, callback).execute();
    }

    public static void placeOrder(String orderJson, NetworkCallback callback) {
        new HttpTask("POST", baseUrl + "api/orders.php", orderJson, callback).execute();
    }

    public static void getOrders(String status, NetworkCallback callback) {
        new HttpTask("GET", baseUrl + "api/orders.php?status=" + status, null, callback).execute();
    }

    public static void updateOrderStatus(int orderId, String status, NetworkCallback callback) {
        new HttpTask("PUT", baseUrl + "api/orders.php?id=" + orderId + "&status=" + status, null, callback).execute();
    }

    public static void addMenuItem(String itemJson, NetworkCallback callback) {
        new HttpTask("POST", baseUrl + "api/menu.php", itemJson, callback).execute();
    }

    public static void updateMenuItem(String itemJson, NetworkCallback callback) {
        new HttpTask("PUT", baseUrl + "api/menu.php", itemJson, callback).execute();
    }

    public static void deleteMenuItem(int itemId, NetworkCallback callback) {
        new HttpTask("DELETE", baseUrl + "api/menu.php?id=" + itemId, null, callback).execute();
    }

    public static void init(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        baseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
    }

    public static void setBaseUrl(Context context, String url) {
        if (url == null || url.trim().isEmpty()) return;
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        baseUrl = url;
        if (context != null) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_BASE_URL, baseUrl)
                    .apply();
        }
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    private static class HttpTask extends AsyncTask<Void, Void, String> {
        private String method;
        private String url;
        private String body;
        private NetworkCallback callback;
        private String error;

        public HttpTask(String method, String url, String body, NetworkCallback callback) {
            this.method = method;
            this.url = url;
            this.body = body;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL urlObj = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod(method);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");

                if (body != null && (method.equals("POST") || method.equals("PUT"))) {
                    conn.setDoOutput(true);
                    OutputStream os = conn.getOutputStream();
                    os.write(body.getBytes());
                    os.flush();
                    os.close();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    return response.toString();
                } else {
                    error = "HTTP Error: " + responseCode;
                    return null;
                }
            } catch (IOException e) {
                error = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                callback.onSuccess(result);
            } else {
                callback.onError(error);
            }
        }
    }
}