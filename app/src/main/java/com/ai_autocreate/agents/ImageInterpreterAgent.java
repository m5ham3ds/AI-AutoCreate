package com.ai_autocreate.agents;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.ai_autocreate.utils.HFClient;
import com.ai_autocreate.utils.JSONLogger;
import com.ai_autocreate.utils.MediaUtils;
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

public class ImageInterpreterAgent {
    private Context context;
    private JSONLogger logger;
    private HFClient hfClient;

    public ImageInterpreterAgent(Context context) {
        this.context = context;
        this.logger = new JSONLogger(context);
        this.hfClient = new HFClient(context);
    }

    public JSONObject analyzeImage(String imagePath) {
        try {
            JSONObject result = new JSONObject();
            result.put("agent", "ImageInterpreterAgent");
            result.put("image_path", imagePath);
            result.put("success", false);

            // Check if image file exists
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                result.put("message", "Image file does not exist");
                logResult(result);
                return result;
            }

            // Load and process image
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap == null) {
                result.put("message", "Failed to load image");
                logResult(result);
                return result;
            }

            // Create image analysis payload
            JSONObject payload = new JSONObject();
            payload.put("inputs", "Analyze this image and provide a detailed description including objects, scenes, colors, mood, and potential story elements.");

            // Get default image model
            String modelEndpoint = getDefaultModelEndpoint();

            // Make request
            JSONObject response = hfClient.requestModel(modelEndpoint, "", payload);

            if (response != null && response.has("0")) {
                JSONArray generatedText = response.getJSONArray("0");
                if (generatedText.length() > 0 && generatedText.getJSONObject(0).has("generated_text")) {
                    String analysis = generatedText.getJSONObject(0).getString("generated_text");

                    // Create analysis object
                    JSONObject analysisObj = new JSONObject();
                    analysisObj.put("description", analysis);
                    analysisObj.put("image_info", getImageInfo(bitmap));

                    result.put("analysis", analysisObj);
                    result.put("success", true);
                    result.put("message", "Image analyzed successfully");
                } else {
                    result.put("message", "Invalid response format from model");
                }
            } else {
                result.put("message", "Failed to get response from model");
            }

            // Clean up bitmap
            MediaUtils.recycleBitmap(bitmap);

            logResult(result);
            return result;

        } catch (JSONException e) {
            logger.log("ImageInterpreterAgent", "Error analyzing image: " + e.getMessage());

            try {
                JSONObject errorResult = new JSONObject();
                errorResult.put("agent", "ImageInterpreterAgent");
                errorResult.put("success", false);
                errorResult.put("message", "Error analyzing image: " + e.getMessage());
                return errorResult;
            } catch (JSONException ex) {
                return null;
            }
        }
    }

    public void analyzeImageAsync(String imagePath, AnalysisCallback callback) {
        new AnalyzeImageTask(imagePath, callback).execute();
    }

    private JSONObject getImageInfo(Bitmap bitmap) {
        JSONObject info = new JSONObject();

        try {
            info.put("width", bitmap.getWidth());
            info.put("height", bitmap.getHeight());
            info.put("aspect_ratio", (double) bitmap.getWidth() / bitmap.getHeight());

            // Analyze dominant colors (simplified)
            info.put("dominant_colors", "Color analysis would go here");

            // Analyze composition (simplified)
            info.put("composition", "Composition analysis would go here");

        } catch (JSONException e) {
            logger.log("ImageInterpreterAgent", "Error getting image info: " + e.getMessage());
        }

        return info;
    }

    private String getDefaultModelEndpoint() {
        // This would normally read from the models configuration
        // For now, we'll return a default endpoint
        return "https://api-inference.huggingface.co/models/nlpconnect/vit-gpt2-image-captioning";
    }

    private void logResult(JSONObject result) {
        try {
            String imagePath = result.getString("image_path");
            String fileName = new File(imagePath).getName();
            File logFile = new File(StoragePaths.getAgentResultsDir() + "/ImageInterpreterAgent/analysis_" + fileName + ".json");
            JSONLogger.writeToFile(logFile, result.toString());
        } catch (JSONException e) {
            logger.log("ImageInterpreterAgent", "Error logging result: " + e.getMessage());
        }
    }

    public interface AnalysisCallback {
        void onAnalysisComplete(JSONObject result);
        void onAnalysisError(String errorMessage);
    }

    private class AnalyzeImageTask extends AsyncTask<Void, Void, JSONObject> {
        private String imagePath;
        private AnalysisCallback callback;
        private String errorMessage;

        public AnalyzeImageTask(String imagePath, AnalysisCallback callback) {
            this.imagePath = imagePath;
            this.callback = callback;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                return analyzeImage(imagePath);
            } catch (Exception e) {
                errorMessage = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null && callback != null) {
                callback.onAnalysisComplete(result);
            } else if (callback != null) {
                callback.onAnalysisError(errorMessage != null ? errorMessage : "Unknown error");
            }
        }
    }
}
