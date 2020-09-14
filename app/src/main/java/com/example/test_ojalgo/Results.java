package com.example.test_ojalgo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Results extends AppCompatActivity
{

    TextView textView;
    SharedPreferences sharedPref;

    ExpressionsBasedModel model;
    String[] baseArray;
    String[] muscles;
    String[] recovery;
    String[] days;
    int[] freq;
    int[] freqRecovery;
    int[] sets;
    float[] avgSets;
    int totalSets;
    int dailyAvgSets;
    int workingDays;
    int restLoop = 0;
    BigDecimal[] dayDiff;
    double thresholdDayDiff;
    String[] activeMuscleVars;

    //OptimiseSecond
    int[] minSets;
    int[] maxSets;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        textView = findViewById(R.id.textViewResults);

        sharedPref = getSharedPreferences("Muscle List", MODE_PRIVATE);
        String baseString = sharedPref.getString("Muscles", "");
        baseString = baseString.substring(0, baseString.length() - 1);

        baseArray = baseString.split(";");
        recovery = sharedPref.getString("Recovery List", "").split(";");
        days = sharedPref.getString("Days", "").split(";");

        muscles = new String[baseArray.length];
        freq = new int[baseArray.length];
        freqRecovery = new int[baseArray.length];
        sets = new int[baseArray.length];
        avgSets = new float[baseArray.length];

        for (int i = 0; i < baseArray.length; i++) {
            String[] items = baseArray[i].split("/");

            muscles[i] = items[0];
            freq[i] = Integer.parseInt(items[1]);
            sets[i] = Integer.parseInt(items[2]);
        }

        //calculate daily average sets in the week
        for (int i = 0; i < freq.length; i++)
        {
            totalSets += sets[i];
        }

        for (int i = 0; i < days.length; i++)
        {
            if (Integer.parseInt(days[i]) == 1)
            {
                workingDays++;
            }
        }

        dailyAvgSets = (totalSets / workingDays);

        int workingDaysInt = workingDays;
        dayDiff = new BigDecimal[workingDaysInt];

        Log.d("totalSets", Double.toString(totalSets));
        Log.d("workingDays", Double.toString(workingDays));
        Log.d("dailyAvgSets", Double.toString(dailyAvgSets));

        //calculate avg sets
        for (int i = 0; i < avgSets.length; i++)
        {
            float avgSetsFloat = (float) sets[i] / (float) freq[i];
            BigDecimal avgSetBD = new BigDecimal(avgSetsFloat).setScale(2, RoundingMode.HALF_UP);
            avgSets[i] = avgSetBD.floatValue();
            Log.d("xdd", "" + avgSetsFloat);

            Log.d("avgSets", "" + avgSets[i]);
        }

        thresholdDayDiff = 0.2;

        StartOptimise();

    }

    public void StartOptimise()
    {
        OptimiseThread optimiseThread = new OptimiseThread();
        optimiseThread.start();
    }


    public void Optimise()
    {

        model = new ExpressionsBasedModel();


        //objective function
        for (int i = 0; i < days.length; i++) {
            int day = Integer.parseInt(days[i]);
            if (day == 1) {
                Variable var = new Variable("Day" + (i + 1));
                var.weight(1);
                //var.lower(0);
                model.addVariable(var);
                Log.d("objFunc", "Added obj func var: " + var.getName());
            }

        }

        //add variables
        for (int i = 0; i < muscles.length; i++) {
            for (int j = 0; j < days.length; j++) {
                String name = muscles[i] + (j + 1);
                Variable var = new Variable(name);
                var.binary();
                model.addVariable(var);
                Log.d("addVars", "Added var: " + var.getName());
            }
        }

        List<Variable> varList = model.getVariables();

        //rest days set to zero
        for (int i = 0; i < days.length; i++) {
            if (Integer.parseInt(days[i]) == 0) {
                for (int j = 0; j < muscles.length; j++) {
                    String name = muscles[j] + (i + 1);
                    Variable var;

                    for (int k = 0; k < varList.size(); k++) {
                        var = varList.get(k);
                        if (var.getName().equals(name)) {
                            var.lower(0).upper(0);
                            Log.d("restDayVars", "Rest day var value set to zero: " + var.getName());
                        }
                    }
                }
            }
        }

        //avg sets constraints
        for (int i = 0; i < days.length; i++) {
            if (Integer.parseInt(days[i]) == 1) {
                Expression expression = model.addExpression("Avg sets constraint day (neg)" + (i + 1)).upper(dailyAvgSets);

                for (int j = 0; j < muscles.length; j++) {
                    String name = muscles[j] + (i + 1);
                    Variable var;

                    for (int k = 0; k < varList.size(); k++) {
                        var = varList.get(k);
                        if (var.getName().equals(name)) {
                            expression.setLinearFactor(var, avgSets[j]);
                            Log.d("avgSetsConstraintNeg", "Linear Factor set for: " + expression.getName() + " args: " + var.getName() + ", " + avgSets[j]);
                        }
                    }
                }

                //objective function variables
                String name = "Day" + (i + 1);
                Variable var;

                for (int j = 0; j < varList.size(); j++) {
                    var = varList.get(j);
                    if (var.getName().equals(name)) {
                        expression.setLinearFactor(var, -1);
                        Log.d("objFuncVarsAvgSetsConstNeg", "Obj function var set for avg sets constraint: " + expression.getName() + ", " + var.getName());
                    }
                }

            }

            // + dayDiff
            if (Integer.parseInt(days[i]) == 1) {
                Expression expression = model.addExpression("Avg sets constraint day (pos)" + (i + 1)).lower(dailyAvgSets);

                for (int j = 0; j < muscles.length; j++) {
                    String name = muscles[j] + (i + 1);
                    Variable var;

                    for (int k = 0; k < varList.size(); k++) {
                        var = varList.get(k);
                        if (var.getName().equals(name)) {
                            expression.setLinearFactor(var, avgSets[j]);
                            Log.d("avgSetsConstraintPos", "Linear Factor set for: " + expression.getName() + " args: " + var.getName() + ", " + avgSets[j]);
                        }
                    }
                }

                //objective function variables
                String name = "Day" + (i + 1);
                Variable var;

                for (int j = 0; j < varList.size(); j++) {
                    var = varList.get(j);
                    if (var.getName().equals(name)) {
                        expression.setLinearFactor(var, 1);
                        Log.d("objFuncVarsAvgSetsConstPos", "Obj function var set for avg sets constraint: " + expression.getName() + ", " + var.getName());
                    }
                }

            }

        }

        //frequency constraints
        for (int i = 0; i < muscles.length; i++) {
            Expression expression = model.addExpression("Frequency constraint for " + muscles[i]).lower(freq[i]).upper(freq[i]);

            for (int j = 0; j < days.length; j++) {
                if (Integer.parseInt(days[j]) == 1) {
                    String name = muscles[i] + (j + 1);
                    Variable var;

                    for (int k = 0; k < varList.size(); k++) {
                        var = varList.get(k);
                        if (var.getName().equals(name)) {
                            expression.setLinearFactor(var, 1);
                            Log.d("freqConstraint", "Muscle added to freq constraint: " + var.getName());
                        }
                    }
                }
            }
        }

        //rest day constraints

        //match frequency with recovery list
        for (int i = 0; i < recovery.length; i++) {
            for (int j = 0; j < muscles.length; j++) {
                if (recovery[i].equals(muscles[j])) {
                    freqRecovery[i] = freq[j];
                    Log.d("freqRecoveryMatch", "Matched freq for recovery list: " + recovery[i] + ", " + freqRecovery[i]);
                }
            }
        }

        for (int i = 0; i < recovery.length - restLoop; i++) {
            for (int j = 0; j < days.length; j++) {
                if (freqRecovery[i] == 2) {
                    Expression expression = model.addExpression("Rest constraint for " + recovery[i] + "/" + j).lower(0).upper(1);

                    for (int k = 0; k < 3; k++) {
                        String name;

                        if ((j + k + 1) == 8) {
                            name = recovery[i] + 1;
                        } else if ((j + k + 1) == 9) {
                            name = recovery[i] + 2;
                        } else {
                            name = recovery[i] + (j + k + 1);
                        }

                        Variable var;

                        for (int l = 0; l < varList.size(); l++) {
                            var = varList.get(l);
                            if (var.getName().equals(name)) {
                                expression.setLinearFactor(var, 1);
                                Log.d("restConstraint", "Var " + var.getName() + " added to expression " + expression.getName());
                            }
                        }

                    }
                } else if (freqRecovery[i] == 3) {
                    Expression expression = model.addExpression("Rest constraint for " + recovery[i] + "/" + j).lower(0).upper(1);

                    for (int k = 0; k < 2; k++) {
                        String name;

                        if ((j + k + 1) == 8) {
                            name = recovery[i] + 1;
                        } else {
                            name = recovery[i] + (j + k + 1);
                        }

                        Variable var;

                        for (int l = 0; l < varList.size(); l++) {
                            var = varList.get(l);

                            if (var.getName().equals(name)) {
                                expression.setLinearFactor(var, 1);
                                Log.d("restConstraint", "Var " + var.getName() + " added to expression " + expression.getName());
                            }
                        }
                    }
                }
            }
        }


        //optimise
        Optimisation.Result result = model.minimise();
        Log.d("optimise", "Optimisation is done");


        //check if there is a result
        Log.d("resultState", "" + result.getState());

        if (result.getState() != Optimisation.State.OPTIMAL) {
            restLoop++;
            Optimise();
        } else {
            //check daily diffs
            int counter = 0;
            for (int i = 0; i < days.length; i++) {
                int day = Integer.parseInt(days[i]);
                if (day == 1) {
                    String name = "Day" + (i + 1);
                    Variable var;

                    for (int j = 0; j < varList.size(); j++) {
                        var = varList.get(j);

                        if (var.getName().equals(name)) {
                            dayDiff[counter] = var.getValue();
                            counter++;
                            Log.d("dayDiff", "Obj func var " + var.getName() + " has the value " + var.getValue());
                        }
                    }
                }
            }

            //check if daily diffs exceed the threshold
            for (int i = 0; i < dayDiff.length; i++) {
                if (dayDiff[i] != null) {
                    if (dayDiff[i].doubleValue() > (dailyAvgSets * thresholdDayDiff)) {

                        if (restLoop == recovery.length) {
                            textView.setText("No result was found");
                            Log.d("noRestConstraints", "All rest constraints have been removed");
                            return;
                        } else {
                            restLoop++;
                            Log.d("tooMuchRest", "DayDiff exceeds 20% " + i);
                            Optimise();
                            return;
                        }
                    }
                }
            }
        }


        //textView.setText(model.toString());


        ////////////
        ////////////
        ////////////
        ////////////


        //declare size of activeMuscleVars
        int totalFreq = 0;

        for (int i = 0; i < freq.length; i++) {
            totalFreq += freq[i];
        }
        activeMuscleVars = new String[totalFreq];

        //get active muscle variables from result
        List<Variable> resultVars = model.getVariables();

        int counter = 0;
        for (int i = (int) workingDays; i < resultVars.size(); i++) {
            Variable var = resultVars.get(i);

            if (var.getValue().intValueExact() == 1) {
                activeMuscleVars[counter] = var.getName();
                Log.d("activeMuscleVar", "" + activeMuscleVars[counter]);
                counter++;
            }
        }

        //OptimiseSecond();

    }


    public void OptimiseTest()
    {

        ExpressionsBasedModel model = new ExpressionsBasedModel();

        //objective function
        Variable x1 = new Variable("x1");
        x1.weight(2);

        model.addVariable(x1);

        Variable x2 = new Variable("x2");
        x2.weight(3);

        model.addVariable(x2);

        //variables not used in the objective function
        Variable x3 = new Variable("x3");

        model.addVariable(x3);

        //constraints
        Expression constr1 = model.addExpression("Constraint 1").lower(4);
        constr1.setLinearFactor(x1, 1);

        Expression constr2 = model.addExpression("Constrain 2").lower(10);
        constr2.setLinearFactor(x1, 1);
        constr2.setLinearFactor(x2, 3);

        Expression constr3 = model.addExpression("Constraint 3").lower(4).upper(4);
        constr3.setLinearFactor(x2, 1);
        constr3.setLinearFactor(x3, 1);

        //optimise
        Optimisation.Result result = model.minimise();
        textView.setText(model.toString());

    }


    class OptimiseThread extends Thread
    {
        @Override
        public void run()
        {

            model = new ExpressionsBasedModel();


            //objective function
            for (int i = 0; i < days.length; i++)
            {
                int day = Integer.parseInt(days[i]);
                if (day == 1)
                {
                    Variable var = new Variable("Day" + (i + 1));
                    var.weight(1);
                    //var.lower(0);
                    model.addVariable(var);
                    Log.d("objFunc", "Added obj func var: " + var.getName());
                }

            }

            //add variables
            for (int i = 0; i < muscles.length; i++)
            {
                for (int j = 0; j < days.length; j++)
                {
                    String name = muscles[i] + (j + 1);
                    Variable var = new Variable(name);
                    var.binary();
                    model.addVariable(var);
                    Log.d("addVars", "Added var: " + var.getName());
                }
            }

            List<Variable> varList = model.getVariables();

            //rest days set to zero
            for (int i = 0; i < days.length; i++)
            {
                if (Integer.parseInt(days[i]) == 0)
                {
                    for (int j = 0; j < muscles.length; j++)
                    {
                        String name = muscles[j] + (i + 1);
                        Variable var;

                        for (int k = 0; k < varList.size(); k++)
                        {
                            var = varList.get(k);
                            if (var.getName().equals(name))
                            {
                                var.lower(0).upper(0);
                                Log.d("restDayVars", "Rest day var value set to zero: " + var.getName());
                            }
                        }
                    }
                }
            }

            //avg sets constraints
            for (int i = 0; i < days.length; i++)
            {
                if (Integer.parseInt(days[i]) == 1)
                {
                    Expression expression = model.addExpression("Avg sets constraint day (neg)" + (i + 1)).upper(dailyAvgSets);

                    for (int j = 0; j < muscles.length; j++)
                    {
                        String name = muscles[j] + (i + 1);
                        Variable var;

                        for (int k = 0; k < varList.size(); k++)
                        {
                            var = varList.get(k);
                            if (var.getName().equals(name)) {
                                expression.setLinearFactor(var, avgSets[j]);
                                Log.d("avgSetsConstraintNeg", "Linear Factor set for: " + expression.getName() + " args: " + var.getName() + ", " + avgSets[j]);
                            }
                        }
                    }

                    //objective function variables
                    String name = "Day" + (i + 1);
                    Variable var;

                    for (int j = 0; j < varList.size(); j++)
                    {
                        var = varList.get(j);
                        if (var.getName().equals(name))
                        {
                            expression.setLinearFactor(var, -1);
                            Log.d("objFuncVarsAvgSetsConstNeg", "Obj function var set for avg sets constraint: " + expression.getName() + ", " + var.getName());
                        }
                    }
                }

                // + dayDiff
                if (Integer.parseInt(days[i]) == 1)
                {
                    Expression expression = model.addExpression("Avg sets constraint day (pos)" + (i + 1)).lower(dailyAvgSets);

                    for (int j = 0; j < muscles.length; j++)
                    {
                        String name = muscles[j] + (i + 1);
                        Variable var;

                        for (int k = 0; k < varList.size(); k++)
                        {
                            var = varList.get(k);
                            if (var.getName().equals(name))
                            {
                                expression.setLinearFactor(var, avgSets[j]);
                                Log.d("avgSetsConstraintPos", "Linear Factor set for: " + expression.getName() + " args: " + var.getName() + ", " + avgSets[j]);
                            }
                        }
                    }

                    //objective function variables
                    String name = "Day" + (i + 1);
                    Variable var;

                    for (int j = 0; j < varList.size(); j++)
                    {
                        var = varList.get(j);
                        if (var.getName().equals(name)) {
                            expression.setLinearFactor(var, 1);
                            Log.d("objFuncVarsAvgSetsConstPos", "Obj function var set for avg sets constraint: " + expression.getName() + ", " + var.getName());
                        }
                    }
                }
            }

            //frequency constraints
            for (int i = 0; i < muscles.length; i++)
            {
                Expression expression = model.addExpression("Frequency constraint for " + muscles[i]).lower(freq[i]).upper(freq[i]);

                for (int j = 0; j < days.length; j++)
                {
                    if (Integer.parseInt(days[j]) == 1)
                    {
                        String name = muscles[i] + (j + 1);
                        Variable var;

                        for (int k = 0; k < varList.size(); k++)
                        {
                            var = varList.get(k);
                            if (var.getName().equals(name))
                            {
                                expression.setLinearFactor(var, 1);
                                Log.d("freqConstraint", "Muscle added to freq constraint: " + var.getName());
                            }
                        }
                    }
                }
            }

            //rest day constraints

            //match frequency with recovery list
            for (int i = 0; i < recovery.length; i++)
            {
                for (int j = 0; j < muscles.length; j++)
                {
                    if (recovery[i].equals(muscles[j]))
                    {
                        freqRecovery[i] = freq[j];
                        Log.d("freqRecoveryMatch", "Matched freq for recovery list: " + recovery[i] + ", " + freqRecovery[i]);
                    }
                }
            }

            for (int i = 0; i < recovery.length - restLoop; i++)
            {
                for (int j = 0; j < days.length; j++)
                {
                    if (freqRecovery[i] == 2)
                    {
                        Expression expression = model.addExpression("Rest constraint for " + recovery[i] + "/" + j).lower(0).upper(1);

                        for (int k = 0; k < 3; k++)
                        {
                            String name;

                            if ((j + k + 1) == 8)
                            {
                                name = recovery[i] + 1;
                            }
                            else if ((j + k + 1) == 9)
                            {
                                name = recovery[i] + 2;
                            }
                            else
                            {
                                name = recovery[i] + (j + k + 1);
                            }

                            Variable var;

                            for (int l = 0; l < varList.size(); l++)
                            {
                                var = varList.get(l);
                                if (var.getName().equals(name))
                                {
                                    expression.setLinearFactor(var, 1);
                                    Log.d("restConstraint", "Var " + var.getName() + " added to expression " + expression.getName());
                                }
                            }
                        }
                    }
                    else if (freqRecovery[i] == 3)
                    {
                        Expression expression = model.addExpression("Rest constraint for " + recovery[i] + "/" + j).lower(0).upper(1);

                        for (int k = 0; k < 2; k++)
                        {
                            String name;

                            if ((j + k + 1) == 8)
                            {
                                name = recovery[i] + 1;
                            }
                            else
                            {
                                name = recovery[i] + (j + k + 1);
                            }

                            Variable var;

                            for (int l = 0; l < varList.size(); l++)
                            {
                                var = varList.get(l);

                                if (var.getName().equals(name))
                                {
                                    expression.setLinearFactor(var, 1);
                                    Log.d("restConstraint", "Var " + var.getName() + " added to expression " + expression.getName());
                                }
                            }
                        }
                    }
                }
            }


            //optimise
            Optimisation.Result result = model.minimise();
            Log.d("optimise", "Optimisation is done");


            //check if there is a result
            Log.d("resultState", "" + result.getState());

            if (result.getState() != Optimisation.State.OPTIMAL)
            {
                restLoop++;
                StartOptimise();
            }
            else
            {
                //check daily diffs
                int counter = 0;
                for (int i = 0; i < days.length; i++)
                {
                    int day = Integer.parseInt(days[i]);
                    if (day == 1) {
                        String name = "Day" + (i + 1);
                        Variable var;

                        for (int j = 0; j < varList.size(); j++)
                        {
                            var = varList.get(j);

                            if (var.getName().equals(name))
                            {
                                dayDiff[counter] = var.getValue();
                                counter++;
                                Log.d("dayDiff", "Obj func var " + var.getName() + " has the value " + var.getValue());
                            }
                        }
                    }
                }

                //check if daily diffs exceed the threshold
                for (int i = 0; i < dayDiff.length; i++)
                {
                    if (dayDiff[i] != null)
                    {
                        if (dayDiff[i].doubleValue() > (dailyAvgSets * thresholdDayDiff))
                        {

                            if (restLoop == recovery.length)
                            {
                                textView.setText("No result was found");
                                Log.d("noRestConstraints", "All rest constraints have been removed");
                                return;
                            }
                            else
                            {
                                restLoop++;
                                Log.d("tooMuchRest", "DayDiff exceeds 20% " + i);
                                StartOptimise();
                                return;
                            }
                        }
                    }
                }
            }


            //textView.setText(model.toString());


            ////////////
            ////////////
            ////////////
            ////////////


            //declare size of activeMuscleVars
            int totalFreq = 0;

            for (int i = 0; i < freq.length; i++)
            {
                totalFreq += freq[i];
            }
            activeMuscleVars = new String[totalFreq];

            //get active muscle variables from result
            List<Variable> resultVars = model.getVariables();

            int counter = 0;
            for (int i = workingDays; i < resultVars.size(); i++)
            {
                Variable var = resultVars.get(i);

                if (var.getValue().intValueExact() == 1)
                {
                    activeMuscleVars[counter] = var.getName();
                    Log.d("activeMuscleVar", "" + activeMuscleVars[counter]);
                    counter++;
                }
            }

            model.destroy();
            OptimiseSecond();
        }

        void OptimiseSecond()
        {
            //calculate min and max sets
            double threshold = 0.2;

            minSets = new int[activeMuscleVars.length];
            maxSets = new int[activeMuscleVars.length];

            int counter = 0;

            for (int i = 0; i < sets.length; i++)
            {
                for (int j = 0; j < freq[i]; j++)
                {
                    minSets[counter] = (int) Math.round(avgSets[i] * (1 - threshold));
                    maxSets[counter] = (int) Math.round(avgSets[i] * (1 + threshold));
                    //Log.d("minMaxSets", "Set min and max sets for " + activeMuscleVars[i + j] + ": " + minSets[i + j] + ", " + maxSets[i + j]);
                    counter++;
                }
            }

            //log
            for (int i = 0; i < minSets.length; i++)
            {
                Log.d("minMaxSetsConstraint", "" + activeMuscleVars[i] + " min: " + minSets[i] + " max: " + maxSets[i]);
            }

            //new model
            ExpressionsBasedModel modelSecond = new ExpressionsBasedModel();

            //objective function
            for (int i = 0; i < days.length; i++)
            {
                int day = Integer.parseInt(days[i]);
                if (day == 1)
                {
                    Variable var = new Variable("Day" + (i + 1));
                    var.weight(1);
                    //var.integer(true);
                    modelSecond.addVariable(var);
                    Log.d("secondObjFunc", "Added obj func var: " + var.getName());
                }
            }

            //add variables + min and max constraints
            for (int i = 0; i < activeMuscleVars.length; i++)
            {
                Variable var = new Variable(activeMuscleVars[i]);
                var.lower(minSets[i]);
                var.upper(maxSets[i]);
                var.integer(true);
                modelSecond.addVariable(var);
                Log.d("secondAddVars", "Added var: " + var.getName() + " lower: " + var.getLowerLimit() + " upper: " + var.getUpperLimit());
            }

            List<Variable> varList = modelSecond.getVariables();

            //dayDiff constraints
            for (int i = 0; i < days.length; i++)
            {
                if (Integer.parseInt(days[i]) == 1)
                {
                    Expression expression = modelSecond.addExpression("DayDiff constraint day (neg)" + (i + 1)).upper(dailyAvgSets);

                    for (int j = 0; j < muscles.length; j++)
                    {
                        String name = muscles[j] + (i + 1);
                        Variable var;

                        for (int k = 0; k < varList.size(); k++)
                        {
                            var = varList.get(k);
                            if (var.getName().equals(name))
                            {
                                expression.setLinearFactor(var, 1);
                                Log.d("dayDiffConstraintNeg", "Linear factor set for: " + expression.getName() + " args: " + var.getName());
                            }
                        }
                    }

                    //objective function variables
                    String name = "Day" + (i + 1);
                    Variable var;

                    for (int j = 0; j < varList.size(); j++)
                    {
                        var = varList.get(j);
                        if (var.getName().equals(name))
                        {
                            expression.setLinearFactor(var, -1);
                            Log.d("objFuncVarDayDiffConstNeg", "Obj function var set for dayDiff constraint: " + expression.getName() + ", " + var.getName());
                        }
                    }
                }

                // + dayDiff
                if (Integer.parseInt(days[i]) == 1)
                {
                    Expression expression = modelSecond.addExpression("DayDiff constraint day (pos)" + (i + 1)).lower(dailyAvgSets);

                    for (int j = 0; j < muscles.length; j++)
                    {
                        String name = muscles[j] + (i + 1);
                        Variable var;

                        for (int k = 0; k < varList.size(); k++)
                        {
                            var = varList.get(k);
                            if (var.getName().equals(name))
                            {
                                expression.setLinearFactor(var, 1);
                                Log.d("dayDiffConstraintPos", "Linear factor set for: " + expression.getName() + " args: " + var.getName());
                            }
                        }
                    }

                    //objective function variables
                    String name = "Day" + (i + 1);
                    Variable var;

                    for (int j = 0; j < varList.size(); j++)
                    {
                        var = varList.get(j);
                        if (var.getName().equals(name))
                        {
                            expression.setLinearFactor(var, 1);
                            Log.d("objFuncVarDayDiffConstPos", "Obj function var set for dayDiff constraint: " + expression.getName() + ", " + var.getName());
                        }
                    }
                }

            }

            //total volume constraints
            for (int i = 0; i < muscles.length; i++)
            {
                Expression expression = modelSecond.addExpression("Total volume constraint for " + muscles[i]).lower(sets[i]).upper(sets[i]);

                for (int j = 0; j < days.length; j++)
                {
                    if (Integer.parseInt(days[j]) == 1)
                    {
                        String name = muscles[i] + (j + 1);
                        Variable var;

                        for (int k = 0; k < varList.size(); k++)
                        {
                            var = varList.get(k);
                            if (var.getName().equals(name))
                            {
                                expression.setLinearFactor(var, 1);
                                Log.d("totalVolumeConstraint", "Muscle added to total volume constraint: " + var.getName());
                            }
                        }
                    }
                }
            }

            Optimisation.Result result = modelSecond.minimise();
            //textView.setText(modelSecond.toString());
            DisplayResults(modelSecond);

        }

        void DisplayResults(ExpressionsBasedModel model)
        {

            textView.setText("");

            for (int i = 0; i < days.length; i++)
            {
                if (Integer.parseInt(days[i]) == 1)
                {
                    textView.setText(textView.getText() + "Day" + (i + 1) + "\n");

                    for (int j = workingDays; j < model.countVariables(); j++)
                    {
                        String endString = String.valueOf((i + 1));
                        if (model.getVariable(j).getName().endsWith(endString))
                        {
                            textView.setText(textView.getText() + model.getVariable(j).getName() + ": " + model.getVariable(j).getValue().intValue() + "\n");
                        }
                    }
                }

                textView.setText(textView.getText() + "\n");
            }
        }

    }
}
