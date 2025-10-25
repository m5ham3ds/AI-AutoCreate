package com.ai_autocreate.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ai_autocreate.R;
import com.ai_autocreate.activities.IdeaGalleryActivity;

import java.util.List;

public class IdeasAdapter extends RecyclerView.Adapter<IdeasAdapter.ViewHolder> {
    private List<IdeaGalleryActivity.IdeaItem> ideasList;
    private Context context;
    private OnIdeaClickListener listener;

    public interface OnIdeaClickListener {
        void onIdeaClick(int position);
        void onIdeaLongClick(int position);
    }

    public IdeasAdapter(Context context, List<IdeaGalleryActivity.IdeaItem> ideasList) {
        this.context = context;
        this.ideasList = ideasList;
    }

    public void setOnIdeaClickListener(OnIdeaClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_idea, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IdeaGalleryActivity.IdeaItem idea = ideasList.get(position);

        holder.titleTextView.setText(idea.title);
        holder.descriptionTextView.setText(idea.description);
        holder.categoryTextView.setText(idea.category);
        holder.dateTextView.setText(idea.createdDate);
    }

    @Override
    public int getItemCount() {
        return ideasList.size();
    }

    public void updateList(List<IdeaGalleryActivity.IdeaItem> newList) {
        ideasList.clear();
        ideasList.addAll(newList);
        notifyDataSetChanged();
    }

    public IdeaGalleryActivity.IdeaItem getItem(int position) {
        return ideasList.get(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;
        TextView categoryTextView;
        TextView dateTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.idea_title);
            descriptionTextView = itemView.findViewById(R.id.idea_description);
            categoryTextView = itemView.findViewById(R.id.idea_category);
            dateTextView = itemView.findViewById(R.id.idea_date);

            itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.onIdeaClick(getAdapterPosition());
                        }
                    }
                });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (listener != null) {
                            listener.onIdeaLongClick(getAdapterPosition());
                        }
                        return true;
                    }
                });
        }
    }
}
