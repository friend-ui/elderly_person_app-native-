package com.little_tools.elderly_person_appnative;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.View;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.Gravity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "launcher_prefs";
    private static final String KEY_FAVORITES = "favorites_pkgs";
    private LinearLayout favoritesContainer;
    private LinearLayout fixedContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

        favoritesContainer = findViewById(R.id.favorites_container);
        fixedContainer = findViewById(R.id.fixed_container);
        Button btnAddFavorite = findViewById(R.id.btn_add_favorite);
        if (btnAddFavorite != null) {
            btnAddFavorite.setOnClickListener(v -> {
                Intent intent = new Intent(this, AppListActivity.class);
                startActivity(intent);
            });
        }

        renderFixedShortcuts();
        renderFavorites();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderFavorites();
    }

    private void renderFavorites() {
        if (favoritesContainer == null) return;
        favoritesContainer.removeAllViews();
        java.util.Set<String> pkgs = getFavorites();
        TextView hint = findViewById(R.id.tv_fav_hint);
        if (pkgs == null || pkgs.isEmpty()) {
            if (hint != null) hint.setVisibility(View.VISIBLE);
            return;
        }
        if (hint != null) hint.setVisibility(View.GONE);
        PackageManager pm = getPackageManager();
        LinearLayout row = null;
        int countInRow = 0;
        for (String pkg : pkgs) {
            if (row == null || countInRow >= 3) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, dp(4), 0, dp(4));
                favoritesContainer.addView(row);
                countInRow = 0;
            }
            CharSequence label = pkg;
            android.graphics.drawable.Drawable icon = null;
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                CharSequence l = pm.getApplicationLabel(info);
                if (l != null) label = l;
                icon = pm.getApplicationIcon(pkg);
            } catch (Exception ignored) {}

            final String labelText = label != null ? label.toString() : pkg;
            LinearLayout tile = createIconTile(icon, labelText);
            tile.setTag(pkg);
            tile.setOnClickListener(v -> launchPackage((String) v.getTag()));
            tile.setOnLongClickListener(v -> {
                final String p = (String) v.getTag();
                new AlertDialog.Builder(this)
                        .setTitle("移除常用")
                        .setMessage("确认移除：" + labelText + "？")
                        .setPositiveButton("移除", (d, which) -> {
                            removeFavorite(p);
                            Toast.makeText(this, "已移除：" + labelText, Toast.LENGTH_SHORT).show();
                            renderFavorites();
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            });
            row.addView(tile, createTileLayoutParams());
            countInRow++;
        }
    }

    private void startSafe(Intent intent) {
        try {
            startActivity(intent);
        } catch (Exception e) {
            // 可以考虑 Toast 提示，但为简洁起见暂不加入依赖
            e.printStackTrace();
        }
    }

    private java.util.Set<String> getFavorites() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        java.util.Set<String> s = sp.getStringSet(KEY_FAVORITES, null);
        return s == null ? new java.util.LinkedHashSet<>() : new java.util.LinkedHashSet<>(s);
    }

    private void addFavorite(String pkg) {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        java.util.Set<String> s = sp.getStringSet(KEY_FAVORITES, null);
        s = (s == null) ? new java.util.LinkedHashSet<>() : new java.util.LinkedHashSet<>(s);
        s.add(pkg);
        sp.edit().putStringSet(KEY_FAVORITES, s).apply();
    }

    private void removeFavorite(String pkg) {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        java.util.Set<String> s = sp.getStringSet(KEY_FAVORITES, null);
        if (s == null) return;
        s = new java.util.LinkedHashSet<>(s);
        if (s.remove(pkg)) {
            sp.edit().putStringSet(KEY_FAVORITES, s).apply();
        }
    }

    private void launchPackage(String pkg) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch != null) {
            startSafe(launch);
        } else {
            Toast.makeText(this, "无法启动：" + pkg, Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(value * d);
    }

    private void renderFixedShortcuts() {
        if (fixedContainer == null) return;
        fixedContainer.removeAllViews();

        // 三列布局
        LinearLayout row = null;
        int count = 0;

        // 1. 电话
        View callTile = createIconTile(getDrawable(android.R.drawable.ic_menu_call), "电话");
        callTile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"));
            startSafe(intent);
        });

        // 2. 相机
        View cameraTile = createIconTile(getDrawable(android.R.drawable.ic_menu_camera), "相机");
        cameraTile.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            if (intent.resolveActivity(getPackageManager()) == null) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory("android.intent.category.APP_CAMERA");
            }
            startSafe(intent);
        });

        // 4. 设置
        View settingsTile = createIconTile(getDrawable(android.R.drawable.ic_menu_manage), "设置");
        settingsTile.setOnClickListener(v -> startSafe(new Intent(Settings.ACTION_SETTINGS)));

        // 5. 所有应用
        View allAppsTile = createIconTile(getDrawable(android.R.drawable.ic_menu_view), "所有应用");
        allAppsTile.setOnClickListener(v -> startActivity(new Intent(this, AppListActivity.class)));

        // 6. SOS
        View sosTile = createIconTile(getDrawable(android.R.drawable.ic_menu_help), "SOS");
        sosTile.setOnClickListener(v -> startSafe(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))));

        View[] tiles = new View[]{callTile, cameraTile, settingsTile, allAppsTile, sosTile};
        for (View tile : tiles) {
            if (row == null || count >= 3) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, dp(4), 0, dp(4));
                fixedContainer.addView(row);
                count = 0;
            }
            row.addView(tile, createTileLayoutParams());
            count++;
        }
    }

    private LinearLayout createIconTile(android.graphics.drawable.Drawable icon, String label) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(8), dp(8), dp(8), dp(8));

        ImageView iv = new ImageView(this);
        int iconSize = dp(48);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        iv.setLayoutParams(ivLp);
        if (icon != null) iv.setImageDrawable(icon);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(16f);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tvLp.topMargin = dp(6);
        tv.setLayoutParams(tvLp);

        tile.addView(iv);
        tile.addView(tv);
        return tile;
    }

    private LinearLayout.LayoutParams createTileLayoutParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.weight = 1f;
        lp.setMarginEnd(dp(8));
        return lp;
    }
}