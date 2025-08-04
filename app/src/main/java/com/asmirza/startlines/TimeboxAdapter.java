package com.asmirza.startlines;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TimeboxAdapter extends RecyclerView.Adapter<TimeboxAdapter.TimeboxViewHolder> {
    private final List<Timebox> timeboxList;
    private final Context context;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public TimeboxAdapter(List<Timebox> timeboxes, Context context) {
        this.timeboxList = timeboxes;
        this.context = context;
    }

    @NonNull
    @Override
    public TimeboxViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timebox, parent, false);
        return new TimeboxViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeboxViewHolder holder, int position) {
        Timebox timebox = timeboxList.get(position);

        holder.startTime.setText(timeFormat.format(timebox.getStartTime()));
        holder.endTime.setText(timebox.getEndTime() > timebox.getStartTime() ? timeFormat.format(timebox.getEndTime()) : "Ongoing");

        holder.scheduleCompliant.setOnCheckedChangeListener(null);
        holder.scheduleCompliant.setChecked(timebox.isScheduleCompliant());

        holder.scheduleCompliant.setOnCheckedChangeListener((buttonView, isChecked) -> {
            timebox.setScheduleCompliant(isChecked);

            // Save the updated list to SharedPreferences
            StartlinesManager.saveTimeboxes(context, timeboxList);
        });
    }

    @Override
    public int getItemCount() {
        return timeboxList.size();
    }

    static class TimeboxViewHolder extends RecyclerView.ViewHolder {
        TextView startTime, endTime;
        CheckBox scheduleCompliant;

        public TimeboxViewHolder(View itemView) {
            super(itemView);
            startTime = itemView.findViewById(R.id.startTime);
            endTime = itemView.findViewById(R.id.endTime);
            scheduleCompliant = itemView.findViewById(R.id.scheduleCompliant);
        }
    }
}
