package com.ai_autocreate.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.ai_autocreate.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class APIKeysAdapter extends RecyclerView.Adapter<APIKeysAdapter.ViewHolder> {
    private List<JSONObject> apiKeysList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
        void onDeleteClick(int position);
    }

    public APIKeysAdapter(Context context, List<JSONObject> apiKeysList, OnItemClickListener listener) {
        this.context = context;
        this.apiKeysList = apiKeysList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_api_key, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject apiKey = apiKeysList.get(position);

        try {
            holder.nameText.setText(apiKey.getString("name"));
            holder.descriptionText.setText(apiKey.optString("description", ""));

            // Mask the API key for security
            String key = apiKey.getString("key");
            if (key.length() > 8) {
                String maskedKey = key.substring(0, 4) + "..." + key.substring(key.length() - 4);
                holder.keyText.setText(maskedKey);
            } else {
                holder.keyText.setText("****");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return apiKeysList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView keyText;
        TextView descriptionText;
        ImageButton deleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.api_key_name);
            keyText = itemView.findViewById(R.id.api_key_value);
            descriptionText = itemView.findViewById(R.id.api_key_description);
            deleteButton = itemView.findViewById(R.id.delete_button);

            itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.onItemClick(getAdapterPosition());
                        }
                    }
                });

            deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.onDeleteClick(getAdapterPosition());
                        }
                    }
                });
        }
    }
}
