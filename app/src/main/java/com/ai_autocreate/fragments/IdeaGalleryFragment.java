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
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ai_autocreate.R;
import com.ai_autocreate.activities.IdeaGalleryActivity;
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

public class IdeaGalleryFragment extends Fragment {
    private GridView ideasGridView;
    private Spinner categorySpinner;
    private ProgressBar progressBar;
    private TextView statusTextView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private IdeasAdapter ideasAdapter;
    private List<IdeaGalleryActivity.IdeaItem> ideasList;
    private JSONLogger logger;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_idea_gallery, container, false);

        // Initialize views
        ideasGridView = view.findViewById(R.id.ideas_grid_view);
        categorySpinner = view.findViewById(R.id.category_spinner);
        progressBar = view.findViewById(R.id.progress_bar);
        statusTextView = view.findViewById(R.id.status_text_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        // Initialize components
        logger = new JSONLogger(getActivity());
        ideasList = new ArrayList<>();
        ideasAdapter = new IdeasAdapter(getActivity(), ideasList);

        // Setup views
        ideasGridView.setAdapter(ideasAdapter);
        setupCategorySpinner();

        // Setup listeners
        setupListeners();

        // Load ideas
        loadIdeas();

        return view;
    }

    private void setupCategorySpinner() {
        String[] categories = {"All", "Video", "Script", "Image", "Audio", "General"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            getActivity(), android.R.layout.simple_spinner_item, categories);
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

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    loadIdeas();
                    swipeRefreshLayout.setRefreshing(false);
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

        List<IdeaGalleryActivity.IdeaItem> filteredList = new ArrayList<>();

        for (IdeaGalleryActivity.IdeaItem idea : ideasList) {
            if (selectedCategory.equals("All") || idea.category.equals(selectedCategory)) {
                filteredList.add(idea);
            }
        }

        ideasAdapter.updateList(filteredList);
        statusTextView.setText(filteredList.size() + " ideas found");
    }

    private void showIdeaDetails(int position) {
        IdeaGalleryActivity.IdeaItem idea = ideasAdapter.getItem(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(idea.title);
        builder.setMessage(idea.description + "\n\nCategory: " + idea.category + 
                           "\nCreated: " + idea.createdDate);
        builder.setPositiveButton(R.string.use_idea, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Use the idea - pass it to parent activity
                    if (getActivity() instanceof IdeaGalleryActivity) {
                        ((IdeaGalleryActivity) getActivity()).useIdea(idea);
                    }
                }
            });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void showIdeaOptions(final int position) {
        final IdeaGalleryActivity.IdeaItem idea = ideasAdapter.getItem(position);

        String[] options = {getString(R.string.edit), getString(R.string.duplicate), getString(R.string.delete)};

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

    private void showEditIdeaDialog(final IdeaGalleryActivity.IdeaItem idea) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_add_idea, null);

        final EditText titleEdit = dialogView.findViewById(R.id.idea_title);
        final EditText descriptionEdit = dialogView.findViewById(R.id.idea_description);
        final Spinner categorySpinner = dialogView.findViewById(R.id.idea_category);

        // Setup category spinner
        String[] categories = {"Video", "Script", "Image", "Audio", "General"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            getActivity(), android.R.layout.simple_spinner_item, categories);
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
                        Toast.makeText(getActivity(), R.string.required_fields_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    editIdea(idea, title, description, category);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showDeleteIdeaDialog(final IdeaGalleryActivity.IdeaItem idea) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

    private void editIdea(IdeaGalleryActivity.IdeaItem idea, String title, String description, String category) {
        idea.title = title;
        idea.description = description;
        idea.category = category;
        idea.lastModifiedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        new SaveIdeasTask().execute();
        ideasAdapter.notifyDataSetChanged();
        Toast.makeText(getActivity(), R.string.idea_updated, Toast.LENGTH_SHORT).show();
    }

    private void duplicateIdea(IdeaGalleryActivity.IdeaItem idea) {
        String newTitle = idea.title + " (Copy)";
        addIdea(newTitle, idea.description, idea.category);
    }

    private void deleteIdea(IdeaGalleryActivity.IdeaItem idea) {
        ideasList.remove(idea);
        ideasAdapter.notifyDataSetChanged();
        new SaveIdeasTask().execute();
        Toast.makeText(getActivity(), R.string.idea_deleted, Toast.LENGTH_SHORT).show();
    }

    private void addIdea(String title, String description, String category) {
        new AddIdeaTask(title, description, category).execute();
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
                        IdeaGalleryActivity.IdeaItem idea = new IdeaGalleryActivity.IdeaItem(
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
                Toast.makeText(getActivity(), R.string.error_loading_ideas, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createDefaultIdeas() {
        ideasList.add(new IdeaGalleryActivity.IdeaItem("1", "Create a time-lapse video", "Generate a time-lapse video from image sequence", "Video", getCurrentDate(), getCurrentDate()));
        ideasList.add(new IdeaGalleryActivity.IdeaItem("2", "Write a short story", "Generate a creative short story with characters and plot", "Script", getCurrentDate(), getCurrentDate()));
        ideasList.add(new IdeaGalleryActivity.IdeaItem("3", "Generate fantasy art", "Create fantasy-style artwork from description", "Image", getCurrentDate(), getCurrentDate()));
        ideasList.add(new IdeaGalleryActivity.IdeaItem("4", "Compose background music", "Generate ambient background music for videos", "Audio", getCurrentDate(), getCurrentDate()));
        ideasList.add(new IdeaGalleryActivity.IdeaItem("5", "AI assistant", "Create an AI assistant for daily tasks", "General", getCurrentDate(), getCurrentDate()));

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

                IdeaGalleryActivity.IdeaItem newIdea = new IdeaGalleryActivity.IdeaItem(id, title, description, category, currentDate, currentDate);
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
                Toast.makeText(getActivity(), R.string.idea_added, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), R.string.error_adding_idea, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class SaveIdeasTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                JSONArray ideasArray = new JSONArray();

                for (IdeaGalleryActivity.IdeaItem idea : ideasList) {
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
}
