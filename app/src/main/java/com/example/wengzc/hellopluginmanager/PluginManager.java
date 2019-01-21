package com.example.wengzc.hellopluginmanager;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.webkit.WebStorage;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PluginManager {

    private Context context;
    private String dexOutputPath;
    private File dexInternalStoragePath;
    private CJClassLoader classLoader;

    private final Map<String, PluginInfo> pluginPkgToInfoMap = new ConcurrentHashMap<>();
    private final Map<String, PluginInfo> pluginIdToInfoMap = new ConcurrentHashMap<>();

    private volatile boolean hasInit = false;
    private volatile Activity appAty;

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
            Object mPackageInfo = ReflectionUtils.getFieldValue(
                    context, "mBase.mPackageInfo", true);

            classLoader = new CJClassLoader(context.getClassLoader());
            ReflectionUtils.setFieldValue(mPackageInfo, "mClassLoader", classLoader, true);
        }catch (Exception e){
            e.printStackTrace();
        }
        hasInit = true;
    }

    public static PluginManager getInstance (){
        return null;
    }


    public void startActivity (Context context, Intent intent){
        performStartActivity(context, intent);
        context.startActivity(intent);
    }

    public void startActivityForResult (Activity activity, Intent intent, int requestCode){
        performStartActivity(context, intent);
        activity.startActivityForResult(intent, requestCode);
    }

    private PluginInfo getPlugActivityInfo (Context context, String plugIdOrPkg){
        PluginInfo plug = null;
        plug = getPluginByPackageName(plugIdOrPkg);
        if (plug == null){
            plug = getPluginById(plugIdOrPkg);
        }
        if (plug == null){
            throw new IllegalArgumentException("");
        }
        return plug;
    }

    public PluginInfo getPluginByPackageName (String packageName){
        return pluginPkgToInfoMap.get(packageName);
    }

    public PluginInfo getPluginById (String pluginId){
        if (pluginId == null){
            return null;
        }
        return pluginIdToInfoMap.get(pluginId);
    }


    private void performStartActivity (Context context, Intent intent){
        checkInit();

        String pluginIdOrPkg;
        String actName;
        ComponentName origComp = intent.getComponent();
        if (origComp != null){
            pluginIdOrPkg = origComp.getPackageName();
            actName = origComp.getClassName();
        }else{
            throw new IllegalArgumentException("");
        }
        PluginInfo plug = getPlugActivityInfo(context, pluginIdOrPkg);
        String className = classLoader.newActivityClassName( plug.getId(), actName);
        ComponentName comp = new ComponentName(context, className);
        intent.setAction(null);
        intent.setComponent(comp);
    }


    private void checkInit (){
        if (!hasInit){
            throw new IllegalStateException("");
        }
    }

    public Collection<PluginInfo> getPlugins (){
        return pluginIdToInfoMap.values();
    }

    public void uninstallPluginById (String pluginId){

    }

    public void uninstallPluginByPkg (String pkg){

    }

    private void uninstallPlugin (String k, boolean isId){
        checkInit();

        PluginInfo pl = isId ? removePlugById(k) : removePlugByPkg(k);
        if (pl == null){
            return;
        }
        if (context instanceof Application){
            if (Build.VERSION.SDK_INT >= 14){
                try{
                    Application.class
                            .getMethod("unregisterComponentCallbacks", Class.forName("android.content.ComponentCallbacks"))
                            .invoke(context, pl.getApplication());
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

    }


    private PluginInfo buildPluginInfo (File pluginApk, String pluginId,
        String targetFileName) throws Exception {
        PluginInfo info = new PluginInfo();
        info.setId(pluginId == null ? pluginApk.getName() : pluginId);

        File privateFile = new File(dexInternalStoragePath,
                targetFileName == null ? pluginApk.getName() : targetFileName);
        info.setFilePath(privateFile.getAbsolutePath());

        if (!pluginApk.getAbsolutePath().equals(privateFile.getAbsolutePath())){
            copyApkoPrivatePath(pluginApk, privateFile);
        }
        String dexPath = privateFile.getAbsolutePath();
        PluginManifestUtil.setManifestInfo(context, dexPath, info);

        PluginClassLoader loader = new PluginClassLoader(dexPath, dexOutputPath, classLoader, info);
        info.setClassLoader(loader);

        try{
            AssetManager am = AssetManager.class.newInstance();
            am.getClass().getMethod("addAssetPath", String.class)
                    .invoke(am, dexPath);
            info.setAssetManager(am);
            Resources ctxres = context.getResources();
            Resources res = new Resources(am, ctxres.getDisplayMetrics(),
                    ctxres.getConfiguration());
            info.setResources(res);

        }catch (Exception e){
            e.printStackTrace();
        }



    }

    void initPluginApplication (final PluginInfo info, Activity actFrom){
        init
    }


    private void copyApkoPrivatePath (File pluginApk, File f){
        FileUtil.copyFile(pluginApk, f);
    }

    private void setApplicationBase (PluginInfo plugin, Application application)
        throws  Exception{

        synchronized (plugin){
            if (plugin.getApplication() != null){
                return;
            }

            plugin.setApplication(application);



        }
    }




    private PluginInfo removePlugById (String pluginId){
        PluginInfo pl = null;
        synchronized (this){
            pl = pluginIdToInfoMap.remove(pluginId);
            if (pl == null){
                return null;
            }
            pluginPkgToInfoMap.remove(pl.getPackageName());
        }
        return pl;
    }

    private PluginInfo removePlugByPkg (String pkg){
        PluginInfo pl = null;
        synchronized (this){
            pl = pluginPkgToInfoMap.remove(pl.getPackageName());
            if (pl == null){
                return null;
            }
            pluginIdToInfoMap.remove(pl.getId());
        }
        return pl;
    }
}
