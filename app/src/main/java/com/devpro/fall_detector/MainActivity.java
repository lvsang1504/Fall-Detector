package com.devpro.fall_detector;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.devpro.fall_detector.adapter.PostAdapter;
import com.devpro.fall_detector.databinding.ActivityMainBinding;
import com.devpro.fall_detector.models.FallResponse;
import com.devpro.fall_detector.network.ApiClient;
import com.devpro.fall_detector.network.ApiService;
import com.devpro.fall_detector.utilities.PreferenceManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import pl.bclogic.pulsator4droid.library.PulsatorLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private PostAdapter _mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
        firebaseMessaging.subscribeToTopic("");

        layoutManager = new LinearLayoutManager(this);
        binding.myRecyclerView.setFocusable(false);


        binding.myRecyclerView.setHasFixedSize(true);
        binding.myRecyclerView.setLayoutManager(layoutManager);

        binding.idR1.setBackground(AppCompatResources.getDrawable(this, R.drawable.ic_shape_background));

        preferenceManager = new PreferenceManager(getApplicationContext());
        getToken();

        getDataHistoryFallDetect();

        binding.btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.btnSkip.setVisibility(View.INVISIBLE);
                binding.pulsator.stop();
                binding.textFallDetect.setText("Chưa phát hiện té ngã ");
                Toast.makeText(MainActivity.this, "Đã bỏ qua cảnh báo!", Toast.LENGTH_LONG).show();
            }
        });
    }

    void actionDialogFallDetector() {

        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        long[] pattern = {0, 3000, 3000};

        v.vibrate(pattern, 0);


        View alertCustomdialog = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_warning, null);

        Button btnOk = alertCustomdialog.findViewById(R.id.btnOk);

        PulsatorLayout pulsatorDialog = alertCustomdialog.findViewById(R.id.pulsatorDialog);
        pulsatorDialog.start();

        //initialize alert builder.
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

        //set our custom alert dialog to tha alertdialog builder
        alert.setView(alertCustomdialog);

        final AlertDialog dialog = alert.create();
        //this line removed app bar from dialog and make it transperent and you see the image is like floating outside dialog box.
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        //finally show the dialog box in android all
        dialog.show();
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                v.cancel();
                dialog.dismiss();
            }
        });
    }

    private void sendNotification(String messageBody) {
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    try {
                        if (response.body() != null) {
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if (responseJson.getInt("failure") == 1) {
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showToast("Notification sent successfully!");
                } else {
                    showToast("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
    }

    private void getDataHistoryFallDetect() {

        DatabaseReference fallDetector = FirebaseDatabase.getInstance().getReference("FallDetector");
        Query fallDetectQuery = fallDetector.orderByKey();
        fallDetectQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<FallResponse> fallList = new ArrayList<FallResponse>();

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    FallResponse fallResponse = postSnapshot.getValue(FallResponse.class);
                    fallList.add(fallResponse);
                }

                Collections.reverse(fallList);

                _mAdapter = new PostAdapter(fallList);
                binding.myRecyclerView.setAdapter(_mAdapter);

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date pasTime = null;
                try {
                    if (fallList.get(0).time != null) {
                        pasTime = dateFormat.parse(fallList.get(0).time);
                    } else {
                        pasTime = new Date();
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                Date nowTime = new Date();

                long dateDiff = nowTime.getTime() - pasTime.getTime();
                long second = TimeUnit.MILLISECONDS.toSeconds(dateDiff);


                if (second < 900) {
                    binding.pulsator.start();
                    binding.textFallDetect.setText("Cảnh báo phát hiện té ngã ");
                    binding.btnSkip.setVisibility(View.VISIBLE);
                    actionDialogFallDetector();
                    try {
                        JSONArray tokens = new JSONArray();
                        tokens.put(preferenceManager.getString(Constants.KEY_FCM_TOKEN));

                        JSONObject data = new JSONObject();
                        data.put(Constants.KEY_MESSAGE, "Phát hiện té ngã! \n " + fallList.get(0).time);

                        JSONObject body = new JSONObject();
                        body.put(Constants.REMOTE_MSG_DATA, data);
                        body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                        System.out.println(body.toString());

                        sendNotification(body.toString());

                    } catch (Exception e) {
                        showToast(e.getMessage());
                    }
                } else {
                    binding.pulsator.stop();
                    binding.textFallDetect.setText("Chưa phát hiện té ngã ");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}