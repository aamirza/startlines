package com.asmirza.startlines;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppRecyclerViewAdapter extends RecyclerView.Adapter<AppRecyclerViewAdapter.ViewHolder> {
    Context context;
    private List<AppInfo> appList;
    private Set<AppInfo> selectedApps = new HashSet<>();
    private Set<String> previouslySelectedAppPackageNames;

    public AppRecyclerViewAdapter(Context context, List<AppInfo> appList, Set<String> previouslySelectedAppPackageNames) {
        this.context = context;
        this.appList = appList;
        this.previouslySelectedAppPackageNames = previouslySelectedAppPackageNames;

        for (AppInfo appInfo : appList) {
            if (previouslySelectedAppPackageNames.contains(appInfo.getPackageName())) {
                selectedApps.add(appInfo);
            }
        }
    }

    @NonNull
    @Override
    public AppRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_app_block, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppRecyclerViewAdapter.ViewHolder holder, int position) {
        AppInfo appInfo = appList.get(position);
        holder.appIconImageView.setImageDrawable(appInfo.getAppIcon());
        holder.appNameTextView.setText(appInfo.getAppName());

        holder.appContainer.setSelected(selectedApps.contains(appInfo));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedApps.contains(appInfo)) {
                    selectedApps.remove(appInfo);
                } else {
                    selectedApps.add(appInfo);
                }
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public Set<AppInfo> getSelectedApps() {
        return selectedApps;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIconImageView;
        TextView appNameTextView;
        ConstraintLayout appContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIconImageView = itemView.findViewById(R.id.app_icon);
            appNameTextView = itemView.findViewById(R.id.app_name);
            appContainer = itemView.findViewById(R.id.item_container);
        }
    }
}

