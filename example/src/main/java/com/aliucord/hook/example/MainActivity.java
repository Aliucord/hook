package com.aliucord.hook.example;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.aliucord.hook.AliuHook;

public class MainActivity extends Activity {
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
                AliuHook.hook(MainActivity.class.getDeclaredMethod("getContent"), this);
                tv.setText(getContent());
            } catch (Throwable ignored) {}
        });
    }

    private String getContent() {
        return "Hello World";
    }
}