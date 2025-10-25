package com.ai_autocreate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.ai_autocreate.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ModelsAdapter extends BaseAdapter {
    private ArrayList<JSONObject> models;
    private Context context;
    private LayoutInflater inflater;

    public interface OnModelClickListener {
        void onModelClick(int position);
        void onTestClick(int position);
        void onDeleteClick(int position);
    }

    private OnModelClickListener listener;

    // Constructor used by ModelsManagerActivity (Context, List<JSONObject>)
    public ModelsAdapter(Context context, List<JSONObject> modelsList) {
        this(context, modelsList, null);
    }

    // Optional constructor with listener
    public ModelsAdapter(Context context, List<JSONObject> modelsList, OnModelClickListener listener) {
        this.context = context;
        this.models = modelsList != null ? new ArrayList<JSONObject>(modelsList) : new ArrayList<JSONObject>();
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return models.size();
    }

    @Override
    public Object getItem(int position) {
        return models.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        TextView nameText;
        TextView typeText;
        TextView categoryText;
        TextView statusText;
        Button testButton;
        Button deleteButton;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_model, parent, false);
            holder = new ViewHolder();
            holder.nameText = (TextView) convertView.findViewById(R.id.model_name);
            holder.typeText = (TextView) convertView.findViewById(R.id.model_type);
            holder.categoryText = (TextView) convertView.findViewById(R.id.model_category);
            holder.statusText = (TextView) convertView.findViewById(R.id.model_status);
            holder.testButton = (Button) convertView.findViewById(R.id.test_button);
            holder.deleteButton = (Button) convertView.findViewById(R.id.delete_button);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        JSONObject model = models.get(position);
        try {
            holder.nameText.setText(model.optString("name", "Unnamed"));
            holder.typeText.setText(model.optString("type", ""));
            holder.categoryText.setText(model.optString("category", ""));
            String status = model.optString("status", "unknown");
            holder.statusText.setText(status);

            boolean isTesting = "testing".equals(status);
            holder.testButton.setEnabled(!isTesting);
            holder.testButton.setText(isTesting ? R.string.testing : R.string.test);

        } catch (Exception e) {
            holder.nameText.setText("Error");
            holder.typeText.setText("");
            holder.categoryText.setText("");
            holder.statusText.setText("");
        }

        // Click listeners
        convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onModelClick(position);
                }
            });
        holder.testButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onTestClick(position);
                }
            });
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onDeleteClick(position);
                }
            });

        return convertView;
    }

    // Helper to update data from Activity
    public void updateData(List<JSONObject> newModels) {
        this.models = newModels != null ? new ArrayList<JSONObject>(newModels) : new ArrayList<JSONObject>();
        notifyDataSetChanged();
    }
}
