package com.ai_autocreate.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ai_autocreate.R;
import com.ai_autocreate.activities.VideoReimaginerActivity;
import com.ai_autocreate.activities.ScriptStudioActivity;
import com.ai_autocreate.activities.ImageToStoryActivity;
import com.ai_autocreate.activities.AudioReconstructorActivity;
import com.ai_autocreate.agents.OrchestratorAgent;
import com.ai_autocreate.agents.SanityCheckAgent;
import com.ai_autocreate.utils.JSONLogger;
import com.ai_autocreate.utils.StoragePaths;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class HomeFragment extends Fragment {
    private EditText promptEditText;
    private Button generateButton;
    private Button videoReimaginerButton;
    private Button scriptStudioButton;
    private Button imageToStoryButton;
    private Button audioReconstructorButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private SwipeRefreshLayout swipeRefreshLayout;

    private JSONLogger logger;
    private OrchestratorAgent orchestratorAgent;
    private SanityCheckAgent sanityCheckAgent;
    private String currentProjectId;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        promptEditText = view.findViewById(R.id.prompt_edit_text);
        generateButton = view.findViewById(R.id.generate_button);
        videoReimaginerButton = view.findViewById(R.id.video_reimaginer_button);
        scriptStudioButton = view.findViewById(R.id.script_studio_button);
        imageToStoryButton = view.findViewById(R.id.image_to_story_button);
        audioReconstructorButton = view.findViewById(R.id.audio_reconstructor_button);
        progressBar = view.findViewById(R.id.progress_bar);
        statusText = view.findViewById(R.id.status_text);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        // Initialize agents and utilities
        logger = new JSONLogger(getActivity());
        orchestratorAgent = new OrchestratorAgent(getActivity());
        sanityCheckAgent = new SanityCheckAgent(getActivity());

        // Setup listeners
        setupListeners();

        // Create new project ID
        createNewProjectId();

        return view;
    }

    private void setupListeners() {
        generateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String prompt = promptEditText.getText().toString().trim();

                    if (prompt.isEmpty()) {
                        Toast.makeText(getActivity(), R.string.please_enter_prompt, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Start generation process
                    new GenerateContentTask().execute(prompt);
                }
            });

        videoReimaginerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), VideoReimaginerActivity.class);
                    intent.putExtra("project_id", currentProjectId);
                    startActivity(intent);
                }
            });

        scriptStudioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), ScriptStudioActivity.class);
                    intent.putExtra("project_id", currentProjectId);
                    startActivity(intent);
                }
            });

        imageToStoryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), ImageToStoryActivity.class);
                    intent.putExtra("project_id", currentProjectId);
                    startActivity(intent);
                }
            });

        audioReconstructorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), AudioReconstructorActivity.class);
                    intent.putExtra("project_id", currentProjectId);
                    startActivity(intent);
                }
            });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    // Refresh content
                    createNewProjectId();
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
    }

    private void createNewProjectId() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        Random random = new Random();
        int rand = random.nextInt(1000);

        currentProjectId = "proj_" + timestamp + "_" + rand;

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
        logger.log("HomeFragment", "Created new project: " + currentProjectId);
    }

    // داخل HomeFragment — استبدل تعريف GenerateContentTask و generateContent بالدور التالي:

    private class GenerateContentTask extends AsyncTask<String, Integer, Boolean> {
        private String errorMessage;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            generateButton.setEnabled(false);
            statusText.setText(R.string.initializing);
            progressBar.setProgress(0);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String prompt = params[0];

            try {
                // Step 1: Run sanity check
                publishProgress(10);               // UI: 10%
                // Avoid direct UI calls here (use publishProgress instead)
                JSONObject sanityResult = sanityCheckAgent.runCheck();
                if (!sanityResult.optBoolean("passed", false)) {
                    errorMessage = sanityResult.optString("message", "Sanity check failed");
                    return false;
                }

                // Step 2: Create project configuration
                publishProgress(20);               // UI: 20%
                JSONObject projectConfig = new JSONObject();
                projectConfig.put("project_id", currentProjectId);
                projectConfig.put("title", prompt.substring(0, Math.min(prompt.length(), 50)));
                projectConfig.put("prompt", prompt);
                projectConfig.put("workflowType", "auto_generate");
                projectConfig.put("created_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));

                // Save project config
                File configFile = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId + "/project.json");
                JSONLogger.writeToFile(configFile, projectConfig.toString());

                // Step 3: Run orchestrator agent
                publishProgress(30);               // UI: 30%
                JSONObject orchestratorInput = new JSONObject();
                orchestratorInput.put("project_id", currentProjectId);
                orchestratorInput.put("prompt", prompt);
                orchestratorInput.put("project_config", projectConfig);

                JSONObject orchestratorResult = orchestratorAgent.process(orchestratorInput);

                if (orchestratorResult == null || !orchestratorResult.optBoolean("success", false)) {
                    errorMessage = orchestratorResult != null ?
                        orchestratorResult.optString("message", "Orchestrator failed") :
                        "Orchestrator returned null";
                    return false;
                }

                // Step 4: Generate content — perform simulated work here and publish progress from the AsyncTask
                // Simulate content generation with progress updates (this loop must be in doInBackground)
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(1000); // Simulate work
                    int prog = 60 + (i * 8); // produce 60,68,76,84,92 (for example)
                    publishProgress(prog);
                }

                // Now call generateContent which performs non-UI file work only
                boolean contentGenerated = generateContent(orchestratorResult);

                if (!contentGenerated) {
                    errorMessage = "Failed to generate content";
                    return false;
                }

                // Step 5: Finalize project
                publishProgress(95);
                projectConfig.put("status", "completed");
                projectConfig.put("completed_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
                JSONLogger.writeToFile(configFile, projectConfig.toString());

                publishProgress(100);
                return true;

            } catch (Exception e) {
                errorMessage = "Error generating content: " + e.getMessage();
                logger.log("HomeFragment", errorMessage);
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values != null && values.length > 0) {
                int p = values[0];
                progressBar.setProgress(p);
                // Update a friendly status message according to progress value
                if (p < 20) {
                    statusText.setText(R.string.initializing);
                } else if (p < 40) {
                    statusText.setText(R.string.running_sanity_check);
                } else if (p < 60) {
                    statusText.setText(R.string.creating_project_config);
                } else if (p < 90) {
                    statusText.setText(R.string.generating_content);
                } else {
                    statusText.setText(R.string.finalizing_project);
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);
            generateButton.setEnabled(true);

            if (success) {
                statusText.setText(R.string.content_generated_successfully);
                showSuccessDialog();
            } else {
                statusText.setText(R.string.content_generation_failed);
                showErrorDialog(errorMessage);
            }
        }
    }

// جعل generateContent لا تتحكم بالـ UI أو تستدعي publishProgress
    private boolean generateContent(JSONObject orchestratorResult) {
        try {
            // Do file-system work and agent calls that don't touch UI
            File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);

            // Create script file
            File scriptFile = new File(projectDir, "script.txt");
            JSONLogger.writeToFile(scriptFile, "Generated script content based on: " +
                                   orchestratorResult.optString("prompt", ""));

            // Placeholder for final video
            File finalVideo = new File(projectDir, "final_video.mp4");
            // (real implementation would write actual content)

            return true;
        } catch (Exception e) {
            logger.log("HomeFragment", "Error in generateContent: " + e.getMessage());
            return false;
        }
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.success)
            .setMessage(R.string.content_generated_successfully)
            .setPositiveButton(R.string.view_project, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Navigate to project viewer or editor
                    // For now, just show a toast
                    Toast.makeText(getActivity(), "Project: " + currentProjectId, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.create_new, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Create new project
                    createNewProjectId();
                    promptEditText.setText("");
                }
            })
            .show();
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.error)
            .setMessage(message)
            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Retry generation
                    String prompt = promptEditText.getText().toString().trim();
                    if (!prompt.isEmpty()) {
                        new GenerateContentTask().execute(prompt);
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
}
