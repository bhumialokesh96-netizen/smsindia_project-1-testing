package com.smsindia.app.ui;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.smsindia.app.R;

public class DeliveryLogActivity extends AppCompatActivity {

    private LinearLayout logsContainer;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_logs);

        logsContainer = findViewById(R.id.logs_container);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadLogs();
    }

    private void loadLogs() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("sent_logs")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    logsContainer.removeAllViews();
                    if (snapshot.isEmpty()) {
                        addText("No logs found yet 💤");
                        return;
                    }
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String phone = doc.getString("phone");
                        long time = doc.getLong("timestamp");
                        String formatted = android.text.format.DateFormat.format("dd MMM, hh:mm a", time).toString();
                        addText("📱 " + phone + "  •  " + formatted);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Failed to load logs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15f);
        tv.setPadding(20, 10, 20, 10);
        logsContainer.addView(tv);
    }
}