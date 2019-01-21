package com.example.wengzc.hellopluginmanager;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginInfo {

    public ResolveInfo getMainActivity() {
        return mainActivity;
    }


    public void setId(String id) {
        this.id = id;
    }

    private String id;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    private String filePath;

    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    public void setPackageInfo(PackageInfo packageInfo) {
        this.packageInfo = packageInfo;
    }

    private PackageInfo packageInfo;
    private ResolveInfo mainActivity;
    private Map<String, ResolveInfo> activities;
    private List<ResolveInfo> services;
    private List<ResolveInfo> receivers;
    private List<ResolveInfo> providers;

    PluginContextWrapper appWrapper;

    public void setClassLoader(PluginClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    private transient PluginClassLoader classLoader;

    private transient Application application;

    public void setApplication(Application application) {
        this.application = application;
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public Resources getResources() {
        return resources;
    }

    public void setResources(Resources resources) {
        this.resources = resources;
    }

    private transient AssetManager assetManager;
    private transient Resources resources;

    public String getPackageName (){
        return packageInfo.packageName;
    }

    private static final int FLAG_FINISH_ACTIVITY_ON_BACKPRESSED = 1;

    private static final int FLAG_INVOKE_SUPER_ON_BACKPRESSED = 2;


    public boolean isFinishActivityOnBackPressed (ActivityInfo act){
        if (act == null){
            return false;
        }
        int flags = getFlags(act);
        return containsFlag(flags, FLAG_FINISH_ACTIVITY_ON_BACKPRESSED);
    }

    public void setInvokeSuperOnbackPressed (ActivityInfo act, boolean invokeSuperOnBackPressed){
        if (act == null){
            return;
        }
        if (invokeSuperOnBackPressed){
            setFlag(act, FLAG_INVOKE_SUPER_ON_BACKPRESSED);
        }else{
            unsetFlag(act, FLAG_INVOKE_SUPER_ON_BACKPRESSED);
        }
    }

    public void setFinishActivityOnBackPressed (ActivityInfo act, boolean finishOnBackPressed){
        if (act == null){
            return;
        }

        if (finishOnBackPressed){
            setFlag(act, FLAG_FINISH_ACTIVITY_ON_BACKPRESSED);
        }else{
            unsetFlag(act, FLAG_FINISH_ACTIVITY_ON_BACKPRESSED);
        }
    }

    ActivityInfo findActivityByClassNameFromPkg (String actName){
        if (packageInfo.activities == null){
            return null;
        }
        for (ActivityInfo act : packageInfo.activities){
            if (act.name.equals(actName)){
                return act;
            }
        }
        return null;
    }

    public ActivityInfo findActivityByClassName (String actName){
        if (packageInfo.activities == null){
            return null;
        }
        ResolveInfo act = activities.get(actName);
        if (act == null){
            return null;
        }
        return act.activityInfo;
    }


    public ActivityInfo findActivityByAction (String action){
        if (activities == null || activities.isEmpty()){
            return null;
        }
        for (ResolveInfo act : activities.values()){
            if (act.filter != null && act.filter.hasAction(action)){
                return act.activityInfo;
            }
        }
        return null;
    }


    public ActivityInfo findReceiverByClassName (String className){
        if (packageInfo.receivers == null){
            return null;
        }

        for (ActivityInfo receiver : packageInfo.receivers){
            if (receiver.name.equals(className)){
                return receiver;
            }
        }
        return null;
    }

    public ServiceInfo findServiceByClassName (String className){
        if (packageInfo.services == null){
            return null;
        }

        for (ServiceInfo service : packageInfo.services){
            if (service.name.equals(className)){
                return service;
            }
        }

        return null;
    }

    public ServiceInfo findServiceByAction (String action){
        if (services == null || services.isEmpty()){
            return null;
        }
        for (ResolveInfo ser : services){
            if (ser.filter != null && ser.filter.hasAction(action)){
                return ser.serviceInfo;
            }
        }
        return null;
    }

    public void addActivity (ResolveInfo activity){
        if (activities == null){
            activities = new HashMap<String, ResolveInfo>();
        }
        activities.put(activity.activityInfo.name, activity);
        if (mainActivity == null
                && activity.filter != null
                && activity.filter.hasAction("android.intent.action.MAIN")
                && activity.filter.hasCategory("android.intent.category.LAUNCHER")
                ){
            mainActivity = activity;
        }
    }


    public void addReceiver (ResolveInfo receiver){
        if (receivers == null){
            receivers = new ArrayList<>();
        }
        receivers.add(receiver);
    }

    public void addService (ResolveInfo service){
        if (services == null){
            services = new ArrayList<>();
        }
        services.add(service);
    }

    public PluginClassLoader getClassLoader (){
        return null;
    }


    private static synchronized int getFlags (ActivityInfo act){
        return act.logo;
    }

    private static synchronized void setFlag (ActivityInfo act, int flag){
        act.logo |= flag;
    }

    private static synchronized void unsetFlag (ActivityInfo act, int flag){
        act.logo &= ~flag;
    }

    private static boolean containsFlag (int vFlags, int flag){
        return (vFlags & flag) == flag;
    }

    public String getId() {
        return id;
    }

    public Application getApplication() {
        return application;
    }

    public boolean isInvokeSuperOnbackPressed (ActivityInfo act){
        if (act == null){
            return true;
        }
        int flags = getFlags(act);
        if (flags == 0){
            return true;
        }
        return containsFlag(flags, FLAG_INVOKE_SUPER_ON_BACKPRESSED);
    }

}
