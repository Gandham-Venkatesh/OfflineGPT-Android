package com.example.mistralapp;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RetryPolicy;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.content.Context;
import android.text.method.ScrollingMovementMethod;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private EditText editTextPrompt;
    private RadioGroup radioGroupTokens;
    private Button buttonSubmit;
    private TextView textViewResponse;
    private TextView textViewTimer;
    private ProgressBar progressBar;



    String BASE_URL = "http://127.0.0.1:8000";
    // or 192.168.x.x if on WiFi

    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextPrompt = findViewById(R.id.editTextPrompt);
        editTextPrompt.setMovementMethod(new ScrollingMovementMethod());

        radioGroupTokens = findViewById(R.id.radioGroupTokens);
        buttonSubmit = findViewById(R.id.buttonSubmit);
        textViewResponse = findViewById(R.id.textViewResponse);
        textViewTimer = findViewById(R.id.textViewTimer);
        progressBar = findViewById(R.id.progressBar);


        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String prompt = editTextPrompt.getText().toString().trim();
                int selectedTokenId = radioGroupTokens.getCheckedRadioButtonId();
                int tokens = 100; // Default
                int timeInSeconds = 30;

                if (selectedTokenId == R.id.radio300) {
                    tokens = 300;
                    timeInSeconds = 60;
                } else if (selectedTokenId == R.id.radio800) {
                    tokens = 800;
                    timeInSeconds = 150;
                }

                startTimer(timeInSeconds);
                progressBar.setVisibility(View.VISIBLE);

                sendPromptToServer(prompt, tokens);

                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    View currentFocus = getCurrentFocus();
                    if (currentFocus != null) {
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                    }
                }
            }
        });
    }

    private void startTimer(int seconds) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        textViewTimer.setVisibility(View.VISIBLE);

        countDownTimer = new CountDownTimer(seconds * 1000L, 1000) {
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                textViewTimer.setText("Timer: " + secondsRemaining + "s");
            }

            public void onFinish() {
                textViewTimer.setText("Done!");
            }
        };
        countDownTimer.start();
    }

    private void sendPromptToServer(String prompt, int token) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = BASE_URL + "/generate";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("prompt", prompt);
            jsonBody.put("max_tokens", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Show loading spinner before request
        progressBar.setVisibility(View.VISIBLE);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    String modelResponse = response.optString("response");
                    textViewResponse.setText(modelResponse);
                    textViewResponse.setMovementMethod(new ScrollingMovementMethod());

                    // ✅ Hide progress bar when response is received
                    progressBar.setVisibility(View.GONE);
                },
                error -> {
                    textViewResponse.setText("Error: " + error.toString());

                    new android.os.Handler().postDelayed(
                            () -> progressBar.setVisibility(View.GONE),
                            1000  // 1 second delay
                    );
                }) {
            @Override
            public RetryPolicy getRetryPolicy() {
                return new DefaultRetryPolicy(
                        150000, // 60 seconds timeout
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                );
            }
        };

        queue.add(request);
    }

}
