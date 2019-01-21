package com.example.wengzc.hellopluginmanager;

import java.lang.reflect.Field;

public class ReflectionUtils {

    public static <T> T getFieldValue (Object obj, String fieldName) throws Exception {
        return getFieldValue(obj, fieldName, true);
    }


    public static <T> T getFieldValue (Object obj, String fieldName, boolean resolveParent) throws Exception {
        Object[] rs = getField(obj, fieldName, resolveParent);
        if (rs == null){
            throw new NoSuchFieldException("");
        }
        Field field = (Field) rs[0];
        Object targetObj = rs[1];
        return (T)field.get(targetObj);
    }

    public static void setFieldValue (Object obj, String fieldName, Object value) throws Exception {
        setFieldValue(obj, fieldName, value, true);
    }

    public static void setFieldValue (Object obj, String fieldName, Object value, boolean resolveParent)
        throws Exception{
        Object[] rs = getField(obj, fieldName, resolveParent);
        if (rs == null){
            throw new NoSuchFieldException("");
        }
        Field field = (Field) rs[0];
        Object targetObj = rs[1];
        field.set(targetObj, value);

    }




    private static Object[] getField (Object obj, String elFieldlName, boolean resolveParent) throws Exception {

        if (obj == null){
            return null;
        }

        String[] fieldNames = elFieldlName.split("[.]");
        Object targetObj = obj;
        Class<?> targetClass = targetObj.getClass();
        Object val = null;
        int i = 0;
        Field field = null;
        Object[] rs = new Object[2];
        for (String fName : fieldNames){
            i++;
            field = getField_(targetClass, fName, resolveParent);
            field.setAccessible(true);
            rs[0] = field;
            rs[1] = targetObj;
            val = field.get(targetObj);
            if (val == null){
                if (i < fieldNames.length){
                    throw new IllegalAccessException("");
                }
                break;
            }
            targetObj = val;
            targetClass = targetObj.getClass();
        }

        return rs;
    }



    public static Field getField_ (Class<?> targetClass, String fieldName, boolean resolveParent) throws Exception {
        NoSuchFieldException noSuchFieldException = null;
        Field rsField = null;
        try{
            Field field = targetClass.getDeclaredField(fieldName);
            rsField = field;
            if (!resolveParent){
                field.setAccessible(true);
                return field;
            }
        }catch (NoSuchFieldException e){
            noSuchFieldException = e;
        }
        if (noSuchFieldException != null){
            if (resolveParent){
                while (true){
                    targetClass = targetClass.getSuperclass();
                    if (targetClass == null){
                        break;
                    }

                    try{
                        Field field = targetClass.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        return rsField = field;
                    }catch (NoSuchFieldException e){
                        if (targetClass.getSuperclass() == null){
                            throw e;
                        }
                    }
                }

            }else{
                throw noSuchFieldException;
            }
        }
        return rsField;
    }


}
