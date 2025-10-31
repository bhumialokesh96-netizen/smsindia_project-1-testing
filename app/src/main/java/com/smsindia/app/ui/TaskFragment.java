package com.smsindia.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smsindia.app.R;
import com.smsindia.app.service.SmsForegroundService;

public class TaskFragment extends Fragment {

    private static final int SMS_PERMISSION_CODE = 1001;

    private Button startBtn, viewLogsBtn;
    private TextView tvStatus;
    private ProgressBar progressBar;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private boolean isRunning = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task, container, false);

        startBtn = v.findViewById(R.id.btn_start_task);
        viewLogsBtn = v.findViewById(R.id.btn_view_logs);
        tvStatus = v.findViewById(R.id.tv_status);
        progressBar = v.findViewById(R.id.progress_bar);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        checkAndRequestSmsPermissions();

        startBtn.setOnClickListener(view -> {
            if (!hasSmsPermissions()) {
                checkAndRequestSmsPermissions();
                return;
            }

            if (isRunning) {
                requireContext().stopService(new Intent(requireContext(), SmsForegroundService.class));
                startBtn.setText("▶ Start SMS Task");
                tvStatus.setText("⏸ Sending paused");
                isRunning = false;
            } else {
                Intent i = new Intent(requireContext(), SmsForegroundService.class);
                requireContext().startForegroundService(i);
                startBtn.setText("⏹ Stop Task");
                tvStatus.setText("🚀 Sending started in background...");
                isRunning = true;
            }
        });

        viewLogsBtn.setOnClickListener(v1 ->
                startActivity(new Intent(requireContext(), DeliveryLogActivity.class)));

        return v;
    }

    private void checkAndRequestSmsPermissions() {
        if (!hasSmsPermissions()) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    SMS_PERMISSION_CODE
            );
        }
    }

    private boolean hasSmsPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
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
                    granted ? "✅ SMS permissions granted" : "❌ Please allow all permissions to continue",
                    Toast.LENGTH_LONG).show();
        }
    }
}