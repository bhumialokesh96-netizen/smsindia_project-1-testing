package com.smsindia.app.workers;

import android.content.Context;
import android.widget.Toast;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Handles starting/stopping the background SMS sending process
 */
public class BackgroundSmsSender {

    public static void start(Context context) {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(context, "⚠️ Please login first", Toast.LENGTH_SHORT).show();
                return;
            }

            Data inputData = new Data.Builder()
                    .putString("userId", user.getUid()) // ✅ pass Firebase user ID
                    .build();

            OneTimeWorkRequest workRequest =
                    new OneTimeWorkRequest.Builder(SmsWorker.class)
                            .setInputData(inputData)
                            .build();

            WorkManager.getInstance(context).enqueue(workRequest);
            Toast.makeText(context, "📤 SMS sending started...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "⚠️ Failed to start SMS task", Toast.LENGTH_SHORT).show();
        }
    }

    public static void stop(Context context) {
        try {
            WorkManager.getInstance(context).cancelAllWork();
            Toast.makeText(context, "⏸ SMS task stopped", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "⚠️ Failed to stop SMS task", Toast.LENGTH_SHORT).show();
        }
    }
}