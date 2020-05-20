package com.thegeekylad.madautomate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import com.thegeekylad.madautomate.Adapters.RuleAdapter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {

            // prompt for superuser access
            Runtime.getRuntime().exec("su");  // prompts for superuser access in case you missed it!

            // get config files
            File dir = new File(Environment.getExternalStorageDirectory(), "mad-automate-rules");
            String[] configNames = dir.list();
            ArrayList<String> configNamesList = new ArrayList<>();
            if (configNames == null) {
                Toast.makeText(this, "[ERROR] Couldn't list files.", Toast.LENGTH_SHORT).show();
                return;
            }

            // initialize config storage
            SharedPreferences preferences = getSharedPreferences("config", MODE_PRIVATE);
            JSONObject rulesObject = new JSONObject(preferences.getString("rules", "{}"));
            int requestCode = preferences.getInt("max", 1);
            // prepare 'rulesObject'
            for (String configName : configNames) {
                configNamesList.add(configName);
                if (rulesObject.has(configName)) continue;
                JSONObject configDetail = new JSONObject();
                configDetail.put("time", "");
                configDetail.put("request_code", requestCode++);
                configDetail.put("switch", false);
                rulesObject.put(configName, configDetail);
            }
            preferences.edit().putInt("max", requestCode).apply();  // updating new value of max pointer for requestCode
            Toast.makeText(this, String.format("New max = %s.", requestCode), Toast.LENGTH_SHORT).show();
            // remove stale rules from 'rulesObject' (reverse lookup)
            Iterator<String> rulesObjectKeys = rulesObject.keys();
            while (rulesObjectKeys.hasNext()) {
                String ruleObjectKey = rulesObjectKeys.next();
                if (configNamesList.indexOf(ruleObjectKey) == -1) rulesObject.remove(ruleObjectKey);
                // [revisit] ideally you're supposed to cancel these alarms too
            }

            // init list
            ListView listView = (ListView) findViewById(R.id.list_view);
            listView.setAdapter(new RuleAdapter(getApplicationContext(), MainActivity.this, rulesObject, listView));
            preferences.edit().putString("rules", rulesObject.toString()).apply();

        } catch (Exception e) {
            Toast.makeText(this, "[ERROR] " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

//        findViewById(R.id.auto_run_button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//                Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
//                PendingIntent pendingIntent = PendingIntent.getBroadcast(
//                        getApplicationContext(),
//                        0,
//                        intent,
//                        0);
//                alarmManager.setExactAndAllowWhileIdle(
//                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                        SystemClock.elapsedRealtime() + 15 * 60 * 1000,
//                        pendingIntent);
//            }
//        });

//        try {
//            Process p = Runtime.getRuntime().exec("su");
//            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
//            writer.write("input text \"Hey there Delilah, what's it like in New York city, I'm a thousand miles away but girl tonight you look so pretty yes you do!\"\n");
//            writer.flush();
//            writer.write("exit\n");
//            writer.flush();
//            p.waitFor();
//            Toast.makeText(this, "Done.", Toast.LENGTH_SHORT).show();
//        } catch (Exception e) {
//            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
//        }
    }
}
