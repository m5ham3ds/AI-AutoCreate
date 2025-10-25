package com.ai_autocreate.agents;

import android.content.Context;
import android.os.AsyncTask;

import com.ai_autocreate.utils.HFClient;
import com.ai_autocreate.utils.JSONLogger;
import com.ai_autocreate.utils.StoragePaths;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray; // CHANGED: add missing import

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScriptGeneratorAgent {
    private Context context;
    private JSONLogger logger;
    private HFClient hfClient;

    public ScriptGeneratorAgent(Context context) {
        this.context = context;
        this.logger = new JSONLogger(context);
        this.hfClient = new HFClient(context);
    }

    public JSONObject generateScript(String prompt) {
        try {
            JSONObject result = new JSONObject();
            result.put("agent", "ScriptGeneratorAgent");
            result.put("prompt", prompt);
            result.put("success", false);

            // Create script generation payload
            String scriptPrompt = "Generate a script based on this prompt: " + prompt + 
                "\n\nThe script should include:\n" +
                "1. Scene descriptions\n" +
                "2. Character dialogue\n" +
                "3. Action descriptions\n" +
                "4. Proper formatting\n\n" +
                "Generate a complete, well-structured script.";

            JSONObject payload = new JSONObject();
            payload.put("inputs", scriptPrompt);
            payload.put("parameters", new JSONObject()
                        .put("max_new_tokens", 800)
                        .put("temperature", 0.3)
                        .put("top_p", 0.9)
                        .put("repetition_penalty", 1.05));

            // Get default text model
            String modelEndpoint = getDefaultModelEndpoint();

            // Make request
            JSONObject response = hfClient.requestModel(modelEndpoint, "", payload);

            if (response != null && response.has("0")) {
                JSONArray generatedText = response.getJSONArray("0");
                if (generatedText.length() > 0 && generatedText.getJSONObject(0).has("generated_text")) {
                    String script = generatedText.getJSONObject(0).getString("generated_text");

                    // Clean up the script (remove the prompt part)
                    if (script.startsWith(scriptPrompt)) {
                        script = script.substring(scriptPrompt.length()).trim();
                    }

                    result.put("script", script);
                    result.put("success", true);
                    result.put("message", "Script generated successfully");
                } else {
                    result.put("message", "Invalid response format from model");
                }
            } else {
                result.put("message", "Failed to get response from model");
            }

            logResult(result);
            return result;

        } catch (JSONException e) {
            logger.log("ScriptGeneratorAgent", "Error generating script: " + e.getMessage());

            try {
                JSONObject errorResult = new JSONObject();
                errorResult.put("agent", "ScriptGeneratorAgent");
                errorResult.put("success", false);
                errorResult.put("message", "Error generating script: " + e.getMessage());
                return errorResult;
            } catch (JSONException ex) {
                return null;
            }
        }
    }

    public void generateScriptAsync(String prompt, ScriptCallback callback) {
        new GenerateScriptTask(prompt, callback).execute();
    }

    private String getDefaultModelEndpoint() {
        // This would normally read from the models configuration
        // For now, we'll return a default endpoint
        return "https://api-inference.huggingface.co/models/microsoft/DialoGPT-medium";
    }

    private void logResult(JSONObject result) {
        try {
            File logFile = new File(StoragePaths.getAgentResultsDir() + "/ScriptGeneratorAgent/script_generation.json");
            JSONLogger.writeToFile(logFile, result.toString());
        } catch (Exception e) {
            logger.log("ScriptGeneratorAgent", "Error logging result: " + e.getMessage());
        }
    }

    public interface ScriptCallback {
        void onScriptGenerated(JSONObject result);
        void onScriptGenerationError(String errorMessage);
    }

    private class GenerateScriptTask extends AsyncTask<Void, Void, JSONObject> {
        private String prompt;
        private ScriptCallback callback;
        private String errorMessage;

        public GenerateScriptTask(String prompt, ScriptCallback callback) {
            this.prompt = prompt;
            this.callback = callback;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                return generateScript(prompt);
            } catch (Exception e) {
                errorMessage = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null && callback != null) {
                callback.onScriptGenerated(result);
            } else if (callback != null) {
                callback.onScriptGenerationError(errorMessage != null ? errorMessage : "Unknown error");
            }
        }
    }
}
