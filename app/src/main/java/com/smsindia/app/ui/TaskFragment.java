package com.smsindia.app.ui;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.smsindia.app.R;
import com.smsindia.app.workers.SmsWorker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskFragment extends Fragment {

    private static final int SMS_PERMISSION_CODE = 1001;
    private static final String SMS_DELIVERED_ACTION = "com.smsindia.SMS_DELIVERED";

    private Button startBtn, viewLogsBtn;
    private TextView tvRecipient, tvMessage, tvStatus, tvProgress;
    private ProgressBar progressBar;

    private boolean isRunning = false;
    private Handler handler;
    private int currentIndex = 0;
    private final List<Map<String, Object>> tasks = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String uid;
    private BroadcastReceiver deliveryReceiver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task, container, false);

        startBtn = v.findViewById(R.id.btn_start_task);
        viewLogsBtn = v.findViewById(R.id.btn_view_logs);
        tvRecipient = v.findViewById(R.id.tv_recipient);
        tvMessage = v.findViewById(R.id.tv_message);
        tvStatus = v.findViewById(R.id.tv_status);
        tvProgress = v.findViewById(R.id.tv_progress);
        progressBar = v.findViewById(R.id.progress_bar);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        handler = new Handler();

        if (auth.getCurrentUser() != null) {
            uid = auth.getCurrentUser().getUid();
        }

        // ✅ Request SMS permissions (Android 13–15)
        checkAndRequestSmsPermissions();

        startBtn.setOnClickListener(view -> {
            if (!hasSmsPermissions()) {
                Toast.makeText(getContext(), "Grant SMS permissions first!", Toast.LENGTH_SHORT).show();
                checkAndRequestSmsPermissions();
                return;
            }
            if (isRunning) {
                stopTask();
            } else {
                startForegroundSmsWorker();  // 🚀 background-safe
            }
        });

        viewLogsBtn.setOnClickListener(v1 ->
                startActivity(new Intent(requireContext(), DeliveryLogActivity.class)));

        loadTasks();
        registerDeliveryReceiver();
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (deliveryReceiver != null) {
            requireContext().unregisterReceiver(deliveryReceiver);
        }
        handler.removeCallbacksAndMessages(null);
    }

    // 🔔 delivery confirmation
    private void registerDeliveryReceiver() {
        deliveryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String phone = intent.getStringExtra("phone");
                switch (getResultCode()) {
                    case android.app.Activity.RESULT_OK:
                        Toast.makeText(context, "✅ Delivered to " + phone, Toast.LENGTH_SHORT).show();
                        updateBalance(uid);
                        break;
                    default:
                        Toast.makeText(context, "❌ Delivery failed to " + phone, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        requireContext().registerReceiver(deliveryReceiver, new IntentFilter(SMS_DELIVERED_ACTION));
    }

    // 🟢 Android 15 runtime SMS permissions
    private void checkAndRequestSmsPermissions() {
        if (!hasSmsPermissions()) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS
                    },
                    SMS_PERMISSION_CODE
            );
        }
    }

    private boolean hasSmsPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            boolean granted = true;
            for (int res : grantResults)
                if (res != PackageManager.PERMISSION_GRANTED) granted = false;
            Toast.makeText(getContext(),
                    granted ? "✅ SMS permissions granted" : "❌ SMS permissions denied",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTasks() {
        tvStatus.setText("Loading tasks...");
        db.collection("sms_tasks").get()
                .addOnSuccessListener(snapshot -> {
                    tasks.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Map<String, Object> data = doc.getData();
                        data.put("id", doc.getId());
                        tasks.add(data);
                    }
                    progressBar.setMax(tasks.size());
                    tvProgress.setText("0 / " + tasks.size());
                    tvStatus.setText("✅ Loaded " + tasks.size() + " tasks");
                })
                .addOnFailureListener(e ->
                        tvStatus.setText("Error loading tasks: " + e.getMessage()));
    }

    // 🚀 Foreground Worker start (Android 15 compliant)
    private void startForegroundSmsWorker() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        Data data = new Data.Builder().putString("userId", userId).build();

        WorkRequest work = new OneTimeWorkRequest.Builder(SmsWorker.class)
                .setInputData(data)
                .addTag("sms_worker")
                .build();

        WorkManager.getInstance(requireContext()).enqueue(work);
        Toast.makeText(getContext(), "🚀 SMS worker started in background", Toast.LENGTH_SHORT).show();

        tvStatus.setText("📤 Sending via foreground worker...");
    }

    private void stopTask() {
        isRunning = false;
        startBtn.setText("▶ Start SMS Task");
        tvStatus.setText("⏸ Sending paused");
        handler.removeCallbacksAndMessages(null);
        WorkManager.getInstance(requireContext()).cancelAllWorkByTag("sms_worker");
    }

    private void markTaskSent(String phone) {
        db.collection("sent_logs").add(new HashMap<String, Object>() {{
            put("userId", uid);
            put("phone", phone);
            put("timestamp", System.currentTimeMillis());
        }});
    }

    private void updateBalance(String userId) {
        if (userId == null) return;
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    double bal = doc.contains("balance") ? doc.getDouble("balance") : 0.0;
                    bal += 0.16;
                    db.collection("users").document(userId).update("balance", bal);
                });
    }
}