package com.example.wengzc.hellopluginmanager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;

import java.io.File;

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



    static File createProxyDex (PluginInfo plugin, String activity){
        return null;
    }


    static File createProxyDex(PluginInfo plugin, String activity, boolean lazy) {
        return null;
    }



}
