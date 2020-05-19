package com.thegeekylad.madautomate.Utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.SystemClock;
import android.widget.Toast;

import com.thegeekylad.madautomate.Action;
import com.thegeekylad.madautomate.AlarmReceiver;
import com.thegeekylad.madautomate.Point;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

public class Helper {

    private Context context;
    private SharedPreferences preferences;

    public Helper(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences("config", Context.MODE_PRIVATE);
    }

    public void setAttribute(String ruleName, String key, String value) {
        try {
            JSONObject rulesObject = new JSONObject(preferences.getString("rules", "{}"));
            JSONObject ruleDetail = (JSONObject) rulesObject.get(ruleName);
            ruleDetail.put(key, value);
            rulesObject.put(ruleName, ruleDetail);
            preferences.edit().putString("rules", rulesObject.toString()).apply();
        } catch (JSONException e) {
            Toast.makeText(context, "[ERROR] " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void setSwitch(String ruleName, boolean isOn) {
        try {
            JSONObject rulesObject = new JSONObject(preferences.getString("rules", "{}"));
            JSONObject ruleDetail = (JSONObject) rulesObject.get(ruleName);
            ruleDetail.put("switch", isOn);
            rulesObject.put(ruleName, ruleDetail);
            preferences.edit().putString("rules", rulesObject.toString()).apply();
        } catch (JSONException e) {
            Toast.makeText(context, "[ERROR] " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public JSONObject getRulesObject() {
        try {
            return new JSONObject(preferences.getString("rules", "{}"));
        } catch (JSONException e) {
            Toast.makeText(context, "[ERROR] " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public boolean handleAlarm(String ruleName, boolean shouldSet) {
        try {
            JSONObject thisRule = (JSONObject) new JSONObject(preferences.getString("rules", "{}")).get(ruleName);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context.getApplicationContext(), AlarmReceiver.class);
            intent.putExtra("rule_name", ruleName);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context.getApplicationContext(),
                    thisRule.getInt("request_code"),
                    intent,
                    0);
            if (!shouldSet) {
                alarmManager.cancel(pendingIntent);
                return true;
            }
            String time = thisRule.getString("time");
            if (time.length() == 0) return false;
            String[] timeConfig = time.split(":");
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeConfig[0]));
            calendar.set(Calendar.MINUTE, Integer.parseInt(timeConfig[1]));
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent);
            return true;
        } catch (JSONException e) {
            Toast.makeText(context, "[ERROR] " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public void deleteRule(String ruleName) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "mad-automate-rules");
            File ruleFile = new File(dir, ruleName);
            if (ruleFile.delete()) {
                JSONObject rulesObject = new JSONObject(preferences.getString("rules", "{}"));
                handleAlarm(ruleName, false);
                rulesObject.remove(ruleName);
                preferences.edit().putString("rules", rulesObject.toString()).apply();
            } else Toast.makeText(context, "Couldn't delete this rule for some reason.", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(context, "[ERROR] " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public ArrayList<Action> deserialize(String ruleName) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "mad-automate-rules");
            BufferedReader reader = new BufferedReader(new FileReader(new File(dir, ruleName)));
            ArrayList<Action> actions = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] actionType = line.split(":");
                if (Integer.parseInt(actionType[0]) == 1) {  // 'commandAction' (either adb or custom)
                    if (Integer.parseInt(actionType[1]) == 0)
                        actions.add(new Action(actionType[2], false));
                    else actions.add(new Action(actionType[2], true));
                }
                else {
                    String[] coordinates = actionType[2].split(" ");
                    if (Integer.parseInt(actionType[1]) == 0)
                        actions.add(new Action(new Point(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]))));
                    else
                        actions.add(new Action(new Point(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]), new Point(Integer.parseInt(coordinates[2]), Integer.parseInt(coordinates[3])))));
                }
            }
            reader.close();
            return actions;
        } catch (IOException e) {
            Toast.makeText(context, "[ERROR] " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

}
