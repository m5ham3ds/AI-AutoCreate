package com.ai_autocreate.agents;

import android.content.Context;
import android.os.AsyncTask;

import com.ai_autocreate.utils.HFClient;
import com.ai_autocreate.utils.JSONLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ModelValidatorAgent {
    private Context context;
    private JSONLogger logger;

    public ModelValidatorAgent(Context context) {
        this.context = context;
        this.logger = new JSONLogger(context);
    }

    public boolean validateModel(JSONObject model) {
        try {
            String endpoint = model.getString("endpoint");
            String type = model.getString("type");
            boolean requiresApiKey = model.optBoolean("requires_api_key", false);
            String apiKey = "";

            if (requiresApiKey && model.has("api_key")) {
                apiKey = model.getString("api_key");
            }

            // Create test payload based on model type
            JSONObject testPayload = createTestPayload(type);

            // Make test request
            HFClient client = new HFClient(context);
            JSONObject response = client.requestModel(endpoint, apiKey, testPayload);

            if (response != null) {
                // Log successful validation
                logValidationResult(model, true, "Model validated successfully");
                return true;
            } else {
                // Log failed validation
                logValidationResult(model, false, "Failed to get response from model");
                return false;
            }
        } catch (Exception e) {
            // Log error
            logValidationResult(model, false, "Error validating model: " + e.getMessage());
            return false;
        }
    }

    private JSONObject createTestPayload(String type) throws JSONException {
        JSONObject payload = new JSONObject();

        switch (type) {
            case "text":
                payload.put("inputs", "Hello, how are you?");
                payload.put("parameters", new JSONObject()
                            .put("max_new_tokens", 10)
                            .put("temperature", 0.1));
                break;
            case "image":
                payload.put("inputs", "A simple test image");
                break;
            case "audio":
                payload.put("inputs", "Test audio generation");
                break;
            default:
                payload.put("inputs", "Test input");
        }

        return payload;
    }

    private void logValidationResult(JSONObject model, boolean isValid, String message) {
        try {
            JSONObject logEntry = new JSONObject();
            logEntry.put("model_id", model.getString("id"));
            logEntry.put("model_name", model.getString("name"));
            logEntry.put("model_type", model.getString("type"));
            logEntry.put("endpoint", model.getString("endpoint"));
            logEntry.put("is_valid", isValid);
            logEntry.put("message", message);
            logEntry.put("timestamp", System.currentTimeMillis());

            logger.log("ModelValidator", logEntry.toString());
        } catch (JSONException e) {
            logger.log("ModelValidator", "Error logging validation result: " + e.getMessage());
        }
    }

    public void validateModelAsync(JSONObject model, ValidationCallback callback) {
        new ValidationTask(model, callback).execute();
    }

    public interface ValidationCallback {
        void onValidationComplete(JSONObject model, boolean isValid, String message);
    }

    private class ValidationTask extends AsyncTask<Void, Void, Boolean> {
        private JSONObject model;
        private ValidationCallback callback;
        private String message;

        public ValidationTask(JSONObject model, ValidationCallback callback) {
            this.model = model;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                return validateModel(model);
            } catch (Exception e) {
                message = "Error validating model: " + e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (callback != null) {
                callback.onValidationComplete(model, result, message);
            }
        }
    }
}
