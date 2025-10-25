package com.ai_autocreate.agents;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;

import com.ai_autocreate.utils.JSONLogger;
import com.ai_autocreate.utils.StoragePaths;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideoAnalyzerAgent {
    private Context context;
    private JSONLogger logger;

    public VideoAnalyzerAgent(Context context) {
        this.context = context;
        this.logger = new JSONLogger(context);
    }

    public JSONObject analyzeVideo(String videoPath) {
        try {
            JSONObject result = new JSONObject();
            result.put("agent", "VideoAnalyzerAgent");
            result.put("video_path", videoPath);
            result.put("success", false);

            // Check if video file exists
            File videoFile = new File(videoPath);
            if (!videoFile.exists()) {
                result.put("message", "Video file does not exist");
                logResult(result);
                return result;
            }

            // Get video metadata
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(videoPath);

                // Extract metadata
                JSONObject metadata = extractMetadata(retriever);
                result.put("metadata", metadata);

                // Extract scenes (simplified)
                JSONArray scenes = extractScenes(videoPath, retriever);
                result.put("scenes", scenes);

                // Extract key frames (simplified)
                JSONArray keyFrames = extractKeyFrames(videoPath, retriever);
                result.put("key_frames", keyFrames);

                // Extract audio information
                JSONObject audioInfo = extractAudioInfo(retriever);
                result.put("audio_info", audioInfo);

                // Generate summary
                String summary = generateSummary(metadata, scenes);
                result.put("summary", summary);

                result.put("success", true);
                result.put("message", "Video analyzed successfully");

            } catch (Exception e) {
                result.put("message", "Error analyzing video: " + e.getMessage());
                logger.log("VideoAnalyzerAgent", "Error analyzing video: " + e.getMessage());
            } finally {
                retriever.release();
            }

            logResult(result);
            return result;

        } catch (JSONException e) {
            logger.log("VideoAnalyzerAgent", "Error creating result: " + e.getMessage());

            try {
                JSONObject errorResult = new JSONObject();
                errorResult.put("agent", "VideoAnalyzerAgent");
                errorResult.put("success", false);
                errorResult.put("message", "Error creating result: " + e.getMessage());
                return errorResult;
            } catch (JSONException ex) {
                return null;
            }
        }
    }

    public void analyzeVideoAsync(String videoPath, AnalysisCallback callback) {
        new AnalysisTask(videoPath, callback).execute();
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

            // Get width and height
            String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);

            if (widthStr != null && heightStr != null) {
                int width = Integer.parseInt(widthStr);
                int height = Integer.parseInt(heightStr);
                metadata.put("width", width);
                metadata.put("height", height);
                metadata.put("resolution", width + "x" + height);
                metadata.put("aspect_ratio", (double) width / height);
            }

            // Get rotation
            String rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            if (rotationStr != null) {
                metadata.put("rotation", Integer.parseInt(rotationStr));
            }

            // Get bitrate
            String bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if (bitrateStr != null) {
                metadata.put("bitrate", Integer.parseInt(bitrateStr));
            }

            // Get frame rate
            String frameRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
            if (frameRateStr != null) {
                metadata.put("frame_rate", Double.parseDouble(frameRateStr));
            }

        } catch (JSONException | NumberFormatException e) {
            logger.log("VideoAnalyzerAgent", "Error extracting metadata: " + e.getMessage());
        }

        return metadata;
    }

    private JSONArray extractScenes(String videoPath, MediaMetadataRetriever retriever) {
        JSONArray scenes = new JSONArray();

        try {
            // Get video duration
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr == null) {
                return scenes;
            }

            long duration = Long.parseLong(durationStr);

            // Simplified scene detection - divide video into equal segments
            int numScenes = 5; // Fixed number of scenes for simplicity
            long segmentDuration = duration / numScenes;

            for (int i = 0; i < numScenes; i++) {
                long startTime = i * segmentDuration;
                long endTime = (i == numScenes - 1) ? duration : (i + 1) * segmentDuration;

                JSONObject scene = new JSONObject();
                scene.put("index", i);
                scene.put("start_time_ms", startTime);
                scene.put("end_time_ms", endTime);
                scene.put("start_time_formatted", formatTime(startTime));
                scene.put("end_time_formatted", formatTime(endTime));
                scene.put("duration_ms", endTime - startTime);

                // Extract frame at scene start
                String framePath = extractFrameAtTime(videoPath, startTime, i);
                if (framePath != null) {
                    scene.put("thumbnail_path", framePath);
                }

                scenes.put(scene);
            }

        } catch (JSONException e) {
            logger.log("VideoAnalyzerAgent", "Error extracting scenes: " + e.getMessage());
        }

        return scenes;
    }

    private JSONArray extractKeyFrames(String videoPath, MediaMetadataRetriever retriever) {
        JSONArray keyFrames = new JSONArray();

        try {
            // Get video duration
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr == null) {
                return keyFrames;
            }

            long duration = Long.parseLong(durationStr);

            // Extract key frames at regular intervals
            int numKeyFrames = 10;
            long interval = duration / numKeyFrames;

            for (int i = 0; i < numKeyFrames; i++) {
                long time = i * interval;

                JSONObject keyFrame = new JSONObject();
                keyFrame.put("index", i);
                keyFrame.put("time_ms", time);
                keyFrame.put("time_formatted", formatTime(time));

                // Extract frame at this time
                String framePath = extractFrameAtTime(videoPath, time, i);
                if (framePath != null) {
                    keyFrame.put("frame_path", framePath);
                }

                keyFrames.put(keyFrame);
            }

        } catch (JSONException e) {
            logger.log("VideoAnalyzerAgent", "Error extracting key frames: " + e.getMessage());
        }

        return keyFrames;
    }

    private String extractFrameAtTime(String videoPath, long timeUs, int index) {
        try {
            // This is a simplified version - in a real implementation, 
            // this would use FFmpeg to extract frames

            // Create frame directory if it doesn't exist
            String frameDir = StoragePaths.getTempDir() + "/frames";
            File dir = new File(frameDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Create frame file path
            String framePath = frameDir + "/frame_" + index + "_" + timeUs + ".jpg";
            File frameFile = new File(framePath);

            // In a real implementation, this would extract the actual frame
            // For now, we'll just create a placeholder file
            frameFile.createNewFile();

            return framePath;

        } catch (Exception e) {
            logger.log("VideoAnalyzerAgent", "Error extracting frame at time " + timeUs + ": " + e.getMessage());
            return null;
        }
    }

    private JSONObject extractAudioInfo(MediaMetadataRetriever retriever) {
        JSONObject audioInfo = new JSONObject();

        try {
            // Check if video has audio
            String hasAudioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO);
            boolean hasAudio = "yes".equalsIgnoreCase(hasAudioStr);
            audioInfo.put("has_audio", hasAudio);

            if (hasAudio) {
                // Get audio sample rate
                String sampleRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
                if (sampleRateStr != null) {
                    audioInfo.put("sample_rate", Integer.parseInt(sampleRateStr));
                }

                // Get number of channels
                String numChannelsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS);
                if (numChannelsStr != null) {
                    audioInfo.put("num_channels", Integer.parseInt(numChannelsStr));
                }
            }

        } catch (JSONException | NumberFormatException e) {
            logger.log("VideoAnalyzerAgent", "Error extracting audio info: " + e.getMessage());
        }

        return audioInfo;
    }

    private String generateSummary(JSONObject metadata, JSONArray scenes) {
        try {
            StringBuilder summary = new StringBuilder();

            // Add basic info
            if (metadata.has("duration_formatted")) {
                summary.append("Duration: ").append(metadata.getString("duration_formatted")).append("\n");
            }

            if (metadata.has("resolution")) {
                summary.append("Resolution: ").append(metadata.getString("resolution")).append("\n");
            }

            // Add scene info
            summary.append("Scenes: ").append(scenes.length()).append("\n");

            // Add scene descriptions
            for (int i = 0; i < scenes.length(); i++) {
                JSONObject scene = scenes.getJSONObject(i);
                summary.append("Scene ").append(i + 1).append(": ");
                summary.append(scene.optString("start_time_formatted", ""));
                summary.append(" - ");
                summary.append(scene.optString("end_time_formatted", ""));
                summary.append("\n");
            }

            return summary.toString();

        } catch (JSONException e) {
            logger.log("VideoAnalyzerAgent", "Error generating summary: " + e.getMessage());
            return "Error generating summary";
        }
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

    private String formatTime(long timeMs) {
        return formatDuration(timeMs);
    }

    private void logResult(JSONObject result) {
        try {
            String videoPath = result.getString("video_path");
            String fileName = new File(videoPath).getName();
            File logFile = new File(StoragePaths.getAgentResultsDir() + "/VideoAnalyzerAgent/analysis_" + fileName + ".json");
            JSONLogger.writeToFile(logFile, result.toString());
        } catch (JSONException e) {
            logger.log("VideoAnalyzerAgent", "Error logging result: " + e.getMessage());
        }
    }

    public interface AnalysisCallback {
        void onAnalysisComplete(JSONObject result);
        void onAnalysisError(String errorMessage);
    }

    private class AnalysisTask extends AsyncTask<Void, Void, JSONObject> {
        private String videoPath;
        private AnalysisCallback callback;
        private String errorMessage;

        public AnalysisTask(String videoPath, AnalysisCallback callback) {
            this.videoPath = videoPath;
            this.callback = callback;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                return analyzeVideo(videoPath);
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
