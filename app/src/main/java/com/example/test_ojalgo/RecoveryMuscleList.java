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

public class RecoveryMuscleList extends AppCompatActivity {

    ListView listView;
    SharedPreferences sharedPref;
    String recoverySharedPref;
    ArrayAdapter arrayAdapter;
    String[] baseArray;
    String[] muscles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recovery_muscle_list);

        sharedPref = getSharedPreferences("Muscle List", MODE_PRIVATE);
        listView = findViewById(R.id.listview);

        sharedPref.edit().putString("Recovery List", "").apply();

        String baseString = sharedPref.getString("Muscles", "");
        baseString = baseString.substring(0, baseString.length() - 1);
        baseArray = baseString.split(";");

        muscles = new String[baseArray.length];
        final ArrayList<String> muscleList = new ArrayList<>();

        //only add muscles which are in the muscle list
        for (int i = 0; i < baseArray.length; i++)
        {
            String[] base = baseArray[i].split("/");
            muscleList.add(base[0]);
        }

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, muscleList);

        recoverySharedPref = sharedPref.getString("Recovery List", "");

        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String list = recoverySharedPref + muscleList.get(position) + ";";
                sharedPref.edit().putString("Recovery List", list).apply();
                muscleList.remove(muscleList.get(position));
                recoverySharedPref = sharedPref.getString("Recovery List", "");
                arrayAdapter.notifyDataSetChanged();

                if (muscleList.isEmpty())
                {
                    CalculateWorkout();
                }
            }
        });
    }

    public void CalculateWorkout()
    {

        Intent intent = new Intent(this, Results.class);
        finish();
        startActivity(intent);

    }

}
