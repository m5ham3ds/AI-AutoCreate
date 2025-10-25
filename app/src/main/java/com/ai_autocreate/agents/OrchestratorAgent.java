package com.ai_autocreate.agents;

import android.content.Context;
import android.os.AsyncTask;

import com.ai_autocreate.utils.FileUtils;
import com.ai_autocreate.utils.HFClient;
import com.ai_autocreate.utils.JSONLogger;
import com.ai_autocreate.utils.StoragePaths;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OrchestratorAgent {
    private Context context;
    private JSONLogger logger;
    private HFClient hfClient;

    public OrchestratorAgent(Context context) {
        this.context = context;
        this.logger = new JSONLogger(context);
        this.hfClient = new HFClient(context);
    }

    public JSONObject process(JSONObject input) {
        try {
            String projectId = input.getString("project_id");
            String prompt = input.getString("prompt");
            JSONObject projectConfig = input.getJSONObject("project_config");

            // Create result object
            JSONObject result = new JSONObject();
            result.put("agent", "OrchestratorAgent");
            result.put("project_id", projectId);
            result.put("success", false);

            // Create steps array
            JSONArray steps = new JSONArray();

            // Step 1: Analyze prompt
            JSONObject analyzeStep = analyzePrompt(prompt);
            steps.put(analyzeStep);

            if (!analyzeStep.optBoolean("success", false)) {
                result.put("message", "Failed to analyze prompt: " + analyzeStep.optString("message", "Unknown error"));
                logResult(result);
                return result;
            }

            // Step 2: Generate script
            JSONObject scriptStep = generateScript(prompt, analyzeStep.optJSONObject("analysis"));
            steps.put(scriptStep);

            if (!scriptStep.optBoolean("success", false)) {
                result.put("message", "Failed to generate script: " + scriptStep.optString("message", "Unknown error"));
                logResult(result);
                return result;
            }

            // Step 3: Generate images
            JSONObject imagesStep = generateImages(scriptStep.optJSONObject("script"));
            steps.put(imagesStep);

            if (!imagesStep.optBoolean("success", false)) {
                result.put("message", "Failed to generate images: " + imagesStep.optString("message", "Unknown error"));
                logResult(result);
                return result;
            }

            // Step 4: Generate audio
            JSONObject audioStep = generateAudio(scriptStep.optJSONObject("script"));
            steps.put(audioStep);

            if (!audioStep.optBoolean("success", false)) {
                result.put("message", "Failed to generate audio: " + audioStep.optString("message", "Unknown error"));
                logResult(result);
                return result;
            }

            // Step 5: Assemble final video
            JSONObject assembleStep = assembleVideo(projectId);
            steps.put(assembleStep);

            if (!assembleStep.optBoolean("success", false)) {
                result.put("message", "Failed to assemble video: " + assembleStep.optString("message", "Unknown error"));
                logResult(result);
                return result;
            }

            // All steps completed successfully
            result.put("success", true);
            result.put("steps", steps);
            result.put("message", "All steps completed successfully");

            logResult(result);
            return result;

        } catch (JSONException e) {
            logger.log("OrchestratorAgent", "Error processing input: " + e.getMessage());

            try {
                JSONObject errorResult = new JSONObject();
                errorResult.put("agent", "OrchestratorAgent");
                errorResult.put("success", false);
                errorResult.put("message", "Error processing input: " + e.getMessage());
                return errorResult;
            } catch (JSONException ex) {
                return null;
            }
        }
    }

    private JSONObject analyzePrompt(String prompt) {
        JSONObject step = new JSONObject();

        try {
            step.put("step_id", "analyze_prompt");
            step.put("success", false);

            // Create analysis payload
            JSONObject payload = new JSONObject();
            payload.put("inputs", "Analyze this prompt and extract key information: " + prompt);
            payload.put("parameters", new JSONObject()
                        .put("max_new_tokens", 100)
                        .put("temperature", 0.1));

            // Get default text model
            String modelEndpoint = getDefaultModelEndpoint("text");

            // Make request
            JSONObject response = hfClient.requestModel(modelEndpoint, "", payload);

            if (response != null && response.has("0")) {
                JSONArray generatedText = response.getJSONArray("0");
                if (generatedText.length() > 0 && generatedText.getJSONObject(0).has("generated_text")) {
                    String analysis = generatedText.getJSONObject(0).getString("generated_text");

                    // Create analysis object
                    JSONObject analysisObj = new JSONObject();
                    analysisObj.put("text", analysis);
                    analysisObj.put("keywords", extractKeywords(prompt));
                    analysisObj.put("sentiment", "neutral"); // Simplified

                    step.put("success", true);
                    step.put("analysis", analysisObj);
                    step.put("message", "Prompt analyzed successfully");
                }
            } else {
                step.put("message", "Failed to get response from model");
            }

        } catch (JSONException e) {
            logger.log("OrchestratorAgent", "Error analyzing prompt: " + e.getMessage());
            try {
                step.put("message", "Error analyzing prompt: " + e.getMessage());
            } catch (JSONException ex) {
                // Ignore
            }
        }

        return step;
    }

    private JSONObject generateScript(String prompt, JSONObject analysis) {
        JSONObject step = new JSONObject();

        try {
            step.put("step_id", "generate_script");
            step.put("success", false);

            // Create script generation payload
            String scriptPrompt = "Generate a script based on this prompt: " + prompt;
            if (analysis != null) {
                scriptPrompt += "\nAnalysis: " + analysis.optString("text", "");
            }
            scriptPrompt += "\n\nGenerate a script with scenes, dialogue, and descriptions.";

            JSONObject payload = new JSONObject();
            payload.put("inputs", scriptPrompt);
            payload.put("parameters", new JSONObject()
                        .put("max_new_tokens", 500)
                        .put("temperature", 0.3));

            // Get default text model
            String modelEndpoint = getDefaultModelEndpoint("text");

            // Make request
            JSONObject response = hfClient.requestModel(modelEndpoint, "", payload);

            if (response != null && response.has("0")) {
                JSONArray generatedText = response.getJSONArray("0");
                if (generatedText.length() > 0 && generatedText.getJSONObject(0).has("generated_text")) {
                    String scriptText = generatedText.getJSONObject(0).getString("generated_text");

                    // Create script object
                    JSONObject scriptObj = new JSONObject();
                    scriptObj.put("text", scriptText);
                    scriptObj.put("scenes", extractScenes(scriptText));
                    scriptObj.put("dialogue", extractDialogue(scriptText));

                    // Save script to file
                    String projectId = getCurrentProjectId();
                    if (projectId != null) {
                        File scriptFile = new File(StoragePaths.getProjectsDir() + "/" + projectId + "/script.txt");
                        FileUtils.writeToFile(scriptFile, scriptText);
                    }

                    step.put("success", true);
                    step.put("script", scriptObj);
                    step.put("message", "Script generated successfully");
                }
            } else {
                step.put("message", "Failed to get response from model");
            }

        } catch (JSONException e) {
            logger.log("OrchestratorAgent", "Error generating script: " + e.getMessage());
            try {
                step.put("message", "Error generating script: " + e.getMessage());
            } catch (JSONException ex) {
                // Ignore
            }
        }

        return step;
    }

    private JSONObject generateImages(JSONObject script) {
        JSONObject step = new JSONObject();

        try {
            step.put("step_id", "generate_images");
            step.put("success", false);

            // Extract image prompts from script
            List<String> imagePrompts = new ArrayList<>();
            if (script != null && script.has("scenes")) {
                JSONArray scenes = script.getJSONArray("scenes");
                for (int i = 0; i < scenes.length(); i++) {
                    JSONObject scene = scenes.getJSONObject(i);
                    String description = scene.optString("description", "");
                    if (!description.isEmpty()) {
                        imagePrompts.add(description);
                    }
                }
            }

            // Generate images for each prompt
            JSONArray generatedImages = new JSONArray();
            for (int i = 0; i < imagePrompts.size(); i++) {
                String prompt = imagePrompts.get(i);

                // Create image generation payload
                JSONObject payload = new JSONObject();
                payload.put("inputs", prompt);

                // Get default image model
                String modelEndpoint = getDefaultModelEndpoint("image");

                // Make request
                JSONObject response = hfClient.requestModel(modelEndpoint, "", payload);

                if (response != null) {
                    // In a real implementation, this would handle the image data
                    // For now, we'll just create a placeholder
                    JSONObject imageInfo = new JSONObject();
                    imageInfo.put("prompt", prompt);
                    imageInfo.put("index", i);
                    imageInfo.put("generated", true);
                    generatedImages.put(imageInfo);
                }
            }

            step.put("success", true);
            step.put("images", generatedImages);
            step.put("message", "Images generated successfully");

        } catch (JSONException e) {
            logger.log("OrchestratorAgent", "Error generating images: " + e.getMessage());
            try {
                step.put("message", "Error generating images: " + e.getMessage());
            } catch (JSONException ex) {
                // Ignore
            }
        }

        return step;
    }

    private JSONObject generateAudio(JSONObject script) {
        JSONObject step = new JSONObject();

        try {
            step.put("step_id", "generate_audio");
            step.put("success", false);

            // Extract dialogue from script
            List<String> dialogueLines = new ArrayList<>();
            if (script != null && script.has("dialogue")) {
                JSONArray dialogue = script.getJSONArray("dialogue");
                for (int i = 0; i < dialogue.length(); i++) {
                    String line = dialogue.getString(i);
                    if (!line.isEmpty()) {
                        dialogueLines.add(line);
                    }
                }
            }

            // Generate audio for each dialogue line
            JSONArray generatedAudio = new JSONArray();
            for (int i = 0; i < dialogueLines.size(); i++) {
                String line = dialogueLines.get(i);

                // Create TTS payload
                JSONObject payload = new JSONObject();
                payload.put("inputs", line);

                // Get default audio model
                String modelEndpoint = getDefaultModelEndpoint("audio");

                // Make request
                JSONObject response = hfClient.requestModel(modelEndpoint, "", payload);

                if (response != null) {
                    // In a real implementation, this would handle the audio data
                    // For now, we'll just create a placeholder
                    JSONObject audioInfo = new JSONObject();
                    audioInfo.put("text", line);
                    audioInfo.put("index", i);
                    audioInfo.put("generated", true);
                    generatedAudio.put(audioInfo);
                }
            }

            step.put("success", true);
            step.put("audio", generatedAudio);
            step.put("message", "Audio generated successfully");

        } catch (JSONException e) {
            logger.log("OrchestratorAgent", "Error generating audio: " + e.getMessage());
            try {
                step.put("message", "Error generating audio: " + e.getMessage());
            } catch (JSONException ex) {
                // Ignore
            }
        }

        return step;
    }

    private JSONObject assembleVideo(String projectId) {
        JSONObject step = new JSONObject();

        try {
            step.put("step_id", "assemble_video");
            step.put("success", false);

            // In a real implementation, this would use FFmpeg to assemble the video
            // For now, we'll just create a placeholder

            // Create placeholder for final video
            File projectDir = new File(StoragePaths.getProjectsDir() + "/" + projectId);
            File finalVideo = new File(projectDir, "final_video.mp4");

            // Simulate video assembly
            Thread.sleep(2000);

            step.put("success", true);
            step.put("output_path", finalVideo.getAbsolutePath());
            step.put("message", "Video assembled successfully");

        } catch (JSONException e) {
            logger.log("OrchestratorAgent", "Error assembling video: " + e.getMessage());
            try {
                step.put("message", "Error assembling video: " + e.getMessage());
            } catch (JSONException ex) {
                // Ignore
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            try {
                step.put("message", "Video assembly interrupted");
            } catch (JSONException ex) {
                // Ignore
            }
        }

        return step;
    }

    private String getDefaultModelEndpoint(String type) {
        // This would normally read from the models configuration
        // For now, we'll return default endpoints

        switch (type) {
            case "text":
                return "https://api-inference.huggingface.co/models/microsoft/DialoGPT-medium";
            case "image":
                return "https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-2";
            case "audio":
                return "https://api-inference.huggingface.co/models/facebook/musicgen-small";
            default:
                return "https://api-inference.huggingface.co/models/microsoft/DialoGPT-medium";
        }
    }

    private String getCurrentProjectId() {
        // This would normally get the current project ID from somewhere
        // For now, we'll return a placeholder
        return "current_project";
    }

    private List<String> extractKeywords(String text) {
        // Simplified keyword extraction
        List<String> keywords = new ArrayList<>();

        // In a real implementation, this would use NLP techniques
        // For now, we'll just split by common words
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (word.length() > 4) {
                keywords.add(word);
            }
        }

        return keywords;
    }

    private JSONArray extractScenes(String scriptText) {
        JSONArray scenes = new JSONArray();

        // Simplified scene extraction
        String[] lines = scriptText.split("\n");
        for (String line : lines) {
            if (line.startsWith("SCENE:") || line.startsWith("Scene:")) {
                try {
                    JSONObject scene = new JSONObject();
                    scene.put("description", line.substring(6).trim());
                    scenes.put(scene);
                } catch (JSONException e) {
                    // Ignore
                }
            }
        }

        return scenes;
    }

    private JSONArray extractDialogue(String scriptText) {
        JSONArray dialogue = new JSONArray();

        // Simplified dialogue extraction
        String[] lines = scriptText.split("\n");
        for (String line : lines) {
            if (line.contains(":") && !line.startsWith("SCENE:") && !line.startsWith("Scene:")) {
                dialogue.put(line.trim());
            }
        }

        return dialogue;
    }

    private void logResult(JSONObject result) {
        try {
            String projectId = result.getString("project_id");
            File logFile = new File(StoragePaths.getAgentResultsDir() + "/OrchestratorAgent/orchestrator_" + projectId + ".json");
            JSONLogger.writeToFile(logFile, result.toString());
        } catch (JSONException e) {
            logger.log("OrchestratorAgent", "Error logging result: " + e.getMessage());
        }
    }
}
