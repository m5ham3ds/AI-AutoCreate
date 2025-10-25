package com.ai_autocreate.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ai_autocreate.R;
import com.ai_autocreate.agents.ImageInterpreterAgent;
import com.ai_autocreate.agents.SanityCheckAgent;
import com.ai_autocreate.utils.JSONLogger;
import com.ai_autocreate.utils.MediaUtils;
import com.ai_autocreate.utils.StoragePaths;
import com.ai_autocreate.utils.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class ImageToStoryActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView imageView;
    private Button selectImageButton;
    private EditText promptEditText;
    private EditText storyEditText;
    private Button generateButton;
    private Button saveButton;
    private ProgressBar progressBar;
    private TextView statusText;

    private String currentProjectId;
    private String selectedImagePath;
    private JSONLogger logger;
    private ImageInterpreterAgent imageInterpreterAgent;
    private SanityCheckAgent sanityCheckAgent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_to_story);

        // Get project ID from intent
        currentProjectId = getIntent().getStringExtra("project_id");
        if (currentProjectId == null) {
            createNewProjectId();
        }

        // Initialize views
        imageView = findViewById(R.id.image_view);
        selectImageButton = findViewById(R.id.select_image_button);
        promptEditText = findViewById(R.id.prompt_edit_text);
        storyEditText = findViewById(R.id.story_edit_text);
        generateButton = findViewById(R.id.generate_button);
        saveButton = findViewById(R.id.save_button);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);

        // Initialize agents and utilities
        logger = new JSONLogger(this);
        imageInterpreterAgent = new ImageInterpreterAgent(this);
        sanityCheckAgent = new SanityCheckAgent(this);

        // Setup listeners
        setupListeners();

        // Initialize UI
        updateUI();
    }

    private void setupListeners() {
        selectImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectImage();
                }
            });

        generateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    generateStory();
                }
            });

        saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveStory();
                }
            });
    }

    private void createNewProjectId() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        Random random = new Random();
        int rand = random.nextInt(1000);

        currentProjectId = "image_to_story_" + timestamp + "_" + rand;

        // Create project directory
        File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);
        if (!projectDir.exists()) {
            projectDir.mkdirs();
        }

        // Create subdirectories
        new File(projectDir, "images").mkdirs();
        new File(projectDir, "stories").mkdirs();
        new File(projectDir, "checkpoints").mkdirs();

        // Log project creation
        logger.log("ImageToStory", "Created new project: " + currentProjectId);
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            selectedImagePath = getRealPathFromURI(imageUri);

            if (selectedImagePath != null) {
                // Copy image to project directory
                File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);
                File destFile = new File(projectDir, "input.jpg");

                if (FileUtils.copyFile(new File(selectedImagePath), destFile)) {
                    selectedImagePath = destFile.getAbsolutePath();

                    // Load image in ImageView
                    Bitmap bitmap = MediaUtils.createImageThumbnail(selectedImagePath, 300, 300);
                    imageView.setImageBitmap(bitmap);

                    updateUI();
                } else {
                    Toast.makeText(this, R.string.error_copying_image, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.error_getting_image_path, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        // This is a simplified version - in a real implementation, 
        // this would handle different URI schemes properly
        return contentUri.getPath();
    }

    private void generateStory() {
        if (selectedImagePath == null) {
            Toast.makeText(this, R.string.please_select_image, Toast.LENGTH_SHORT).show();
            return;
        }

        String prompt = promptEditText.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_prompt, Toast.LENGTH_SHORT).show();
            return;
        }

        new GenerateStoryTask().execute(prompt);
    }

    private void saveStory() {
        try {
            String storyContent = storyEditText.getText().toString();
            if (storyContent.isEmpty()) {
                Toast.makeText(this, R.string.story_is_empty, Toast.LENGTH_SHORT).show();
                return;
            }

            File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);
            File storyFile = new File(projectDir, "story.txt");

            JSONLogger.writeToFile(storyFile, storyContent);

            // Save project metadata
            JSONObject projectData = new JSONObject();
            projectData.put("project_id", currentProjectId);
            projectData.put("title", "Image to Story Project");
            projectData.put("workflowType", "image_to_story");
            projectData.put("source_image", selectedImagePath);
            projectData.put("created_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
            projectData.put("last_modified", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));

            File configFile = new File(projectDir, "project.json");
            JSONLogger.writeToFile(configFile, projectData.toString());

            Toast.makeText(this, R.string.story_saved_successfully, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            logger.log("ImageToStory", "Error saving story: " + e.getMessage());
            Toast.makeText(this, R.string.error_saving_story, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        boolean hasImage = selectedImagePath != null;
        selectImageButton.setText(hasImage ? R.string.change_image : R.string.select_image);
        generateButton.setEnabled(hasImage);
    }

    private class GenerateStoryTask extends AsyncTask<String, Integer, String> {
        private String errorMessage;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            generateButton.setEnabled(false);
            statusText.setText(R.string.analyzing_image);
        }

        @Override
        protected String doInBackground(String... params) {
            String prompt = params[0];

            try {
                // Step 1: Run sanity check
                publishProgress(10);
                JSONObject sanityResult = sanityCheckAgent.runCheck();
                if (!sanityResult.optBoolean("passed", false)) {
                    errorMessage = sanityResult.optString("message", "Sanity check failed");
                    return null;
                }

                // Step 2: Analyze image
                publishProgress(30);
                statusText.setText(R.string.analyzing_image);

                JSONObject imageAnalysis = imageInterpreterAgent.analyzeImage(selectedImagePath);
                if (imageAnalysis == null || !imageAnalysis.optBoolean("success", false)) {
                    errorMessage = imageAnalysis != null ? 
                        imageAnalysis.optString("message", "Image analysis failed") : 
                        "Image analysis returned null";
                    return null;
                }

                // Step 3: Generate story
                publishProgress(60);
                statusText.setText(R.string.generating_story);

                // Simulate story generation
                Thread.sleep(2000);

                // For now, we'll create a simple story based on the prompt
                String story = "Once upon a time, " + prompt + ". This is a generated story based on the image and prompt provided. The story would normally be generated by an AI model based on the image analysis and user prompt.";

                publishProgress(90);
                return story;

            } catch (Exception e) {
                errorMessage = "Error generating story: " + e.getMessage();
                logger.log("ImageToStory", errorMessage);
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String story) {
            progressBar.setVisibility(View.GONE);
            generateButton.setEnabled(true);

            if (story != null) {
                storyEditText.setText(story);
                statusText.setText(R.string.story_generated_successfully);
                Toast.makeText(ImageToStoryActivity.this, R.string.story_generated_successfully, Toast.LENGTH_SHORT).show();
            } else {
                statusText.setText(R.string.story_generation_failed);
                showErrorDialog(errorMessage);
            }
        }
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.error)
            .setMessage(message)
            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String prompt = promptEditText.getText().toString().trim();
                    if (!prompt.isEmpty()) {
                        new GenerateStoryTask().execute(prompt);
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_to_story, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_export_story) {
            exportStory();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void exportStory() {
        // Export story functionality
        Toast.makeText(this, R.string.export_story_feature, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up bitmap resources
        if (imageView != null) {
            imageView.setImageDrawable(null);
        }
    }
}
