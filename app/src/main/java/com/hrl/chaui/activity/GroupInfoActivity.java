package com.hrl.chaui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hrl.chaui.R;
import com.hrl.chaui.adapter.GroupMemberAdapter;
import com.hrl.chaui.bean.User;
import com.hrl.chaui.util.http;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.hrl.chaui.MyApplication.groupMemberData;
import static com.hrl.chaui.MyApplication.modifyUser;

public class GroupInfoActivity extends AppCompatActivity {
    public SharedPreferences sp;
    public SharedPreferences.Editor editor;
    public TextView back_arrow;
    public View group_name_view,nickname_view;
    public TextView group_name;
    public TextView nickname;
    public TextView delete;
    public GridView gridView;
    public Intent intent;
    public Bundle bundle;
    public Bundle bundle1;
    Bundle bundle_modifyName;
    User group;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_group_info);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.top_bottom));
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        SharedPreferences userId=getSharedPreferences("data_userID",MODE_PRIVATE); //??????ID??????
        sp=getSharedPreferences("data_"+userId.getInt("user_id",-1),MODE_PRIVATE); //??????ID????????????????????????
        editor = sp.edit();
        intent=getIntent();
        bundle = intent.getExtras();
        bundle_modifyName=new Bundle();
        bundle_modifyName.putString("from","group");
        int group_id=bundle.getInt("contact_id");
        group= (User) intent.getSerializableExtra("targetUser");
        delete=findViewById(R.id.delete);
        gridView=findViewById(R.id.gridview);
        back_arrow=findViewById(R.id.back_arrow);
        group_name=findViewById(R.id.group_name);
        nickname=findViewById(R.id.nickname);
        nickname_view=findViewById(R.id.nickname_view);
        group_name_view=findViewById(R.id.group_name_view);

        sendByPost(group_id, modifyUser.getUser_id());

        group_name_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(modifyUser.getRank()==0||modifyUser.getRank()==1){
                    Intent intent_modifyName=new Intent(GroupInfoActivity.this,ModifyNameActivity.class);
                    bundle_modifyName.putString("which","group_name");
                    bundle_modifyName.putString("group_name",group_name.getText().toString());
                    intent_modifyName.putExtras(bundle_modifyName);
                    intent_modifyName.putExtra("group",group);
                    startActivity(intent_modifyName);
                }
            }
        });
        nickname_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_modifyName=new Intent(GroupInfoActivity.this,ModifyNameActivity.class);
                bundle_modifyName.putString("which","nickname");
                bundle_modifyName.putString("nickname",nickname.getText().toString());
                intent_modifyName.putExtras(bundle_modifyName);
                startActivity(intent_modifyName);
            }
        });

        back_arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                back();
            }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendByPost_delete(modifyUser.getUser_id(),group.getUser_id());
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            back();
        }
        return true;
    }
    private void back(){
        Intent intent_back=new Intent(GroupInfoActivity.this, GroupChatActivity.class);
        if(bundle1!=null){
            if(bundle1.getString("group_name")!=null){
                group.setUser_name(bundle1.getString("group_name"));
                group.setSelect(true);
            }
        }
        intent_back.putExtra("targetUser",group);
        intent_back.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent_back);
        overridePendingTransition(0, R.anim.slide_right_out);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent=getIntent();
        bundle1=intent.getExtras();
        if(bundle1!=null){
            if(bundle1.getBoolean("isModify")){
                String name;
                if(bundle1.getString("group_name")!=null){
                    name=bundle1.getString("group_name");
                    group_name.setText(name);
                    sendByPost_groupname(bundle1.getInt("group_id"),name,null);
                }
                if(bundle1.getString("nickname")!=null){
                    name=bundle1.getString("nickname");
                    nickname.setText(name);
                    sendByPost_nickname(modifyUser.getUser_id(),bundle1.getInt("group_id"),name,modifyUser.getRank());
                }
            }
        }
    }

    /** ??????????????????????????????
     * @param group_id
     * @param user_id
     */
    private void sendByPost(int group_id, int user_id) {
        JSONObject json=new JSONObject();
        json.put("group_id",group_id);
        json.put("user_id",user_id);
        String path = getResources().getString(R.string.request_local)+"/groupSearch";
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
                Toast.makeText(GroupInfoActivity.this, "?????????????????????", Toast.LENGTH_SHORT).show();
                Looper.loop();
                e.printStackTrace();
            }
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String info=response.body().string();
                System.out.println("***********group_info"+info);
                JSONObject json= JSON.parseObject(info);
                bundle_modifyName.putInt("group_id",json.getInteger("group_id"));
                JSONArray members=json.getJSONArray("members");
                groupMemberData=new ArrayList<>();
                for(int i=0;i<members.size();i++){
                    JSONObject obj= members.getJSONObject(i);
                    User user=new User();
                    user.setUser_img(obj.getString("user_img"));
                    user.setUser_name(obj.getString("user_name"));
                    user.setUser_id(obj.getInteger("user_id"));
                    user.setUser_phone(obj.getString("user_phone"));
                    user.setNickname(obj.getString("nickname"));
                    if(obj.getString("friend_note")!=null){
                        user.setUser_note(obj.getString("friend_note"));
                    }
                    if(obj.getInteger("user_id").equals(modifyUser.getUser_id())){
                        modifyUser.setNickname(obj.getString("nickname"));
                        modifyUser.setRank(obj.getInteger("rank"));
                    }
                    groupMemberData.add(user);
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                group_name.setText(json.getString("group_name"));
                                nickname.setText(modifyUser.getNickname());
                                GroupMemberAdapter mGroupMemberAdapter=new GroupMemberAdapter(getApplicationContext(),groupMemberData);
                                mGroupMemberAdapter.setGroup_name(bundle.getString("group_name"));
                                gridView.setAdapter(mGroupMemberAdapter);
                                mGroupMemberAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    /**???????????????
     * @param user_id ????????????ID
     * @param group_id ?????????????????????ID
     * @param user_nickname ????????????????????????
     * @param rank  ???????????????????????????0????????????1???????????????2???
     */
    private void sendByPost_nickname(int user_id,int group_id, String user_nickname,Integer rank) {
        JSONObject json=new JSONObject();
        json.put("group_id",group_id);
        json.put("user_id",user_id);
        json.put("nickname",user_nickname);
        json.put("rank",rank);
        String path = getResources().getString(R.string.request_local)+"/memberUpdate";
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
                Toast.makeText(GroupInfoActivity.this, "?????????????????????", Toast.LENGTH_SHORT).show();
                Looper.loop();
                e.printStackTrace();
            }
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String info = response.body().string();
                System.out.println("***********group_info" + info);
                JSONObject json = JSON.parseObject(info);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        nickname.setText(user_nickname);
                    }
                });

            }
        });
    }

    /** ????????????
     * @param group_id
     * @param group_Name
     * @param group_notice
     */
    private void sendByPost_groupname(int group_id, String group_Name,String group_notice) {
        JSONObject json=new JSONObject();
        json.put("group_id",group_id);
        json.put("group_name",group_Name);
        json.put("group_notice",group_notice);
        String path = getResources().getString(R.string.request_local)+"/groupUpdate";
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
                Toast.makeText(GroupInfoActivity.this, "?????????????????????", Toast.LENGTH_SHORT).show();
                Looper.loop();
                e.printStackTrace();
            }
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String info = response.body().string();
                System.out.println("***********group_info" + info);
                JSONObject json = JSONObject.parseObject(info);
                if(json.getString("msg").equals("update success")){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            group_name.setText(group_Name);
                        }
                    });
                    http.sendByPost(GroupInfoActivity.this,modifyUser.getUser_id());
                    Looper.prepare();
                    Toast.makeText(GroupInfoActivity.this, "????????????!", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }else{
                    Looper.prepare();
                    Toast.makeText(GroupInfoActivity.this, "????????????", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }


            }
        });
    }

    private void sendByPost_delete(int user_id, int group_id) {
        JSONObject json=new JSONObject();
        json.put("group_id",group_id);
        json.put("user_id",user_id);
        String path = getResources().getString(R.string.request_local)+"/memberDelete";
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
                Toast.makeText(GroupInfoActivity.this, "?????????????????????", Toast.LENGTH_SHORT).show();
                Looper.loop();
                e.printStackTrace();
            }
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String info = response.body().string();
                System.out.println("***********group_info" + info);
                JSONObject json = JSONObject.parseObject(info);
                if(json.getString("msg").equals("delete success")){
                    http.sendByPost(GroupInfoActivity.this,modifyUser.getUser_id());
                    Looper.prepare();
                    Toast.makeText(GroupInfoActivity.this, "????????????!", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                    Intent intent=new Intent(GroupInfoActivity.this,GroupActivity.class);
                    startActivity(intent);
                    finish();
                }else{
                    Looper.prepare();
                    Toast.makeText(GroupInfoActivity.this, "????????????", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }

            }
        });
    }

}
