package com.hrl.chaui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.aliyun.apsaravideo.sophon.bean.RTCAuthInfo;
import com.aliyun.apsaravideo.sophon.videocall.VideoCallActivity;
import com.aliyun.rtc.voicecall.bean.AliUserInfoResponse;
import com.aliyun.rtc.voicecall.ui.AliRtcChatActivity;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.hrl.chaui.adapter.ChatAdapter;
import com.hrl.chaui.bean.MsgType;
import com.hrl.chaui.bean.User;
import com.hrl.chaui.dao.imp.MessageDaoImp;
import com.hrl.chaui.util.LogUtil;
import com.hrl.chaui.bean.Message;
import com.hrl.chaui.R;
import com.hrl.chaui.bean.AudioMsgBody;
import com.hrl.chaui.bean.FileMsgBody;
import com.hrl.chaui.bean.ImageMsgBody;
import com.hrl.chaui.bean.MsgSendStatus;
import com.hrl.chaui.bean.TextMsgBody;
import com.hrl.chaui.bean.VideoMsgBody;
import com.hrl.chaui.util.ChatUiHelper;
import com.hrl.chaui.util.FileUtils;
import com.hrl.chaui.util.MqttByAli;
import com.hrl.chaui.util.MqttService;
import com.hrl.chaui.util.PictureFileUtil;
import com.hrl.chaui.util.RTCHelper;
import com.hrl.chaui.util.value;
import com.hrl.chaui.widget.MediaManager;
import com.hrl.chaui.widget.RecordButton;
import com.hrl.chaui.widget.StateButton;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.entity.LocalMedia;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.hrl.chaui.util.Constant.*;

/**
 * 要从其他地方跳到该ChatActivity，需要满足以下两个条件
 * （1）SharedPreferences中有 key=“user_id”的整形
 * （2）Intent中包含有聊天对象的User对象，该对象中至少需要设置 id、name 这两个字段。
 */
public class ChatActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    @BindView(R.id.llContent)
    LinearLayout mLlContent;
    @BindView(R.id.rv_chat_list)
    RecyclerView mRvChat;
    @BindView(R.id.et_content)
    EditText mEtContent;
    @BindView(R.id.bottom_layout)
    RelativeLayout mRlBottomLayout;//表情,添加底部布局
    @BindView(R.id.ivAdd)
    ImageView mIvAdd;
    @BindView(R.id.ivEmo)
    ImageView mIvEmo;
    @BindView(R.id.btn_send)
    StateButton mBtnSend;//发送按钮
    @BindView(R.id.ivAudio)
    ImageView mIvAudio;//录音图片
    @BindView(R.id.btnAudio)
    RecordButton mBtnAudio;//录音按钮
    @BindView(R.id.rlEmotion)
    LinearLayout mLlEmotion;//表情布局
    @BindView(R.id.llAdd)
    LinearLayout mLlAdd;//添加布局
    @BindView(R.id.swipe_chat)
    SwipeRefreshLayout mSwipeRefresh;//下拉刷新

    private ChatAdapter mAdapter;
    public static final int REQUEST_CODE_IMAGE = 0000;
    public static final int REQUEST_CODE_VEDIO = 1111;
    public static final int REQUEST_CODE_FILE = 2222;

    private String targetClientID =  null;
    private String userClientID = null;
    private User targetUser = null;


    private final String TAG = "chatTest";
    private MqttByAli mqtt = null;

    private MessageReceiver messageReceiver = null; // 接收message arrive 广播
    private MessageDaoImp messageDaoImp = MessageDaoImp.getInstance();

    // 和Service的连接。
    private MqttServiceConnection connection = null;

    // 对方不在线的Handler
    NoOnlineHandler noOnlineHandler = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
//        // 获取登录用户信息
        SharedPreferences sharedPreferences = getSharedPreferences("data", Context.MODE_PRIVATE);
        userClientID = "GID_test@@@" + sharedPreferences.getInt("user_id", -1);
        Log.e(TAG, "chatActivity onCreate()" +  "  userClientID:" + userClientID);


        // 权限请求
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.INTERNET);

        for (String s : permissions) { // 请求权限
            if (ContextCompat.checkSelfPermission(this, s) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{s}, 1);
            }
        }


        // Service可能会被系统回收，同时使用startService 和 bindService 以防 Service被销毁
        Intent mqttServiceIntent = new Intent(this, MqttService.class);
        startService(mqttServiceIntent);
        connection = new MqttServiceConnection();
        // 绑定MqttService 获取 MqttByAli对象
        bindService(mqttServiceIntent, connection, Context.BIND_AUTO_CREATE);


        // 动态注册 MessageReceiver 广播接收器
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MqttService.MESSAGEARRIVEACTION);
        messageReceiver = new MessageReceiver();
        registerReceiver(messageReceiver, intentFilter);

        // 初始化对方不在线的Handler
        noOnlineHandler = new NoOnlineHandler(this);

        initContent();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode) {
            case 1: {
                if (grantResults.length!=0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, permissions[0] + " denied", Toast.LENGTH_SHORT).show();
                    this.finish();
                }
                break;
            }
        }
    }

    @Override
    public void onResume() {
        // 获取通信对方的信息
        Intent intent = getIntent();
        targetUser = (User) intent.getSerializableExtra("targetUser");
        Log.e(TAG, "ChatActivity onResume()" + "  targetUser:" + targetUser.getName());

        //  设置名称
        targetClientID = "GID_test@@@" + targetUser.getId();
        TextView textView =(TextView) findViewById(R.id.common_toolbar_title);
        textView.setText(targetUser.getName());

        // 获取通信记录并显示
        try {
            int uncheckedNums =  messageDaoImp.queryUncheckMessageNums(this, userClientID, targetClientID);
            uncheckedNums = uncheckedNums <= 10 ? 10 : uncheckedNums; // 最少10条
            List<Message> messageList =  messageDaoImp.queryMessage(this, userClientID, targetClientID, uncheckedNums);

            // 更新数据库消息状态为已查询
            String[] uuids = new String[messageList.size()];
            for (int i = 0; i < messageList.size(); i++) {
                uuids[i] = messageList.get(i).getUuid();
            }
            messageDaoImp.checkMessage(this, uuids);

            // 时间大的在后面（新到的消息在最后面）
            Collections.reverse(messageList);
            // 显示在显示屏上
            mAdapter.addData(messageList);
            // 位置滑到最新消息
            mRvChat.scrollToPosition(mAdapter.getItemCount()-1);

        } catch (IOException e) {
            Log.e(TAG,"messageDao query 出错了！！");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            Log.e(TAG,"messageDao query 出错了！！");
            e.printStackTrace();
        }

        super.onResume();
    }


    @Override
    protected void onDestroy() {
        Log.e(TAG, "ChatActivity Destory!!");

        //注销 动态注册的Receiver
        unregisterReceiver(messageReceiver);

        // 解绑
        unbindService(connection);
        super.onDestroy();
    }

    private ImageView ivAudio;

    protected void initContent() {
        ButterKnife.bind(this);
        mAdapter = new ChatAdapter(this, new ArrayList<Message>(), "ChatActivity");
        LinearLayoutManager mLinearLayout = new LinearLayoutManager(this);
        mRvChat.setLayoutManager(mLinearLayout);
        mRvChat.setAdapter(mAdapter);
        mSwipeRefresh.setOnRefreshListener(this);
        initChatUi();
    }


    @SuppressLint("ClickableViewAccessibility")
    private void initChatUi() {
        //mBtnAudio
        final ChatUiHelper mUiHelper = ChatUiHelper.with(this);
        mUiHelper.bindContentLayout(mLlContent)
                .bindttToSendButton(mBtnSend)
                .bindEditText(mEtContent)
                .bindBottomLayout(mRlBottomLayout)
                .bindEmojiLayout(mLlEmotion)
                .bindAddLayout(mLlAdd)
                .bindToAddButton(mIvAdd)
                .bindToEmojiButton(mIvEmo)
                .bindAudioBtn(mBtnAudio)
                .bindAudioIv(mIvAudio)
                .bindEmojiData();

        //底部布局弹出,聊天列表上滑
        mRvChat.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    mRvChat.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mAdapter.getItemCount() > 0) {
                                mRvChat.smoothScrollToPosition(mAdapter.getItemCount() - 1);
                            }
                        }
                    });
                }
            }
        });

        //点击空白区域关闭键盘
        mRvChat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mUiHelper.hideBottomLayout(false);
                mUiHelper.hideSoftInput();
                mEtContent.clearFocus();
                mIvEmo.setImageResource(R.mipmap.ic_emoji);
                return false;
            }
        });

        // 录音结束，发送录音文件
        ((RecordButton) mBtnAudio).setOnFinishedRecordListener(new RecordButton.OnFinishedRecordListener() {
            @Override
            public void onFinishedRecord(String audioPath, int time) {
                LogUtil.d("录音结束回调");
                File file = new File(audioPath);
                if (file.exists()) {
                    sendAudioMessage(audioPath, time);
                }
            }
        });



        // 聊天框消息点击事件
        mAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() { // RecyclerView Item 点击事件
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) { // 播放音频、视频

                // 获取点击的Item
                Message msg = mAdapter.getItem(position);
                // 跳转去 ItemShowActivity
                Intent intent = new Intent(ChatActivity.this, ItemShowActivity.class);
                switch(view.getId()) {
                    case R.id.rlAudio: {
                        // Audio Item的点击事件
                        // 播放音频
                        final boolean isSend = mAdapter.getItem(position).getSenderId().equals(userClientID);
                        if (ivAudio != null) {
                            if (isSend) {
                                ivAudio.setBackgroundResource(R.mipmap.audio_animation_list_right_3);
                            } else {
                                ivAudio.setBackgroundResource(R.mipmap.audio_animation_list_left_3);
                            }
                            ivAudio = null;
                            MediaManager.reset();
                        } else {
                            ivAudio = view.findViewById(R.id.ivAudio);
                            MediaManager.reset();
                            if (isSend) {
                                ivAudio.setBackgroundResource(R.drawable.audio_animation_right_list);
                            } else {
                                ivAudio.setBackgroundResource(R.drawable.audio_animation_left_list);
                            }
                            AnimationDrawable drawable = (AnimationDrawable) ivAudio.getBackground();
                            drawable.start();
                            MediaManager.playSound(ChatActivity.this, ((AudioMsgBody) mAdapter.getData().get(position).getBody()).getLocalPath(), new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    if (isSend) {
                                        ivAudio.setBackgroundResource(R.mipmap.audio_animation_list_right_3);
                                    } else {
                                        ivAudio.setBackgroundResource(R.mipmap.audio_animation_list_left_3);
                                    }

                                    MediaManager.release();
                                }
                            });
                        }
                        break;
                    }
                    case R.id.chat_item_content_text: {
                        // 点击文本item ，全屏显示该文本
                        TextMsgBody textMsgBody = (TextMsgBody) msg.getBody();
                        intent.putExtra("msgType", "text");
                        intent.putExtra("textMsg", textMsgBody.getMessage());
                        startActivity(intent);
                        break;
                    }
                    case R.id.bivPic: {
                        // 点击图片item，全屏显示
                        ImageMsgBody imageMsgBody = (ImageMsgBody) msg.getBody();
                        intent.putExtra("msgType", "image");
                        intent.putExtra("imagePath", imageMsgBody.getThumbUrl());
                        startActivity(intent);
                        break;
                    }
                    case R.id.ivPlay: {
                        // 视频的点击事件
                        VideoMsgBody videoMsgBody = (VideoMsgBody) msg.getBody();
                        String videoPath = videoMsgBody.getLocalPath();
                        intent.putExtra("msgType", "video");
                        intent.putExtra("videoPath", videoPath);
                        startActivity(intent);
                        break;
                    }
                    case R.id.rc_msg_iv_file_type_image: {
                        // 文件的点击事件
                        FileMsgBody fileMsgBody = (FileMsgBody) msg.getBody();
                        String filePath = fileMsgBody.getLocalPath();
                        intent.putExtra("msgType", "file");
                        intent.putExtra("filePath", filePath);
                        startActivity(intent);
                        break;
                    }
                }
            }
        });


        // title bar 的返回箭头
        ImageView titleBack = (ImageView) findViewById(R.id.title_back);
        titleBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatActivity.this.finish();
            }
        });

    }


    // 聊天框上滑查看历史记录
    @Override
    public void onRefresh() {
        //下拉刷新模拟获取历史消息

        // 获取最上面的item
        if (mAdapter.getItemCount() > 0) {
            // 聊天框内有元素
            Message topItem = mAdapter.getItem(0);
            long sendTime = topItem.getSentTime();
            try {
                List<Message> historyMessage = messageDaoImp.queryMessage(this, userClientID, targetClientID, 10, sendTime);
                if (historyMessage.size() == 0) {
                    Toast.makeText(this, "没有历史记录", Toast.LENGTH_SHORT).show();
                } else {
                    Collections.reverse(historyMessage);
                    // 消息插入到最前面
                    mAdapter.addData(0, historyMessage);
                    mRvChat.scrollToPosition(historyMessage.size()-1);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        } else {
            // 聊天框内没有元素
            try {
                List<Message> historyMessage = messageDaoImp.queryMessage(this, userClientID, targetClientID, 10);
                if (historyMessage.size() == 0) {
                    Toast.makeText(this, "没有历史记录", Toast.LENGTH_SHORT).show();
                } else {
                    Collections.reverse(historyMessage);
                    // 消息插入到最前面
                    mAdapter.addData(0, historyMessage);
                    mRvChat.scrollToPosition(historyMessage.size()-1);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        mSwipeRefresh.setRefreshing(false);
    }


    // 点击 ”相册“、”图片“、”视频“、”文件“、”位置“、”通话“ 后触发的点击事件。
    @OnClick({R.id.btn_send, R.id.rlPhoto, R.id.rlVideo, R.id.rlLocation, R.id.rlFile, R.id.rlPhone, R.id.rlVideoCall})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_send:
                sendTextMsg(mEtContent.getText().toString());
                mEtContent.setText("");
                break;
            case R.id.rlPhoto:
                PictureFileUtil.openGalleryPic(ChatActivity.this, REQUEST_CODE_IMAGE);
                break;
            case R.id.rlVideo:
                PictureFileUtil.openGalleryAudio(ChatActivity.this, REQUEST_CODE_VEDIO);
                break;
            case R.id.rlFile:
                PictureFileUtil.openFile(ChatActivity.this, REQUEST_CODE_FILE);
                break;
            case R.id.rlLocation:
                break;
            case R.id.rlPhone: {
                // 语音通话
                Intent voiceCallIntent = new Intent(this, AliRtcChatActivity.class);
                String channelID = RTCHelper.getChannelID(userClientID, targetClientID);
                AliUserInfoResponse.AliUserInfo aliUserInfo = RTCHelper.getAliUserInfo(channelID, userClientID);
                voiceCallIntent.putExtra("channel", channelID);
                voiceCallIntent.putExtra("rtcAuthInfo", aliUserInfo);
                voiceCallIntent.putExtra("user2Name", targetUser.getName());

                new Thread(()->{

                    try {
                        boolean isOnline = MqttByAli.checkIsOnline(targetClientID);
                        if (isOnline) {
                            mqtt.sendP2PVoiceCallRequest(targetClientID);
                            startActivity(voiceCallIntent);
                        } else {
                            android.os.Message msg =  new android.os.Message();
                            msg.what = NOTONLINE;
                            noOnlineHandler.sendMessage(msg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        android.os.Message msg =  new android.os.Message();
                        msg.what = CHECKONLINEERR;
                        noOnlineHandler.sendMessage(msg);
                    }

                }).start();

                break;
            }
            case R.id.rlVideoCall: {
                Intent videoCallIntent = new Intent(this, VideoCallActivity.class);
                String channelID = RTCHelper.getNumsChannelID(userClientID, targetClientID);
                SharedPreferences sharedPreferences = getSharedPreferences("data", Context.MODE_PRIVATE);
                String userName = sharedPreferences.getString("user_name", "unknown");
                RTCAuthInfo info = RTCHelper.getVideoCallRTCAuthInfo(channelID, userClientID);
                videoCallIntent.putExtra("channel", channelID);
                videoCallIntent.putExtra("username", userName);
                videoCallIntent.putExtra("rtcAuthInfo", info);

                new Thread(()->{
                    try {
                        boolean isOnline = MqttByAli.checkIsOnline(targetClientID);
                        if (isOnline) {
                            mqtt.sendP2PVideoCallRequest(targetClientID);
                            startActivity(videoCallIntent);
                        } else {
                            android.os.Message msg =  new android.os.Message();
                            msg.what = NOTONLINE;
                            noOnlineHandler.sendMessage(msg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        android.os.Message msg =  new android.os.Message();
                        msg.what = CHECKONLINEERR;
                        noOnlineHandler.sendMessage(msg);
                    }
                }).start();
                break;
            }
        }
    }


    // 接收onViewClicked中的PictureFileUtil方法回调。(即获取图片、视频、文件选择Activity的选择结果)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_FILE:
                    String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
                    LogUtil.d("获取到的文件路径:" + filePath);
                    sendFileMessage(userClientID, targetClientID, filePath);
                    break;
                case REQUEST_CODE_IMAGE:
                    // 图片选择结果回调
                    List<LocalMedia> selectListPic = PictureSelector.obtainMultipleResult(data);
                    for (LocalMedia media : selectListPic) {
                        LogUtil.d("获取图片路径成功:" + media.getPath());
                        sendImageMessage(media);
                    }
                    break;
                case REQUEST_CODE_VEDIO:
                    // 视频选择结果回调
                    List<LocalMedia> selectListVideo = PictureSelector.obtainMultipleResult(data);
                    for (LocalMedia media : selectListVideo) {
                        LogUtil.d("获取视频路径成功:" + media.getPath());
                        sendVedioMessage(media);
                    }
                    break;
            }
        }
    }


    //文本消息
    private void sendTextMsg(String hello)  {
        final Message mMessgae = getBaseSendMessage(MsgType.TEXT);
        TextMsgBody mTextMsgBody = new TextMsgBody();
        mTextMsgBody.setMessage(hello);
        mMessgae.setBody(mTextMsgBody);
        //开始发送
        mAdapter.addData(mMessgae);
        //更新在聊天框内
        updateMsg(mMessgae);

        // 将message存储数据库
        try {
            messageDaoImp.insertMessage(this, mMessgae);
        } catch (IOException e) {
            Log.e(TAG, "text message insert err, message:" + mMessgae);
            e.printStackTrace();
        }


        // 通过mqtt发送
        Log.e(TAG, "sendTextMsg:" + hello + " targerClientID:" + targetClientID);
        mqtt.sendTextP2P(hello, targetClientID);

    }

    //图片消息
    private void sendImageMessage(final LocalMedia media) {
        final Message mMessgae = getBaseSendMessage(MsgType.IMAGE);
        ImageMsgBody mImageMsgBody = new ImageMsgBody();
        mImageMsgBody.setThumbUrl(media.getCompressPath()); // 显示压缩图
        mImageMsgBody.setLocalPath(media.getPath()); // 本地图片
        mMessgae.setBody(mImageMsgBody);
        //开始发送
        mAdapter.addData(mMessgae);
        //显示在聊天框中
        updateMsg(mMessgae);

        // 将message存储数据库
        try {
            messageDaoImp.insertMessage(this, mMessgae);
        } catch (IOException e) {
            Log.e(TAG, "image message insert err, message:" + mMessgae);
            e.printStackTrace();
        }

        // 通过 mqtt 发送图片文件
        File file = new File(media.getPath());
        mqtt.sendImageP2P(file, targetClientID);

    }

    //视频消息
    private void sendVedioMessage(final LocalMedia media) {
        final Message mMessgae = getBaseSendMessage(MsgType.VIDEO);
        //生成缩略图路径
        String videoPath = media.getPath();
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(videoPath);
        Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime();
        String imgname = System.currentTimeMillis() + ".jpg";
//        String urlpath = Environment.getExternalStorageDirectory() + "/" + imgname;
        String urlpath = value.imgLocalPath + "/" + imgname;

        File f = new File(urlpath);
        try {
            if (f.exists()) {
                f.delete();
            }
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            LogUtil.d("视频缩略图路径获取失败：" + e.toString());
            e.printStackTrace();
        }
        VideoMsgBody mImageMsgBody = new VideoMsgBody();
        mImageMsgBody.setExtra(urlpath);
        mImageMsgBody.setLocalPath(videoPath);
        mMessgae.setBody(mImageMsgBody);
        //开始发送
        mAdapter.addData(mMessgae);

        //更新item
        updateMsg(mMessgae);

        // 将message存储数据库
        try {
            messageDaoImp.insertMessage(this, mMessgae);
        } catch (IOException e) {
            Log.e(TAG, "text message insert err, message:" + mMessgae);
            e.printStackTrace();
        }

        // mqtt 发送
        File file = new File(videoPath);
        mqtt.sendVideoP2P(file, targetClientID);

    }

    //文件消息
    private void sendFileMessage(String from, String to, final String path) {
        final Message mMessgae = getBaseSendMessage(MsgType.FILE);
        FileMsgBody mFileMsgBody = new FileMsgBody();
        mFileMsgBody.setLocalPath(path);
        mFileMsgBody.setDisplayName(FileUtils.getFileName(path));
        mFileMsgBody.setSize(FileUtils.getFileLength(path));
        mMessgae.setBody(mFileMsgBody);
        //开始发送
        mAdapter.addData(mMessgae);
        // 发送
        updateMsg(mMessgae);

        // 将message存储数据库
        try {
            messageDaoImp.insertMessage(this, mMessgae);
        } catch (IOException e) {
            Log.e(TAG, "text message insert err, message:" + mMessgae);
            e.printStackTrace();
        }

        File file = new File(path);
        mqtt.sendFileP2P(file, targetClientID);

    }

    //语音消息
    private void sendAudioMessage(final String path, int time) {
        final Message mMessgae = getBaseSendMessage(MsgType.AUDIO);
        AudioMsgBody mFileMsgBody = new AudioMsgBody();
        mFileMsgBody.setLocalPath(path);
        mFileMsgBody.setDuration(time);
        mMessgae.setBody(mFileMsgBody);
        //开始发送
        mAdapter.addData(mMessgae);
        //模拟两秒后发送成功
        updateMsg(mMessgae);

        // 将message存储数据库
        try {
            messageDaoImp.insertMessage(this, mMessgae);
        } catch (IOException e) {
            Log.e(TAG, "text message insert err, message:" + mMessgae);
            e.printStackTrace();
        }

        File file = new File(path);
        mqtt.sendAudioP2P(file, targetClientID, time);
    }


    // 设置发送的Message的基本信息
    private Message getBaseSendMessage(MsgType msgType) {
        Message mMessgae = new Message();
        mMessgae.setUuid(UUID.randomUUID() + "");
        mMessgae.setSenderId(userClientID);
        mMessgae.setTargetId(targetClientID);
        mMessgae.setSentTime(System.currentTimeMillis());
        mMessgae.setSentStatus(MsgSendStatus.SENDING);
        mMessgae.setMsgType(msgType);
        mMessgae.setCheck(true);
        mMessgae.setMsgId(null);
        mMessgae.setGroup(false);
        return mMessgae;
    }


    private Message getBaseReceiveMessage(MsgType msgType) {
        Message mMessgae = new Message();
        mMessgae.setUuid(UUID.randomUUID() + "");
        mMessgae.setSenderId(targetClientID);
        mMessgae.setTargetId(userClientID);
        mMessgae.setSentTime(System.currentTimeMillis());
        mMessgae.setSentStatus(MsgSendStatus.SENDING);
        mMessgae.setMsgType(msgType);
        return mMessgae;
    }


    private void updateMsg(final Message mMessgae) { // 更新子条目
        mRvChat.scrollToPosition(mAdapter.getItemCount() - 1);

        int position = 0;
        mMessgae.setSentStatus(MsgSendStatus.SENT);
        //更新单个子条目
        for (int i = 0; i < mAdapter.getData().size(); i++) {
            Message mAdapterMessage = mAdapter.getData().get(i);
            if (mMessgae.getUuid().equals(mAdapterMessage.getUuid())) {
                position = i;
            }
        }
        mAdapter.notifyItemChanged(position);
    }


    private class MqttServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MqttService.LocalBinder localBinder = (MqttService.LocalBinder) service;
            mqtt = localBinder.getService().getMqtt();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }


    private class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 接收mqttService接收的消息
            Message message = (Message) intent.getSerializableExtra("message");

            // 如果该消息的通信双方 和 该私聊通信双方吻合，就显示出来。
            if(message.getSenderId().equals(targetClientID) || message.getTargetId().equals(targetClientID)) {
                mAdapter.addData(message);
                mRvChat.scrollToPosition(mAdapter.getItemCount()-1); // 滚到最新消息那
                // 把数据库中该消息状态改为已经查看
                messageDaoImp = MessageDaoImp.getInstance();
                messageDaoImp.checkMessage(ChatActivity.this, message.getUuid());
            }
        }
    }


    public String getTargetClientID() {
        return targetClientID;
    }

    public String getUserClientID() {
        return userClientID;
    }

    private static class NoOnlineHandler extends Handler {
        private final WeakReference<ChatActivity> mTarget;

        private NoOnlineHandler(ChatActivity activity) {
            mTarget = new WeakReference<ChatActivity>(activity);
        }


        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case NOTONLINE:
                    Toast.makeText(mTarget.get(), "用户不在线", Toast.LENGTH_SHORT).show();
                    break;
                case CHECKONLINEERR:
                    Toast.makeText(mTarget.get(), "检测用户是否在线时出错",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

}
