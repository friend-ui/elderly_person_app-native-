package com.little_tools.elderly_person_appnative;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class DebugEntryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 直接跳转到桌面 MainActivity，便于从原生桌面图标进入调试
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
