package com.example.wengzc.hellopluginmanager;

public class CJClassLoader extends ClassLoader{

    private String plugId;
    private String actName;

    public CJClassLoader (ClassLoader parent){
        super(parent);
    }

    String newActivityClassName (String plugId, String actName){
        this.plugId = plugId;
        this.actName = actName;
        return ActivityOverider.targetClassName;
    }


    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        if (plugId != null){
            String pluginId = plugId;

            PluginInfo plugin = PluginManager.getInstance().getPluginById(pluginId);
            if (plugin != null){
                try{
                    if (className.equals(ActivityOverider.targetClassName)){
                        String actClassName = actName;
                        return plugin.getClassLoader().loadActivityClass(actClassName);
                    }else{
                        return plugin.getClassLoader().loadClass(className);
                    }

                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return super.loadClass(className, resolve);
    }
}
