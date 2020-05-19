package com.thegeekylad.madautomate.Adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.thegeekylad.madautomate.Action;
import com.thegeekylad.madautomate.Point;
import com.thegeekylad.madautomate.R;
import com.thegeekylad.madautomate.Utils.Helper;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Iterator;

public class RuleAdapter extends BaseAdapter {

    private ArrayList<JSONObject> rulesList;
    private Context context;
    private Activity activity;
    private JSONObject rulesObject;
    private ListView listView;
    private Helper helper;

    public RuleAdapter(Context context, Activity activity, JSONObject rulesObject, ListView listView) {
        this.context = context;
        this.activity = activity;
        this.rulesObject = rulesObject;
        this.listView = listView;
        this.helper = new Helper(context);

        try {
            this.rulesList = new ArrayList<>();
            Iterator<String> rulesObjectKeys = rulesObject.keys();
            while (rulesObjectKeys.hasNext()) {

                String ruleKey = rulesObjectKeys.next();
                JSONObject rule = (JSONObject) rulesObject.get(ruleKey);

                JSONObject rulesDetail = new JSONObject();
                rulesDetail.put("rule", ruleKey);
                rulesDetail.put("time", rule.get("time"));
                rulesDetail.put("request_code", rule.get("request_code"));
                rulesDetail.put("switch", rule.get("switch"));

                rulesList.add(rulesDetail);
            }
        } catch (JSONException e) {
            Toast.makeText(context, "[ERROR] " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String[] getActionsArr(ArrayList<Action> actions) {
        ArrayList<String> actionsList = new ArrayList<>();
        int i=1;
        for (Action a : actions) {
            String cmd;
            if (a.touchAction != null) {
                Point p = a.touchAction;
                cmd = String.format("(%s) %s\tx=%s\ty=%s\t", i++, (p.toPoint == null) ? "TAP" : "SWIPE", p.x, p.y);
                if (p.toPoint != null) cmd += String.format("x2=%s\ty2=%s", p.toPoint.x, p.toPoint.y);
            } else
                cmd = String.format("(%s) [%s] %s", i++, (a.isCustomCommandAction) ? "Custom" : "System", a.commandAction);
            actionsList.add(cmd);
        }
        return actionsList.toArray(new String[]{});
    }

    @Override
    public int getCount() {
        return rulesList.size();
    }

    @Override
    public JSONObject getItem(int position) {
        return rulesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView != null) return convertView;

        convertView = LayoutInflater.from(context).inflate(R.layout.layout_automation_rule, parent, false);

        final View view = convertView;
//        final String ruleName = ((TextView) view.findViewById(R.id.rule_name)).getText().toString();
        JSONObject thisRule = getItem(position);

        try {
            ((TextView) convertView.findViewById(R.id.rule_name)).setText(thisRule.getString("rule"));  // set rule name
            ((TextView) convertView.findViewById(R.id.rule_trigger_time)).setText(timeify(thisRule.getString("time")));  // set rule time
            ((ToggleButton) convertView.findViewById(R.id.rule_switch_button)).setChecked(thisRule.getBoolean("switch"));  // set rule switch

            // set rule steps
            LinearLayout commandLayout = (LinearLayout) convertView.findViewById(R.id.rule_steps);  // set rule steps
            String ruleName = ((TextView) view.findViewById(R.id.rule_name)).getText().toString();
            String[] commandArr = getActionsArr(helper.deserialize(ruleName));
            for (String cmd : commandArr) {
                View cmdView = LayoutInflater.from(context).inflate(R.layout.layout_command, parent, false);
                ((TextView) cmdView.findViewById(R.id.command)).setText(cmd);
                commandLayout.addView(cmdView);
            }

        } catch (JSONException e) {
            Toast.makeText(context, "[ERROR] " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // click handler for expansion
        convertView.setOnClickListener(new View.OnClickListener() {  // [revisit] weirdly, this works but it shouldn't: entire view accepts click inputs including the expanded body but clicking on that DOES NOT collapse it
            @Override
            public void onClick(View v) {
                View detailView = v.findViewById(R.id.rule_detail);
                if (detailView.getVisibility() == View.GONE) {
                    view.setBackgroundColor(Color.parseColor("#efefef"));
                    detailView.setVisibility(View.VISIBLE);
                }
                else {
                    view.setBackgroundColor(Color.parseColor("#ffffff"));
                    detailView.setVisibility(View.GONE);
                }
            }
        });

        // click handler for toggling switch state
        ((ToggleButton) view.findViewById(R.id.rule_switch_button)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String ruleName = ((TextView) view.findViewById(R.id.rule_name)).getText().toString();
                helper.setSwitch(ruleName, isChecked);
                if (!helper.handleAlarm(ruleName, isChecked)) {
                    Toast.makeText(context, "Trigger time is yet to be set.", Toast.LENGTH_SHORT).show();
                    ((ToggleButton) buttonView).setChecked(false);
                    return;
                }
                listView.setAdapter(new RuleAdapter(context, activity, helper.getRulesObject(), listView));
                Toast.makeText(context, String.format("Rule will be triggered %s.", (isChecked) ? "ON" :"OFF"), Toast.LENGTH_SHORT).show();
            }
        });

        // click handler for deleting rule
        view.findViewById(R.id.rule_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ruleName = ((TextView) view.findViewById(R.id.rule_name)).getText().toString();
                helper.deleteRule(ruleName);
                listView.setAdapter(new RuleAdapter(context, activity, helper.getRulesObject(), listView));
            }
        });

        // press down handler for selecting time
        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                final TimePicker timePicker = new TimePicker(context);
                new AlertDialog.Builder(activity)
                        .setView(timePicker)
                        .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                helper.setAttribute(((TextView) v.findViewById(R.id.rule_name)).getText().toString(), "time", String.format("%s:%s", timePicker.getHour(), timePicker.getMinute()));
                                listView.setAdapter(new RuleAdapter(context, activity, helper.getRulesObject(), listView));
                                Toast.makeText(context, String.format("Rule will be triggered at %s:%s.", timePicker.getHour(), timePicker.getMinute()), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .create()
                        .show();
                return false;
            }
        });

        return convertView;
    }

    private String timeify(String time) {
        if (time.length() == 0) return "";
        String[] timeBits = time.split(":");
        return timeBits[0] + ":" + ((timeBits[1].length() == 1) ? "0" + timeBits[1] : timeBits[1]);
    }

    private class CommandAdapter extends BaseAdapter {

        private String[] commandArr;

        CommandAdapter(String[] commandArr) {
            this.commandArr = commandArr;
        }

        @Override
        public int getCount() {
            return commandArr.length;
        }

        @Override
        public String getItem(int position) {
            return commandArr[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(context).inflate(R.layout.layout_command, parent, false);
            ((TextView) convertView.findViewById(R.id.command)).setText(getItem(position));
            return convertView;
        }
    }

}
