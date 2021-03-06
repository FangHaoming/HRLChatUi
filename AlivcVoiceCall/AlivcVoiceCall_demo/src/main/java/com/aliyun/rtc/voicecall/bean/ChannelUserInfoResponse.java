package com.aliyun.rtc.voicecall.bean;

import java.util.List;

public class ChannelUserInfoResponse {


    /**
     * result : true
     * requestId :
     * message :
     * code : 200
     * data : {"requestId":"4C69BD0A-537F-48D3-BAA2-B35D4066BE93","timestamp":1587026895,"isChannelExist":true,"channelProfile":0,"commTotalNum":0,"interactiveUserNum":0,"liveUserNum":0,"userList":[],"interactiveUserList":[],"liveUserList":[]}
     */

    private String result;
    private String requestId;
    private String message;
    private String code;
    private DataBean data;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        /**
         * requestId : 4C69BD0A-537F-48D3-BAA2-B35D4066BE93
         * timestamp : 1587026895
         * isChannelExist : true
         * channelProfile : 0
         * commTotalNum : 0
         * interactiveUserNum : 0
         * liveUserNum : 0
         * userList : []
         * interactiveUserList : []
         * liveUserList : []
         */

        private String requestId;
        private int timestamp;
        private boolean isChannelExist;
        private int channelProfile;
        private int commTotalNum;
        private int interactiveUserNum;
        private int liveUserNum;
        private List<?> userList;
        private List<?> interactiveUserList;
        private List<?> liveUserList;

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public int getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(int timestamp) {
            this.timestamp = timestamp;
        }

        public boolean isIsChannelExist() {
            return isChannelExist;
        }

        public void setIsChannelExist(boolean isChannelExist) {
            this.isChannelExist = isChannelExist;
        }

        public int getChannelProfile() {
            return channelProfile;
        }

        public void setChannelProfile(int channelProfile) {
            this.channelProfile = channelProfile;
        }

        public int getCommTotalNum() {
            return commTotalNum;
        }

        public void setCommTotalNum(int commTotalNum) {
            this.commTotalNum = commTotalNum;
        }

        public int getInteractiveUserNum() {
            return interactiveUserNum;
        }

        public void setInteractiveUserNum(int interactiveUserNum) {
            this.interactiveUserNum = interactiveUserNum;
        }

        public int getLiveUserNum() {
            return liveUserNum;
        }

        public void setLiveUserNum(int liveUserNum) {
            this.liveUserNum = liveUserNum;
        }

        public List<?> getUserList() {
            return userList;
        }

        public void setUserList(List<?> userList) {
            this.userList = userList;
        }

        public List<?> getInteractiveUserList() {
            return interactiveUserList;
        }

        public void setInteractiveUserList(List<?> interactiveUserList) {
            this.interactiveUserList = interactiveUserList;
        }

        public List<?> getLiveUserList() {
            return liveUserList;
        }

        public void setLiveUserList(List<?> liveUserList) {
            this.liveUserList = liveUserList;
        }
    }
}
