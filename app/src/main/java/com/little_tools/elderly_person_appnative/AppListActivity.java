package com.little_tools.elderly_person_appnative;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

public class AppListActivity extends AppCompatActivity {

    static class AppItem {
        final String label;
        final String packageName;
        AppItem(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
        @Override public String toString() { return label; }
    }

    private static final String PREFS = "launcher_prefs";
    private static final String KEY_FAVORITES = "favorites_pkgs";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        ListView listView = findViewById(R.id.list_apps);
        List<AppItem> data = loadLaunchableApps();
        ArrayAdapter<AppItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, data);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, android.view.View view, int position, long id) {
                AppItem item = (AppItem) parent.getItemAtPosition(position);
                new AlertDialog.Builder(AppListActivity.this)
                        .setTitle(item.label)
                        .setItems(new CharSequence[]{"启动应用", "加入常用"}, (dialog, which) -> {
                            if (which == 0) {
                                Intent launch = getPackageManager().getLaunchIntentForPackage(item.packageName);
                                if (launch != null) {
                                    try { startActivity(launch); } catch (Exception e) { e.printStackTrace(); }
                                }
                            } else if (which == 1) {
                                addFavorite(item.packageName);
                                Toast.makeText(AppListActivity.this, "已添加到常用：" + item.label, Toast.LENGTH_SHORT).show();
                                // 返回首页，触发 MainActivity.onResume() 刷新
                                finish();
                            }
                        })
                        .show();
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, android.view.View view, int position, long id) {
                AppItem item = (AppItem) parent.getItemAtPosition(position);
                addFavorite(item.packageName);
                Toast.makeText(AppListActivity.this, "已添加到常用：" + item.label, Toast.LENGTH_SHORT).show();
                // 返回首页，触发 MainActivity.onResume() 刷新
                finish();
                return true;
            }
        });
    }

    private List<AppItem> loadLaunchableApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
        List<AppItem> out = new ArrayList<>();
        for (ResolveInfo info : infos) {
            CharSequence labelCS = info.loadLabel(pm);
            String label = labelCS != null ? labelCS.toString() : info.activityInfo.packageName;
            String pkg = info.activityInfo.packageName;
            out.add(new AppItem(label, pkg));
        }
        Collections.sort(out, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem o1, AppItem o2) {
                return o1.label.compareToIgnoreCase(o2.label);
            }
        });
        return out;
    }

    private void addFavorite(String pkg) {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        Set<String> s = sp.getStringSet(KEY_FAVORITES, null);
        if (s == null) s = new LinkedHashSet<>(); else s = new LinkedHashSet<>(s);
        s.add(pkg);
        sp.edit().putStringSet(KEY_FAVORITES, s).apply();
    }
}
