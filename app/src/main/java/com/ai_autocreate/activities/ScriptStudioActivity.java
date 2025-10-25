package com.ai_autocreate.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import com.ai_autocreate.agents.ScriptGeneratorAgent;
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

public class ScriptStudioActivity extends AppCompatActivity {
    private EditText scriptEditText;
    private EditText promptEditText;
    private Button generateButton;
    private Button saveButton;
    private Button loadButton;
    private ProgressBar progressBar;
    private TextView statusText;

    private String currentProjectId;
    private JSONLogger logger;
    private ScriptGeneratorAgent scriptGeneratorAgent;
    private SanityCheckAgent sanityCheckAgent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_studio);

        // Get project ID from intent
        currentProjectId = getIntent().getStringExtra("project_id");
        if (currentProjectId == null) {
            createNewProjectId();
        }

        // Initialize views
        scriptEditText = findViewById(R.id.script_edit_text);
        promptEditText = findViewById(R.id.prompt_edit_text);
        generateButton = findViewById(R.id.generate_button);
        saveButton = findViewById(R.id.save_button);
        loadButton = findViewById(R.id.load_button);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);

        // Initialize agents and utilities
        logger = new JSONLogger(this);
        scriptGeneratorAgent = new ScriptGeneratorAgent(this);
        sanityCheckAgent = new SanityCheckAgent(this);

        // Setup listeners
        setupListeners();

        // Load existing script if available
        loadExistingScript();
    }

    private void setupListeners() {
        generateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String prompt = promptEditText.getText().toString().trim();
                    if (prompt.isEmpty()) {
                        Toast.makeText(ScriptStudioActivity.this, R.string.please_enter_prompt, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new GenerateScriptTask().execute(prompt);
                }
            });

        saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveScript();
                }
            });

        loadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLoadScriptDialog();
                }
            });
    }

    private void createNewProjectId() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        Random random = new Random();
        int rand = random.nextInt(1000);

        currentProjectId = "script_studio_" + timestamp + "_" + rand;

        // Create project directory
        File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);
        if (!projectDir.exists()) {
            projectDir.mkdirs();
        }

        // Create subdirectories
        new File(projectDir, "scripts").mkdirs();
        new File(projectDir, "checkpoints").mkdirs();

        // Log project creation
        logger.log("ScriptStudio", "Created new project: " + currentProjectId);
    }

    private void loadExistingScript() {
        try {
            File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);
            File scriptFile = new File(projectDir, "script.txt");

            if (scriptFile.exists()) {
                String scriptContent = JSONLogger.readFromFile(scriptFile);
                scriptEditText.setText(scriptContent);
            }
        } catch (Exception e) {
            logger.log("ScriptStudio", "Error loading existing script: " + e.getMessage());
        }
    }

    private void saveScript() {
        try {
            String scriptContent = scriptEditText.getText().toString();
            if (scriptContent.isEmpty()) {
                Toast.makeText(this, R.string.script_is_empty, Toast.LENGTH_SHORT).show();
                return;
            }

            File projectDir = new File(StoragePaths.getProjectsDir() + "/" + currentProjectId);
            File scriptFile = new File(projectDir, "script.txt");

            JSONLogger.writeToFile(scriptFile, scriptContent);

            // Save project metadata
            JSONObject projectData = new JSONObject();
            projectData.put("project_id", currentProjectId);
            projectData.put("title", "Script Project");
            projectData.put("workflowType", "script_studio");
            projectData.put("created_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
            projectData.put("last_modified", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));

            File configFile = new File(projectDir, "project.json");
            JSONLogger.writeToFile(configFile, projectData.toString());

            Toast.makeText(this, R.string.script_saved_successfully, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            logger.log("ScriptStudio", "Error saving script: " + e.getMessage());
            Toast.makeText(this, R.string.error_saving_script, Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoadScriptDialog() {
        // This would show a dialog to load existing scripts
        // For now, we'll just show a toast
        Toast.makeText(this, R.string.load_script_feature, Toast.LENGTH_SHORT).show();
    }

    private class GenerateScriptTask extends AsyncTask<String, Integer, String> {
        private String errorMessage;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            generateButton.setEnabled(false);
            statusText.setText(R.string.generating_script);
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

                // Step 2: Generate script
                publishProgress(30);
                JSONObject scriptResult = scriptGeneratorAgent.generateScript(prompt);

                if (scriptResult == null || !scriptResult.optBoolean("success", false)) {
                    errorMessage = scriptResult != null ? 
                        scriptResult.optString("message", "Script generation failed") : 
                        "Script generation returned null";
                    return null;
                }

                publishProgress(80);
                return scriptResult.optString("script", "");

            } catch (Exception e) {
                errorMessage = "Error generating script: " + e.getMessage();
                logger.log("ScriptStudio", errorMessage);
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String script) {
            progressBar.setVisibility(View.GONE);
            generateButton.setEnabled(true);

            if (script != null) {
                scriptEditText.setText(script);
                statusText.setText(R.string.script_generated_successfully);
                Toast.makeText(ScriptStudioActivity.this, R.string.script_generated_successfully, Toast.LENGTH_SHORT).show();
            } else {
                statusText.setText(R.string.script_generation_failed);
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
                        new GenerateScriptTask().execute(prompt);
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.script_studio, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_export_script) {
            exportScript();
            return true;
        } else if (id == R.id.action_import_script) {
            importScript();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void exportScript() {
        // Export script functionality
        Toast.makeText(this, R.string.export_script_feature, Toast.LENGTH_SHORT).show();
    }

    private void importScript() {
        // Import script functionality
        Toast.makeText(this, R.string.import_script_feature, Toast.LENGTH_SHORT).show();
    }
}
