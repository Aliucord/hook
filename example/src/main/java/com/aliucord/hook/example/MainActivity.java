/*
 * This file is part of AliuHook, an android java hooking library based on lsplant
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.hook.example;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class MainActivity extends Activity {
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = findViewById(R.id.text);
        Button btn = findViewById(R.id.button);

        tv.setText(getContent());
        btn.setText("Click to hook");

        btn.setOnClickListener(v -> {
            try {
                XposedBridge.hookMethod(MainActivity.class.getDeclaredMethod("getContent"), new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult("get hooked");
                    }
                });

                XposedBridge.hookMethod(MainActivity.class.getDeclaredMethod("getContent"), new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(param.getResult() + " nerd");
                    }
                });
                tv.setText(getContent() + "\nOriginal: " + XposedBridge.invokeOriginalMethod(MainActivity.class.getDeclaredMethod("getContent"), this, null));
            } catch (Throwable t) {
                Log.e("AliuHook example", "bruh moment", t);
            }
        });
    }

    private String getContent() {
        return "hook me if you can";
    }
}