package com.smsindia.app.workers;

import android.telephony.SmsManager;
import android.util.Log;

public class SendSmsTask {

    public static void sendSms(String phone, String message) {
        try {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phone, null, message, null, null);
            Log.d("SendSmsTask", "✅ Sent SMS to: " + phone);
        } catch (Exception e) {
            Log.e("SendSmsTask", "❌ Failed to send SMS to " + phone, e);
        }
    }
}