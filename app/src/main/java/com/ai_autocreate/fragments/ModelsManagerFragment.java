package com.ai_autocreate.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ai_autocreate.R;
import com.ai_autocreate.activities.ModelsManagerActivity;
import com.ai_autocreate.adapters.ModelsAdapter;
import com.ai_autocreate.agents.ModelValidatorAgent;
import com.ai_autocreate.utils.FileUtils;
import com.ai_autocreate.utils.JSONLogger;
import com.ai_autocreate.utils.StoragePaths;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModelsManagerFragment extends Fragment {
    private ListView modelsListView;
    private Button addModelButton;
    private Button testAllButton;
    private ProgressBar progressBar;
    private TextView statusTextView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ModelsAdapter modelsAdapter;
    private List<JSONObject> modelsList;
    private JSONLogger logger;
    private ModelValidatorAgent validatorAgent;
    private File modelsConfigFile;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_models_manager, container, false);

        // Initialize views
        modelsListView = view.findViewById(R.id.models_list);
        addModelButton = view.findViewById(R.id.add_model_button);
        testAllButton = view.findViewById(R.id.test_all_button);
        progressBar = view.findViewById(R.id.progress_bar);
        statusTextView = view.findViewById(R.id.status_text_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        // Initialize components
        logger = new JSONLogger(getActivity());
        validatorAgent = new ModelValidatorAgent(getActivity());

        // Initialize models config file
        String configPath = StoragePaths.getConfigDir() + "/models_default.json";
        modelsConfigFile = new File(configPath);

        // Load models
        loadModels();

        // Setup adapter
        modelsAdapter = new ModelsAdapter(getActivity(), modelsList, new ModelsAdapter.OnModelClickListener() {
                @Override
                public void onModelClick(int position) {
                    showModelOptionsDialog(position);
                }

                @Override
                public void onTestClick(int position) {
                    new TestModelTask(position).execute();
                }

                @Override
                public void onDeleteClick(int position) {
                    showDeleteModelDialog(position);
                }
            });

        modelsListView.setAdapter(modelsAdapter);

        // Setup listeners
        setupListeners();

        return view;
    }

    private void setupListeners() {
        addModelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddModelDialog();
                }
            });

        testAllButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new TestAllModelsTask().execute();
                }
            });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    loadModels();
                    swipeRefreshLayout.setRefreshing(false);
                }
            });

        modelsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    showModelOptionsDialog(position);
                }
            });
    }

    private void loadModels() {
        new LoadModelsTask().execute();
    }

    private void showAddModelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_add_model, null);

        final EditText nameEdit = dialogView.findViewById(R.id.model_name);
        final EditText idEdit = dialogView.findViewById(R.id.model_id);
        final EditText endpointEdit = dialogView.findViewById(R.id.model_endpoint);
        final EditText apiKeyEdit = dialogView.findViewById(R.id.model_api_key);
        final Spinner typeSpinner = dialogView.findViewById(R.id.model_type);
        final Spinner categorySpinner = dialogView.findViewById(R.id.model_category);
        final EditText descriptionEdit = dialogView.findViewById(R.id.model_description);

        builder.setTitle(R.string.add_new_model)
            .setView(dialogView)
            .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = nameEdit.getText().toString().trim();
                    String id = idEdit.getText().toString().trim();
                    String endpoint = endpointEdit.getText().toString().trim();
                    String apiKey = apiKeyEdit.getText().toString().trim();
                    String type = typeSpinner.getSelectedItem().toString();
                    String category = categorySpinner.getSelectedItem().toString();
                    String description = descriptionEdit.getText().toString().trim();

                    if (name.isEmpty() || id.isEmpty() || endpoint.isEmpty()) {
                        Toast.makeText(getActivity(), R.string.required_fields_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new AddModelTask(name, id, endpoint, apiKey, type, category, description).execute();
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

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_add_model, null);

            final EditText nameEdit = dialogView.findViewById(R.id.model_name);
            final EditText idEdit = dialogView.findViewById(R.id.model_id);
            final EditText endpointEdit = dialogView.findViewById(R.id.model_endpoint);
            final EditText apiKeyEdit = dialogView.findViewById(R.id.model_api_key);
            final Spinner typeSpinner = dialogView.findViewById(R.id.model_type);
            final Spinner categorySpinner = dialogView.findViewById(R.id.model_category);
            final EditText descriptionEdit = dialogView.findViewById(R.id.model_description);

            // Populate fields with existing data
            nameEdit.setText(model.getString("name"));
            idEdit.setText(model.getString("id"));
            endpointEdit.setText(model.getString("endpoint"));

            if (model.has("api_key")) {
                apiKeyEdit.setText(model.getString("api_key"));
            }

            descriptionEdit.setText(model.getString("description"));

            builder.setTitle(R.string.edit_model)
                .setView(dialogView)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = nameEdit.getText().toString().trim();
                        String id = idEdit.getText().toString().trim();
                        String endpoint = endpointEdit.getText().toString().trim();
                        String apiKey = apiKeyEdit.getText().toString().trim();
                        String type = typeSpinner.getSelectedItem().toString();
                        String category = categorySpinner.getSelectedItem().toString();
                        String description = descriptionEdit.getText().toString().trim();

                        if (name.isEmpty() || id.isEmpty() || endpoint.isEmpty()) {
                            Toast.makeText(getActivity(), R.string.required_fields_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        new EditModelTask(position, name, id, endpoint, apiKey, type, category, description).execute();
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
            String name = model.getString("name");

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.delete_model)
                .setMessage(getString(R.string.confirm_delete_model, name))
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new DeleteModelTask(position).execute();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        } catch (JSONException e) {
            logger.log("ModelsManager", "Error showing delete dialog: " + e.getMessage());
        }
    }

    private class LoadModelsTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            statusTextView.setText(R.string.loading_models);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                modelsList = new ArrayList<>();

                if (modelsConfigFile.exists()) {
                    String jsonContent = FileUtils.readFromFile(modelsConfigFile);
                    JSONArray modelsArray = new JSONArray(jsonContent);

                    for (int i = 0; i < modelsArray.length(); i++) {
                        modelsList.add(modelsArray.getJSONObject(i));
                    }
                } else {
                    // Create default models
                    createDefaultModels();
                }

                return true;
            } catch (Exception e) {
                logger.log("ModelsManager", "Error loading models: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);

            if (success) {
                modelsAdapter.notifyDataSetChanged();
                statusTextView.setText(modelsList.size() + " models loaded");
            } else {
                statusTextView.setText(R.string.error_loading_models);
                Toast.makeText(getActivity(), R.string.error_loading_models, Toast.LENGTH_SHORT).show();
            }
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
        }
    }

    private class AddModelTask extends AsyncTask<Void, Void, Boolean> {
        private String name, id, endpoint, apiKey, type, category, description;

        public AddModelTask(String name, String id, String endpoint, String apiKey, 
                            String type, String category, String description) {
            this.name = name;
            this.id = id;
            this.endpoint = endpoint;
            this.apiKey = apiKey;
            this.type = type;
            this.category = category;
            this.description = description;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
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

                return true;
            } catch (JSONException e) {
                logger.log("ModelsManager", "Error adding model: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                modelsAdapter.notifyDataSetChanged();
                statusTextView.setText(modelsList.size() + " models loaded");

                // Test the new model
                new TestModelTask(modelsList.size() - 1).execute();

                Toast.makeText(getActivity(), R.string.model_added_successfully, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), R.string.error_adding_model, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class EditModelTask extends AsyncTask<Void, Void, Boolean> {
        private int position;
        private String name, id, endpoint, apiKey, type, category, description;

        public EditModelTask(int position, String name, String id, String endpoint, String apiKey,
                             String type, String category, String description) {
            this.position = position;
            this.name = name;
            this.id = id;
            this.endpoint = endpoint;
            this.apiKey = apiKey;
            this.type = type;
            this.category = category;
            this.description = description;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                JSONObject model = modelsList.get(position);
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

                return true;
            } catch (JSONException e) {
                logger.log("ModelsManager", "Error updating model: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                modelsAdapter.notifyDataSetChanged();

                // Test the updated model
                new TestModelTask(position).execute();

                Toast.makeText(getActivity(), R.string.model_updated_successfully, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), R.string.error_updating_model, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class DeleteModelTask extends AsyncTask<Void, Void, Boolean> {
        private int position;

        public DeleteModelTask(int position) {
            this.position = position;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                modelsList.remove(position);
                saveModels();
                return true;
            } catch (Exception e) {
                logger.log("ModelsManager", "Error deleting model: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                modelsAdapter.notifyDataSetChanged();
                statusTextView.setText(modelsList.size() + " models loaded");
                Toast.makeText(getActivity(), R.string.model_deleted_successfully, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), R.string.error_deleting_model, Toast.LENGTH_SHORT).show();
            }
        }
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

                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
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

            Toast.makeText(getActivity(), R.string.all_models_tested, Toast.LENGTH_SHORT).show();
        }
    }
}
