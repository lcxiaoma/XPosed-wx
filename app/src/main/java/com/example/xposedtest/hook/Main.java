package com.example.xposedtest.hook;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import fr.arnaudguyon.xmltojsonlib.XmlToJson;

public class Main implements IXposedHookLoadPackage {

    private static Activity launcherUiActivity = null;
    public static final String name = "com.tencent.mm";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("------package name:"+lpparam.packageName);
        if(lpparam.packageName.equals(name)&&lpparam.processName.equals(name)){
            XposedBridge.log("------我是微信包 i am wechat");

            XposedHelpers.findAndHookMethod("com.tencent.mm.ui.LauncherUI", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    XposedBridge.log(" XP已检测到 com.tencent.mm.ui.LauncherUI.onCreate");
                    launcherUiActivity = (Activity) param.thisObject;
                }
            });

            XposedHelpers.findAndHookMethod("com.tencent.wcdb.database.SQLiteDatabase",
                    lpparam.classLoader,
                    "insert",
                    String.class,
                    String.class,
                    ContentValues.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            String tableName = (String) param.args[0];
                            XposedBridge.log("===tableName:"+tableName);
                            String args1 = (String) param.args[1];
                            XposedBridge.log("===args1:"+args1);
                            ContentValues contentValues = (ContentValues) param.args[2];
                            Set<String> strings =  contentValues.keySet();
                            for (String key:strings){
                                Object value = contentValues.get(key);
                                //XposedBridge.log("===key:"+key+"---value:"+value);
                            }
                            //判断是否是消息类型
                            if(tableName==null||!tableName.equals("message")){
                                return;
                            }
                            Integer type = contentValues.getAsInteger("type");
                            XposedBridge.log("===type:"+type);
                            if (null == type) {
                                return;
                            }
                            if (type == 436207665) {
                                // 处理红包消息
                                handleLuckyMoney(contentValues, lpparam);
                            }
                            //hook 微信主界面的onCreate方法，获得主界面对象
//                            XposedHelpers.findAndHookMethod("com.tencent.mm.ui.LauncherUI", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
//                                @Override
//                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                                    super.afterHookedMethod(param);
//                                    launcherUiActivity = (Activity) param.thisObject;
//                                }
//                            });
                        }
                    }
            );

            //获取拆包界面获取到的intent信息
            XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Activity activity = (Activity) param.thisObject;
                    String key_native_url = activity.getIntent().getStringExtra("key_native_url");
                    String key_username = activity.getIntent().getStringExtra("key_username");
                    int key_way = activity.getIntent().getIntExtra("key_way", 0);
                    XposedBridge.log("key_native_url: " + key_native_url + "\n");
                    XposedBridge.log("key_way: " + key_way + "\n");
                    XposedBridge.log("key_username: " + key_username + "\n");
                }
            });
            //chaibao C方法
            XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI", lpparam.classLoader, "c", int.class, int.class, String.class, XposedHelpers.findClass("com.tencent.mm.ah.m", lpparam.classLoader), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    XposedBridge.log("拆包方法C方法");
                    Field buttonField = XposedHelpers.findField(param.thisObject.getClass(), "lMN");
                    final Button kaiButton = (Button) buttonField.get(param.thisObject);
                    kaiButton.performClick();
                }
            });
            XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI", lpparam.classLoader, "a", XposedHelpers.findClass("com.tencent.mm.plugin.luckymoney.b.ad", lpparam.classLoader), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    XposedBridge.log("拆包方法A方法");
                }
            });
        }
    }

    private void handleLuckyMoney(ContentValues contentValues, XC_LoadPackage.LoadPackageParam lpparam) throws Exception{
        if(launcherUiActivity!=null){
            XposedBridge.log("call method com.tencent.mm.bm.d b, start LuckyMoneyReceiveUI");
            String talker = contentValues.getAsString("talker");
            String content = contentValues.getAsString("content");
            if (!content.startsWith("<msg")) {
                content = content.substring(content.indexOf("<msg"));
            }
            XmlToJson xmlToJson = new XmlToJson.Builder(content).build();
            JSONObject wcpayinfo = xmlToJson.toJson().getJSONObject("msg").getJSONObject("appmsg").getJSONObject("wcpayinfo");
            String nativeUrlString = wcpayinfo.getString("nativeurl");
            XposedBridge.log("nativeUrlString:"+nativeUrlString);
            Intent paramau = new Intent();
            paramau.putExtra("key_way", 1);
            paramau.putExtra("key_native_url", nativeUrlString);
            paramau.putExtra("key_username", talker);
            XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.tencent.mm.br.d",lpparam.classLoader),"b",launcherUiActivity,"luckymoney",".ui.LuckyMoneyReceiveUI",paramau);
        }else{
            XposedBridge.log("launcherUiActivity is null");
        }
    }
}
