package com.example.wengzc.hellopluginmanager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;

import com.android.dex.DexFormat;
import com.android.dx.Code;
import com.android.dx.Comparison;
import com.android.dx.DexMaker;
import com.android.dx.FieldId;
import com.android.dx.Label;
import com.android.dx.Local;
import com.android.dx.MethodId;
import com.android.dx.TypeId;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static java.lang.reflect.Member.PUBLIC;
import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PROTECTED;
import static java.lang.reflect.Modifier.STATIC;

public class ActivityClassGenerator {

    private static final String FIELD_ASSETMANAGER = "mAssetManager";
    private static final String FIELD_RESOURCES = "mResources";
    private static final String FIELD_M_ONCREATED = "mOnCreated";

    public static void createActivityDex (String superClassName, String targetClassName,
                                          File saveTo, String pluginId, String pkgName) throws IOException {
        byte[] dex = createActivityDex(superClassName, targetClassName, pluginId, pkgName);
        if (saveTo.getName().endsWith(".dex")){
            FileUtil.writeToFile(dex, saveTo);
        }else{
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(saveTo));
            jarOutputStream.putNextEntry(new JarEntry(DexFormat.DEX_IN_JAR_NAME));
            jarOutputStream.write(dex);
            jarOutputStream.closeEntry();
            jarOutputStream.close();
        }
    }


    public static <S, D extends S> byte[] createActivityDex (final String superClassName,
    final String targetClassName, final String pluginId, String pkgName){

        DexMaker dexMaker = new DexMaker();

        TypeId<D> generateType = TypeId.get('L'+targetClassName.replace('.', '/')+";");
        TypeId<S> superType = TypeId.get('L' + superClassName.replace('.', '/') + ";");

        //类声明
        dexMaker.declare(generateType, "", PUBLIC | FINAL, superType);

        //字段声明
        declareFields(dexMaker, generateType, superType, pluginId, pkgName);

        //构造方法声明
        declare_constructor(dexMaker, generateType, superType);

        //onCreate() 声明
        declareMethod_onCreate(dexMaker, generateType, superType);

        //getAssets()
        declareMethod_getAssets(dexMaker, generateType, superType);

        //getResources()
        declareMethod_getResources(dexMaker, generateType, superType);

        declareMethod_startActivityForResult(dexMaker, generateType,superType);
        // 声明 方法：public void onBackPressed()
        declareMethod_onBackPressed(dexMaker, generateType, superType);

        declareMethod_startService(dexMaker, generateType, superType);
        declareMethod_bindService(dexMaker, generateType, superType);
        declareMethod_unbindService(dexMaker, generateType, superType);
        declareMethod_stopService(dexMaker, generateType, superType);

        declareMethod_attachBaseContext(dexMaker, generateType, superType);

        declareMethod_getComponentName(dexMaker, generateType, superType, superClassName);
        declareMethod_getPackageName(dexMaker, generateType, pkgName);
        declareMethod_getIntent(dexMaker, generateType, superType);

        byte[] dex = dexMaker.generate();
        return dex;
    }

    private static <S, D extends S> void declareMethod_attachBaseContext(
            DexMaker dexMaker, TypeId<D> generatedType, TypeId<S> superType) {
        // Types
        TypeId<Context> Context = TypeId.get(Context.class);
        TypeId<AssetManager> AssetManager = TypeId.get(AssetManager.class);
        TypeId<Resources> Resources = TypeId.get(Resources.class);
        FieldId<D, AssetManager> assertManager = generatedType.getField(AssetManager,
                FIELD_ASSETMANAGER);
        FieldId<D, Resources> resources = generatedType.getField(Resources, FIELD_RESOURCES);
        TypeId<ActivityOverider> ActivityOverider = TypeId
                .get(ActivityOverider.class);
        TypeId<DisplayMetrics> DisplayMetrics = TypeId
                .get(DisplayMetrics.class);
        TypeId<Configuration> Configuration = TypeId.get(Configuration.class);

        MethodId<D, Void> method = generatedType.getMethod(TypeId.VOID,
                "attachBaseContext", Context);
        Code methodCode = dexMaker.declare(method, PROTECTED);
        TypeId<Object[]> ObjArr = TypeId.get(Object[].class);
        // locals -- 一个方法内的本地变量必须提前声明在所有操作之前
        Local<D> localThis = methodCode.getThis(generatedType);
        Local<Object[]> rsArr = methodCode.newLocal(ObjArr);
        Local<Object> rsArr0 = methodCode.newLocal(TypeId.OBJECT);
        Local<Object> rsArr1 = methodCode.newLocal(TypeId.OBJECT);
        Local<Context> base = methodCode.getParameter(0, Context);
        Local<Context> newbase = methodCode.newLocal(Context);
        Local<Integer> index0 = methodCode.newLocal(TypeId.INT);
        Local<Integer> index1 = methodCode.newLocal(TypeId.INT);
        Local<AssetManager> localAsm = methodCode.newLocal(AssetManager);
        Local<Resources> superRes = methodCode.newLocal(Resources);
        Local<DisplayMetrics> mtrc = methodCode.newLocal(DisplayMetrics);
        Local<Configuration> cfg = methodCode.newLocal(Configuration);
        Local<Resources> resLocal = methodCode.newLocal(Resources);
        Local<String> pluginId = get_pluginId(generatedType, methodCode);
        methodCode.loadConstant(index0, 0);
        methodCode.loadConstant(index1, 1);
        // codes:
        //  Object [] rs = ActivitiOverrider.overrideAttachBaseContext(_pluginId, activity, base);
        MethodId<ActivityOverider, Object[]> methodOverride = ActivityOverider.getMethod(ObjArr,
                "overrideAttachBaseContext",TypeId.STRING,TypeId.get(Activity.class),Context);
        methodCode.invokeStatic(methodOverride, rsArr, pluginId,localThis,base);
        methodCode.aget(rsArr0, rsArr, index0);
        methodCode.aget(rsArr1, rsArr, index1);
        methodCode.cast(newbase, rsArr0);// base = rs[0];
        methodCode.cast(localAsm, rsArr1);// localAsm = rs[1];


        methodCode.iput(assertManager, localThis, localAsm);
        // superRes = base.getResources();
        MethodId<Context, Resources> methodGetResources = Context.getMethod(Resources,
                "getResources");
        methodCode.invokeVirtual(methodGetResources, superRes, base);

        //
        // superRes.getDisplayMetrics()
        MethodId<Resources, DisplayMetrics> getDisplayMetrics = Resources
                .getMethod(DisplayMetrics, "getDisplayMetrics");
        methodCode.invokeVirtual(getDisplayMetrics, mtrc, superRes);
        //
        // superRes.getConfiguration()
        MethodId<Resources, Configuration> getConfiguration = Resources
                .getMethod(Configuration, "getConfiguration");
        methodCode.invokeVirtual(getConfiguration, cfg, superRes);
        //
        // res = new Resources(asm, superRes.getDisplayMetrics(), superRes.getConfiguration());

        MethodId<Resources, Void> res_constructor = Resources.getConstructor(
                AssetManager, DisplayMetrics, Configuration);
        methodCode.newInstance(resLocal, res_constructor, localAsm, mtrc, cfg);
        methodCode.iput(resources, localThis, resLocal);

        MethodId<S, Void> superMethod = superType.getMethod(TypeId.VOID,
                "attachBaseContext", Context);
        methodCode.invokeSuper(superMethod, null, localThis, newbase);
        methodCode.returnVoid();
    }

    private static <S, D extends S> void declareMethod_getIntent(
            DexMaker dexMaker, TypeId<D> generatedType, TypeId<S> superType) {
        TypeId<Intent> Intent = TypeId.get(Intent.class);
        TypeId<ComponentName> ComponentName = TypeId.get(ComponentName.class);
        String methodName = "getIntent";
        MethodId<D, Intent> method = generatedType
                .getMethod(Intent, methodName);
        MethodId<S, Intent> superMethod = superType
                .getMethod(Intent, methodName);

        Code code = dexMaker.declare(method, PUBLIC);
        Local<D> localThis = code.getThis(generatedType);
        Local<Intent> i = code.newLocal(Intent);
        Local<ComponentName> localComp =  code.newLocal(ComponentName);

        MethodId<D, ComponentName> getComponent = generatedType
                .getMethod(ComponentName, "getComponentName");

        code.invokeVirtual(getComponent, localComp, localThis);

        MethodId<Intent, Intent> setComponent = Intent
                .getMethod(Intent, "setComponent",ComponentName);

        code.invokeSuper(superMethod, i, localThis);
        code.invokeVirtual(setComponent, i, i, localComp);
        code.returnValue(i);
    }

    private static <S, D extends S> void declareMethod_getPackageName(DexMaker dexMaker, TypeId<D> generatedType, String pkgName){
        MethodId<D, String> method = generatedType.getMethod(TypeId.STRING,
                "getPackageName");
        Code methodCode = dexMaker.declare(method, PROTECTED);
        Local<String> pkg = methodCode.newLocal(TypeId.STRING);
        methodCode.loadConstant(pkg, pkgName);
        methodCode.returnValue(pkg);
    }

    private static <S, D extends S> void declareMethod_getComponentName(
            DexMaker dexMaker, TypeId<D> generatedType, TypeId<S> superType,String superClassName) {
        TypeId<ComponentName> ComponentName = TypeId.get(ComponentName.class);
        MethodId<D, ComponentName> method = generatedType.getMethod(ComponentName,
                "getComponentName");
        Code methodCode = dexMaker.declare(method, PROTECTED);
        Local<String> pkg =  methodCode.newLocal(TypeId.STRING);
        Local<String> cls =  methodCode.newLocal(TypeId.STRING);
        Local<ComponentName> localComp =  methodCode.newLocal(ComponentName);
        {
            FieldId<D, String> fieldPkg = generatedType.getField(TypeId.STRING,
                    "_pkg");
            methodCode.sget(fieldPkg, pkg);
        }
        methodCode.loadConstant(cls, superClassName);

        MethodId<ComponentName, Void> comp_constructor = ComponentName.getConstructor(
                TypeId.STRING,TypeId.STRING);
        methodCode.newInstance(localComp, comp_constructor, pkg, cls);
        methodCode.returnValue(localComp);
    }

    private static <S, D extends S> void declareMethod_startActivityForResult(
            DexMaker dexMaker, TypeId<D> generatedType, TypeId<S> superType) {
        TypeId<Intent> intent = TypeId.get(Intent.class);
        TypeId<Integer> requestCode = TypeId.INT;
        TypeId<Bundle> bundle = TypeId.get(Bundle.class);

        TypeId<?>[] params;
        String methodName = "startActivityForResult";
        final boolean isNewSdk = android.os.Build.VERSION.SDK_INT > 10;
        if (isNewSdk) {
            params = new TypeId[] { intent, requestCode, bundle };
        } else {
            params = new TypeId[] { intent, requestCode };
        }
        MethodId<D, Void> method = generatedType.getMethod(TypeId.VOID,
                methodName, params);
        MethodId<S, Void> superMethod = superType.getMethod(TypeId.VOID,
                methodName, params);
        Code methodCode = dexMaker.declare(method, PUBLIC);
        TypeId<ActivityOverider> ActivityOverider = TypeId
                .get(ActivityOverider.class);
        MethodId<ActivityOverider, Intent> methodOveride = ActivityOverider
                .getMethod(intent, "overrideStartActivityForResult",
                        TypeId.get(Activity.class),TypeId.STRING,
                        intent, requestCode, bundle);
        // locals
        Local<D> localThis = methodCode.getThis(generatedType);
        Local<Intent> newIntent = methodCode.newLocal(intent);
        Local<Bundle> nullParamBundle = methodCode.newLocal(bundle);
        Local<String> pluginId = get_pluginId(generatedType, methodCode);
        methodCode.loadConstant(nullParamBundle, null);
        Local<?> args[];
        if (isNewSdk) {
            args = new Local[] {localThis
                    , pluginId
                    , methodCode.getParameter(0, intent)//
                    , methodCode.getParameter(1, requestCode)//
                    , methodCode.getParameter(2, bundle)//
            };
            methodCode.invokeStatic(methodOveride, newIntent, args);
            // super.startActivityForResult(...)

            methodCode.invokeSuper(superMethod, null,
                    localThis//
                    , newIntent//
                    , methodCode.getParameter(1, requestCode)//
                    , methodCode.getParameter(2, bundle) //
            );
        } else {
            args = new Local[] {localThis
                    , pluginId
                    , methodCode.getParameter(0, intent)//
                    , methodCode.getParameter(1, requestCode)//
                    ,nullParamBundle
            };
            methodCode.invokeStatic(methodOveride, newIntent, args);
            methodCode.invokeSuper(superMethod, null,
                    localThis//
                    , newIntent//
                    , methodCode.getParameter(1, requestCode)//
            );
        }
        methodCode.returnVoid();
    }

    /**
     * 生成以下代码： <br/>
     *
     * <pre>
     * public void onBackPressed() {
     * 	if (ActivityOverider.overrideOnbackPressed(this, pluginId)) {
     * 		super.onBackPressed();
     * 	}
     * }
     * </pre>
     *
     */

    private static <S, D extends S> void declareMethod_onBackPressed(
            DexMaker dexMaker, TypeId<D> generatedType, TypeId<S> superType) {
        TypeId<ActivityOverider> ActivityOverider = TypeId
                .get(ActivityOverider.class);
        MethodId<D, Void> method = generatedType.getMethod(TypeId.VOID,
                "onBackPressed");
        Code methodCode = dexMaker.declare(method, PUBLIC);
        // locals -- 一个方法内的本地变量必须提前声明在所有操作之前
        Local<D> localThis = methodCode.getThis(generatedType);
        Local<Boolean> localBool = methodCode.newLocal(TypeId.BOOLEAN);
        Local<Boolean> localFalse = methodCode.newLocal(TypeId.BOOLEAN);
        Local<String> pluginId = get_pluginId(generatedType, methodCode);

        methodCode.loadConstant(localFalse, false);

        MethodId<ActivityOverider, Boolean> methodOveride = ActivityOverider
                .getMethod(TypeId.BOOLEAN, "overrideOnbackPressed"
                        , TypeId.get(Activity.class), TypeId.STRING);
        methodCode.invokeStatic(methodOveride, localBool, localThis, pluginId);
        // codeBlock: if start
        Label localBool_isInvokeSuper = new Label();
        methodCode.compare(Comparison.EQ, localBool_isInvokeSuper, localBool,
                localFalse);
        MethodId<S, Void> superMethod = superType.getMethod(TypeId.VOID,
                "onBackPressed");
        methodCode.invokeSuper(superMethod, null, localThis);
        methodCode.mark(localBool_isInvokeSuper);
        // codeBlock: if end
        methodCode.returnVoid();
    }

    private static <S, D extends S> void declareMethod_startService(
            DexMaker dexMaker, TypeId<D> generatedType, TypeId<S> superType) {
        TypeId<ActivityOverider> ActivityOverider = TypeId
                .get(ActivityOverider.class);
        TypeId<ComponentName> returnType = TypeId.get(ComponentName.class);
        TypeId<Intent> Intent = TypeId.get(Intent.class);
        MethodId<D, ComponentName> method = generatedType.getMethod(returnType,
                "startService",Intent);
        MethodId<ActivityOverider, ComponentName> methodOveride = ActivityOverider
                .getMethod(returnType, "overrideStartService"
                        ,TypeId.get(Activity.class),TypeId.STRING
                        ,Intent);
        Code methodCode = dexMaker.declare(method, PUBLIC);
        // locals
        Local<D> localThis = methodCode.getThis(generatedType);
        Local<ComponentName> localComponentName = methodCode.newLocal(returnType);
        Local<String> pluginId = get_pluginId(generatedType, methodCode);

        methodCode.invokeStatic(methodOveride,
                localComponentName//
                ,localThis, pluginId
                , methodCode.getParameter(0, Intent)
        );
        methodCode.returnValue(localComponentName);
    }

    private static <S, D extends S> void declareMethod_bindService(
            DexMaker dexMaker, TypeId<D> generatedType, TypeId<S> superType) {
        //boolean bindService(intent, conn, flags);
        TypeId<ActivityOverider> ActivityOverider = TypeId
                .get(ActivityOverider.class);
        TypeId<Boolean> returnType = TypeId.BOOLEAN;
        TypeId<Intent> Intent = TypeId.get(Intent.class);
        TypeId<ServiceConnection> Conn = TypeId.get(ServiceConnection.class);
        MethodId<D, Boolean> method = generatedType.getMethod(returnType,
                "bindService",Intent,Conn,TypeId.INT);
        MethodId<ActivityOverider, Boolean> methodOveride = ActivityOverider
                .getMethod(returnType, "overrideBindService"
                        ,TypeId.get(Activity.class),TypeId.STRING
                        ,Intent,Conn,TypeId.INT);
        Code methodCode = dexMaker.declare(method, PUBLIC);
        // locals
        Local<D> localThis = methodCode.getThis(generatedType);
        Local<Boolean> localBool = methodCode.newLocal(returnType);
        Local<String> pluginId = get_pluginId(generatedType, methodCode);

        methodCode.invokeStatic(methodOveride,
                localBool//
                ,localThis, pluginId
                , methodCode.getParameter(0, Intent)
                , methodCode.getParameter(1, Conn)
                , methodCode.getParameter(2, TypeId.INT)
        );
        methodCode.returnValue(localBool);
    }

    private static <S, D extends S> void declareMethod_unbindService(
            DexMaker dexMaker, TypeId<D> generatedType, TypeId<S> superType) {
        //void unbindService( conn);
        TypeId<ActivityOverider> ActivityOverider = TypeId
                .get(ActivityOverider.class);
        TypeId<ServiceConnection> Conn = TypeId.get(ServiceConnection.class);
        MethodId<D, Void> method = generatedType.getMethod(TypeId.VOID,
                "unbindService",Conn);
        MethodId<ActivityOverider, Void> methodOveride = ActivityOverider
                .getMethod(TypeId.VOID, "overrideUnbindService"
                        ,TypeId.get(Activity.class),TypeId.STRING
                        ,Conn);
        Code methodCode = dexMaker.declare(method, PUBLIC);
        // locals
        Local<D> localThis = methodCode.getThis(generatedType);
        Local<String> pluginId = get_pluginId(generatedType, methodCode);

        methodCode.invokeStatic(methodOveride,
                null//
                ,localThis, pluginId
                , methodCode.getParameter(0, Conn)
        );
        methodCode.returnVoid();
    }

    private static <S, D extends S> void declareMethod_stopService(
            DexMaker dexMaker, TypeId<D> generatedType, TypeId<S> superType) {
        //boolean stopService(intent);
        TypeId<ActivityOverider> ActivityOverider = TypeId
                .get(ActivityOverider.class);
        TypeId<Boolean> returnType = TypeId.BOOLEAN;
        TypeId<Intent> Intent = TypeId.get(Intent.class);
        //
        MethodId<D, Boolean> method = generatedType.getMethod(returnType,
                "stopService",Intent);
        MethodId<ActivityOverider, Boolean> methodOveride = ActivityOverider
                .getMethod(returnType, "overrideStopService"
                        ,TypeId.get(Activity.class),TypeId.STRING
                        ,Intent);
        Code methodCode = dexMaker.declare(method, PUBLIC);
        // locals
        Local<D> localThis = methodCode.getThis(generatedType);
        Local<Boolean> localBool = methodCode.newLocal(returnType);
        Local<String> pluginId = get_pluginId(generatedType, methodCode);

        methodCode.invokeStatic(methodOveride,
                localBool//
                ,localThis, pluginId
                , methodCode.getParameter(0, Intent)
        );
        methodCode.returnValue(localBool);
    }

    private static <S, D extends S> void declareMethod_getResources(
            DexMaker dexMaker, TypeId<D> generatedType, TypeId<S> superType) {
        TypeId<Resources> Resources = TypeId.get(Resources.class);
        MethodId<D, Resources> getResources = generatedType.getMethod(
                Resources, "getResources");
        Code code = dexMaker.declare(getResources, PUBLIC);
        Local<D> localThis = code.getThis(generatedType);
        Local<Resources> localRes = code.newLocal(Resources);
        Local<Resources> nullV = code.newLocal(Resources);
        code.loadConstant(nullV, null);
        FieldId<D, Resources> res = generatedType.getField(Resources, FIELD_RESOURCES);
        code.iget(res, localRes, localThis);
        Label localResIsNull = new Label();
        code.compare(Comparison.NE, localResIsNull, localRes, nullV);
        MethodId<S, Resources> superGetResources = superType.getMethod(
                Resources, "getResources");
        code.invokeSuper(superGetResources, localRes, localThis);
        code.mark(localResIsNull);
        code.returnValue(localRes);
    }

    private static <S, D extends S> void declareMethod_getAssets
            (DexMaker dexMaker, TypeId<D> generatedType, TypeId<S> superType){
        TypeId<AssetManager> assetManagerType = TypeId.get(AssetManager.class);
        MethodId<D, AssetManager> getAssets = generatedType.getMethod(assetManagerType, "getAssets");
        Code code = dexMaker.declare(getAssets, PUBLIC);
        Local<D> localThis = code.getThis(generatedType);
        Local<AssetManager> localAsm = code.newLocal(assetManagerType);
        Local<AssetManager> nullV = code.newLocal(assetManagerType);

        code.loadConstant(nullV, null);
        FieldId<D, AssetManager> res = generatedType.getField(assetManagerType,
                FIELD_ASSETMANAGER);
        code.iget(res, localAsm, localThis);
        Label localAsmIsNull = new Label();
        code.compare(Comparison.NE, localAsmIsNull, localAsm, nullV);
        MethodId<S, AssetManager> superGetAssetManager = superType.getMethod(
                assetManagerType, "getAssets");
        code.invokeSuper(superGetAssetManager, localAsm, localThis);
        code.mark(localAsmIsNull);
        code.returnValue(localAsm);
    }




    private static <S, D extends S> void declareMethod_onCreate (DexMaker dexMaker, TypeId<D> generatedType, TypeId<S> superType){
        TypeId<Bundle> bundleType = TypeId.get(Bundle.class);
        TypeId<ActivityOverider> activityOveriderType = TypeId.get(ActivityOverider.class);

        MethodId<D, Void> method = generatedType.getMethod(TypeId.VOID, "onCreate", bundleType);
        Code methodCode = dexMaker.declare(method, PROTECTED);

        Local<D> localThis = methodCode.getThis(generatedType);
        Local<Bundle> localBundle = methodCode.getParameter(0, bundleType);
        Local<Boolean> localCreated = methodCode.newLocal(TypeId.BOOLEAN);
        Local<String> pluginId = get_pluginId(generatedType, methodCode);

        FieldId<D, Boolean> beforeOnCreate = generatedType.getField(TypeId.BOOLEAN, FIELD_M_ONCREATED);
        methodCode.loadConstant(localCreated, true);
        methodCode.iput(beforeOnCreate, localThis, localCreated);

        MethodId<S, Void> superMethod = superType.getMethod(TypeId.VOID, "onCreate", bundleType);
        methodCode.invokeSuper(superMethod, null, localThis, localBundle);
        methodCode.returnVoid();
    }

    private static <D> Local<String> get_pluginId (TypeId<D> generatedType, Code methodCode){
        Local<String> pluginId = methodCode.newLocal(TypeId.STRING);
        FieldId<D, String> fieldId = generatedType.getField(TypeId.STRING, "_pluginId");
        methodCode.sget(fieldId, pluginId);
        return pluginId;
    }

    private static <S, D extends  S> void declareFields (DexMaker dexMaker, TypeId<D> generatedType,
        TypeId<S> superType, String pluginId, String pkgName){
        FieldId<D, String> _pluginid = generatedType.getField(TypeId.STRING, "_pluginId");
        dexMaker.declare(_pluginid, PRIVATE | STATIC | FINAL, pluginId );
        FieldId<D, String> _pkg = generatedType.getField(TypeId.STRING, "_pkg");
        dexMaker.declare(_pkg, PRIVATE | STATIC | FINAL, pkgName);

        TypeId<AssetManager> assetManagerType = TypeId.get(AssetManager.class);
        TypeId<Resources> resourcesType = TypeId.get(Resources.class);
        FieldId<D, AssetManager> asm  = generatedType.getField(assetManagerType, FIELD_ASSETMANAGER);
        dexMaker.declare(asm, PRIVATE, null);
        FieldId<D, Resources> res  = generatedType.getField(resourcesType, FIELD_RESOURCES);
        dexMaker.declare(res, PRIVATE, null);
        FieldId<D, Boolean> beforeOnCreate  = generatedType.getField(TypeId.BOOLEAN, FIELD_M_ONCREATED);
        dexMaker.declare(beforeOnCreate, PRIVATE, null);

    }

    private static <S, D extends S> void declare_constructor (DexMaker dexMaker, TypeId<D> generatedType, TypeId<S> superType){
        MethodId<D, Void> method = generatedType.getConstructor();
        Code constructorCode = dexMaker.declare(method, PUBLIC);
        Local<D> localThis = constructorCode.getThis(generatedType);
        MethodId<S, Void> superConstructor = superType.getConstructor();
        constructorCode.invokeDirect(superConstructor, null, localThis);
        constructorCode.returnVoid();
    }




}
