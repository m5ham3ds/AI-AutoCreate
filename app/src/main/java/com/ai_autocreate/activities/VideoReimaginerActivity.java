package com.ai_autocreate.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.ai_autocreate.R;
import com.ai_autocreate.agents.OrchestratorAgent;
import com.ai_autocreate.agents.SanityCheckAgent;
import com.ai_autocreate.agents.VideoAnalyzerAgent;
import com.ai_autocreate.utils.JSONLogger;
import com.ai_autocreate.utils.StoragePaths;
import com.ai_autocreate.utils.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class VideoReimaginerActivity extends AppCompatActivity {
    private static final int PICK_VIDEO_REQUEST = 1;

    private VideoView videoView;
    private Button selectVideoButton;
    private EditText promptEditText;
    private Button processButton;
    private ProgressBar progressBar;
    private TextView statusText;

    private String currentProjectId;
    private String selectedVideoPath;
    private JSONLogger logger;
    private VideoAnalyzerAgent videoAnalyzerAgent;
    private OrchestratorAgent orchestratorAgent;
    private SanityCheckAgent sanityCheckAgent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_reimaginer);

        // Get project ID from intent
        currentProjectId = getIntent().getStringExtra("project_id");
        if (currentProjectId == null) {
            createNewProjectId();
        }

        // Initialize views
        videoView = findViewById(R.id.video_view);
        selectVideoButton = findViewById(R.id.select_video_button);
        promptEditText = findViewById(R.id.prompt_edit_text);
        processButton = findViewById(R.id.process_button);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);

        // Initialize agents and utilities
        logger = new JSONLogger(this);
        videoAnalyzerAgent = new VideoAnalyzerAgent(this);
        orchestratorAgent = new OrchestratorAgent(this);
        sanityCheckAgent = new SanityCheckAgent(this);

        // Setup listeners
        setupListeners();

        // Initialize UI
        updateUI();
    }

    private void setupListeners() {
        selectVideoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectVideo();
                }
            });

        processButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    processVideo();
                }
            });
    }

    private void createNewProjectId() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        Random random = new Random();
        int rand = random.nextInt(1000);

        currentProjectId = "video_reimaginer_" + timestamp + "_" + rand;

        // Create project directory
        File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);
        if (!projectDir.exists()) {
            projectDir.mkdirs();
        }

        // Create subdirectories
        new File(projectDir, "frames").mkdirs();
        new File(projectDir, "audio").mkdirs();
        new File(projectDir, "checkpoints").mkdirs();

        // Log project creation
        logger.log("VideoReimaginer", "Created new project: " + currentProjectId);
    }

    private void selectVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        startActivityForResult(intent, PICK_VIDEO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri videoUri = data.getData();
            selectedVideoPath = getRealPathFromURI(videoUri);

            if (selectedVideoPath != null) {
                // Copy video to project directory
                File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);
                File destFile = new File(projectDir, "input.mp4");

                if (FileUtils.copyFile(new File(selectedVideoPath), destFile)) {
                    selectedVideoPath = destFile.getAbsolutePath();

                    // Load video in VideoView
                    videoView.setVideoURI(Uri.parse(selectedVideoPath));
                    videoView.start();

                    updateUI();
                } else {
                    Toast.makeText(this, R.string.error_copying_video, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.error_getting_video_path, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        // This is a simplified version - in a real implementation, 
        // this would handle different URI schemes properly
        return contentUri.getPath();
    }

    private void processVideo() {
        if (selectedVideoPath == null) {
            Toast.makeText(this, R.string.please_select_video, Toast.LENGTH_SHORT).show();
            return;
        }

        String prompt = promptEditText.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_prompt, Toast.LENGTH_SHORT).show();
            return;
        }

        new ProcessVideoTask().execute(prompt);
    }

    private void updateUI() {
        boolean hasVideo = selectedVideoPath != null;
        selectVideoButton.setText(hasVideo ? R.string.change_video : R.string.select_video);
        processButton.setEnabled(hasVideo);
    }

    private class ProcessVideoTask extends AsyncTask<String, Integer, Boolean> {
        private String errorMessage;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            processButton.setEnabled(false);
            statusText.setText(R.string.initializing);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String prompt = params[0];

            try {
                // Step 1: Run sanity check
                publishProgress(10);
                statusText.setText(R.string.running_sanity_check);

                JSONObject sanityResult = sanityCheckAgent.runCheck();
                if (!sanityResult.optBoolean("passed", false)) {
                    errorMessage = sanityResult.optString("message", "Sanity check failed");
                    return false;
                }

                // Step 2: Analyze video
                publishProgress(20);
                statusText.setText(R.string.analyzing_video);

                JSONObject videoAnalysis = videoAnalyzerAgent.analyzeVideo(selectedVideoPath);
                if (videoAnalysis == null || !videoAnalysis.optBoolean("success", false)) {
                    errorMessage = videoAnalysis != null ? 
                        videoAnalysis.optString("message", "Video analysis failed") : 
                        "Video analysis returned null";
                    return false;
                }

                // Step 3: Create project configuration
                publishProgress(30);
                statusText.setText(R.string.creating_project_config);

                JSONObject projectConfig = new JSONObject();
                projectConfig.put("project_id", currentProjectId);
                projectConfig.put("title", prompt.substring(0, Math.min(prompt.length(), 50)));
                projectConfig.put("prompt", prompt);
                projectConfig.put("workflowType", "video_reimagine");
                projectConfig.put("source_video", selectedVideoPath);
                projectConfig.put("video_analysis", videoAnalysis);
                projectConfig.put("created_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));

                // Save project config
                File configFile = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId + "/project.json");
                JSONLogger.writeToFile(configFile, projectConfig.toString());

                // Step 4: Run orchestrator agent
                publishProgress(40);
                statusText.setText(R.string.running_orchestrator);

                JSONObject orchestratorInput = new JSONObject();
                orchestratorInput.put("project_id", currentProjectId);
                orchestratorInput.put("prompt", prompt);
                orchestratorInput.put("project_config", projectConfig);
                orchestratorInput.put("video_analysis", videoAnalysis);

                JSONObject orchestratorResult = orchestratorAgent.process(orchestratorInput);

                if (orchestratorResult == null || !orchestratorResult.optBoolean("success", false)) {
                    errorMessage = orchestratorResult != null ? 
                        orchestratorResult.optString("message", "Orchestrator failed") : 
                        "Orchestrator returned null";
                    return false;
                }

                // Step 5: Generate reimagined video
                publishProgress(60);
                statusText.setText(R.string.generating_reimagined_video);

                // Simulate video generation with progress updates
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(1000); // Simulate work
                    publishProgress(60 + (i * 6));
                }

                // Create placeholder for final video
                File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);
                File finalVideo = new File(projectDir, "final_reimagined.mp4");

                // Step 6: Finalize project
                publishProgress(90);
                statusText.setText(R.string.finalizing_project);

                // Update project config with results
                projectConfig.put("status", "completed");
                projectConfig.put("completed_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
                projectConfig.put("final_video", finalVideo.getAbsolutePath());

                JSONLogger.writeToFile(configFile, projectConfig.toString());

                publishProgress(100);
                return true;

            } catch (Exception e) {
                errorMessage = "Error processing video: " + e.getMessage();
                logger.log("VideoReimaginer", errorMessage);
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);
            processButton.setEnabled(true);

            if (success) {
                statusText.setText(R.string.video_processed_successfully);

                // Show success dialog
                showSuccessDialog();
            } else {
                statusText.setText(R.string.video_processing_failed);

                // Show error dialog
                showErrorDialog(errorMessage);
            }
        }
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.success)
            .setMessage(R.string.video_processed_successfully)
            .setPositiveButton(R.string.view_result, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // View the processed video
                    File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);
                    File finalVideo = new File(projectDir, "final_reimagined.mp4");

                    if (finalVideo.exists()) {
                        videoView.setVideoURI(Uri.parse(finalVideo.getAbsolutePath()));
                        videoView.start();
                    } else {
                        Toast.makeText(VideoReimaginerActivity.this, R.string.error_loading_video, Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton(R.string.close, null)
            .show();
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.error)
            .setMessage(message)
            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Retry processing
                    String prompt = promptEditText.getText().toString().trim();
                    if (!prompt.isEmpty()) {
                        new ProcessVideoTask().execute(prompt);
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.video_reimaginer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_save_project) {
            // Save project
            Toast.makeText(this, R.string.project_saved, Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Release video resources
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
}
