package com.example.wengzc.hellopluginmanager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;

import java.io.File;
import java.lang.reflect.Field;

public class ActivityOverider {

    static final String targetClassName = "androidx.pluginmgr.PluginActivity";


    //------------------ service -------------------------

    public static ComponentName overrideStartService (Activity fromAct, String pluginId, Intent intent){
        return fromAct.startService(intent);
    }

    public static boolean overrideBindService (Activity fromAct, String pluginId, Intent intent,
                                               ServiceConnection connection, int flags){
        return fromAct.bindService(intent, connection, flags);
    }

    public static void overrideUnbindService (Activity fromAct, String pluginId, ServiceConnection connection){
        fromAct.unbindService(connection);
    }

    public static boolean overrideStopService (Activity fromAct, String pluginId, Intent intent){
        return fromAct.stopService(intent);
    }


    //------------------ activity -------------------------


    public static Intent overrideStartActivityForResult(Activity fromAct, String pluginId, Intent intent,
                                                        int requestCode, Bundle options) {
        PluginManager pluginManager = PluginManager.getInstance();
        if (intent.getComponent() != null && intent.getComponent().getClassName() != null){
            ComponentName componentName = intent.getComponent();
            String pkg = componentName.getPackageName();
            String toActName = componentName.getClassName();
            PluginInfo thisPlugin = pluginManager.getPluginById(pluginId);
            ActivityInfo actInThisApk = null;
            PluginInfo plug = thisPlugin;
            if (pkg != null){
                if (pkg.equals(thisPlugin.getPackageName())){
                    actInThisApk = thisPlugin.findActivityByClassName(toActName);

                }else{
                    PluginInfo otherPlug = pluginManager.getPluginByPackageName(pkg);
                    if (otherPlug != null){
                        plug = otherPlug;
                        actInThisApk = otherPlug.findActivityByClassName(toActName);
                    }
                }

            } else {
                actInThisApk = thisPlugin.findActivityByClassName(toActName);
            }

            if (actInThisApk != null){
                setPluginIntent(intent, plug, actInThisApk.name);
            }else{
                for (PluginInfo pluginInfo : pluginManager.getPlugins()){
                    if (pluginInfo == thisPlugin){
                        continue;
                    }
                    ActivityInfo otherAct = pluginInfo.findActivityByClassName(toActName);
                    if (otherAct != null){
                        setPluginIntent(intent, pluginInfo, otherAct.name);
                    }
                }
            }

        }else if (intent.getAction() != null){
            String action = intent.getAction();
            PluginInfo thisPlugin = pluginManager.getPluginById(pluginId);
            ActivityInfo actInThisApk = thisPlugin.findActivityByAction(action);
            if (actInThisApk != null){
                setPluginIntent(intent, thisPlugin, actInThisApk.name);
            }else{
                for (PluginInfo pluginInfo : pluginManager.getPlugins()){
                    if (pluginInfo == thisPlugin){
                        continue;
                    }
                    ActivityInfo otherAct = pluginInfo.findActivityByAction(action);
                    if (otherAct != null){
                        setPluginIntent(intent, pluginInfo, otherAct.name);
                        break;
                    }
                }
            }
        }
        return intent;
    }



    private static void setPluginIntent (Intent intent, PluginInfo plugin, String actName){
        PluginManager pluginManager = PluginManager.getInstance();
        String pluginId = plugin.getId();
        createProxyDex(plugin, actName);
        String act = pluginManager.getFrameworkClassLoader().newActivityClassName(pluginId, actName);
        ComponentName componentName = new ComponentName(pluginManager.getContext(), act);
        intent.setComponent(componentName);
    }

    static File getProxyActivityDexPath (String pluginId, String activity){
        File folder = new File(getPluginBaseDir(pluginId) + "/activities/");
        folder.mkdirs();
        String suffix = ".dex";
        if (Build.VERSION.SDK_INT < 11){
            suffix = ".jar";
        }
        File savePath = new File(folder, activity + suffix);
        return savePath;
    }


    static File createProxyDex (PluginInfo plugin, String activity){
        return createProxyDex(plugin, activity, true);
    }


    static File createProxyDex(PluginInfo plugin, String activity, boolean lazy) {
        File savePath = getProxyActivityDexPath(plugin.getId(), activity);
        createProxyDex(plugin, activity, savePath, lazy);
        return savePath;
    }


    static File getPluginLibDir (String pluginId){
        File folder = new File(getPluginBaseDir(pluginId) + "/lib/");
        return folder;
    }

    static File getPluginBaseDir(String pluginId) {
        String pluginPath = PluginManager.getInstance().getDexInternalStoragePath().getAbsolutePath();
        String pluginDir = pluginPath + '/' + pluginId + "-dir";
        File folder = new File(pluginDir);
        folder.mkdirs();
        return folder;
    }


    private static void createProxyDex (PluginInfo plugin, String activity, File saveDir, boolean lazy){
        if (lazy && saveDir.exists()){
            return;
        }
        try{
            String pkgName = plugin.getPackageName();
            ActivityClassGenerator.createActivityDex(activity, targetClassName, saveDir, plugin.getId(), pkgName);

        }catch (Exception e){
            e.printStackTrace();
        }
    }




    private static void changeActivityInfo (Context activity){
        final String actName = activity.getClass().getSuperclass().getName();
        if (!activity.getClass().getName().equals(targetClassName)){
            return;
        }
        Field field_mActivityInfo = null;
        try{
            field_mActivityInfo = Activity.class.getDeclaredField("mActivityInfo");
            field_mActivityInfo.setAccessible(true);
        }catch (Exception e){
            e.printStackTrace();
            return;
        }
        PluginManager pluginManager = PluginManager.getInstance();
        PluginInfo plugin =  pluginManager.getPluginByPackageName(activity.getPackageName());

        ActivityInfo actInfo = plugin.findActivityByClassName(actName);
        actInfo.applicationInfo = plugin.getPackageInfo().applicationInfo;
        try{
            field_mActivityInfo.set(activity, actInfo);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static boolean overrideOnbackPressed (Activity fromAct, String pluginId){
        PluginInfo plinfo = PluginManager.getInstance().getPluginById(pluginId);
        String actName = fromAct.getClass().getSuperclass().getName();
        ActivityInfo actInfo = plinfo.findActivityByClassName(actName);
        boolean finish = plinfo.isFinishActivityOnBackPressed(actInfo);
        if (finish){
            fromAct.finish();
        }
        boolean ivsuper = plinfo.isInvokeSuperOnbackPressed(actInfo);
        return ivsuper;
    }



    public static void callback_onCreate (String pluginId, Activity fromAct){

        PluginManager con = PluginManager.getInstance();
        PluginInfo plugin = con.getPluginById(pluginId);

        try{
            Field applicationField = Activity.class.getDeclaredField("mApplication");
            applicationField.setAccessible(true);
            applicationField.set(fromAct, plugin.getApplication());
        } catch (Exception e){
            e.printStackTrace();
        }

    }






}
