package com.example.wengzc.hellopluginmanager;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.android.dx.BinaryOp;
import com.android.dx.Code;
import com.android.dx.DexMaker;
import com.android.dx.FieldId;
import com.android.dx.Local;
import com.android.dx.MethodId;
import com.android.dx.TypeId;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Modifier;

public class MainActivity extends AppCompatActivity {

    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView) findViewById(R.id.tv);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //System.out.println("hello dexmaker");

                new Thread(){

                    @Override
                    public void run() {
                        testDexMaker();
                    }
                }.start();
            }
        });
    }

    private File externalDir (){
        return Environment.getExternalStorageDirectory();
    }

    private File dexmakerTargetDir (){
        //return getCacheDir();
        return getDir("666", MODE_PRIVATE);
    }

    private void testDexMaker() {
        try{
            DexMaker dexMaker = new DexMaker();

            TypeId helloWorld = TypeId.get("LHelloWorld;");
            dexMaker.declare(helloWorld, "HelloWorld.generated", Modifier.PUBLIC, TypeId.OBJECT);
            generateHelloMethod(dexMaker, helloWorld);


            File outputDir = new File(dexmakerTargetDir(), "dexmaker");
            if (!outputDir.exists()){
                outputDir.mkdir();
            }
            if (!outputDir.exists()){
                outputDir.mkdir();
            }
            ClassLoader loader = dexMaker.generateAndLoad(this.getClassLoader(), outputDir);
            Class helloworldClass = loader.loadClass("HelloWorld");

            helloworldClass.getMethod("hello").invoke(null);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static void generateHelloMethod(DexMaker dexMaker, TypeId<?> declaringType) {
        TypeId<System> systemType = TypeId.get(System.class);
        TypeId<PrintStream> printStreamType = TypeId.get(PrintStream.class);

        MethodId hello = declaringType.getMethod(TypeId.VOID, "hello");
        Code code = dexMaker.declare(hello, Modifier.STATIC | Modifier.PUBLIC);
        Local<Integer> a = code.newLocal(TypeId.INT);
        Local<Integer> b = code.newLocal(TypeId.INT);
        Local<Integer> c = code.newLocal(TypeId.INT);
        Local<String> s = code.newLocal(TypeId.STRING);
        Local<PrintStream> localSystemOut = code.newLocal(printStreamType);

        code.loadConstant(a, 0xabcd);
        code.loadConstant(b, 0xaaaa);
        code.op(BinaryOp.SUBTRACT, c, a, b);

        MethodId<Integer, String> toHexString = TypeId.get(Integer.class).getMethod(TypeId.STRING, "toHexString", TypeId.INT);
        code.invokeStatic(toHexString, s, c);

        FieldId<System, PrintStream> systemOutField = systemType.getField(printStreamType, "out");
        code.sget(systemOutField, localSystemOut);
        MethodId<PrintStream, Void> printlnMethod = printStreamType.getMethod(TypeId.VOID, "println", TypeId.STRING);
        code.invokeVirtual(printlnMethod, null, localSystemOut, s);

        code.returnVoid();
    }
}
