package com.ai_autocreate.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.ai_autocreate.R;
import com.ai_autocreate.adapters.APIKeysAdapter;
import com.ai_autocreate.utils.PrefsObfuscator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class APIKeysActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private APIKeysAdapter adapter;
    private List<JSONObject> apiKeysList;
    private PrefsObfuscator prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_keys);

        recyclerView = findViewById(R.id.api_keys_recycler_view);
        prefs = new PrefsObfuscator(this);

        // Load API keys
        loadAPIKeys();

        // Setup adapter
        adapter = new APIKeysAdapter(this, apiKeysList, new APIKeysAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position) {
                    showEditAPIKeyDialog(position);
                }

                @Override
                public void onDeleteClick(int position) {
                    showDeleteAPIKeyDialog(position);
                }
            });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Add FAB click listener
        findViewById(R.id.add_api_key_fab).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddAPIKeyDialog();
                }
            });
    }

    private void loadAPIKeys() {
        apiKeysList = new ArrayList<>();
        String apiKeysJson = prefs.getString("api_keys", "[]");

        try {
            JSONArray jsonArray = new JSONArray(apiKeysJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                apiKeysList.add(jsonArray.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveAPIKeys() {
        JSONArray jsonArray = new JSONArray();
        for (JSONObject apiKey : apiKeysList) {
            jsonArray.put(apiKey);
        }
        prefs.putString("api_keys", jsonArray.toString());
    }

    private void showAddAPIKeyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_api_key, null);

        final EditText nameEdit = dialogView.findViewById(R.id.api_key_name);
        final EditText keyEdit = dialogView.findViewById(R.id.api_key_value);
        final EditText descriptionEdit = dialogView.findViewById(R.id.api_key_description);

        builder.setTitle(R.string.add_api_key)
            .setView(dialogView)
            .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = nameEdit.getText().toString().trim();
                    String key = keyEdit.getText().toString().trim();
                    String description = descriptionEdit.getText().toString().trim();

                    if (name.isEmpty() || key.isEmpty()) {
                        Toast.makeText(APIKeysActivity.this, R.string.required_fields_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        JSONObject newApiKey = new JSONObject();
                        newApiKey.put("name", name);
                        newApiKey.put("key", key);
                        newApiKey.put("description", description);

                        apiKeysList.add(newApiKey);
                        saveAPIKeys();
                        adapter.notifyDataSetChanged();

                        Toast.makeText(APIKeysActivity.this, R.string.api_key_added, Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(APIKeysActivity.this, R.string.error_adding_api_key, Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showEditAPIKeyDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_api_key, null);

        final EditText nameEdit = dialogView.findViewById(R.id.api_key_name);
        final EditText keyEdit = dialogView.findViewById(R.id.api_key_value);
        final EditText descriptionEdit = dialogView.findViewById(R.id.api_key_description);

        try {
            JSONObject apiKey = apiKeysList.get(position);
            nameEdit.setText(apiKey.getString("name"));
            keyEdit.setText(apiKey.getString("key"));
            descriptionEdit.setText(apiKey.optString("description", ""));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        builder.setTitle(R.string.edit_api_key)
            .setView(dialogView)
            .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = nameEdit.getText().toString().trim();
                    String key = keyEdit.getText().toString().trim();
                    String description = descriptionEdit.getText().toString().trim();

                    if (name.isEmpty() || key.isEmpty()) {
                        Toast.makeText(APIKeysActivity.this, R.string.required_fields_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        JSONObject apiKey = apiKeysList.get(position);
                        apiKey.put("name", name);
                        apiKey.put("key", key);
                        apiKey.put("description", description);

                        saveAPIKeys();
                        adapter.notifyDataSetChanged();

                        Toast.makeText(APIKeysActivity.this, R.string.api_key_updated, Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(APIKeysActivity.this, R.string.error_updating_api_key, Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showDeleteAPIKeyDialog(final int position) {
        try {
            final JSONObject apiKey = apiKeysList.get(position);
            String name = apiKey.getString("name");

            new AlertDialog.Builder(this)
                .setTitle(R.string.delete_api_key)
                .setMessage(getString(R.string.confirm_delete_api_key, name))
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        apiKeysList.remove(position);
                        saveAPIKeys();
                        adapter.notifyDataSetChanged();

                        Toast.makeText(APIKeysActivity.this, R.string.api_key_deleted, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.api_keys, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_import_api_keys) {
            // Implement import functionality
            return true;
        } else if (id == R.id.action_export_api_keys) {
            // Implement export functionality
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
