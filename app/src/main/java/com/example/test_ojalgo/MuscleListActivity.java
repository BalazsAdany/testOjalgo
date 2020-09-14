package com.example.test_ojalgo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class MuscleListActivity extends AppCompatActivity
{

    ListView listView;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_muscle_list);

        sharedPref = getSharedPreferences("Muscle List", MODE_PRIVATE);
        listView = findViewById(R.id.listview);

        final ArrayList<String> muscleList = new ArrayList<>();

        muscleList.add("Chest");
        muscleList.add("Back");
        muscleList.add("Quads");
        muscleList.add("Hamstrings");
        muscleList.add("Glutes");
        muscleList.add("Calves");
        muscleList.add("Triceps");
        muscleList.add("Biceps");
        muscleList.add("Front delts");
        muscleList.add("Side delts");
        muscleList.add("Rear delts");
        muscleList.add("Abs");
        muscleList.add("Forearms");
        muscleList.add("Traps");

        String[] sharePrefMuscles = sharedPref.getString("Muscles", "").split(";");

        if (sharePrefMuscles != null)
        {
            for (int i = 0; i < sharePrefMuscles.length; i++)
            {
                String[] muscle = sharePrefMuscles[i].split("/");
                muscleList.remove(muscle[0]);
            }
        }

        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, muscleList);

        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                StartNewActivity(muscleList.get(position).toString());
            }
        });
    }

    private void StartNewActivity(String muscle)
    {

        Intent intent = new Intent(this, FreqSetsActivity.class);
        intent.putExtra("Selected muscle", muscle);
        finish();
        startActivity(intent);
        
    }

}
