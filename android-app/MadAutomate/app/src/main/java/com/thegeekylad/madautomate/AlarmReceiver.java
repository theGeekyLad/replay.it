package com.thegeekylad.madautomate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.widget.Toast;

import com.thegeekylad.madautomate.Utils.Helper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Helper helper = new Helper(context);
            String ruleName = intent.getStringExtra("rule_name");
            ArrayList<Action> actions = helper.deserialize(ruleName);
            for (Action action : actions) {
                if (action.touchAction != null) {
                    Point gesture = action.touchAction;
                    if (gesture.toPoint == null)  // tap
                        runCommand(String.format("input tap %s %s", gesture.x, gesture.y));
                    else  // swipe
                        runCommand(String.format("input swipe %s %s %s %s", gesture.x, gesture.y, gesture.toPoint.x, gesture.toPoint.y));
                } else {
                    if (!action.isCustomCommandAction)
                        runCommand(action.commandAction.substring(10));
                    else {
                        /*
                         * Assumes all custom action bits are separated by spaces, so handle accordingly
                         */
                        String[] commandActionBits = action.commandAction.split(" ");
                        // sleeper custom action
                        if (commandActionBits[0].equals("wait"))
                            Thread.sleep(Integer.parseInt(commandActionBits[1]) * 1000);
                    }
                }
                Thread.sleep(1000);  // let it breathe
            }
            helper.setSwitch(ruleName, false);
            helper.handleAlarm(ruleName, false);
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
//        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void runCommand(String cmd) throws Exception {
        Process p = Runtime.getRuntime().exec("su");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
        writer.write(cmd + "\n");
        writer.flush();
        writer.write("exit\n");
        writer.flush();
        writer.close();
        p.waitFor();
    }
}
