package com.netstorm.dscpmark;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IptablesHelper {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnRulesAppliedListener {
        void onRulesApplied();
    }

    private static String runSuCommand(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader is = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            
            String line;
            while ((line = is.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
            os.close();
            is.close();
        } catch (Exception e) {
            Log.e("IptablesHelper", "SU Command Failed: " + command, e);
        }
        return output.toString();
    }

    public static String checkSystemRequirements(Context context) {
        String idOut = runSuCommand("id");
        if (idOut == null || !idOut.contains("uid=0")) {
            return context.getString(R.string.error_root);
        }

        String whichOut = runSuCommand("which iptables");
        String verOut = runSuCommand("iptables --version");
        if (whichOut.trim().isEmpty() && !verOut.contains("iptables")) {
            return context.getString(R.string.error_iptables);
        }

        String targetOut = runSuCommand("cat /proc/net/ip_tables_targets | grep -i dscp");
        if (targetOut == null || !targetOut.toLowerCase().contains("dscp")) {
            return context.getString(R.string.error_dscp);
        }
        return null;
    }

    public static void deleteMainRule() {
        executor.execute(() -> runSuCommand("iptables -t mangle -D OUTPUT -j DSCPMARK 2>/dev/null"));
    }

    public static void createMainRule() {
        executor.execute(() -> runSuCommand("iptables -t mangle -A OUTPUT -j DSCPMARK"));
    }

    public static void applyRules(Context context, OnRulesAppliedListener listener) {
        executor.execute(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(context.getApplicationContext());
            Map<String, AppItem> rules = dbHelper.getSavedRules();
            PackageManager pm = context.getPackageManager();

            StringBuilder script = new StringBuilder();
            script.append("iptables -t mangle -F DSCPMARK 2>/dev/null\n");
            script.append("iptables -t mangle -X DSCPMARK 2>/dev/null\n");
            
            script.append("iptables -t mangle -N DSCPMARK\n");

            for (AppItem item : rules.values()) {
                int currentUid = -1;
                if ("android".equals(item.packageName)) {
                    currentUid = 0;
                } else {
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(item.packageName, 0);
                        currentUid = ai.uid;
                    } catch (PackageManager.NameNotFoundException e) {
                        continue;
                    }
                }

                script.append("iptables -t mangle -A DSCPMARK -m owner --uid-owner ")
                      .append(currentUid)
                      .append(" -j DSCP --set-dscp ")
                      .append(item.dscpMark)
                      .append("\n");
            }
            runSuCommand(script.toString());

            if (listener != null) {
                mainHandler.post(listener::onRulesApplied);
            }
        });
    }

    public static void applyRules(Context context) {
        applyRules(context, null);
    }
}