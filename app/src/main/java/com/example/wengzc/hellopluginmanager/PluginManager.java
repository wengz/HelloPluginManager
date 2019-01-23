package com.example.wengzc.hellopluginmanager;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.webkit.WebStorage;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PluginManager implements FileFilter{

    private static final PluginManager instance = new PluginManager();
    private Context context;

    private final Map<String, PluginInfo> pluginPkgToInfoMap = new ConcurrentHashMap<>();
    private final Map<String, PluginInfo> pluginIdToInfoMap = new ConcurrentHashMap<>();


    private String dexOutputPath;
    private File dexInternalStoragePath;
    private CJClassLoader classLoader;

    private volatile boolean hasInit = false;
    private volatile Activity appAty;

    private PluginManager (){}

    public static PluginManager getInstance (Context context){
        if (instance.hasInit || context == null){
            if (instance.appAty == null && context instanceof Activity){
                instance.appAty = (Activity) context;
            }
            return instance;
        }else{
            Context ctx = context;
            if (context instanceof  Activity){
                instance.appAty = (Activity) context;
                ctx = ((Activity) context).getApplication();
            }else if (context instanceof Service){
                ctx = ((Service) context).getApplication();
            }else if (context instanceof Application){
                ctx = context;
            }else {
                ctx = context.getApplicationContext();
            }
            synchronized (PluginManager.class){
                instance.init(ctx);
            }
            return instance;
        }
    }


    public Collection<PluginInfo> loadPlugin (final File pluginSrcDirFile)
        throws Exception{
        checkInit();
        if (pluginSrcDirFile == null || !pluginSrcDirFile.exists()){
            return null;
        }
        if (pluginSrcDirFile.isFile()){
            PluginInfo one = loadPluginWithId(pluginSrcDirFile, null, null);
            return Collections.singletonList(one);
        }

        synchronized (this){
            pluginPkgToInfoMap.clear();
            pluginIdToInfoMap.clear();
        }

        File[] pluginApks = pluginSrcDirFile.listFiles(this);
        if (pluginApks == null || pluginApks.length < 1){
            throw new FileNotFoundException("");
        }
        for (File pluginApk : pluginApks){
            PluginInfo pluginInfo = buildPlugInfo(pluginApk, null, null);
            if (pluginInfo != null){
                savePluginToMap(pluginInfo);
            }
        }
        return pluginIdToInfoMap.values();
    }

    public PluginInfo loadPluginWithId (File pluginApk, String pluginId)
        throws Exception{
        return loadPluginWithId(pluginApk, pluginId, null);
    }

    public PluginInfo loadPluginWithId (File pluginApk, String pluginId,
                String targetFileName) throws Exception{
        checkInit();
        PluginInfo pluginInfo = buildPlugInfo(pluginApk, pluginId, targetFileName);
        if (pluginInfo != null){
            savePluginToMap(pluginInfo);
        }
        return pluginInfo;
    }

    private synchronized void savePluginToMap (PluginInfo pluginInfo){
        pluginPkgToInfoMap.put(pluginInfo.getPackageName(), pluginInfo);
        pluginIdToInfoMap.put(pluginInfo.getId(), pluginInfo);
    }

    private PluginInfo buildPlugInfo (File pluginApk, String pluginId,
              String targetFileName) throws Exception {
        PluginInfo info = new PluginInfo();
        info.setId(pluginId == null ? pluginApk.getName() : pluginId);

        File privateFile = new File(dexInternalStoragePath,
                targetFileName == null ? pluginApk.getName() : targetFileName);
        info.setFilePath(privateFile.getAbsolutePath());

        if (! pluginApk.getAbsolutePath().equals(privateFile.getAbsolutePath())){
            copyApkoPrivatePath(pluginApk, privateFile);
        }

        String dexPath = privateFile.getAbsolutePath();
        PluginManifestUtil.setManifestInfo(context, dexPath, info);

        PluginClassLoader loader = new PluginClassLoader(dexPath, dexOutputPath, classLoader, info);
        info.setClassLoader(loader);

        try{
            AssetManager assetManager = AssetManager.class.newInstance();
            assetManager.getClass().getMethod("addAssetPath", String.class).invoke(assetManager, dexPath);
            info.setAssetManager(assetManager);

            Resources ctxResource = context.getResources();
            Resources resources = new Resources(assetManager, ctxResource.getDisplayMetrics(), ctxResource.getConfiguration());
            info.setResources(resources);

        }catch (Exception e){
            e.printStackTrace();
        }

        if (appAty != null){
            initPluginApplication(info, appAty, true);
        }

        return info;
    }



    /**
     *
     * @param context  ApplicationContext ??
     */
    private void init (Context context){
        this.context = context;

        File optimizedDexPath = context.getDir("dexPath", Context.MODE_PRIVATE);
        if (!optimizedDexPath.exists()){
            optimizedDexPath.mkdirs();
        }
        dexOutputPath = optimizedDexPath.getAbsolutePath();

        dexInternalStoragePath = context.getDir("plugins", Context.MODE_PRIVATE);
        dexInternalStoragePath.mkdirs();

        //修改Application的ClassLoader
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


    public boolean startMainActivity (Context context, String pkgOrId){
        PluginInfo plug =  getPlugActivityInfo(context, pkgOrId);
        if (classLoader == null){
            return false;
        }
        if (plug.getMainActivity() == null){
            return false;
        }
        if  (plug.getMainActivity().activityInfo == null){
            return false;
        }
        String className = classLoader.newActivityClassName(plug.getId(),
                plug.getMainActivity().activityInfo.name);
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, className));
        context.startActivity(intent);
        return true;
    }

    public static PluginManager getInstance (){
        return instance;
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


    public PluginInfo getPluginByPackageName (String packageName){
        return pluginPkgToInfoMap.get(packageName);
    }

    public PluginInfo getPluginById (String pluginId){
        if (pluginId == null){
            return null;
        }
        return pluginIdToInfoMap.get(pluginId);
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
        uninstallPlugin(pluginId, true);
    }

    public void uninstallPluginByPkg (String pkg){
        uninstallPlugin(pkg, false);
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

    void initPluginApplication (final PluginInfo info, Activity actFrom) throws Exception {
        initPluginApplication(info, actFrom, false);
    }

    void initPluginApplication (final PluginInfo plugin, Activity actFrom, boolean onLoad) throws Exception {
        if (!onLoad && plugin.getApplication() != null){
            return;
        }
        final String className = plugin.getPackageInfo().applicationInfo.name;
        if (className == null){
            if (onLoad){
                return;
            }
            Application application = new Application();
            setApplicationBase(plugin, application);
            return;
        }

        Runnable setApplicationTask = new Runnable() {
            @Override
            public void run() {
                ClassLoader loader = plugin.getClassLoader();
                try{
                    Class<?> applicationClass = loader.loadClass(className);
                    Application application = (Application) applicationClass.newInstance();
                    setApplicationBase(plugin, application);
                    application.onCreate();

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };

        if (actFrom == null){
            if (onLoad){
                return;
            }
            setApplicationTask.run();
        }else{
            actFrom.runOnUiThread(setApplicationTask);
        }
    }


    private void setApplicationBase (PluginInfo plugin, Application application) throws Exception {

        synchronized (plugin){
            if (plugin.getApplication() != null){
                return;
            }
            plugin.setApplication(application);

            PluginContextWrapper ctxWrapper = new PluginContextWrapper(context, plugin);
            plugin.appWrapper = ctxWrapper;

            Method attachMethod = Application.class.getDeclaredMethod("attach", Context.class);
            attachMethod.setAccessible(true);
            attachMethod.invoke(application, ctxWrapper);
            if (context instanceof Application){
                if (Build.VERSION.SDK_INT >= 14){
                    Application.class.getMethod(
                            "registerComponentCallbacks",
                            Class.forName("android.content.ComponentCallbacks"))
                            .invoke(context, application);
                }
            }
        }
    }


    private void copyApkoPrivatePath (File pluginApk, File f){
        FileUtil.copyFile(pluginApk, f);
    }

    File getDexInternalStoragePath (){
        return dexInternalStoragePath;
    }

    Context getContext (){
        return context;
    }

    CJClassLoader getFrameworkClassLoader (){
        return classLoader;
    }

    @Override
    public boolean accept(File pathname) {
        if (pathname.isDirectory()){
            return false;
        }
        String fname = pathname.getName();
        return fname.endsWith(".apk");
    }
}
