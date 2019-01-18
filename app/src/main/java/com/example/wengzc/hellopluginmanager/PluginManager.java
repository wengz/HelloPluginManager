package com.example.wengzc.hellopluginmanager;

import android.content.Context;

import java.io.File;

public class PluginManager {

    private Context context;
    private String dexOutputPath;
    private File dexInternalStoragePath;

    private volatile boolean hasInit = false;


    private PluginManager (){}


    private void init (Context context){
        this.context = context;

        File optimizedDexPath = context.getDir("dexPath", Context.MODE_PRIVATE);
        if (!optimizedDexPath.exists()){
            optimizedDexPath.mkdirs();
        }
        dexOutputPath = optimizedDexPath.getAbsolutePath();

        dexInternalStoragePath = context.getDir("plugins", Context.MODE_PRIVATE);
        dexInternalStoragePath.mkdirs();

        try{


        }catch (Exception e){
            e.printStackTrace();
        }
        hasInit = true;

    }

    public static PluginManager getInstance (){
        return null;
    }


    public PluginInfo getPluginById (String pluginId){
        return null;
    }

}
