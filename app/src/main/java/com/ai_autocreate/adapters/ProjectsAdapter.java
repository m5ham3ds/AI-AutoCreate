package com.ai_autocreate.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ai_autocreate.R;
import com.ai_autocreate.utils.MediaUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectsAdapter extends RecyclerView.Adapter<ProjectsAdapter.ViewHolder> {
    private List<JSONObject> projectsList;
    private Context context;
    private OnProjectClickListener listener;

    public interface OnProjectClickListener {
        void onProjectClick(int position);
        void onProjectDelete(int position);
        void onProjectExport(int position);
    }

    public ProjectsAdapter(Context context, List<JSONObject> projectsList, OnProjectClickListener listener) {
        this.context = context;
        this.projectsList = projectsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_project, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject project = projectsList.get(position);

        try {
            holder.titleTextView.setText(project.getString("title"));
            holder.typeTextView.setText(project.getString("workflowType"));
            holder.dateTextView.setText(formatDate(project.getString("created_at")));

            // Load thumbnail if available
            String videoPath = project.optString("source_video", "");
            if (!videoPath.isEmpty()) {
                File videoFile = new File(videoPath);
                if (videoFile.exists()) {
                    holder.thumbnailImageView.setImageBitmap(MediaUtils.createVideoThumbnail(videoPath, 200, 120));
                }
            }

            // Set status
            String status = project.optString("status", "unknown");
            holder.statusTextView.setText(status);

            switch (status) {
                case "completed":
                    holder.statusTextView.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                    break;
                case "failed":
                case "error":
                    holder.statusTextView.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
                    break;
                case "processing":
                    holder.statusTextView.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
                    break;
                default:
                    holder.statusTextView.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return projectsList.size();
    }

    public JSONObject getItem(int position) {
        return projectsList.get(position);
    }

    public void updateList(List<JSONObject> newList) {
        projectsList.clear();
        projectsList.addAll(newList);
        notifyDataSetChanged();
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateString;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailImageView;
        TextView titleTextView;
        TextView typeTextView;
        TextView dateTextView;
        TextView statusTextView;
        Button openButton;
        Button deleteButton;
        Button exportButton;

        public ViewHolder(View itemView) {
            super(itemView);
            thumbnailImageView = itemView.findViewById(R.id.project_thumbnail);
            titleTextView = itemView.findViewById(R.id.project_title);
            typeTextView = itemView.findViewById(R.id.project_type);
            dateTextView = itemView.findViewById(R.id.project_date);
            statusTextView = itemView.findViewById(R.id.project_status);
            openButton = itemView.findViewById(R.id.open_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
            exportButton = itemView.findViewById(R.id.export_button);

            itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.onProjectClick(getAdapterPosition());
                        }
                    }
                });

            openButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.onProjectClick(getAdapterPosition());
                        }
                    }
                });

            deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.onProjectDelete(getAdapterPosition());
                        }
                    }
                });

            exportButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.onProjectExport(getAdapterPosition());
                        }
                    }
                });
        }
    }
}
