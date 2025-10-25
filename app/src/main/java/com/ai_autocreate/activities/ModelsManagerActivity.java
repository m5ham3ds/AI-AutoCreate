package com.ai_autocreate.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.ai_autocreate.R;
import com.ai_autocreate.adapters.ModelsAdapter;
import com.ai_autocreate.agents.ModelValidatorAgent;
import com.ai_autocreate.utils.FileUtils;
import com.ai_autocreate.utils.JSONLogger;
import com.ai_autocreate.utils.PrefsObfuscator;
import com.ai_autocreate.utils.StoragePaths;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModelsManagerActivity extends AppCompatActivity {
    private ListView modelsListView;
    private Button addModelButton;
    private Button testAllButton;
    private ModelsAdapter modelsAdapter;
    private List<JSONObject> modelsList;
    private PrefsObfuscator prefs;
    private JSONLogger logger;
    private ModelValidatorAgent validatorAgent;
    private File modelsConfigFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_models_manager);

        // Initialize components
        modelsListView = (ListView) findViewById(R.id.models_list);
        addModelButton = (Button) findViewById(R.id.add_model_button);
        testAllButton = (Button) findViewById(R.id.test_all_button);

        prefs = new PrefsObfuscator(this);
        logger = new JSONLogger(this);
        validatorAgent = new ModelValidatorAgent(this);

        // Initialize models config file
        String configPath = StoragePaths.getConfigDir() + "/models_default.json";
        modelsConfigFile = new File(configPath);

        // Load models
        loadModels();

        // Setup adapter
        // Setup adapter — safe initialization
        if (modelsList == null) {
            modelsList = new ArrayList<JSONObject>();
        }

// If your ModelsAdapter expects ArrayList specifically, wrap it:
        ArrayList<JSONObject> adapterList;
        if (modelsList instanceof ArrayList) {
            adapterList = (ArrayList<JSONObject>) modelsList;
        } else {
            adapterList = new ArrayList<JSONObject>(modelsList);
        }

        modelsAdapter = new ModelsAdapter(this, adapterList);
        modelsListView.setAdapter(modelsAdapter);

        // Setup listeners
        setupListeners();
    }

    private void setupListeners() {
        // Add model button
        addModelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddModelDialog();
                }
            });

        // Test all models button
        testAllButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new TestAllModelsTask().execute();
                }
            });

        // Model item click
        modelsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    showModelOptionsDialog(position);
                }
            });
    }

    private void loadModels() {
        modelsList = new ArrayList<>();

        try {
            if (modelsConfigFile.exists()) {
                String jsonContent = FileUtils.readFromFile(modelsConfigFile);
                JSONArray modelsArray = new JSONArray(jsonContent);

                for (int i = 0; i < modelsArray.length(); i++) {
                    modelsList.add(modelsArray.getJSONObject(i));
                }
            } else {
                // Create default models if file doesn't exist
                createDefaultModels();
            }
        } catch (Exception e) {
            logger.log("ModelsManager", "Error loading models: " + e.getMessage());
            Toast.makeText(this, "Error loading models", Toast.LENGTH_SHORT).show();
        }
    }

    private void createDefaultModels() {
        try {
            // Create default models
            JSONObject huggingFaceModel = new JSONObject();
            huggingFaceModel.put("id", "huggingface-default");
            huggingFaceModel.put("name", "Hugging Face Default");
            huggingFaceModel.put("type", "text");
            huggingFaceModel.put("endpoint", "https://api-inference.huggingface.co/models/");
            huggingFaceModel.put("requires_api_key", true);
            huggingFaceModel.put("description", "Default Hugging Face model");
            huggingFaceModel.put("category", "text");
            huggingFaceModel.put("status", "untested");

            JSONObject geminiModel = new JSONObject();
            geminiModel.put("id", "gemini-pro");
            geminiModel.put("name", "Gemini Pro");
            geminiModel.put("type", "text");
            geminiModel.put("endpoint", "https://generativelanguage.googleapis.com/v1beta/models/");
            geminiModel.put("requires_api_key", true);
            geminiModel.put("description", "Google's Gemini Pro model");
            geminiModel.put("category", "text");
            geminiModel.put("status", "untested");

            modelsList.add(huggingFaceModel);
            modelsList.add(geminiModel);

            // Save to file
            saveModels();
        } catch (JSONException e) {
            logger.log("ModelsManager", "Error creating default models: " + e.getMessage());
        }
    }

    private void saveModels() {
        try {
            JSONArray modelsArray = new JSONArray();

            for (JSONObject model : modelsList) {
                modelsArray.put(model);
            }

            FileUtils.writeToFile(modelsConfigFile, modelsArray.toString());
        } catch (Exception e) {
            logger.log("ModelsManager", "Error saving models: " + e.getMessage());
            Toast.makeText(this, "Error saving models", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddModelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_model, null);

        final EditText nameEdit = (EditText) dialogView.findViewById(R.id.model_name);
        final EditText idEdit = (EditText) dialogView.findViewById(R.id.model_id);
        final EditText endpointEdit = (EditText) dialogView.findViewById(R.id.model_endpoint);
        final EditText apiKeyEdit = (EditText) dialogView.findViewById(R.id.model_api_key);
        final Spinner typeSpinner = (Spinner) dialogView.findViewById(R.id.model_type);
        final Spinner categorySpinner = (Spinner) dialogView.findViewById(R.id.model_category);
        final EditText descriptionEdit = (EditText) dialogView.findViewById(R.id.model_description);

        builder.setTitle(R.string.add_new_model)
            .setView(dialogView)
            .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        String name = nameEdit.getText().toString().trim();
                        String id = idEdit.getText().toString().trim();
                        String endpoint = endpointEdit.getText().toString().trim();
                        String apiKey = apiKeyEdit.getText().toString().trim();
                        String type = typeSpinner.getSelectedItem().toString();
                        String category = categorySpinner.getSelectedItem().toString();
                        String description = descriptionEdit.getText().toString().trim();

                        if (name.isEmpty() || id.isEmpty() || endpoint.isEmpty()) {
                            Toast.makeText(ModelsManagerActivity.this, 
                                           R.string.required_fields_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONObject newModel = new JSONObject();
                        newModel.put("name", name);
                        newModel.put("id", id);
                        newModel.put("endpoint", endpoint);
                        newModel.put("api_key", apiKey);
                        newModel.put("type", type);
                        newModel.put("category", category);
                        newModel.put("description", description);
                        newModel.put("requires_api_key", !apiKey.isEmpty());
                        newModel.put("status", "untested");

                        modelsList.add(newModel);
                        saveModels();
                        modelsAdapter.notifyDataSetChanged();

                        // Test the new model
                        new TestModelTask(modelsList.size() - 1).execute();

                        Toast.makeText(ModelsManagerActivity.this, 
                                       R.string.model_added_successfully, Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        logger.log("ModelsManager", "Error adding model: " + e.getMessage());
                        Toast.makeText(ModelsManagerActivity.this, 
                                       R.string.error_adding_model, Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showModelOptionsDialog(final int position) {
        final JSONObject model = modelsList.get(position);

        try {
            String[] options = {getString(R.string.test_model), getString(R.string.edit_model), 
                getString(R.string.delete_model)};

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(model.getString("name"))
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // Test model
                                new TestModelTask(position).execute();
                                break;
                            case 1: // Edit model
                                showEditModelDialog(position);
                                break;
                            case 2: // Delete model
                                showDeleteModelDialog(position);
                                break;
                        }
                    }
                })
                .show();
        } catch (JSONException e) {
            logger.log("ModelsManager", "Error showing model options: " + e.getMessage());
        }
    }

    private void showEditModelDialog(final int position) {
        final JSONObject model = modelsList.get(position);

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_model, null);

            final EditText nameEdit = (EditText) dialogView.findViewById(R.id.model_name);
            final EditText idEdit = (EditText) dialogView.findViewById(R.id.model_id);
            final EditText endpointEdit = (EditText) dialogView.findViewById(R.id.model_endpoint);
            final EditText apiKeyEdit = (EditText) dialogView.findViewById(R.id.model_api_key);
            final Spinner typeSpinner = (Spinner) dialogView.findViewById(R.id.model_type);
            final Spinner categorySpinner = (Spinner) dialogView.findViewById(R.id.model_category);
            final EditText descriptionEdit = (EditText) dialogView.findViewById(R.id.model_description);

            // Populate fields with existing data
            nameEdit.setText(model.getString("name"));
            idEdit.setText(model.getString("id"));
            endpointEdit.setText(model.getString("endpoint"));

            if (model.has("api_key")) {
                apiKeyEdit.setText(model.getString("api_key"));
            }

            descriptionEdit.setText(model.getString("description"));

            // Set spinners
            String type = model.getString("type");
            String category = model.getString("category");

            // Find positions in spinners (implementation depends on your spinner adapters)
            int typePosition = getSpinnerPosition(typeSpinner, type);
            int categoryPosition = getSpinnerPosition(categorySpinner, category);

            typeSpinner.setSelection(typePosition);
            categorySpinner.setSelection(categoryPosition);

            builder.setTitle(R.string.edit_model)
                .setView(dialogView)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            String name = nameEdit.getText().toString().trim();
                            String id = idEdit.getText().toString().trim();
                            String endpoint = endpointEdit.getText().toString().trim();
                            String apiKey = apiKeyEdit.getText().toString().trim();
                            String type = typeSpinner.getSelectedItem().toString();
                            String category = categorySpinner.getSelectedItem().toString();
                            String description = descriptionEdit.getText().toString().trim();

                            if (name.isEmpty() || id.isEmpty() || endpoint.isEmpty()) {
                                Toast.makeText(ModelsManagerActivity.this, 
                                               R.string.required_fields_empty, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            model.put("name", name);
                            model.put("id", id);
                            model.put("endpoint", endpoint);
                            model.put("api_key", apiKey);
                            model.put("type", type);
                            model.put("category", category);
                            model.put("description", description);
                            model.put("requires_api_key", !apiKey.isEmpty());
                            model.put("status", "untested");

                            saveModels();
                            modelsAdapter.notifyDataSetChanged();

                            // Test the updated model
                            new TestModelTask(position).execute();

                            Toast.makeText(ModelsManagerActivity.this, 
                                           R.string.model_updated_successfully, Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            logger.log("ModelsManager", "Error updating model: " + e.getMessage());
                            Toast.makeText(ModelsManagerActivity.this, 
                                           R.string.error_updating_model, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        } catch (JSONException e) {
            logger.log("ModelsManager", "Error showing edit dialog: " + e.getMessage());
        }
    }

    private void showDeleteModelDialog(final int position) {
        try {
            final JSONObject model = modelsList.get(position);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.delete_model)
                .setMessage(getString(R.string.confirm_delete_model, model.getString("name")))
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        modelsList.remove(position);
                        saveModels();
                        modelsAdapter.notifyDataSetChanged();

                        Toast.makeText(ModelsManagerActivity.this, 
                                       R.string.model_deleted_successfully, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        } catch (JSONException e) {
            logger.log("ModelsManager", "Error showing delete dialog: " + e.getMessage());
        }
    }

    private int getSpinnerPosition(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private class TestModelTask extends AsyncTask<Void, Void, Boolean> {
        private int position;
        private JSONObject model;

        public TestModelTask(int position) {
            this.position = position;
            this.model = modelsList.get(position);
        }

        @Override
        protected void onPreExecute() {
            try {
                model.put("status", "testing");
                modelsAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                logger.log("ModelsManager", "Error updating model status: " + e.getMessage());
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                return validatorAgent.validateModel(model);
            } catch (Exception e) {
                logger.log("ModelsManager", "Error testing model: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            try {
                model.put("status", result ? "valid" : "invalid");
                modelsAdapter.notifyDataSetChanged();

                String message = result ? 
                    getString(R.string.model_test_successful) : 
                    getString(R.string.model_test_failed);

                Toast.makeText(ModelsManagerActivity.this, message, Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                logger.log("ModelsManager", "Error updating model status: " + e.getMessage());
            }
        }
    }

    private class TestAllModelsTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected void onPreExecute() {
            testAllButton.setEnabled(false);
            testAllButton.setText(R.string.testing);
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (int i = 0; i < modelsList.size(); i++) {
                try {
                    JSONObject model = modelsList.get(i);
                    model.put("status", "testing");

                    publishProgress(i);

                    boolean isValid = validatorAgent.validateModel(model);
                    model.put("status", isValid ? "valid" : "invalid");

                    Thread.sleep(500); // Small delay between tests
                } catch (Exception e) {
                    logger.log("ModelsManager", "Error testing model at position " + i + ": " + e.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            modelsAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Void result) {
            testAllButton.setEnabled(true);
            testAllButton.setText(R.string.test_all);
            modelsAdapter.notifyDataSetChanged();

            Toast.makeText(ModelsManagerActivity.this, 
                           R.string.all_models_tested, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.models_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_import_models) {
            // Implement import models functionality
            return true;
        } else if (id == R.id.action_export_models) {
            // Implement export models functionality
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
