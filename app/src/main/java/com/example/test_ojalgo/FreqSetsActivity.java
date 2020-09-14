package com.example.test_ojalgo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class FreqSetsActivity extends AppCompatActivity {

    String newMuscle;
    String sets;
    String freq;
    String musclesInList;
    int countMuscles;

    Button addButton;
    EditText freqText;
    EditText setsText;

    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_freq_sets);

        freqText = findViewById(R.id.editTextFreq);
        setsText = findViewById(R.id.editTextSets);


        Intent intent = getIntent();
        newMuscle = intent.getStringExtra("Selected muscle");

        sharedPref = getSharedPreferences("Muscle List", MODE_PRIVATE);
        countMuscles = sharedPref.getInt("Count", MODE_PRIVATE);
        musclesInList = sharedPref.getString("Muscles", "");

        addButton = findViewById(R.id.buttonAdd);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (freqText != null && setsText != null)
                {
                    freq = freqText.getText().toString();
                    sets = setsText.getText().toString();

                    sharedPref.edit().putInt("Count", (countMuscles + 1)).apply();
                    musclesInList = musclesInList + newMuscle + "/" + freq + "/" + sets + ";";
                    sharedPref.edit().putString("Muscles", musclesInList).apply();

                    finish();
                }

            }
        });

    }
}
