package com.example.wengzc.hellopluginmanager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import dalvik.system.DexClassLoader;

public class PluginClassLoader extends DexClassLoader{

    private final String tag;
    private final PluginInfo thisPlugin;
    private final String optimizedDirectory;
    private final String libraryPath;


    private final Map<String, ClassLoader> proxyActivityLoaderMap;

    public PluginClassLoader (String dexPath, String optimizedDir, ClassLoader parent, PluginInfo plugin){
        super(dexPath, optimizedDir, plugin.getPackageInfo().applicationInfo.nativeLibraryDir, parent);
        thisPlugin = plugin;
        proxyActivityLoaderMap = new HashMap<String, ClassLoader>();
        this.libraryPath = plugin.getPackageInfo().applicationInfo.nativeLibraryDir;
        this.optimizedDirectory = optimizedDir;
        this.tag = "";
    }


    Class<?> loadActivityClass (final String actClassName) throws ClassNotFoundException {
        File dexSavePath = ActivityOverider.createProxyDex(thisPlugin, actClassName, true);
        ClassLoader actLoader = proxyActivityLoaderMap.get(actClassName);
        if (actLoader == null){
            actLoader = new DexClassLoader(dexSavePath.getAbsolutePath(), optimizedDirectory, libraryPath, this){
                @Override
                protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                   if (ActivityOverider.targetClassName.equals(name)){
                        Class<?> c = findLoadedClass(name);
                        if (c == null){
                            c = findClass(name);
                        }
                        if (resolve){
                            resolveClass(c);
                        }
                        return c;
                   }
                    return super.loadClass(name, resolve);
                }
            };
            proxyActivityLoaderMap.put(actClassName, actLoader);
        }
        return actLoader.loadClass(ActivityOverider.targetClassName);
    }

    protected Object getClassLoadingLock (String name){
        return name.hashCode();
    }


    private Class<?> findByParent(String name, boolean throwEx) throws ClassNotFoundException {
        Class<?> c = null;
        try{
            ClassLoader parent = getParent();
            if (parent != null){
                if (parent.getClass() == CJClassLoader.class){
                    parent = parent.getParent();
                }
                if (parent != null){
                    c = parent.loadClass(name);
                }
            }

        }catch (ClassNotFoundException e) {
            if (throwEx){
                throw e;
            }
        }
        return c;
    }


    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)){
            Class<?> c = findLoadedClass(name);
            if (c == null){
                if (name.startsWith("android.support.")){
                    try{
                        c = findClass(name);
                    }catch (ClassNotFoundException e){
                        e.printStackTrace();
                    }
                    if (c == null){
                        c = findByParent(name, true);
                    }
                }else{
                    c = findByParent(name, false);
                    if (c == null){
                        c = findClass(name);
                    }
                }
            }
            if (resolve){
                resolveClass(c);
            }
            return c;
        }
    }
}
