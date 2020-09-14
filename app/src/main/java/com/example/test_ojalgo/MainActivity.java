package com.example.test_ojalgo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

import java.math.BigDecimal;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button buttonNewMuscle;
    Button buttonReset;
    Button buttonNext;
    SharedPreferences sharedPref;
    TableLayout tableLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = getSharedPreferences("Muscle List", MODE_PRIVATE);
        tableLayout = findViewById(R.id.tableLayout);

        buttonNewMuscle = findViewById(R.id.buttonNewMuscle);
        buttonNewMuscle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                StartNewActivity();

            }
        });

        buttonReset = findViewById(R.id.buttonReset);
        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                sharedPref.edit().clear().apply();
                tableLayout.removeAllViews();
            }
        });

        buttonNext = findViewById(R.id.buttonNext);
        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String days = "";

                Switch sw1 = findViewById(R.id.switch1);
                Switch sw2 = findViewById(R.id.switch2);
                Switch sw3 = findViewById(R.id.switch3);
                Switch sw4 = findViewById(R.id.switch4);
                Switch sw5 = findViewById(R.id.switch5);
                Switch sw6 = findViewById(R.id.switch6);
                Switch sw7 = findViewById(R.id.switch7);

                if (sw1.isChecked())
                {
                    days = days + "1" + ";";
                }
                else
                {
                    days = days + "0" + ";";
                }
                if (sw2.isChecked())
                {
                    days = days + "1" + ";";
                }
                else
                {
                    days = days + "0" + ";";
                }
                if (sw3.isChecked())
                {
                    days = days + "1" + ";";
                }
                else
                {
                    days = days + "0" + ";";
                }
                if (sw4.isChecked())
                {
                    days = days + "1" + ";";
                }
                else
                {
                    days = days + "0" + ";";
                }
                if (sw5.isChecked())
                {
                    days = days + "1" + ";";
                }
                else
                {
                    days = days + "0" + ";";
                }
                if (sw6.isChecked())
                {
                    days = days + "1" + ";";
                }
                else
                {
                    days = days + "0" + ";";
                }
                if (sw7.isChecked())
                {
                    days = days + "1" + ";";
                }
                else
                {
                    days = days + "0" + ";";
                }

                sharedPref.edit().putString("Days", days).apply();

                StartNext();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        FillTable();
    }

    public void FillTable()
    {

        tableLayout.removeAllViews();

        if (!sharedPref.getString("Muscles", "").equals(""))
        {

            String[] savedMuscles = sharedPref.getString("Muscles", "").split(";");

            for (int i = 0; i < savedMuscles.length; i++)
            {
                String[] muscle = savedMuscles[i].split("/");

                TableRow tableRow = new TableRow(this);
                TextView muscleName = new TextView(this);
                TextView freq = new TextView(this);
                TextView sets = new TextView(this);

                muscleName.setText(muscle[0]);
                freq.setText(muscle[1]);
                sets.setText(muscle[2]);

                tableRow.addView(muscleName);
                tableRow.addView(freq);
                tableRow.addView(sets);


                tableRow.setGravity(1);
                tableLayout.addView(tableRow);

            }
        }
    }

    public void StartNewActivity()
    {
        Intent intent = new Intent(this, MuscleListActivity.class);
        startActivity(intent);
    }

    public void StartNext()
    {
        Intent intent = new Intent(this, RecoveryMuscleList.class);
        startActivity(intent);
    }

    private void dietTest()
    {

        //TextView textView = (TextView)findViewById(R.id.textTuesday);

        ExpressionsBasedModel model = new ExpressionsBasedModel();

        //model.addVariable(new Variable("corn").lower(0).upper(10).weight(0.18));

        Variable day1Difference = new Variable("Day1Difference");
        day1Difference.weight(1);

        Variable day2Difference = new Variable("Day2Difference");
        day2Difference.weight(1);

        Variable day3Difference = new Variable("Day3Difference");
        day3Difference.weight(1);

        model.addVariable(day1Difference);
        model.addVariable(day2Difference);
        model.addVariable(day3Difference);

        int[] days = new int[]{1, 0, 1, 0, 1, 0 ,0};
        String[] exercises = new String[]{"c", "b", "q"};
        int[] avgSets = new int[]{5, 6, 3};

        for (int i = 0; i < days.length; i++)
        {
            if (days[i] != 1)
            {
                for (int j = 0; j < exercises.length; j++)
                {
                    String name = exercises[j] + (i + 1);
                    Variable var = new Variable(name);
                    var.binary();
                    var.setValue(BigDecimal.ZERO);
                }
            }
            else
            {
                String name = "Day " + i;
                Expression expr = model.addExpression(name).lower(0).upper(9.33);


                for (int j = 0; j < 3; j++)
                {
                    Variable var = new Variable(exercises[j] + (i + 1));
                    var.binary();
                    expr.setLinearFactor(var, avgSets[j]);
                }

                if (i == 1)
                {
                    expr.setLinearFactor(day1Difference, -1);
                }
                else if (i == 3)
                {
                    expr.setLinearFactor(day2Difference, -1);
                }
                else if (i == 5)
                {
                    expr.setLinearFactor(day3Difference, -1);
                }

            }
        }

        //freq
        for (int i = 0; i < exercises.length; i++)
        {
            Expression freq = model.addExpression("Frequency " + (i +1)).lower(2).upper(2);
            List<Variable> lol = model.getVariables();

            for (int j = 0; j < days.length; j++)
            {
                if (days[j] == 1)
                {
                    String name = exercises[i] + j;
                    Variable var;

                    for (int k = 0; k < lol.size(); k++)
                    {
                        var = lol.get(k);
                        if (var.getName().equals(name))
                        {
                            freq.setLinearFactor(var, 1);
                        }
                    }
                }
            }
        }

        //
        Variable bread = new Variable("Bread");
        bread.lower(0);
        bread.upper(10);
        bread.weight(0.05);

        model.addVariable(bread);

        Variable corn = new Variable("Corn");
        corn.lower(0);
        corn.upper(10);
        corn.weight(0.18);

        model.addVariable(corn);

        Variable milk = new Variable("Milk");
        milk.lower(0);
        milk.upper(10);
        milk.weight(0.23);

        model.addVariable(milk);


        // Create a vitamin A constraint.
        // Set lower and upper limits and then specify how much vitamin A a serving of each of th

        Expression vitaminA = model.addExpression("Vitamin A").lower(5000).upper(50000);
        //vitaminA.set(bread, 0).set(corn, 107).set(milk, 500);
        vitaminA.setLinearFactor(bread, 0);
        vitaminA.setLinearFactor(corn, 107);
        vitaminA.setLinearFactor(milk, 500);



        Expression calories = model.addExpression("Calories").lower(2000).upper(2250);
        //calories.set(bread, 65).set(corn, 72).set(milk, 121);
        calories.setLinearFactor(bread, 65);
        calories.setLinearFactor(corn, 72);
        calories.setLinearFactor(milk, 121);

        bread.integer(true);
        corn.integer(true);
        milk.integer(true);

        // Solve the problem - minimise the cost
        Optimisation.Result result = model.minimise();

        //textView.setText(result.toString());

    }

}
