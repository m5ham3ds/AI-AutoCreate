package com.ai_autocreate.agents;

import android.content.Context;
import android.media.MediaMetadataRetriever;
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

public class AudioContextAgent {
    private Context context;
    private JSONLogger logger;
    private HFClient hfClient;

    public AudioContextAgent(Context context) {
        this.context = context;
        this.logger = new JSONLogger(context);
        this.hfClient = new HFClient(context);
    }

    public JSONObject analyzeAudio(String audioPath) {
        try {
            JSONObject result = new JSONObject();
            result.put("agent", "AudioContextAgent");
            result.put("audio_path", audioPath);
            result.put("success", false);

            // Check if audio file exists
            File audioFile = new File(audioPath);
            if (!audioFile.exists()) {
                result.put("message", "Audio file does not exist");
                logResult(result);
                return result;
            }

            // Get audio metadata
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(audioPath);

                // Extract metadata
                JSONObject metadata = extractMetadata(retriever);
                result.put("metadata", metadata);

                // Create audio analysis payload
                JSONObject payload = new JSONObject();
                payload.put("inputs", "Analyze this audio and provide insights about its content, mood, instruments, and potential applications for reconstruction.");

                // Get default audio model
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
                        analysisObj.put("audio_features", extractAudioFeatures(retriever));

                        result.put("analysis", analysisObj);
                        result.put("success", true);
                        result.put("message", "Audio analyzed successfully");
                    } else {
                        result.put("message", "Invalid response format from model");
                    }
                } else {
                    result.put("message", "Failed to get response from model");
                }

            } catch (Exception e) {
                result.put("message", "Error analyzing audio: " + e.getMessage());
                logger.log("AudioContextAgent", "Error analyzing audio: " + e.getMessage());
            } finally {
                retriever.release();
            }

            logResult(result);
            return result;

        } catch (JSONException e) {
            logger.log("AudioContextAgent", "Error creating result: " + e.getMessage());

            try {
                JSONObject errorResult = new JSONObject();
                errorResult.put("agent", "AudioContextAgent");
                errorResult.put("success", false);
                errorResult.put("message", "Error creating result: " + e.getMessage());
                return errorResult;
            } catch (JSONException ex) {
                return null;
            }
        }
    }

    public void analyzeAudioAsync(String audioPath, AnalysisCallback callback) {
        new AnalyzeAudioTask(audioPath, callback).execute();
    }

    private JSONObject extractMetadata(MediaMetadataRetriever retriever) {
        JSONObject metadata = new JSONObject();

        try {
            // Get duration
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                long duration = Long.parseLong(durationStr);
                metadata.put("duration_ms", duration);
                metadata.put("duration_seconds", duration / 1000.0);
                metadata.put("duration_formatted", formatDuration(duration));
            }

            // Get bitrate
            String bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if (bitrateStr != null) {
                metadata.put("bitrate", Integer.parseInt(bitrateStr));
            }

            // Get sample rate
            String sampleRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            if (sampleRateStr != null) {
                metadata.put("sample_rate", Integer.parseInt(sampleRateStr));
            }

            // Get number of channels
            String numChannelsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS);
            if (numChannelsStr != null) {
                metadata.put("num_channels", Integer.parseInt(numChannelsStr));
            }

            // Get title and artist if available
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if (title != null) {
                metadata.put("title", title);
            }

            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (artist != null) {
                metadata.put("artist", artist);
            }

        } catch (JSONException | NumberFormatException e) {
            logger.log("AudioContextAgent", "Error extracting metadata: " + e.getMessage());
        }

        return metadata;
    }

    private JSONObject extractAudioFeatures(MediaMetadataRetriever retriever) {
        JSONObject features = new JSONObject();

        try {
            // This is a simplified version - in a real implementation, 
            // this would extract actual audio features

            features.put("tempo", "120 BPM"); // Placeholder
            features.put("key", "C Major"); // Placeholder
            features.put("genre", "Unknown"); // Placeholder
            features.put("mood", "Neutral"); // Placeholder
            features.put("instruments", "Various"); // Placeholder

        } catch (JSONException e) {
            logger.log("AudioContextAgent", "Error extracting audio features: " + e.getMessage());
        }

        return features;
    }

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds %= 60;
        minutes %= 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    private String getDefaultModelEndpoint() {
        // This would normally read from the models configuration
        // For now, we'll return a default endpoint
        return "https://api-inference.huggingface.co/models/facebook/musicgen-small";
    }

    private void logResult(JSONObject result) {
        try {
            String audioPath = result.getString("audio_path");
            String fileName = new File(audioPath).getName();
            File logFile = new File(StoragePaths.getAgentResultsDir() + "/AudioContextAgent/analysis_" + fileName + ".json");
            JSONLogger.writeToFile(logFile, result.toString());
        } catch (JSONException e) {
            logger.log("AudioContextAgent", "Error logging result: " + e.getMessage());
        }
    }

    public interface AnalysisCallback {
        void onAnalysisComplete(JSONObject result);
        void onAnalysisError(String errorMessage);
    }

    private class AnalyzeAudioTask extends AsyncTask<Void, Void, JSONObject> {
        private String audioPath;
        private AnalysisCallback callback;
        private String errorMessage;

        public AnalyzeAudioTask(String audioPath, AnalysisCallback callback) {
            this.audioPath = audioPath;
            this.callback = callback;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                return analyzeAudio(audioPath);
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
