package com.ai_autocreate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ai_autocreate.R;
import com.ai_autocreate.activities.ActivityLogActivity;

import java.util.ArrayList;
import java.util.List;

public class ActivityLogAdapter extends BaseAdapter {
    private List<ActivityLogActivity.LogEntry> logList;
    private Context context;
    private LayoutInflater inflater;

    public ActivityLogAdapter(Context context, List<ActivityLogActivity.LogEntry> logList) {
        this.context = context;
        this.logList = logList != null ? logList : new ArrayList<ActivityLogActivity.LogEntry>();
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void updateList(List<ActivityLogActivity.LogEntry> newList) {
        this.logList.clear();
        if (newList != null) this.logList.addAll(newList);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return logList.size();
    }

    @Override
    public ActivityLogActivity.LogEntry getItem(int position) {
        return logList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        TextView titleTextView;
        TextView messageTextView;
        TextView timestampTextView;
        TextView typeTextView;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_log, parent, false);
            holder = new ViewHolder();
            holder.titleTextView = (TextView) convertView.findViewById(R.id.log_title);
            holder.messageTextView = (TextView) convertView.findViewById(R.id.log_message);
            holder.timestampTextView = (TextView) convertView.findViewById(R.id.log_timestamp);
            holder.typeTextView = (TextView) convertView.findViewById(R.id.log_type);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ActivityLogActivity.LogEntry log = getItem(position);
        if (log != null) {
            holder.titleTextView.setText(log.title != null ? log.title : "");
            holder.messageTextView.setText(log.message != null ? log.message : "");
            holder.timestampTextView.setText(log.timestamp != null ? log.timestamp : "");
            holder.typeTextView.setText(log.type != null ? log.type.toUpperCase() : "");

            // color based on type (optional)
            if ("error".equals(log.type) || "failure".equals(log.type)) {
                holder.typeTextView.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            } else if ("success".equals(log.type) || "completed".equals(log.type)) {
                holder.typeTextView.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            } else {
                holder.typeTextView.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            }
        }

        return convertView;
    }
}
