package com.hrl.chaui.util;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hrl.chaui.R;
import com.hrl.chaui.activity.LoginActivity;
import com.hrl.chaui.activity.MainActivity;
import com.hrl.chaui.bean.User;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.hrl.chaui.MyApplication.contactData;
import static com.hrl.chaui.MyApplication.friendData;
import static com.hrl.chaui.MyApplication.groupData;
import static com.hrl.chaui.MyApplication.modifyUser;

public class http {
    public static void sendByPost(Context mContext, Integer user_id) {
        JSONObject json = new JSONObject();
        json.put("user_id", user_id);
        String path = mContext.getResources().getString(R.string.request_local)+"/userContacts";
        OkHttpClient client = new OkHttpClient();
        final FormBody formBody = new FormBody.Builder()
                .add("json", json.toJSONString())
                .build();
        Request request = new Request.Builder()
                .url(path)
                .post(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Looper.prepare();
                Toast.makeText(mContext, "服务器连接失败", Toast.LENGTH_SHORT).show();
                Looper.loop();
                e.printStackTrace();
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String info = response.body().string();
                JSONObject json = JSON.parseObject(info);
                JSONArray contacts = json.getJSONArray("contacts");
                System.out.println("*********contact" + json.toJSONString()+mContext);
                contactData = new ArrayList<>();
                groupData = new ArrayList<>();
                friendData=new ArrayList<>();
                contactData.add((User) new User("新的朋友","-1").setTop(true).setBaseIndexTag("↑"));
                contactData.add((User) new User("群聊","-1").setTop(true).setBaseIndexTag("↑"));
                for (int i = 0; i < contacts.size(); i++) {
                    JSONObject obj = contacts.getJSONObject(i);
                    if (obj.getInteger("type") == 0) {
                        contactData.add(JSONObject.parseObject(obj.toJSONString(),User.class));
                        friendData.add(JSONObject.parseObject(obj.toJSONString(),User.class));
                    } else {
                        groupData.add(JSONObject.parseObject(obj.toJSONString(),User.class));
                    }
                }

            }
        });
    }
    public static void sendByPostLogin(Context mContext,String user_phone, String user_pwd) {
        JSONObject json=new JSONObject();
        json.put("user_phone",user_phone);
        json.put("user_pwd",user_pwd);
        String path = mContext.getResources().getString(R.string.request_local)+"/userLogin";
        OkHttpClient client = new OkHttpClient();
        final FormBody formBody = new FormBody.Builder()
                .add("json", json.toJSONString())
                .build();
        System.out.println("*********"+json.toJSONString());
        Request request = new Request.Builder()
                .url(path)
                .post(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Looper.prepare();
                Toast.makeText(mContext, "服务器连接失败", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(mContext, LoginActivity.class);
                mContext.startActivity(intent);
                Looper.loop();
                e.printStackTrace();
            }
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String info=response.body().string();
                JSONObject json= JSON.parseObject(info);
                System.out.println("**********info in splash"+info);
                Intent intent;
                /*Intent intentService=new Intent(mContext, MqttService.class);
                mContext.startService(intentService);*/ //TODO 自动登录时不在跳转页启动服务
                switch (Integer.parseInt(json.get("status").toString())){
                    case 2:
                        http.sendByPost(mContext,json.getInteger("user_id"));  //重新登陆更新通讯录
                        intent = new Intent(mContext, MainActivity.class);
                        mContext.startActivity(intent);
                        modifyUser.setUser_id(json.getInteger("user_id"));     //重新登录更新modifyUser
                        modifyUser.setUser_name(json.getString("user_name"));
                        modifyUser.setUser_gender(json.getString("user_gender"));
                        modifyUser.setUser_pwd(user_pwd);
                        modifyUser.setUser_phone(json.getString("user_phone"));
                        modifyUser.setUser_img(json.getString("user_img"));
                        modifyUser.setUser_sign(json.getString("user_sign"));
                        break;
                    case 1:

                        Looper.prepare();
                        Toast.makeText(mContext, "密码错误!", Toast.LENGTH_SHORT).show();
                        intent = new Intent(mContext, LoginActivity.class);
                        mContext.startActivity(intent);
                        Looper.loop();
                        break;
                    case 0:
                        Looper.prepare();
                        Toast.makeText(mContext, "该账户不存在!", Toast.LENGTH_SHORT).show();
                        intent = new Intent(mContext, LoginActivity.class);
                        mContext.startActivity(intent);
                        Looper.loop();
                        break;
                }
            }
        });
    }
}
