package com.hrl.chaui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;
import com.hrl.chaui.R;
import com.hrl.chaui.activity.LoginActivity;
import com.hrl.chaui.activity.ModifyActivity;
import com.hrl.chaui.activity.ResetPwdActivity;
import com.hrl.chaui.util.AppManager;
import com.hrl.chaui.util.http;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;
import static com.hrl.chaui.MyApplication.friendRequest;
import static com.hrl.chaui.MyApplication.isImgChange;
import static com.hrl.chaui.MyApplication.modifyUser;

public class MineFragment extends Fragment {
    TextView name;
    TextView sign;
    TextView phone;
    CircleImageView img;
    SharedPreferences sp;
    SharedPreferences.Editor editor;
    public final int Modify = 1;
    public final int ResetPwd = 2;
    Bundle bundle;
    String img_uri;

    @Nullable
    @Override
    public View onCreateView(@NonNull  LayoutInflater inflater, @Nullable  ViewGroup container, @Nullable  Bundle savedInstanceState) {
        View root=inflater.inflate(R.layout.layout_mine,container,false);
        img=root.findViewById(R.id.user_img);
        name=root.findViewById(R.id.user_name);
        sign=root.findViewById(R.id.user_sign);
        phone=root.findViewById(R.id.user_phone);
        TextView modify=root.findViewById(R.id.modify);
        TextView modify_pwd=root.findViewById(R.id.modify_pwd);
        TextView change=root.findViewById(R.id.change);
        phone.setText("?????????: "+modifyUser.getUser_phone());

        SharedPreferences userId=Objects.requireNonNull(getContext()).getSharedPreferences("data_userID",MODE_PRIVATE); //???????????????ID
        sp=Objects.requireNonNull(getContext()).getSharedPreferences("data_"+userId.getInt("user_id",-1),MODE_PRIVATE); //??????ID????????????????????????
        SharedPreferences.Editor editorID=userId.edit();
        editor=sp.edit();
        img_uri="";

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.modify:
                        Intent intent=new Intent(getActivity(), ModifyActivity.class);
                        startActivityForResult(intent, Modify);
                        break;
                    case R.id.modify_pwd:
                        Intent intent2=new Intent(getActivity(), ResetPwdActivity.class);
                        startActivityForResult(intent2,ResetPwd);
                        break;
                    case R.id.change:
                        //TODO ???????????? ?????????mqttService  ????????????
                        /*Service service= Objects.requireNonNull(getContext()).getSystemService(MqttService.class);
                        service.stopSelf();*/
                        //????????????????????????????????????????????????
                        editor.putString("user_gender", modifyUser.getUser_gender());
                        editor.putInt("user_id",modifyUser.getUser_id());
                        editor.putString("user_img",modifyUser.getUser_img());
                        editor.putString("user_name",modifyUser.getUser_name());
                        editor.putString("user_phone",modifyUser.getUser_phone());
                        editor.putString("user_sign",modifyUser.getUser_sign());
                        editor.putString("user_pwd",modifyUser.getUser_pwd());
                        editor.apply();
                        editor.putString("friReqMessage", JSONObject.toJSONString(friendRequest));
                        editorID.putInt("user_id",-1);
                        editorID.apply();
                        isImgChange=false;
                        friendRequest=new ArrayList<>();
                        AppManager.AppExit(getContext());
                        Intent intent1=new Intent(getContext(),LoginActivity.class);
                        Objects.requireNonNull(getContext()).startActivity(intent1);
                        Objects.requireNonNull(getActivity()).finish();
                        break;
                    case R.id.user_img:
                        imgMax(((BitmapDrawable)img.getDrawable()).getBitmap());
                    default:
                        break;
                }
            }
        };

        modify.setOnClickListener(listener);
        modify_pwd.setOnClickListener(listener);
        change.setOnClickListener(listener);
        img.setOnClickListener(listener);

        name.setText(modifyUser.getUser_name());
        //Glide.with(Objects.requireNonNull(getContext())).load(getContext().getString(R.string.app_prefix_img)+sp.getString("user_img","")).into(img);
        sign.setText("????????????: "+(modifyUser.getUser_sign()==null?"":modifyUser.getUser_sign()));
        phone.setText("?????????: "+modifyUser.getUser_phone());


        return root;
    }


    private void showDialog(){
        final AlertDialog.Builder dialog=new AlertDialog.Builder(getContext());
        dialog.setMessage("?????????????????????????????????????????????");
        dialog.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //db.delete("QQ_Login","QQname=? AND QQpwd=?",new String[]{sp.getString("QQname",""),sp.getString("QQpwd","")});
                Intent intent4=new Intent(getContext(), LoginActivity.class);
                startActivity(intent4);
                Objects.requireNonNull(getActivity()).finish();
            }
        });
        dialog.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        dialog.show();
    }
/*
    @Override
    public void onFragmentResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==Modify) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Bundle bundle = data.getExtras();
                    Log.i("json in Mine",bundle.getString("json"));
                }
            }
        }
    }

 */


    @Override
    public void onResume() {
        super.onResume();
        Log.i("isImgChang me resu",""+isImgChange);
        Log.i("user_img in Mine",sp.getString("user_img", ""));
        if (!sp.getString("user_img", "").equals("") && !isImgChange) {
            Glide.with(Objects.requireNonNull(getContext())).load(getString(R.string.app_prefix_img) +sp.getString("user_img", "")).into(img);
        } else if (!"".equals(sp.getString("img_uri","")) && isImgChange) {
            Log.i("isImgChange in Modify",""+isImgChange);
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(sp.getString("img_uri","")));
                img.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==Modify) {
            if (resultCode == Activity.RESULT_OK) {
                //Log.i("json in Main",data.getStringExtra("json"));
                //MineFragment.onFragmentResult(requestCode,resultCode,data);
                bundle = data.getExtras();
                isImgChange=bundle.getBoolean("isImgChange");
                Log.i("isImg onResult",""+isImgChange);
                img_uri=bundle.getString("img_uri");
                if(bundle.getBoolean("isModify")||bundle.getBoolean("isImgChange")){
                    sendByPost_upDate(bundle.getString("json"));
                }
            }
        }
        if(requestCode==ResetPwd){
            if(resultCode== Activity.RESULT_OK){
                System.out.println("*********bundle "+data);
                if(data!=null){
                    Bundle bundle = data.getExtras();
                    System.out.println("*********userUpdate  "+bundle.getString("json"));
                    boolean isModify = bundle.getBoolean("isModify");
                    if (isModify) {
                        String json=bundle.getString("json");
                        sendByPost_upDate(json);
                    }
                }
            }
        }


    }

    private void sendByPost_upDate(String jsonString) {
        String path = getResources().getString(R.string.request_local)+"/userUpdate";
        OkHttpClient client = new OkHttpClient();
        final FormBody formBody = new FormBody.Builder()
                .add("json", jsonString)
                .build();
        System.out.println("*********userUpdate "+jsonString);
        Request request = new Request.Builder()
                .url(path)
                .post(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Looper.prepare();
                Toast.makeText(getContext(), "?????????????????????", Toast.LENGTH_SHORT).show();
                Looper.loop();
                e.printStackTrace();
            }
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String info=response.body().string();
                JSONObject json= JSON.parseObject(info);
                System.out.println("*****userUpdate return "+info);
                if(json.getString("msg").equals("update success")){
                    http.sendByPostLogin(Objects.requireNonNull(getContext()),modifyUser.getUser_phone(),modifyUser.getUser_pwd()); //??????????????????????????????,?????????user_img
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSONObject json=JSON.parseObject(jsonString);
                            name.setText(json.getString("user_name"));
                            //Glide.with(Objects.requireNonNull(getContext())).load(getContext().getString(R.string.app_prefix_img)+sp.getString("user_img","")).into(img);
                            sign.setText("????????????: "+modifyUser.getUser_sign());
                            //TODO ????????????????????????
                            Log.i("sendby isImgChang mine",""+isImgChange);
                            Log.i("sendby user_img mine",modifyUser.getUser_img());
                            if (!"".equals(modifyUser.getUser_img()) && !isImgChange) {
                                Glide.with(Objects.requireNonNull(getContext())).load(getString(R.string.app_prefix_img) +modifyUser.getUser_img()).into(img);
                            } else {
                                try {
                                    Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(sp.getString("img_uri","")));
                                    img.setImageBitmap(bitmap);
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    editor.putString("user_img",modifyUser.getUser_img());
                    editor.putString("user_name",modifyUser.getUser_name());
                    editor.putString("user_sign",modifyUser.getUser_sign());
                    editor.putString("user_gender",modifyUser.getUser_gender());
                    editor.putString("user_pwd",modifyUser.getUser_pwd());
                    editor.apply();
                    Looper.prepare();
                    Toast.makeText(getContext(),"????????????!",Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
                else{
                    modifyUser.setUser_pwd(sp.getString("user_pwd",""));
                    Looper.prepare();
                    Toast.makeText(getContext(),"????????????",Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            }
        });
    }

    /**
     * ??????????????????     */
    public void imgMax(Bitmap bitmap) {

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View imgEntryView = inflater.inflate(R.layout.dialog_photo_entry, null);
        // ??????????????????????????????
        final AlertDialog dialog = new AlertDialog.Builder(getContext()).create();
        ImageView img = (ImageView) imgEntryView.findViewById(R.id.large_image);
        img.setImageBitmap(bitmap);
        // ?????????????????????????????????????????????????????????????????????
        // imageDownloader.download(imageBmList.get(0),img);
        dialog.setView(imgEntryView); // ?????????dialog
        dialog.show();
        // ???????????????????????????????????????????????????????????????dialog????????????dialog???????????????
        imgEntryView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View paramView) {
                dialog.cancel();
            }
        });
    }
}
