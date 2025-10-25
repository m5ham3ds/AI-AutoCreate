package com.ai_autocreate.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ai_autocreate.R;
import com.ai_autocreate.adapters.IdeasAdapter;
import com.ai_autocreate.utils.JSONLogger;
import com.ai_autocreate.utils.StoragePaths;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class IdeaGalleryActivity extends AppCompatActivity {
    private GridView ideasGridView;
    private Spinner categorySpinner;
    private EditText searchEditText;
    private Button searchButton;
    private Button addButton;
    private Button refreshButton;
    private ProgressBar progressBar;
    private TextView statusTextView;

    private IdeasAdapter ideasAdapter;
    private List<IdeaItem> ideasList;
    private JSONLogger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idea_gallery);

        // Initialize views
        ideasGridView = findViewById(R.id.ideas_grid_view);
        categorySpinner = findViewById(R.id.category_spinner);
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        addButton = findViewById(R.id.add_button);
        refreshButton = findViewById(R.id.refresh_button);
        progressBar = findViewById(R.id.progress_bar);
        statusTextView = findViewById(R.id.status_text_view);

        // Initialize components
        logger = new JSONLogger(this);
        ideasList = new ArrayList<>();
        ideasAdapter = new IdeasAdapter(this, ideasList);

        // Setup views
        ideasGridView.setAdapter(ideasAdapter);
        setupCategorySpinner();

        // Setup listeners
        setupListeners();

        // Load ideas
        loadIdeas();
    }

    private void setupCategorySpinner() {
        String[] categories = {"All", "Video", "Script", "Image", "Audio", "General"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void setupListeners() {
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    filterIdeas();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Do nothing
                }
            });

        searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchIdeas();
                }
            });

        addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddIdeaDialog();
                }
            });

        refreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    refreshIdeas();
                }
            });

        ideasGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    showIdeaDetails(position);
                }
            });

        ideasGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    showIdeaOptions(position);
                    return true;
                }
            });
    }

    private void loadIdeas() {
        new LoadIdeasTask().execute();
    }

    private void filterIdeas() {
        String selectedCategory = (String) categorySpinner.getSelectedItem();

        List<IdeaItem> filteredList = new ArrayList<>();

        for (IdeaItem idea : ideasList) {
            if (selectedCategory.equals("All") || idea.category.equals(selectedCategory)) {
                filteredList.add(idea);
            }
        }

        ideasAdapter.updateList(filteredList);
        statusTextView.setText(filteredList.size() + " ideas found");
    }

    private void searchIdeas() {
        String searchTerm = searchEditText.getText().toString().trim();
        if (searchTerm.isEmpty()) {
            filterIdeas();
            return;
        }

        List<IdeaItem> searchResults = new ArrayList<>();
        String selectedCategory = (String) categorySpinner.getSelectedItem();

        for (IdeaItem idea : ideasList) {
            boolean categoryMatch = selectedCategory.equals("All") || idea.category.equals(selectedCategory);
            boolean searchMatch = idea.title.toLowerCase().contains(searchTerm.toLowerCase()) ||
                idea.description.toLowerCase().contains(searchTerm.toLowerCase());

            if (categoryMatch && searchMatch) {
                searchResults.add(idea);
            }
        }

        ideasAdapter.updateList(searchResults);
        statusTextView.setText(searchResults.size() + " ideas found for \"" + searchTerm + "\"");
    }

    private void refreshIdeas() {
        // This would typically fetch new ideas from a server
        // For now, we'll just reload existing ideas
        loadIdeas();
    }

    private void showIdeaDetails(int position) {
        IdeaItem idea = ideasAdapter.getItem(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(idea.title);
        builder.setMessage(idea.description + "\n\nCategory: " + idea.category + 
                           "\nCreated: " + idea.createdDate);
        builder.setPositiveButton(R.string.use_idea, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Use the idea - this would typically pass it to another activity
                    Toast.makeText(IdeaGalleryActivity.this, R.string.idea_selected, Toast.LENGTH_SHORT).show();
                }
            });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void showIdeaOptions(int position) {
        final IdeaItem idea = ideasAdapter.getItem(position);

        String[] options = {getString(R.string.edit), getString(R.string.duplicate), getString(R.string.delete)};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(idea.title)
            .setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: // Edit
                            showEditIdeaDialog(idea);
                            break;
                        case 1: // Duplicate
                            duplicateIdea(idea);
                            break;
                        case 2: // Delete
                            showDeleteIdeaDialog(idea);
                            break;
                    }
                }
            })
            .show();
    }

    private void showAddIdeaDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_idea, null);

        final EditText titleEdit = dialogView.findViewById(R.id.idea_title);
        final EditText descriptionEdit = dialogView.findViewById(R.id.idea_description);
        final Spinner categorySpinner = dialogView.findViewById(R.id.idea_category);

        // Setup category spinner
        String[] categories = {"Video", "Script", "Image", "Audio", "General"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        builder.setTitle(R.string.add_new_idea)
            .setView(dialogView)
            .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String title = titleEdit.getText().toString().trim();
                    String description = descriptionEdit.getText().toString().trim();
                    String category = (String) categorySpinner.getSelectedItem();

                    if (title.isEmpty() || description.isEmpty()) {
                        Toast.makeText(IdeaGalleryActivity.this, R.string.required_fields_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    addIdea(title, description, category);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showEditIdeaDialog(final IdeaItem idea) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_idea, null);

        final EditText titleEdit = dialogView.findViewById(R.id.idea_title);
        final EditText descriptionEdit = dialogView.findViewById(R.id.idea_description);
        final Spinner categorySpinner = dialogView.findViewById(R.id.idea_category);

        // Setup category spinner
        String[] categories = {"Video", "Script", "Image", "Audio", "General"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        // Populate fields
        titleEdit.setText(idea.title);
        descriptionEdit.setText(idea.description);

        // Set category selection
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(idea.category)) {
                categorySpinner.setSelection(i);
                break;
            }
        }

        builder.setTitle(R.string.edit_idea)
            .setView(dialogView)
            .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String title = titleEdit.getText().toString().trim();
                    String description = descriptionEdit.getText().toString().trim();
                    String category = (String) categorySpinner.getSelectedItem();

                    if (title.isEmpty() || description.isEmpty()) {
                        Toast.makeText(IdeaGalleryActivity.this, R.string.required_fields_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    editIdea(idea, title, description, category);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showDeleteIdeaDialog(final IdeaItem idea) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_idea)
            .setMessage(getString(R.string.confirm_delete_idea, idea.title))
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteIdea(idea);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void addIdea(String title, String description, String category) {
        new AddIdeaTask(title, description, category).execute();
    }

    private void editIdea(IdeaItem idea, String title, String description, String category) {
        idea.title = title;
        idea.description = description;
        idea.category = category;
        idea.lastModifiedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        new SaveIdeasTask().execute();
        ideasAdapter.notifyDataSetChanged();
        Toast.makeText(this, R.string.idea_updated, Toast.LENGTH_SHORT).show();
    }

    private void duplicateIdea(IdeaItem idea) {
        String newTitle = idea.title + " (Copy)";
        addIdea(newTitle, idea.description, idea.category);
    }

    private void deleteIdea(IdeaItem idea) {
        ideasList.remove(idea);
        ideasAdapter.notifyDataSetChanged();
        new SaveIdeasTask().execute();
        Toast.makeText(this, R.string.idea_deleted, Toast.LENGTH_SHORT).show();
    }

    private class LoadIdeasTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            statusTextView.setText(R.string.loading_ideas);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                File ideasFile = new File(StoragePaths.getConfigDir() + "/ideas.json");

                if (ideasFile.exists()) {
                    String jsonContent = JSONLogger.readFromFile(ideasFile);
                    JSONArray ideasArray = new JSONArray(jsonContent);

                    ideasList.clear();
                    for (int i = 0; i < ideasArray.length(); i++) {
                        JSONObject ideaObj = ideasArray.getJSONObject(i);
                        IdeaItem idea = new IdeaItem(
                            ideaObj.getString("id"),
                            ideaObj.getString("title"),
                            ideaObj.getString("description"),
                            ideaObj.getString("category"),
                            ideaObj.optString("created_date", ""),
                            ideaObj.optString("last_modified_date", "")
                        );
                        ideasList.add(idea);
                    }

                    return true;
                } else {
                    // Create some default ideas
                    createDefaultIdeas();
                    return true;
                }
            } catch (Exception e) {
                logger.log("IdeaGallery", "Error loading ideas: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);

            if (success) {
                ideasAdapter.updateList(ideasList);
                filterIdeas();
            } else {
                statusTextView.setText(R.string.error_loading_ideas);
                Toast.makeText(IdeaGalleryActivity.this, R.string.error_loading_ideas, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createDefaultIdeas() {
        ideasList.add(new IdeaItem("1", "Create a time-lapse video", "Generate a time-lapse video from image sequence", "Video", getCurrentDate(), getCurrentDate()));
        ideasList.add(new IdeaItem("2", "Write a short story", "Generate a creative short story with characters and plot", "Script", getCurrentDate(), getCurrentDate()));
        ideasList.add(new IdeaItem("3", "Generate fantasy art", "Create fantasy-style artwork from description", "Image", getCurrentDate(), getCurrentDate()));
        ideasList.add(new IdeaItem("4", "Compose background music", "Generate ambient background music for videos", "Audio", getCurrentDate(), getCurrentDate()));
        ideasList.add(new IdeaItem("5", "AI assistant", "Create an AI assistant for daily tasks", "General", getCurrentDate(), getCurrentDate()));

        new SaveIdeasTask().execute();
    }

    private class AddIdeaTask extends AsyncTask<Void, Void, Boolean> {
        private String title;
        private String description;
        private String category;

        public AddIdeaTask(String title, String description, String category) {
            this.title = title;
            this.description = description;
            this.category = category;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                String id = String.valueOf(System.currentTimeMillis());
                String currentDate = getCurrentDate();

                IdeaItem newIdea = new IdeaItem(id, title, description, category, currentDate, currentDate);
                ideasList.add(0, newIdea); // Add to beginning of list

                return new SaveIdeasTask().doInBackground();
            } catch (Exception e) {
                logger.log("IdeaGallery", "Error adding idea: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                ideasAdapter.updateList(ideasList);
                filterIdeas();
                Toast.makeText(IdeaGalleryActivity.this, R.string.idea_added, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(IdeaGalleryActivity.this, R.string.error_adding_idea, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class SaveIdeasTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                JSONArray ideasArray = new JSONArray();

                for (IdeaItem idea : ideasList) {
                    JSONObject ideaObj = new JSONObject();
                    ideaObj.put("id", idea.id);
                    ideaObj.put("title", idea.title);
                    ideaObj.put("description", idea.description);
                    ideaObj.put("category", idea.category);
                    ideaObj.put("created_date", idea.createdDate);
                    ideaObj.put("last_modified_date", idea.lastModifiedDate);
                    ideasArray.put(ideaObj);
                }

                File ideasFile = new File(StoragePaths.getConfigDir() + "/ideas.json");
                JSONLogger.writeToFile(ideasFile, ideasArray.toString());

                return true;
            } catch (Exception e) {
                logger.log("IdeaGallery", "Error saving ideas: " + e.getMessage());
                return false;
            }
        }
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.idea_gallery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_import_ideas) {
            importIdeas();
            return true;
        } else if (id == R.id.action_export_ideas) {
            exportIdeas();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void importIdeas() {
        Toast.makeText(this, R.string.import_ideas_feature, Toast.LENGTH_SHORT).show();
    }

    private void exportIdeas() {
        Toast.makeText(this, R.string.export_ideas_feature, Toast.LENGTH_SHORT).show();
    }

    public static class IdeaItem {
        public String id;
        public String title;
        public String description;
        public String category;
        public String createdDate;
        public String lastModifiedDate;

        public IdeaItem(String id, String title, String description, String category, String createdDate, String lastModifiedDate) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.category = category;
            this.createdDate = createdDate;
            this.lastModifiedDate = lastModifiedDate;
        }
    }
}
