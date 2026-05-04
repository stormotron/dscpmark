package com.netstorm.dscpmark;

import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AppAdapter adapter;
    private List<AppItem> appList;
    private DatabaseHelper dbHelper;
    private View mainContent;
    private View loadingView;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainContent = findViewById(R.id.mainContent);
        loadingView = findViewById(R.id.loadingView);

        TextView tvHeader = findViewById(R.id.tvHeader);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvHeader.setText(getString(R.string.version_label, pInfo.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            tvHeader.setText(getString(R.string.version_unknown));
        }

        recyclerView = findViewById(R.id.recyclerViewApps);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new DatabaseHelper(this);

        etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        Button btnApply = findViewById(R.id.btnApply);
        btnApply.setOnClickListener(v -> {
            dbHelper.saveRules(appList);
            IptablesHelper.deleteMainRule();
            IptablesHelper.applyRules(this, () -> {
                Toast.makeText(this, R.string.rules_applied, Toast.LENGTH_SHORT).show();
            });
            IptablesHelper.createMainRule();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mainContent != null) mainContent.setVisibility(View.GONE);
        if (loadingView != null) loadingView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initData();
    }

    private void initData() {
        loadingView.setVisibility(View.VISIBLE);
        mainContent.setVisibility(View.GONE);
        if (etSearch != null) {
            etSearch.setText("");
        }

        new Thread(() -> {
            String error = IptablesHelper.checkSystemRequirements(this);
            if (error != null) {
                runOnUiThread(() -> showErrorAndExit(error));
                return;
            }

            List<AppItem> loadedApps = loadApps();

            runOnUiThread(() -> {
                appList = loadedApps;
                if (adapter == null) {
                    adapter = new AppAdapter(appList);
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.updateData(appList);
                }
                loadingView.setVisibility(View.GONE);
                mainContent.setVisibility(View.VISIBLE);
            });
        }).start();
    }

    private List<AppItem> loadApps() {
        List<AppItem> list = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Map<String, AppItem> savedRules = dbHelper.getSavedRules();

        AppItem systemItem = new AppItem(
                "android",
                getString(R.string.android_system),
                pm.getDefaultActivityIcon(),
                0
        );
        if (savedRules.containsKey(systemItem.packageName)) {
            systemItem.isSelected = true;
            systemItem.dscpMark = savedRules.get(systemItem.packageName).dscpMark;
        }
        list.add(systemItem);

        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(getPackageName())) continue;

            if (pm.getLaunchIntentForPackage(packageInfo.packageName) != null) {
                String appName = packageInfo.loadLabel(pm).toString();
                AppItem item = new AppItem(
                        packageInfo.packageName,
                        appName,
                        packageInfo.loadIcon(pm),
                        packageInfo.uid
                );

                if (savedRules.containsKey(item.packageName)) {
                    item.isSelected = true;
                    item.dscpMark = savedRules.get(item.packageName).dscpMark;
                }
                list.add(item);
            }
        }

        Collections.sort(list, (o1, o2) -> {
            if (o1.uid == 0) return -1;
            if (o2.uid == 0) return 1;

            String name1 = o1.appName;
            String name2 = o2.appName;
            if (name1 == null || name1.isEmpty()) return 1;
            if (name2 == null || name2.isEmpty()) return -1;

            boolean is1English = isLatin(name1.charAt(0));
            boolean is2English = isLatin(name2.charAt(0));

            if (is1English != is2English) {
                return is1English ? 1 : -1;
            }
            return name1.compareToIgnoreCase(name2);
        });
        return list;
    }

    private boolean isLatin(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private void showErrorAndExit(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error_init)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.exit, (dialog, which) -> finish())
                .show();
    }
}