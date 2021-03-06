package com.hrl.chaui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.hrl.chaui.R;
import com.hrl.chaui.bean.Message;
import com.hrl.chaui.bean.MsgType;
import com.hrl.chaui.bean.TextMsgBody;
import com.hrl.chaui.bean.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static com.hrl.chaui.MyApplication.contactData;
import static com.hrl.chaui.MyApplication.groupData;

public class MessageAdapter extends BaseQuickAdapter<Message, BaseViewHolder> {

    private Context mContext;

    public MessageAdapter(Context context,@Nullable List<Message> data) {
        super(R.layout.item_message,data); // 设置item样式
        this.mContext=context;
    }

    @Override
    protected void convert(BaseViewHolder helper, Message item) {
        setContent(helper, item);
    }

    private void setContent(BaseViewHolder helper, Message item){

        // 通话对象
        SharedPreferences userId=mContext.getSharedPreferences("data_userID",MODE_PRIVATE); //用户ID清单
        SharedPreferences sp=mContext.getSharedPreferences("data_"+userId.getInt("user_id",-1),MODE_PRIVATE); //根据ID获取用户数据文件
        String userID = "GID_test@@@" + sp.getInt("user_id", -1);
        String otherID = null;
        String name = null;
        String imgUrl = null;

        if (!item.isGroup()) {
            otherID = item.getSenderId().equals(userID) ? item.getTargetId() : item.getSenderId();
            Log.e(TAG, "otherID:" + otherID);
            String tmp = otherID.split("@@@")[1];
            int otherIDInt = Integer.parseInt(tmp);
            for(User user : contactData)  {
                if (user.getUser_id() != null && user.getUser_id() == otherIDInt) {
                    name = user.getUser_name();
                    imgUrl = user.getUser_img();
                    break;
                }
            }
        } else {
            otherID = item.getTargetId();
            for (User group : groupData) {
                if (String.valueOf(group.getUser_id()).equals(otherID)) {
                    name = group.getUser_name();
                    if (group.getUser_img() != null) {
                        imgUrl = group.getUser_img();
                    }
                }
            }
        }

        if (name == null)
            name = "陌生人";
        helper.setText(R.id.message_item_title, name);


        // 设置消息头像
        if (imgUrl != null) {
            Glide.with(mContext)
                    .load(mContext.getResources().getString(R.string.app_prefix_img) + imgUrl)
                    .into((ImageView) helper.getView(R.id.message_item_photo));
        }


        // 发送时间
        long sendTime = item.getSentTime();
        String sendTimeText = getSendTimeText(sendTime);
        helper.setText(R.id.message_item_time_text, sendTimeText);

        // 显示在消息名称下方
        String msgInfo;
        MsgType msgType = item.getMsgType();
        switch (msgType) {
            case GROUP_INVITE:
            case TEXT:
                msgInfo = ((TextMsgBody)item.getBody()).getMessage();
                break;
            case  IMAGE:
                msgInfo = "[图片]";
                break;
            case AUDIO:
                msgInfo = "[语音]";
                break;
            case VIDEO:
                msgInfo = "[视频]";
                break;
            case FILE:
                msgInfo = "[文件]";
                break;
            default:
                msgInfo = "[消息]";
                break;
        }
        helper.setText(R.id.message_item_info, msgInfo);

        // 未查看的消息显示小红点
        if (item.isCheck() == false)
            helper.setVisible(R.id.message_item_redpoint, true);
        else
            helper.setVisible(R.id.message_item_redpoint, false);



    }



    private String getSendTimeText(long sendTime) {
        Date date = new Date(sendTime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String dateStr = dateFormat.format(date);
        String nowStr = dateFormat.format(System.currentTimeMillis());
        String[] dateInfo = dateStr.split("-");
        String[] nowInfo = nowStr.split("-");

        String year = dateInfo[0];
        String month = dateInfo[1];
        String day = dateInfo[2];
        String hour = dateInfo[3];
        String minute = dateInfo[4];
        String second = dateInfo[5];

        String nowYear = nowInfo[0];
        String nowMonth = nowInfo[1];
        String nowDay = nowInfo[2];
        String nowHour = nowInfo[3];
        String nowMinute = nowInfo[4];
        String nowSecond = nowInfo[5];

        int nowDayInt = Integer.valueOf(nowDay);
        int nowMonthInt = Integer.valueOf(nowMonth);
        int nowYearInt = Integer.valueOf(nowYear);

        String prevDay; // 前一天
        if (nowDayInt - 1 != 0) {
            prevDay = String.valueOf(nowDayInt - 1);
        } else {
            if (nowMonthInt == 1 || nowMonthInt == 2 || nowMonthInt == 4 || nowMonthInt == 6 || nowMonthInt == 8 || nowMonthInt == 9 || nowMonthInt == 11) {
                prevDay = String.valueOf(31);
            } else if (nowMonthInt != 3) {
                prevDay = String.valueOf(30);
            } else if ((nowYearInt % 4 == 0 && nowYearInt % 100 != 0) | nowYearInt % 400 == 0) {
                prevDay = String.valueOf(29);
            } else{
                prevDay = String.valueOf(28);
            }
        }

        if (!year.equals(nowYear)) {
            return year + "年" + Integer.valueOf(month) + "月" + Integer.valueOf(day) + "日";
        } else if (!month.equals(nowMonth) || (!day.equals(nowDay) && !day.equals(prevDay))) {
            return Integer.valueOf(month) + "月" + Integer.valueOf(day) + "日";
        } else if (day.equals(prevDay)){
            return "昨天";
        } else {
            return Integer.valueOf(hour) + ":" + minute;
        }

    }

}
