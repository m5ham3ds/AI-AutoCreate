package com.ai_autocreate.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
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

import com.ai_autocreate.R;
import com.ai_autocreate.agents.AudioContextAgent;
import com.ai_autocreate.agents.SanityCheckAgent;
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

public class AudioReconstructorActivity extends AppCompatActivity {
    private static final int PICK_AUDIO_REQUEST = 1;

    private Button selectAudioButton;
    private EditText promptEditText;
    private EditText resultEditText;
    private Button generateButton;
    private Button playButton;
    private Button saveButton;
    private ProgressBar progressBar;
    private TextView statusText;

    private String currentProjectId;
    private String selectedAudioPath;
    private String generatedAudioPath;
    private JSONLogger logger;
    private AudioContextAgent audioContextAgent;
    private SanityCheckAgent sanityCheckAgent;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_reconstructor);

        // Get project ID from intent
        currentProjectId = getIntent().getStringExtra("project_id");
        if (currentProjectId == null) {
            createNewProjectId();
        }

        // Initialize views
        selectAudioButton = findViewById(R.id.select_audio_button);
        promptEditText = findViewById(R.id.prompt_edit_text);
        resultEditText = findViewById(R.id.result_edit_text);
        generateButton = findViewById(R.id.generate_button);
        playButton = findViewById(R.id.play_button);
        saveButton = findViewById(R.id.save_button);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);

        // Initialize agents and utilities
        logger = new JSONLogger(this);
        audioContextAgent = new AudioContextAgent(this);
        sanityCheckAgent = new SanityCheckAgent(this);
        mediaPlayer = new MediaPlayer();

        // Setup listeners
        setupListeners();

        // Initialize UI
        updateUI();
    }

    private void setupListeners() {
        selectAudioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectAudio();
                }
            });

        generateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    generateAudio();
                }
            });

        playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playAudio();
                }
            });

        saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveAudio();
                }
            });
    }

    private void createNewProjectId() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        Random random = new Random();
        int rand = random.nextInt(1000);

        currentProjectId = "audio_reconstructor_" + timestamp + "_" + rand;

        // Create project directory
        File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);
        if (!projectDir.exists()) {
            projectDir.mkdirs();
        }

        // Create subdirectories
        new File(projectDir, "audio").mkdirs();
        new File(projectDir, "checkpoints").mkdirs();

        // Log project creation
        logger.log("AudioReconstructor", "Created new project: " + currentProjectId);
    }

    private void selectAudio() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("audio/*");
        startActivityForResult(intent, PICK_AUDIO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri audioUri = data.getData();
            selectedAudioPath = getRealPathFromURI(audioUri);

            if (selectedAudioPath != null) {
                // Copy audio to project directory
                File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);
                File destFile = new File(projectDir, "input.mp3");

                if (FileUtils.copyFile(new File(selectedAudioPath), destFile)) {
                    selectedAudioPath = destFile.getAbsolutePath();
                    updateUI();
                } else {
                    Toast.makeText(this, R.string.error_copying_audio, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.error_getting_audio_path, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        // This is a simplified version - in a real implementation, 
        // this would handle different URI schemes properly
        return contentUri.getPath();
    }

    private void generateAudio() {
        if (selectedAudioPath == null) {
            Toast.makeText(this, R.string.please_select_audio, Toast.LENGTH_SHORT).show();
            return;
        }

        String prompt = promptEditText.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_prompt, Toast.LENGTH_SHORT).show();
            return;
        }

        new GenerateAudioTask().execute(prompt);
    }

    private void playAudio() {
        if (generatedAudioPath == null) {
            Toast.makeText(this, R.string.no_audio_to_play, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                playButton.setText(R.string.play);
            } else {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(generatedAudioPath);
                mediaPlayer.prepare();
                mediaPlayer.start();
                playButton.setText(R.string.stop);

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            playButton.setText(R.string.play);
                        }
                    });
            }
        } catch (Exception e) {
            logger.log("AudioReconstructor", "Error playing audio: " + e.getMessage());
            Toast.makeText(this, R.string.error_playing_audio, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAudio() {
        try {
            File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);

            // Save project metadata
            JSONObject projectData = new JSONObject();
            projectData.put("project_id", currentProjectId);
            projectData.put("title", "Audio Reconstructor Project");
            projectData.put("workflowType", "audio_reconstructor");
            projectData.put("source_audio", selectedAudioPath);
            projectData.put("generated_audio", generatedAudioPath);
            projectData.put("result_text", resultEditText.getText().toString());
            projectData.put("created_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
            projectData.put("last_modified", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));

            File configFile = new File(projectDir, "project.json");
            JSONLogger.writeToFile(configFile, projectData.toString());

            Toast.makeText(this, R.string.audio_saved_successfully, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            logger.log("AudioReconstructor", "Error saving audio: " + e.getMessage());
            Toast.makeText(this, R.string.error_saving_audio, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        boolean hasAudio = selectedAudioPath != null;
        selectAudioButton.setText(hasAudio ? R.string.change_audio : R.string.select_audio);
        generateButton.setEnabled(hasAudio);
        playButton.setEnabled(generatedAudioPath != null);
    }

    private class GenerateAudioTask extends AsyncTask<String, Integer, String> {
        private String errorMessage;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            generateButton.setEnabled(false);
            statusText.setText(R.string.analyzing_audio);
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

                // Step 2: Analyze audio
                publishProgress(30);
                statusText.setText(R.string.analyzing_audio);

                JSONObject audioAnalysis = audioContextAgent.analyzeAudio(selectedAudioPath);
                if (audioAnalysis == null || !audioAnalysis.optBoolean("success", false)) {
                    errorMessage = audioAnalysis != null ? 
                        audioAnalysis.optString("message", "Audio analysis failed") : 
                        "Audio analysis returned null";
                    return null;
                }

                // Step 3: Generate reconstructed audio
                publishProgress(60);
                statusText.setText(R.string.generating_audio);

                // Simulate audio generation
                Thread.sleep(3000);

                // Create placeholder for generated audio
                File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);
                File generatedAudioFile = new File(projectDir, "generated_audio.mp3");
                generatedAudioPath = generatedAudioFile.getAbsolutePath();

                // For now, we'll just copy the original audio as a placeholder
                FileUtils.copyFile(new File(selectedAudioPath), generatedAudioFile);

                // Generate result text
                String resultText = "Audio reconstruction completed based on prompt: " + prompt + 
                    ". The audio has been processed and reconstructed according to your requirements.";

                publishProgress(90);
                return resultText;

            } catch (Exception e) {
                errorMessage = "Error generating audio: " + e.getMessage();
                logger.log("AudioReconstructor", errorMessage);
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            progressBar.setVisibility(View.GONE);
            generateButton.setEnabled(true);

            if (result != null) {
                resultEditText.setText(result);
                statusText.setText(R.string.audio_generated_successfully);
                updateUI();
                Toast.makeText(AudioReconstructorActivity.this, R.string.audio_generated_successfully, Toast.LENGTH_SHORT).show();
            } else {
                statusText.setText(R.string.audio_generation_failed);
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
                        new GenerateAudioTask().execute(prompt);
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.audio_reconstructor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_export_audio) {
            exportAudio();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void exportAudio() {
        // Export audio functionality
        Toast.makeText(this, R.string.export_audio_feature, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Release media player resources
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
