package com.asmirza.startlines;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import java.util.List;

public class TimeboxListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TimeboxAdapter timeboxAdapter;
    private List<Timebox> timeboxList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timebox_list);

        recyclerView = findViewById(R.id.timebox_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load the saved timebox list
        timeboxList = StartlinesManager.loadTimeboxes(this, false);

        // Pass the timebox list to the adapter
        timeboxAdapter = new TimeboxAdapter(timeboxList, this);
        recyclerView.setAdapter(timeboxAdapter);
    }
}