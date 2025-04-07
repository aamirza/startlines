package com.asmirza.startlines;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WifiAdapter extends RecyclerView.Adapter<WifiAdapter.WifiViewHolder> {
    private List<String> wifiList;
    private Set<String> selectedWifi;

    public WifiAdapter(List<String> wifiList, Set<String> selectedWifi) {
        this.wifiList = wifiList;
        this.selectedWifi = new HashSet<>(selectedWifi);
    }

    @NonNull
    @Override
    public WifiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wifi_item, parent, false);
        return new WifiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiViewHolder holder, int position) {
        String wifiName = wifiList.get(position);
        holder.wifiCheckBox.setText(wifiName);

        holder.wifiCheckBox.setOnCheckedChangeListener(null);

        holder.wifiCheckBox.setChecked(selectedWifi.contains(wifiName));

        holder.wifiCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedWifi.add(wifiName);
            } else {
                selectedWifi.remove(wifiName);
            }
        });
    }

    public void updateWifiList(List<String> newWifiList, Set<String> previousSelections) {
        this.wifiList.clear();
        this.wifiList.addAll(newWifiList);

        this.selectedWifi.retainAll(newWifiList);
        this.selectedWifi.addAll(previousSelections);

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return wifiList.size();
    }

    public Set<String> getSelectedWifi() {
        return selectedWifi;
    }

    static class WifiViewHolder extends RecyclerView.ViewHolder {
        CheckBox wifiCheckBox;

        WifiViewHolder(View itemView) {
            super(itemView);
            wifiCheckBox = itemView.findViewById(R.id.wifi_checkbox);
        }
    }
}
