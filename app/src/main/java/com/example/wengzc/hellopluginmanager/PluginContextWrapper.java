package com.example.wengzc.hellopluginmanager;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;

import java.io.File;

public class PluginContextWrapper extends ContextWrapper {

    private PluginInfo plugin;
    private static final String tag = "";
    private ApplicationInfo applicationInfo;
    private File fileDir;

    public PluginContextWrapper (Context base, PluginInfo plugin){
        super(base);
        this.plugin = plugin;
        applicationInfo = new ApplicationInfo(super.getApplicationInfo());
        applicationInfo.sourceDir = plugin.getFilePath();
        applicationInfo.dataDir = ActivityOverider.getPluginBaseDir(plugin.getId()).getAbsolutePath();
        fileDir = new File(ActivityOverider.getPluginBaseDir(plugin.getId()).getAbsolutePath()+"/files/");
    }

    @Override
    public File getFilesDir() {
        if (!fileDir.exists()){
            fileDir.mkdirs();
        }
        return fileDir;
    }

    @Override
    public String getPackageResourcePath() {
        return super.getPackageResourcePath();
    }

    @Override
    public String getPackageCodePath() {
        return super.getPackageCodePath();
    }

    @Override
    public File getCacheDir() {
        return super.getCacheDir();
    }

    @Override
    public PackageManager getPackageManager() {
        return super.getPackageManager();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }


    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public String getPackageName() {
        return plugin.getPackageName();
    }

    @Override
    public Resources getResources() {
        return plugin.getResources();
    }


    @Override
    public AssetManager getAssets() {
        return plugin.getAssetManager();
    }
}
