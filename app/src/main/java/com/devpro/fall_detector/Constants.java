package com.devpro.fall_detector;

import java.util.HashMap;

public class Constants {

    public static final String KEY_USER_ID = "userId";
    public static final String KEY_NAME = "name";
    public static final String KEY_USER = "user";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_FCM_TOKEN = "fcmToken";
    public static final String KEY_PREFERENCE_NAME = "fallDetection";
    public static final String REMOTE_MSG_AUTHORIZATION = "Authorization";
    public static final String REMOTE_MSG_CONTENT_TYPE = "Content-Type";
    public static final String REMOTE_MSG_DATA = "data";
    public static final String REMOTE_MSG_REGISTRATION_IDS = "registration_ids";

    public static HashMap<String, String> remoteMsgHeaders = null;

    public static HashMap<String, String> getRemoteMsgHeaders() {
        if (remoteMsgHeaders == null) {
            remoteMsgHeaders = new HashMap<>();
            remoteMsgHeaders.put(REMOTE_MSG_AUTHORIZATION, "key=AAAAACIrBEs:APA91bHgnw7W44NuB6D0LPjgoBX40DVOHXge8acT88g74G_YD63faOkkazdL6CbQNLtT1op7pG48CUc_dxzkeZsBMze0VVlFarKmAge9p8pqtPw32NpLm8A4Z964_7ivlAoFJ1fxu1d4");
            remoteMsgHeaders.put(REMOTE_MSG_CONTENT_TYPE, "application/json");
        }
        return remoteMsgHeaders;
    }
}
