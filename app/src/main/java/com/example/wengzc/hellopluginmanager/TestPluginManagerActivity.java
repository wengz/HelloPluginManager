package com.example.wengzc.hellopluginmanager;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class TestPluginManagerActivity extends Activity{

    Button loadBtn;
    Button startBtn;
    Button classBtn;
    PluginManager pluginManager;
    static final String sdcard = Environment.getExternalStorageDirectory().getPath();
    PluginInfo pluginInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_plugin_manager);

        loadBtn = (Button) findViewById(R.id.bt_load_plugin);
        startBtn = (Button) findViewById(R.id.bt_start_plugin);
        classBtn = (Button) findViewById(R.id.bt_class_id);

        pluginManager = PluginManager.getInstance(this);

        loadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pluginApkPath = sdcard + "/support.apk";
                loadPlugin(pluginApkPath);
            }
        });
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPlugin();
            }
        });
        classBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                examClass();
            }
        });
    }

    private void examClass (){
        String simpleClassId = String.class.hashCode()+"";
        Log.d("Plugin Manager xxx", simpleClassId);
    }

    private void loadPlugin (final String apkPath){
        new Thread(){

            @Override
            public void run() {

                try{
                    pluginInfo = pluginManager.loadPlugin(new File(apkPath)).iterator().next();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void startPlugin  (){
        pluginManager.startMainActivity(this, pluginInfo.getPackageName());
    }
}
