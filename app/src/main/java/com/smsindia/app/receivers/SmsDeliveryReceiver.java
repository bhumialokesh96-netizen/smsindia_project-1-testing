package com.smsindia.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class SmsDeliveryReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String userId = intent.getStringExtra("userId");
        String docId = intent.getStringExtra("docId");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String status = "failed";

        switch (getResultCode()) {
            case android.app.Activity.RESULT_OK:
                status = "delivered";
                Toast.makeText(context, "✅ SMS Delivered", Toast.LENGTH_SHORT).show();

                // ✅ Update user balance after successful delivery
                if (userId != null) {
                    db.collection("users").document(userId)
                            .update("balance", FieldValue.increment(0.16));
                }
                if (docId != null) {
                    db.collection("sms_tasks").document(docId).delete();
                }
                break;

            default:
                Toast.makeText(context, "❌ SMS Delivery Failed", Toast.LENGTH_SHORT).show();
                break;
        }

        // 📜 Log all results
        if (userId != null && docId != null) {
            Map<String, Object> log = new HashMap<>();
            log.put("userId", userId);
            log.put("taskId", docId);
            log.put("timestamp", System.currentTimeMillis());
            log.put("status", status);
            db.collection("delivery_logs").add(log);
        }
    }
}