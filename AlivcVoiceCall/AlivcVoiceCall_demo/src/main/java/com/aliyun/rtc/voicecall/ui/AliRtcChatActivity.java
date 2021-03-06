package com.aliyun.rtc.voicecall.ui;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alivc.rtc.AliRtcAuthInfo;
import com.alivc.rtc.AliRtcEngine;
import com.alivc.rtc.AliRtcEngineEventListener;
import com.alivc.rtc.AliRtcEngineImpl;
import com.alivc.rtc.AliRtcEngineNotify;
import com.alivc.rtc.AliRtcRemoteUserInfo;
import com.alivc.rtc.device.utils.StringUtils;
import com.aliyun.rtc.voicecall.R;
import com.aliyun.rtc.voicecall.adapter.BgAdapter;
import com.aliyun.rtc.voicecall.adapter.BgmAdapter;
import com.aliyun.rtc.voicecall.bean.AliUserInfoResponse;
import com.aliyun.rtc.voicecall.bean.ChannelStartTimeResponse;
import com.aliyun.rtc.voicecall.constant.Constant;
import com.aliyun.rtc.voicecall.network.OkHttpCientManager;
import com.aliyun.rtc.voicecall.network.OkhttpClient;
import com.aliyun.rtc.voicecall.utils.BitmapUtil;
import com.aliyun.rtc.voicecall.utils.FileUtil;
import com.aliyun.rtc.voicecall.utils.PermissionUtil;
import com.aliyun.rtc.voicecall.utils.TimeConverterUtil;
import com.aliyun.rtc.voicecall.utils.ToastUtils;
import com.aliyun.rtc.voicecall.utils.UIHandlerUtil;
import com.aliyun.rtc.voicecall.view.AlivcTipDialog;
import com.aliyun.rtc.voicecall.view.TitleBar;
import com.aliyun.svideo.common.utils.NetWatchdogUtils;
import com.aliyun.svideo.common.utils.ScreenUtils;
import com.aliyun.svideo.common.utils.ThreadUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.webrtc.alirtcInterface.AliParticipantInfo;
import org.webrtc.alirtcInterface.AliStatusInfo;
import org.webrtc.alirtcInterface.AliSubscriberInfo;
import org.webrtc.alirtcInterface.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;


public class AliRtcChatActivity extends AppCompatActivity implements TitleBar.MenuBtnListener, View.OnClickListener, BgmAdapter.OnPlayBtnClickListener, BgAdapter.OnBgClickListener, TitleBar.BackBtnListener, NetWatchdogUtils.NetChangeListener, PermissionUtil.PermissionGrantedListener {
    private static final String TAG = AliRtcChatActivity.class.getSimpleName();
    private AliRtcEngineImpl mEngine;
    private boolean mEnableSilent;
    /**
     * ????????????
     */
    private ImageView mIvUser1;
    /**
     * ??????2?????????
     */
    private ImageView mIvUser2;
    /**
     * ????????????
     */
    private TextView mTvSilent;
    /**
     * ????????????
     */
    private TextView mTvRingOff;
    /**
     * ????????????????????????????????????
     */
    private TextView mTvLoudSpeaker;
    private String mChannelId;
    private AliUserInfoResponse.AliUserInfo mRtcAuthInfo;
    private DrawerLayout mDrawerLayout;
    private List<File> mFiles = new ArrayList<>();
    private List<Bitmap> mBgBitmaps = new ArrayList<>();
    private BgmAdapter mBgmAdapter;
    private BgAdapter mBgAdapter;
    private TextView mIvUser2Name;
    private static final int TITLE_BAR_EMNU_SETTING_ID = 1111;
    private static final int TITLE_BAR_EMNU_BACK_ID = 11111;
    private LinearLayout mLeftDrawLayout;
    private Pair<File, Boolean> mSelectedBgmData;
    private TextView mTvExperienceTime;
    private TextView mTvWaitting;
    private TextView mTvHintTimeout;
    private ImageButton mIbHintLineClose;
    private LinearLayout mLlHintLine;
    private TimeCountRunnable mTimeCountRunnable;
    private NetWatchdogUtils mNetWatchdogUtils;
    private boolean showUser1etBad, showUser2etBad = false;
    private int currBgm;
    private boolean noAudioPermission = false;
    private String user2Uid;
    private String user2Name;// ??????2?????????????????????Intent??????
    private long mUser2LoginTime;
    private TitleBar mTitleBar;
    private long channelStartTime;
    private CallCancelReceiver callCancelReceiver;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alivc_voicecall_activity_rtc_chat);
        //getData
        getDataForIntent();
        //???????????????Receiver
        initReceiver();
        //?????????view
        initView();
        //?????????view??????
        initEvent();
        //?????????rtc??????
        initAlivcRtcEngine();
        initBgm();
        checkUserOnline();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)   {
            int statusBarHeight = getStatusBarHeight();
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mTitleBar.getLayoutParams();
            layoutParams.setMargins(0, statusBarHeight, 0, 0);
            mTitleBar.setLayoutParams(layoutParams);
        }
    }

    private void initReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("CALLCANCEL");
        callCancelReceiver = new CallCancelReceiver();
        registerReceiver(callCancelReceiver, intentFilter);
    }



    /**
     * ?????????????????????
     *
     * @return ???????????????
     */
    public int getStatusBarHeight() {
        try {
            // ?????????????????????
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            return getResources().getDimensionPixelSize(resourceId);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNetWatchdogUtils == null) {
            //??????????????????
            mNetWatchdogUtils = new NetWatchdogUtils(AliRtcChatActivity.this);
            mNetWatchdogUtils.setNetChangeListener(this);
            mNetWatchdogUtils.startWatch();
        }
        channelStartTime = System.currentTimeMillis();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //????????????????????????
        PermissionUtil.requestPermissions(AliRtcChatActivity.this, new String[] {PermissionUtil.PERMISSION_RECORD_AUDIO}, PermissionUtil.PERMISSION_REQUEST_CODE, AliRtcChatActivity.this);
    }

    //????????????????????????????????????
    private void checkUserOnline() {
        if (mRtcAuthInfo != null) {
            boolean userOnline = mEngine.isUserOnline(mRtcAuthInfo.getUserid());
            if (userOnline) {
                //????????????????????????
                startPublish();
            } else {
                joinChannel(mChannelId);
            }
        }

    }

    /**
     * ??????bgm??????
     */
    private void initBgm() {
        ThreadUtils.runOnSubThread(new LoadBgmRunnable());
    }

    /**
     * ???????????????????????????
     */
    private void initBgmList(List<File> files) {
        mFiles.clear();
        mFiles.add(0, null);
        mFiles.addAll(files);
        UIHandlerUtil.getInstance().postRunnable(new Runnable() {
            @Override
            public void run() {
                if (mBgmAdapter != null) {
                    mBgmAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    /**
     * ??????intent????????????????????????
     */
    private void getDataForIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            Bundle b = intent.getExtras();
            if (b != null) {
                //?????????
                mChannelId = b.getString("channel");
                //????????????
                mRtcAuthInfo = (AliUserInfoResponse.AliUserInfo) b.getSerializable("rtcAuthInfo");
                //????????????
                user2Name = b.getString("user2Name");
            }
        }

        // ???????????????????????????????????????Activity???????????????ChannelID???mRTCAuthInfo????????????
//                mChannelId = "GID_test1GID_test2";
//                mRtcAuthInfo = new AliUserInfoResponse.AliUserInfo();
//                mRtcAuthInfo.setAppid("e71sevhr");
//                mRtcAuthInfo.setTimestamp((int)(System.currentTimeMillis()/1000+84000));
//                mRtcAuthInfo.setNonce("AK-f815391a-796a-4d43-9b40-c5b5dfb7d14c");
//                mRtcAuthInfo.setUserid("GID_test1");
//                ArrayList<String> list = new ArrayList<>();
//                list.add("https://rgslb.rtc.aliyuncs.com");
//                mRtcAuthInfo.setGslb(list);
//                mRtcAuthInfo.setTurn(null);
//                String token = "";
//                try {
//                    token = createToken(mRtcAuthInfo.getAppid(),"b41a99a514d27ebbb9594b9f5a3aa475",mChannelId,mRtcAuthInfo.getUserid(),mRtcAuthInfo.getNonce(),(long)mRtcAuthInfo.getTimestamp());
//                } catch (NoSuchAlgorithmException e) {
//                    e.printStackTrace();
//                }
//                mRtcAuthInfo.setToken(token);
    }

    public static String createToken(
            String appId, String appKey, String channelId, String userId,
            String nonce, Long timestamp
    ) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(appId.getBytes());
        digest.update(appKey.getBytes());
        digest.update(channelId.getBytes());
        digest.update(userId.getBytes());
        digest.update(nonce.getBytes());
        digest.update(Long.toString(timestamp).getBytes());

        String token = DatatypeConverter.printHexBinary(digest.digest()).toLowerCase();
        return token;
    }


    private void initEvent() {
        mTvSilent.setOnClickListener(this);
        mTvRingOff.setOnClickListener(this);
        mTvLoudSpeaker.setOnClickListener(this);
    }

    /**
     * ?????????rtc??????
     */
    private void initAlivcRtcEngine() {
        mEngine = AliRtcEngine.getInstance(getApplicationContext());
        mEngine.setAudioOnlyMode(true);
        mEngine.setRtcEngineNotify(mEngineNotify);
        mEngine.setRtcEngineEventListener(mEventListener);
        //?????????????????????
        boolean speakerOn = mEngine.isSpeakerOn();
        if (!speakerOn) {
            // true?????????????????????false???????????????
            //??????????????????????????????
            //????????????????????????
            UIHandlerUtil.getInstance().postRunnable(new EnableSpeakerPhoneRunnable(true));
        }
    }

    @SuppressLint("WrongConstant")
    private void initView() {
        mTitleBar = (TitleBar) findViewById(R.id.alivc_voicecall_title_bar);
        TitleBar leftDrawerLayoutTitleBar = (TitleBar) findViewById(R.id.alivc_voicecall_left_drawlayout_title_bar);
        mIvUser1 = (ImageView) findViewById(R.id.alivc_voicecall_iv_user1);
        mIvUser2 = (ImageView) findViewById(R.id.alivc_voicecall_iv_user2);
        mIvUser2Name = (TextView) findViewById(R.id.alivc_voicecall_tv_user2_name);
        mTvSilent = (TextView) findViewById(R.id.alivc_voicecall_tv_silent);
        mTvRingOff = (TextView) findViewById(R.id.alivc_voicecall_tv_ring_off);
        mTvLoudSpeaker = (TextView) findViewById(R.id.alivc_voicecall_tv_loud_speaker);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.alivc_voicecall_btn_drawerlayout);
        RecyclerView mRcyBgm = (RecyclerView) findViewById(R.id.alivc_voicecall_rcy_bgm);
        RecyclerView mRcyBg = (RecyclerView) findViewById(R.id.alivc_voicecall_rcy_bg);
        mLeftDrawLayout = (LinearLayout) findViewById(R.id.alivc_voicecall_ll_left_drawlayout);
        mTvExperienceTime = (TextView) findViewById(R.id.alivc_voicecall_tv_experience_time);
        mTvWaitting = (TextView) findViewById(R.id.alivc_voicecall_tv_waiting);
        mTvHintTimeout = (TextView) findViewById(R.id.alivc_voicecall_tv_hint_timeout);
        mIbHintLineClose = (ImageButton) findViewById(R.id.alivc_voicecall_ibn_close);
        mLlHintLine = (LinearLayout) findViewById(R.id.alivc_voicecall_ll_hint_line);

        mIbHintLineClose.setOnClickListener(this);

        mTitleBar.setTitle(String.format(getString(R.string.alivc_voicecall_string_title_channel_id), user2Name));
        mTitleBar.setTitleTextColor(Color.WHITE);
        mTitleBar.setBackBtnListener(this);
        mTitleBar.setMenuIcon(R.mipmap.alivc_voicecall_icon_mine_setting);
        mTitleBar.setMenuBtnId(TITLE_BAR_EMNU_SETTING_ID);
        mTitleBar.setMenuBtnListener(this);

        leftDrawerLayoutTitleBar.setTitle(R.string.alivc_voicecall_string_text_setting);
        leftDrawerLayoutTitleBar.setMenuIcon(R.mipmap.alivc_voice_call_btn_close);
        leftDrawerLayoutTitleBar.setMenuBtnId(TITLE_BAR_EMNU_BACK_ID);
        leftDrawerLayoutTitleBar.setMenuBtnListener(this);

        /**
         * ?????????????????????
         */
        mTvLoudSpeaker.setSelected(true);

        refulshUser2View(null, false);
        mIvUser1.setImageBitmap(BitmapUtil.createCircleImage(AliRtcChatActivity.this, R.drawable.alivc_voice_call_icon_user1));
        LinearLayoutManager bgmManager = new LinearLayoutManager(AliRtcChatActivity.this);
        bgmManager.setOrientation(LinearLayout.VERTICAL);
        mRcyBgm.setLayoutManager(bgmManager);
        mBgmAdapter = new BgmAdapter(mFiles, AliRtcChatActivity.this);
        mBgmAdapter.setOnPlayBtnClickListener(this);
        mRcyBgm.setAdapter(mBgmAdapter);

        LinearLayoutManager bgManager = new LinearLayoutManager(AliRtcChatActivity.this);
        bgManager.setOrientation(LinearLayout.HORIZONTAL);
        mRcyBg.setLayoutManager(bgManager);
        mBgBitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.alivc_voice_call_bg_setting_1));
        mBgBitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.alivc_voice_call_bg_setting_2));
        mBgBitmaps.add(BitmapFactory.decodeResource(getResources(), R.drawable.alivc_voice_call_bg_setting_3));
        mBgAdapter = new BgAdapter(mBgBitmaps, AliRtcChatActivity.this);
        mBgAdapter.setOnBgClickListener(this);
        mRcyBg.setAdapter(mBgAdapter);
        /*
         * ??????????????????
         */
        mBgAdapter.setSelectedPosition(0);
        mDrawerLayout.setBackground(new BitmapDrawable(mBgBitmaps.get(0)));
        mLeftDrawLayout.setBackground(new BitmapDrawable(mBgBitmaps.get(0)));
        mBgmAdapter.setSelectedPosition(0);
        //???????????????????????????
        ViewGroup.LayoutParams layoutParams = mLeftDrawLayout.getLayoutParams();
        layoutParams.width = ScreenUtils.getWidth(this);
        mLeftDrawLayout.setLayoutParams(layoutParams);
        //??????????????????
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        //?????????????????????
        //        mTvExperienceTime.setVisibility(View.VISIBLE);
    }


    /**
     * ????????????????????????(???????????????????????????)
     */
    private AliRtcEngineEventListener mEventListener = new AliRtcEngineEventListener() {

        /**
         * ?????????????????????
         * @param i ?????????
         */
        @Override
        public void onJoinChannelResult(int i) {
            Log.i(TAG, "onJoinChannelResult: " + i);
            //????????????????????????
            startPublish();
            //????????????????????????
            getChannelStartTimeLocal();
        }

        /**
         * ?????????????????????
         * @param i ?????????
         */
        @Override
        public void onLeaveChannelResult(int i) {
            //??????1????????????
        }

        /**
         * ???????????????
         * @param i ?????????
         * @param s publishId
         */
        @Override
        public void onPublishResult(int i, String s) {

        }

        /**
         * ???????????????????????????
         * @param i ?????????
         */
        @Override
        public void onUnpublishResult(int i) {

        }

        /**
         * ?????????????????????
         * @param s userid
         * @param i ?????????
         * @param aliRtcVideoTrack ?????????track
         * @param aliRtcAudioTrack ?????????track
         */
        @Override
        public void onSubscribeResult(String s, int i, AliRtcEngine.AliRtcVideoTrack aliRtcVideoTrack,
                                      AliRtcEngine.AliRtcAudioTrack aliRtcAudioTrack) {
        }

        /**
         * ???????????????
         * @param i ?????????
         * @param s userid
         */
        @Override
        public void onUnsubscribeResult(int i, String s) {
        }

        /**
         * ???????????????????????????
         */
        @Override
        public void onNetworkQualityChanged(String s, AliRtcEngine.AliRtcNetworkQuality aliRtcNetworkQuality, AliRtcEngine.AliRtcNetworkQuality aliRtcNetworkQuality1) {
//            Log.i(TAG, "onNetworkQualityChanged: s --> " + s + ",aliRtcNetworkQuality --> " + aliRtcNetworkQuality + ",liRtcNetworkQuality1 --> " + aliRtcNetworkQuality1);
            if (aliRtcNetworkQuality1.getValue() >= AliRtcEngine.AliRtcNetworkQuality.Network_Bad.getValue() && aliRtcNetworkQuality1.getValue() <= AliRtcEngine.AliRtcNetworkQuality.Network_VeryBad.getValue()) {//???????????????
                if (StringUtils.equals(mRtcAuthInfo.getUserid(), s) && !showUser1etBad) {//??????
                    showUser1etBad = true;
                    UIHandlerUtil.getInstance().postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.showInCenter(AliRtcChatActivity.this, getString(R.string.alivc_voicecall_string_user1_network_not_better));
                        }
                    });
                } else if (!StringUtils.equals(mRtcAuthInfo.getUserid(), s) && !showUser2etBad) { //??????2
                    showUser2etBad = true;
                    UIHandlerUtil.getInstance().postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.showInCenter(AliRtcChatActivity.this, getString(R.string.alivc_voicecall_string_user2_network_not_better));
                        }
                    });
                }
            }
        }

        /**
         * ?????????????????????
         * @param i
         */
        @Override
        public void onOccurWarning(int i) {

        }

        /**
         * ?????????????????????
         * @param error ?????????
         */
        @Override
        public void onOccurError(final int error) {
            UIHandlerUtil.getInstance().postRunnable(new Runnable() {
                @Override
                public void run() {
                    showRtcErrorDialog(error);
                }
            });
        }

        /**
         * ????????????????????????
         */
        @Override
        public void onPerformanceLow() {

        }

        /**
         * ????????????????????????
         */
        @Override
        public void onPermormanceRecovery() {

        }

        /**
         * ????????????
         */
        @Override
        public void onConnectionLost() {
            Log.i(TAG, "onConnectionLost: ");
        }

        /**
         * ??????????????????
         */
        @Override
        public void onTryToReconnect() {
            Log.i(TAG, "onTryToReconnect: ");
        }

        /**
         * ???????????????
         */
        @Override
        public void onConnectionRecovery() {
            Log.i(TAG, "onConnectionRecovery: ");
        }


    };


    /**
     * ???rtc sdk???????????????
     *
     * @param error ?????????
     */
    private void showRtcErrorDialog(int error) {
        AlivcTipDialog alivcTipDialog = new AlivcTipDialog.Builder(AliRtcChatActivity.this)
        .setTitle(getString(R.string.alivc_voicecall_string_title_dialog_tip))
        .setDes(getString(R.string.alivc_voicecall_string_error_rtc_normal))
        .setButtonType(AlivcTipDialog.ONE_BUTTON)
        .setOneBtnStr(getString(R.string.alivc_voicecall_string_confrim_btn))
        .setOneButtonClickListener(new AlivcTipDialog.OneButtonClickListener() {

            @Override
            public void onClicked() {
                finish();
            }
        })
        .create();
        alivcTipDialog.setCanceledOnTouchOutside(false);
        alivcTipDialog.setCancelable(false);
        if (!alivcTipDialog.isShowing()) {
            alivcTipDialog.show();
        }
    }

    /**
     * SDK????????????(???????????????????????????)
     */
    private AliRtcEngineNotify mEngineNotify = new AliRtcEngineNotify() {
        /**
         * ???????????????????????????????????????OB???observer?????????
         * @param aliRtcEngine ??????????????????
         * @param s userid
         */
        @Override
        public void onRemoteUserUnPublish(AliRtcEngine aliRtcEngine, String s) {
            Log.i(TAG, "onRemoteUserUnPublish: ");
        }

        /**
         * ????????????????????????
         * @param s userid
         */
        @Override
        public void onRemoteUserOnLineNotify(String s) {
            Log.i(TAG, "onRemoteUserOnLineNotify: s --> " + s);
            user2Uid = s;
            mUser2LoginTime = System.currentTimeMillis();
            if (mEngine != null && !TextUtils.isEmpty(s)) {
                final AliRtcRemoteUserInfo userInfo = mEngine.getUserInfo(s);
                //??????user2??????
                UIHandlerUtil.getInstance().postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        refulshUser2View(userInfo, true);
                    }
                });
            }
        }

        /**
         * ????????????????????????
         * @param s userid
         */
        @Override
        public void onRemoteUserOffLineNotify(String s) {
            Log.i(TAG, "onRemoteUserOffLineNotify: s --> " + s);
            if (StringUtils.equals(user2Uid, s) && System.currentTimeMillis() - mUser2LoginTime > 1000) {
                user2Uid = "";
                if (mEngine != null && !TextUtils.isEmpty(s)) {
                    final AliRtcRemoteUserInfo userInfo = mEngine.getUserInfo(s);
                    //??????user2??????
                    UIHandlerUtil.getInstance().postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            refulshUser2View(userInfo, false);
                            ToastUtils.showInCenter(AliRtcChatActivity.this, getString(R.string.alivc_voicecall_string_user2_leave_channel));
                        }
                    });
                }
            }
        }

        /**
         * ??????????????????????????????????????????
         * @param s userid
         * @param aliRtcAudioTrack ?????????
         * @param aliRtcVideoTrack ?????????
         */
        @Override
        public void onRemoteTrackAvailableNotify(String s, AliRtcEngine.AliRtcAudioTrack aliRtcAudioTrack,
                AliRtcEngine.AliRtcVideoTrack aliRtcVideoTrack) {
        }

        /**
         * ???????????????????????????UI??????????????????
         * @param s userid
         * @param aliRtcAudioTrack ?????????
         * @param aliRtcVideoTrack ?????????
         */
        @Override
        public void onSubscribeChangedNotify(String s, AliRtcEngine.AliRtcAudioTrack aliRtcAudioTrack,
                                             AliRtcEngine.AliRtcVideoTrack aliRtcVideoTrack) {

        }

        /**
         * ????????????
         * @param aliSubscriberInfos ????????????????????????user??????
         * @param i ??????????????????
         */
        @Override
        public void onParticipantSubscribeNotify(AliSubscriberInfo[] aliSubscriberInfos, int i) {

        }

        /**
         * ?????????????????????
         * @param s callId
         * @param s1 stream_label
         * @param s2 track_label ??????video???audio
         * @param i ??????
         */
        @Override
        public void onFirstFramereceived(String s, String s1, String s2, int i) {
        }

        /**
         * ?????????????????????
         * @param s callId
         * @param s1 stream_label
         * @param s2 track_label ??????video???audio
         * @param i ??????
         */
        @Override
        public void onFirstPacketSent(String s, String s1, String s2, int i) {
        }

        /**
         *????????????????????????
         * @param callId ????????????callId
         * @param streamLabel ????????????????????????
         * @param trackLabel ???????????????????????????
         * @param timeCost ??????
         */
        @Override
        public void onFirstPacketReceived(String callId, String streamLabel, String trackLabel, int timeCost) {

        }

        /**
         * ????????????????????????
         * @param aliParticipantInfos ????????????????????????user??????
         * @param i ??????????????????
         */
        @Override
        public void onParticipantUnsubscribeNotify(AliParticipantInfo[] aliParticipantInfos, int i) {

        }

        /**
         * ?????????????????????????????????????????????
         * @param i
         */
        @Override
        public void onBye(int i) {
            Log.i(TAG, "onBye: " + i);
        }

        @Override
        public void onParticipantStatusNotify(AliStatusInfo[] aliStatusInfos, int i) {

        }

    };

    /**
     * ??????????????????
     *
     * @param id
     */
    @Override
    public void onMenuBtnClicked(int id) {
        switch (id) {
        case TITLE_BAR_EMNU_SETTING_ID:
            drawerRightFrame();
            break;
        case TITLE_BAR_EMNU_BACK_ID:
            closeRightFrame();
            break;
        default:
            break;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.alivc_voicecall_tv_silent) {
            togglePublishState();
        } else if (id == R.id.alivc_voicecall_tv_ring_off) {
            finish();
        } else if (id == R.id.alivc_voicecall_tv_loud_speaker) {
            toggleSpeakerOnState();
        } else if (id == R.id.alivc_voicecall_ibn_close) {
            mLlHintLine.setVisibility(View.GONE);
        }
    }

    /*????????????????????????????????????*/
    private void stopautoAndLeaveChannel() {
        //?????? ???????????? ?????????????????? ???????????? finish
        endPublish();
        stopBgm();
        leaveChannel();
        //????????????
        if (mTimeCountRunnable != null) {
            mTimeCountRunnable.setLoop(false);
        }
    }

    /**
     * ??????????????????
     */
    private void stopBgm() {
        if (mEngine != null) {
            mEngine.stopAudioAccompany();
        }
    }

    /**
     * ??????????????????????????????
     */
    private void toggleSpeakerOnState() {
        if (mEngine != null) {
            boolean isSpeakerOn = !mEngine.isSpeakerOn();
            //??????????????????????????????
            UIHandlerUtil.getInstance().postRunnable(new EnableSpeakerPhoneRunnable(isSpeakerOn));
            changeLoudSpeakerState(isSpeakerOn);
        }
    }

    /**
     * ????????????????????????
     */
    private void togglePublishState() {
        //?????? ????????????
        if (mEnableSilent) {//???????????????????????????????????????
            startPublish();
        } else {
            endPublish();
        }
        mEnableSilent = !mEnableSilent;
        changeSilentBtnState(mEnableSilent);
    }

    /**
     * ??????????????????????????????
     */
    private void changeLoudSpeakerState(boolean b)  {
        mTvLoudSpeaker.setSelected(b);
        ToastUtils.showInCenter(AliRtcChatActivity.this, getString(b ? R.string.alivc_voicecall_string_text_loud_speaker_enable : R.string.alivc_voicecall_string_text_loud_speaker_unenable));

    }

    /**
     * ??????????????????????????????
     */
    private void changeSilentBtnState(boolean enableSilent) {
        mTvSilent.setSelected(enableSilent);
        ToastUtils.showInCenter(AliRtcChatActivity.this, getString(enableSilent ? R.string.alivc_voicecall_string_text_silent_enable : R.string.alivc_voicecall_string_text_silent_unenable));
    }

    /**
     * ?????????????????????????????????
     *
     * @param file ????????????
     */
    @Override
    public void onPlayBtnClickListener(File file, boolean playing) {
        mSelectedBgmData = new Pair<>(file, playing);
        if (mEngine != null && file != null) {
            /*
             * onlyLocalPlay    ????????????????????????true?????????????????????false????????????????????????
             * replaceMic   ??????????????????????????????true?????????????????????????????????????????????false?????????????????????
             * */
            int index = mFiles.indexOf(file);
            if (index != currBgm) {
                mEngine.stopAudioEffect(currBgm);
                currBgm = index;
            }
            if (playing) {
                int i = mEngine.playAudioEffect(currBgm, file.getPath(), -1, false);
                Log.i(TAG, "OnPlayBtnClickListener: ??????????????????????????? --> " + i);
            } else {
                mEngine.stopAudioEffect(currBgm);
            }
        } else if (mEngine != null) {//???????????????
            mEngine.stopAudioEffect(currBgm);
        }
    }

    /**
     * ????????????????????????
     *
     * @param bitmap bgm
     */
    @Override
    public void onBgClickListener(Bitmap bitmap) {
        mDrawerLayout.setBackground(new BitmapDrawable(bitmap));
        //        mLeftDrawLayout.setBackground(new BitmapDrawable(bitmap));
    }

    /**
     * titlebar ????????????????????????
     */
    @Override
    public void onBackBtnClicked() {
        AlivcTipDialog alivcTipDialog = new AlivcTipDialog.Builder(AliRtcChatActivity.this)
        .setTitle(getString(R.string.alivc_voicecall_string_leave_channel))
        .setDes(getString(R.string.alivc_voicecall_string_hint_leave_channel))
        .setButtonType(AlivcTipDialog.TWO_BUTTON)
        .setCancelStr(getString(R.string.alivc_voicecall_string_confirm_leave_channel))
        .setConfirmStr(getString(R.string.alivc_voicecall_string_continue_to_experience))
        .setTwoButtonClickListener(new AlivcTipDialog.TwoButtonClickListener() {
            @Override
            public void onCancel() {
                finish();
            }

            @Override
            public void onConfirm() {
//                ToastUtils.showInCenter(AliRtcChatActivity.this, getString(R.string.alivc_voicecall_string_continue_to_experience));
            }
        })
        .create();
        alivcTipDialog.setCanceledOnTouchOutside(false);
        alivcTipDialog.setCancelable(false);
        alivcTipDialog.show();
    }

    /**
     * ??????????????????????????????
     */
    private class EnableSpeakerPhoneRunnable implements Runnable {
        boolean enableSpeakerPhone;

        public EnableSpeakerPhoneRunnable(boolean enableSpeakerPhone) {
            this.enableSpeakerPhone = enableSpeakerPhone;
        }

        @Override
        public void run() {
            if (mEngine != null) {
                mEngine.enableSpeakerphone(this.enableSpeakerPhone);
            }
        }
    }

    /**
     * ???????????????
     */
    public void startPublish() {
        if (mEngine == null) {
            return;
        }
        //?????????????????????
        //true??????????????????????????????false???????????????
        mEngine.configLocalAudioPublish(!noAudioPermission);
        //        //true??????????????????????????????false???????????????
        //        mEngine.configLocalCameraPublish(true);
        //        //true??????????????????????????????false???????????????
        //        mEngine.configLocalScreenPublish(true);
        //        //true????????????????????????????????????false???????????????
        //        mEngine.configLocalSimulcast(true, AliRtcEngine.AliRtcVideoTrack.AliRtcVideoTrackCamera);
        mEngine.publish();
    }

    /**
     * ?????????????????????
     */
    public void endPublish() {
        if (mEngine == null) {
            return;
        }
        mEngine.configLocalAudioPublish(false);
        //        mEngine.configLocalCameraPublish(false);
        //        mEngine.configLocalScreenPublish(false);
        //        mEngine.configLocalSimulcast(false, AliRtcEngine.AliRtcVideoTrack.AliRtcVideoTrackCamera);
        mEngine.publish();
    }

    /**
     * ????????????
     */
    public void leaveChannel() {
        //        float sdkCode = getSdkCode();
        //
        //        if (sdkCode > 1.7f){
        //            mEngine.leaveChannel();
        //        }
        if (mEngine != null) {
            mEngine.leaveChannel();
        }
    }

    /**
     * ??????????????????????????????user2 view
     *
     * @param userInfo ????????????
     */
    public void refulshUser2View(AliRtcRemoteUserInfo userInfo, boolean online) {
        if (mIvUser2 != null && mIvUser2Name != null) {
            mIvUser2Name.setVisibility(online ? View.INVISIBLE : View.VISIBLE);
            mIvUser2Name.setText(user2Name);
            mIvUser2.setImageBitmap(BitmapUtil.createCircleImage(AliRtcChatActivity.this,
                                    online ? R.drawable.alivc_voice_call_icon_user2 : R.drawable.alivc_voice_call_icon_user2_gray));
        }
//        mTvExperienceTime.setVisibility(online ? View.VISIBLE : View.GONE);
//        mTvWaitting.setVisibility(online ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopautoAndLeaveChannel();
        ToastUtils.cancel();
        UIHandlerUtil.getInstance().clearAllMsgAndRunnable();
        if (mNetWatchdogUtils != null) {
            mNetWatchdogUtils.stopWatch();
            mNetWatchdogUtils.setNetChangeListener(null);
            mNetWatchdogUtils = null;
        }
        unregisterReceiver(callCancelReceiver);
    }

    /**
     * ??????????????????
     */
    @SuppressLint("WrongConstant")
    public void drawerRightFrame() {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(Gravity.END, true);
            mBgmAdapter.notifyDataSetChanged();
            mBgAdapter.notifyDataSetChanged();
        }
    }

    /**
     * ??????????????????
     */
    @SuppressLint("WrongConstant")
    private void closeRightFrame() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(Gravity.END, true);
            //????????????????????????
            if (mSelectedBgmData != null && mSelectedBgmData.first != null && mEngine != null) {
                //????????????????????????
                mEngine.stopAudioAccompany();
                mEngine.stopAudioEffect(currBgm);
                //???????????????????????????
                mEngine.startAudioAccompany(mSelectedBgmData.first.getPath(), false, false, -1);
            } else if (mSelectedBgmData != null && mSelectedBgmData.first == null) {
                //????????????????????????
                mEngine.stopAudioAccompany();
                mEngine.stopAudioEffect(currBgm);
            }
        }
    }

    private void joinChannel(String channel) {
        List<String> gslb = mRtcAuthInfo.getGslb();
        AliRtcAuthInfo userInfo = new AliRtcAuthInfo();
        userInfo.setConferenceId(channel);//??????ID
        userInfo.setAppid(mRtcAuthInfo.getAppid());/* ??????ID */
        userInfo.setNonce(mRtcAuthInfo.getNonce());/* ????????? */
        userInfo.setTimestamp(mRtcAuthInfo.getTimestamp());/* ?????????*/
        userInfo.setUserId(mRtcAuthInfo.getUserid());/* ??????ID */
        userInfo.setGslb(gslb.toArray(new String[0]));/* GSLB??????*/
        userInfo.setToken(mRtcAuthInfo.getToken());/*????????????Token*/
        mEngine.joinChannel(userInfo,  getString(R.string.alivc_voicecall_string_me));/* ?????????????????? */
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.END)) {
            mDrawerLayout.closeDrawer(Gravity.END);
        } else {
            onBackBtnClicked();
        }
    }

    /**
     * ??????????????????
     *
     * @param minute ???
     * @param second ???
     */
    public void reflushExperienceTimeView(final int minute, final int second) {
        UIHandlerUtil.getInstance().postRunnable(new Runnable() {
            @Override
            public void run() {
                if (mTvExperienceTime.getVisibility() != View.VISIBLE && !TextUtils.isEmpty(user2Uid)) {
                    mTvExperienceTime.setVisibility(View.VISIBLE);
                    mTvWaitting.setVisibility(View.INVISIBLE);
                } else if (TextUtils.isEmpty(user2Uid)) {
                    mTvExperienceTime.setVisibility(View.INVISIBLE);
                    mTvWaitting.setVisibility(View.VISIBLE);
                }
                mTvExperienceTime.setText(String.format(getString(R.string.alivc_voicecall_string_experience_time_format), minute < 10 ? "0" + minute : String.valueOf(minute), second < 10 ? "0" + second : String.valueOf(second)));
//                if (minute >= 9) {
//                    mTvExperienceTime.setTextColor(Color.RED);
//                }
            }
        });

    }

    private void getChannelStartTimeByNet() {
        String url = Constant.getChannelStartTime();
        Map<String, String> params = createChannelStartTimeParams();
        OkHttpCientManager.getInstance().doGet(url, params, new OkhttpClient.HttpCallBack() {
            @Override
            public void onSuccess(String result) {
                Log.i(TAG, "onSuccess: " + result);
                try {
                    ChannelStartTimeResponse channelStartTimeResponse = new Gson().fromJson(result, ChannelStartTimeResponse.class);
                    if (channelStartTimeResponse != null && channelStartTimeResponse.getData() != null) {
                        long startTimes = TimeConverterUtil.utc2LocalTime(channelStartTimeResponse.getData().getChannelStartTimeUtc(), Constant.UTC_TIME_FORMAT_STRING);
                        //???????????????
                        if (mTimeCountRunnable == null) {
                            mTimeCountRunnable = new TimeCountRunnable(startTimes);
                            ThreadUtils.runOnSubThread(mTimeCountRunnable);
                        }
                    }
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFaild(String errorMsg) {
                Log.i(TAG, "onFaild: " + errorMsg);
            }
        });
    }

    private void getChannelStartTimeLocal() {
        if (mTimeCountRunnable == null) {
            mTimeCountRunnable = new TimeCountRunnable(channelStartTime);
            ThreadUtils.runOnSubThread(mTimeCountRunnable);
        }
    }


    private Map<String, String> createChannelStartTimeParams() {
        Map<String, String> params = new HashMap<>();
        params.put(Constant.NEW_TOKEN_PARAMS_KEY_CHANNELID, mChannelId);
        return params;
    }

    private class LoadBgmRunnable implements Runnable {
        @Override
        public void run() {
            String externalCacheDirPath = FileUtil.getExternalCacheDirPath(AliRtcChatActivity.this, Constant.CACHE_PATH);
            File[] files = FileUtil.getFiles(externalCacheDirPath);
            if (files == null || files.length == 0) {
                List<File> bgms = new ArrayList<>();
                try {
                    String[] list = getAssets().list(Constant.ASSETS_BGM_PATH);
                    if (list != null && list.length > 0) {
                        for (String s : list) {
                            InputStream open = getAssets().open(Constant.ASSETS_BGM_PATH + File.separator + s);
                            File bgmFile = FileUtil.writeFile(open, externalCacheDirPath + File.separator + s);
                            bgms.add(bgmFile);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                initBgmList(bgms);
            } else {
                initBgmList(Arrays.asList(files));
            }
        }
    }

    private class TimeCountRunnable implements Runnable {
        private int mMinute = 0;
        private int mSecond = 0;
        private boolean loop = true;
        private long mStartTime;

        public TimeCountRunnable(long channelStartTimeTs) {
            mStartTime = channelStartTimeTs;
        }

        public void setLoop(boolean loop) {
            this.loop = loop;
        }

        @Override
        public void run() {
            while (loop) {
                SystemClock.sleep(1000);
                long tempTime = System.currentTimeMillis() - mStartTime;
                Date date = new Date(tempTime);
                mSecond = date.getSeconds();
                mMinute = date.getMinutes();
//                if (mMinute >= Constant.EXPERIANCE_TIME_OUT_TIME && loop) {
//                    //????????????????????????
//                    loop = false;
//                    UIHandlerUtil.getInstance().postRunnable(new ShowTimeOutDialogRunnable());
//                }
                reflushExperienceTimeView(mMinute, mSecond);
            }
        }
    }

    /**
     * ????????????
     */
    @Override
    public void onWifiTo4G() {
        Log.i(TAG, "onWifiTo4G: ");
    }

    @Override
    public void on4GToWifi() {
        Log.i(TAG, "on4GToWifi: ");
    }

    @Override
    public void onReNetConnected(boolean isReconnect) {
    }

    @Override
    public void onNetUnConnected() {
        mTvHintTimeout.setText(R.string.alivc_voicecall_string_network_conn_error);
        mTvHintTimeout.setTextColor(getResources().getColorStateList(R.color.color_selector_red));
        mIbHintLineClose.setVisibility(View.GONE);
//        mLlHintLine.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPermissionGranted() {
        noAudioPermission = false;
        startPublish();
    }

    /**
     * ?????????
     */
    @Override
    public void onPermissionCancel() {
        noAudioPermission = true;
        startPublish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_CODE) {
            PermissionUtil.requestPermissionsResult(AliRtcChatActivity.this, PermissionUtil.PERMISSION_REQUEST_CODE, permissions, grantResults, AliRtcChatActivity.this);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private class CallCancelReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(AliRtcChatActivity.this, "??????????????????", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

}
