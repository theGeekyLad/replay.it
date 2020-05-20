import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;

/**
 * @author Rahul Pillai (theGeekyLad)
 */

public class ADBAutomate {

    private volatile ArrayList<Action> actions = new ArrayList<>();
    private String adb;

    // entry point to the whole program
    public static void main(String[] args) {
        ADBAutomate adbAutomate = new ADBAutomate();
        adbAutomate.automate(true);
    }

    private void automate(boolean isUnix) {

        try {

            // set adb path based on environment
            adb = isUnix ? "./adb/platform-tools-linux-macos/adb" : ".\\adb\\platform-tools-windows\\adb.exe";
            drawLine();
            System.out.println("Enironment: " + (isUnix ? "Linux / MacOS" : "Windows"));
            drawLine();

            // simple query list of devices - throws the adb authentication pop-up
            Process devicesProcess = Runtime.getRuntime().exec(adb + " devices");
            devicesProcess.waitFor();
            String devicesLine;
            int countDevicesLine = 0;
            boolean foundListDevicesLine = false;
            BufferedReader reader = new BufferedReader(new InputStreamReader(devicesProcess.getInputStream()));
            while ((devicesLine = reader.readLine()) != null) {
                if (devicesLine.contains("devices attached")) foundListDevicesLine = true;
                countDevicesLine++;
            }

            // quit if criteria isn't met
            if (!foundListDevicesLine || countDevicesLine != 3) {
                if (!foundListDevicesLine)
                    System.out.println("ADB isn't working for some reason, please contact the developer.");
                else if (countDevicesLine > 3)
                    System.out.println("Looks like there's more than one connected Android device.\n" +
                            "Program / connected devices may malfunction.\n" +
                            "Check the following:\n" +
                            "- Disconnect the Android devices that aren't required\n" +
                            "- Check if an Android emulator is running and shut it down");
                else
                    System.out.println("Couldn't detect any connected Android devices.\n" +
                            "Check the following:\n" +
                            "- Ensure USB debugging is enabled on the connected Android device\n" +
                            "- Ensure device drivers are installed.");
                drawLine();
                System.exit(1);
            }

            // first time app install
            String thisAppLine = null;
            boolean hasThisApp = false;
            Process listThisApp = Runtime.getRuntime().exec(adb + " shell pm list packages | grep com.thegeekylad.madautomate");
            listThisApp.waitFor();
            BufferedReader listThisAppReader = new BufferedReader(new InputStreamReader(listThisApp.getInputStream()));
            while ((thisAppLine = listThisAppReader.readLine()) != null)
                if (thisAppLine.startsWith("package")) hasThisApp = true;
            if (!hasThisApp) {
                System.out.println("Welcome to the first time run! The setup installs the companion\n" +
                        "Android app and launches it. Please accept necessary permissions on the app\n" +
                        "in order to ensure smooth functioning. Thanks!");
                Runtime.getRuntime().exec(adb + " install replay.it.apk").waitFor();
                Runtime.getRuntime().exec(adb + " shell am start com.thegeekylad.madautomate/.MainActivity").waitFor();
                System.out.println("\nSetup complete, please re-run the program now.");
                drawLine();
                System.exit(0);
            }

            // start sniffing for gestures
            GrabGesturesThread gesturesThread = new GrabGesturesThread();
            gesturesThread.start();

            message();
            int t;
            String cmd = "";
            boolean dontExecute = false;
            Scanner scanner = new Scanner(System.in);
            outer:
            while (scanner.hasNext()) {
                int ch = Integer.parseInt(scanner.nextLine());
                switch (ch) {
                    case 1:
                        cmd = adb + " shell input keyevent 26";
                        actions.add(new Action(cmd, false));
                        System.out.println("Added: power button will be triggered.");
                        break;
                    case 2:
                        System.out.print("Enter some text: ");
                        cmd = adb + " shell input text \"" + scanner.nextLine() + "\"";
                        actions.add(new Action(cmd, false));
                        System.out.println("Added: text will be sent.");
                        break;
                    case 3:
                        cmd = adb + " shell input keyevent 66";
                        actions.add(new Action(cmd, false));
                        System.out.println("Added: ENTER will be clicked.");
                        break;
                    case 4:
                        cmd = adb + " shell input keyevent 4";
                        actions.add(new Action(cmd, false));
                        System.out.println("Added: BACK will be pressed.");
                        break;
                    case 5:
                        cmd = adb + " shell input keyevent 3";
                        actions.add(new Action(cmd, false));
                        System.out.println("Added: Returns to home screen.");
                        break;
                    case 6:
                        System.out.print("Enter delay (s): ");
                        while (true) {
                            try {
                                cmd = "wait " + (t = Integer.parseInt(scanner.nextLine()));
                                break;
                            } catch (NumberFormatException e) {
                                System.out.println("Delay must be a number.");
                            }
                        }
                        actions.add(new Action(cmd, true));
                        dontExecute = true;
                        System.out.println("Added: delay of " + t + "s will be mandated.");
                        break;
                    case 7:
                        System.out.print("Keycode: ");
                        cmd = adb + " shell input keyevent " + (t = Integer.parseInt(scanner.nextLine()));
                        actions.add(new Action(cmd, false));
                        System.out.println("Added: key " + t + " will be triggered.");
                        break;
                    case 8:
                        System.out.print("Enter the package and activity name as: package_name/.activity_name: ");  // com.whatsapp/.Main
                        cmd = adb + " shell am start " + scanner.nextLine();
                        actions.add(new Action(cmd, false));
                        System.out.println("Added: package will be launched.");
                        break;
                    case 9:
                        System.out.print("Enter the app name: ");  // com.whatsapp/.Main
                        String appName = scanner.nextLine(), line;
                        StringBuilder resultPackages = new StringBuilder();
                        Process listPackagesProcess = Runtime.getRuntime().exec(adb + " shell pm list packages | grep " + appName);
                        listPackagesProcess.waitFor();
                        BufferedReader packageListReader = new BufferedReader(new InputStreamReader(listPackagesProcess.getInputStream()));
                        while ((line = packageListReader.readLine()) != null)
                            resultPackages.append(line.split(":")[1]).append('\n');
                        packageListReader.close();
                        System.out.println(resultPackages.toString().trim());

                        System.out.print("Enter the package name from the above list: ");
                        String packageName = scanner.nextLine();
                        Process listMainActivityProcess = Runtime.getRuntime().exec(adb + " shell pm dump " + packageName + " | grep -A 1 MAIN | sed -n '2 p'");
                        listMainActivityProcess.waitFor();
                        BufferedReader mainActivityReader = new BufferedReader(new InputStreamReader(listMainActivityProcess.getInputStream()));
                        System.out.println("\nCopy this: " + mainActivityReader.readLine().trim().split(" ")[1]);

                        packageListReader.close();
                        dontExecute = true;
                        break;
                    case 10:
                        Action action = actions.get(actions.size() - 1);
                        System.out.println(translateAction(action));
                        actions.remove(actions.size() - 1);
                        dontExecute = true;
                        break;
                    case 11:
                        break outer;  // stops execution of thread
                    default:
                        message();
                }
                if (!dontExecute) Runtime.getRuntime().exec(cmd);  // sample run the command
                dontExecute = false;
                drawLine();
            }

            // cease accepting touch input as it all is about to end
            gesturesThread.stop();

            // print all recorded points
            Thread.sleep(1000);  // let it breathe

            // quit if there aren't any commands recorded
            if (actions.size() == 0) {
                System.out.println("\nCouldn't find any recorded commands, quitting.");
                drawLine();
                scanner.close();
                System.exit(0);
            }

            drawLine(null);
            System.out.println("COMMANDS RECORDED");
            drawLine();
            logRules(actions);
            drawLine();

            // replay
            replay(scanner);

            // make portable for sending to device
            serializeAndSend(scanner);

            scanner.close();

        } catch (Exception e) {
            e.printStackTrace();
//            System.err.println(e.getMessage());
        }
    }

    private void replay(Scanner scanner) throws Exception {
        System.out.println("\nReady to replay? (1 - yes / * - no):");
        if (Integer.parseInt(scanner.nextLine()) == 1) {
            while (true) {
                System.out.println("Time to replay!");
                for (Action action : actions) {
                    if (action.touchAction != null) {
                        Point gesture = action.touchAction;
                        if (gesture.toPoint == null)  // tap
                            Runtime.getRuntime().exec(String.format(adb + " shell input tap %s %s", gesture.x, gesture.y)).waitFor();
                        else  // touch action
                            Runtime.getRuntime().exec(String.format(adb + " shell input swipe %s %s %s %s", gesture.x, gesture.y, gesture.toPoint.x, gesture.toPoint.y)).waitFor();
                    } else {  // command action
                        if (!action.isCustomCommandAction)
                            Runtime.getRuntime().exec(action.commandAction).waitFor();
                        else {
                            String[] commandActionBits = action.commandAction.split(" ");
                            /*
                             * This section presents to you the custom actions that can be created and handled
                             * accordingly. As a sample, here's a custom action that holds a simple delay basis the
                             * number of seconds that the user input in the automate() method above.
                             *
                             * Note: It assumes all custom action bits are separated by spaces, so handle accordingly.
                             *
                             * Add conditions like so:
                             */
                            if (commandActionBits[0].equals("wait"))  // [custom actions] sleeper
                                Thread.sleep(Integer.parseInt(commandActionBits[1]) * 1000);
                        }
                    }
                    Thread.sleep(1000);  // let it breathe
                }
                System.out.println("Play again? (1 - yes / * - no):");
                if (Integer.parseInt(scanner.nextLine()) != 1) break;
            }
        }
    }

    private void serializeAndSend(Scanner scanner) throws Exception {
        System.out.print("\nSave config as (filename): ");
//        Scanner scanner = new Scanner(System.in);
        String serFile, serDir = "config";
        while (true) {
            serFile = scanner.nextLine().trim();
            if (serFile.split(" ").length > 1)
                System.out.println("Ensure there aren't spaces in the filename.");
            else break;
        }
        if (!new File("config").exists() && !new File("config").mkdir()) {
            serDir = null;
            System.out.println("Couldn't create 'config' directory, saving outside.");
        }
        serialize(serDir, serFile);
        System.out.println("Serialization done.");
//        scanner.close();
    }

    private void drawLine() {  System.out.println("-----------------------------------------------------------"); }
    private void drawLine(Object obj) {  System.out.println("\n"); drawLine(); }  // leaves a line and writes dashes

    private void serialize(String dir, String fileName) throws Exception {
        File file = (dir == null) ? new File(fileName) : new File(dir, fileName);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (Action action : actions) {
            String line;
            if (action.touchAction == null) {  // 'commandAction' (either adb or custom)
                if (action.isCustomCommandAction)
                    line = String.format("1:1:%s", action.commandAction);
                else line = String.format("1:0:%s", action.commandAction);
            }
            else {  // 'touchAction'
                if (action.touchAction.toPoint == null)
                    line = String.format("0:0:%s %s", action.touchAction.x, action.touchAction.y);
                else
                    line = String.format("0:1:%s %s %s %s", action.touchAction.x, action.touchAction.y, action.touchAction.toPoint.x, action.touchAction.toPoint.y);
            }
            writer.write(line);
            writer.newLine();
            writer.flush();
        }
        writer.close();
        Runtime.getRuntime().exec(adb + " push " + file.getPath() + " /sdcard/mad-automate-rules").waitFor();
    }

    private ArrayList<Action> deserialize(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        ArrayList<Action> actions = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] actionType = line.split(":");
            if (Integer.parseInt(actionType[0]) == 1) {  // 'commandAction' (either adb or custom)
                if (Integer.parseInt(actionType[1]) == 0)
                    actions.add(new Action(actionType[2], false));
                else actions.add(new Action(actionType[2], true));
            }
            else {  // 'touchAction'
                String[] coordinates = actionType[2].split(" ");
                if (Integer.parseInt(actionType[1]) == 0)
                    actions.add(new Action(new Point(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]))));
                else
                    actions.add(new Action(new Point(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]), new Point(Integer.parseInt(coordinates[2]), Integer.parseInt(coordinates[3])))));
            }
        }
        reader.close();
        return actions;
    }

    private void logRules(ArrayList<Action> actions) {
        int i=1;
        for (Action a : actions)
            System.out.println(String.format("(%s) %s", i++, translateAction(a)));
    }

    private String translateAction(Action a) {
        int i=1;
        String cmd;
        if (a.touchAction != null) {
            Point p = a.touchAction;
            cmd = String.format("%s\tx=%s\ty=%s\t", (p.toPoint == null) ? "TAP" : "SWIPE", p.x, p.y);
            if (p.toPoint != null) cmd += String.format("x2=%s\ty2=%s", p.toPoint.x, p.toPoint.y);
        } else
            cmd = String.format("[%s] %s", (a.isCustomCommandAction) ? "Custom" : "System", a.commandAction);
        return cmd;
    }

    private void message() {
//        drawLine();
        System.out.println("Add an action:\n" +
                "(1) Trigger power button\n" +
                "(2) Type in text\n" +
                "(3) Hit ENTER\n" +
                "(4) Go back\n" +
                "(5) Go to home screen\n" +
                "(6) Simulate wait\n" +
                "(7) Add misc. keycode\n" +
                "(8) Launch app\n" +
                "(9) Search for app package & activity info (command works on Linux and MacOS only)\n" +
                "(10) Delete last step\n" +
                "(11) Save workflow and quit\n" +
                "(or) Make a gesture action (no selection)");
        drawLine();
    }

    private class GrabGesturesThread extends Thread {

        @Override
        public void run() {
            super.run();

            try {
                long cacheTime = System.currentTimeMillis();
                boolean isTapping = true;
                Point swipePoint = new Point();
                Point cacheSwipePoint = new Point();

                while (true) {

                    // capturing
                    Process process = Runtime.getRuntime().exec(adb + " shell getevent -lc 10 | grep [_][XY]");
                    BufferedReader processReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    String[] xy = new String[2];
                    for (int i=0; (line = processReader.readLine()) != null && i < 2;) {
                        if (line.contains("ABS_MT_POSITION_X")) {
                            xy[i] = line.split("[ ]+")[3];
                            i++;
                        }
                        else if (line.contains("ABS_MT_POSITION_Y")) {
                            xy[i] = line.split("[ ]+")[3];
                            i++;
                        }
                    }

                    // TERMINATE / SKIP conditions
                    if (xy[0] == null || xy[1] == null) continue;  // exit if outlier
                    if (Integer.parseInt(xy[0], 16) < 100 && Integer.parseInt(xy[1], 16) < 100) break;

                    // get current timestamp
                    Calendar calendar = Calendar.getInstance();
                    String timestamp = String.format("[%s:%s]", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));

                    // tap (could be a source tap for a swipe too)
                    if (System.currentTimeMillis() - cacheTime > 200) {
                        if (!isTapping) {
                            swipePoint.toPoint = new Point(cacheSwipePoint.x, cacheSwipePoint.y);
                            actions.add(new Action(swipePoint));
                            isTapping = true;
                            System.out.println(String.format("%s\nSwiped till x = %s\ny = %s", timestamp, Integer.parseInt(xy[0], 16), Integer.parseInt(xy[1], 16)));
                            drawLine();
                        }
                        System.out.println(String.format("%s\nTapped at x = %s\ny = %s", timestamp, Integer.parseInt(xy[0], 16), Integer.parseInt(xy[1], 16)));
                        drawLine();
                        actions.add(new Action(new Point(Integer.parseInt(xy[0], 16), Integer.parseInt(xy[1], 16))));
                    }

                    // swipe (the one before the next "tap" is the end of the gesture
                    else {
                        if (isTapping) {
                            swipePoint = actions.get(actions.size()-1).touchAction;
                            int popIndex = actions.size() - 1;
                            while (actions.get(popIndex).touchAction == null && popIndex >= 0) popIndex--;  // finding index from the last, to pop
                            actions.remove(popIndex);
                            isTapping = false;
                        }
                        cacheSwipePoint = new Point(Integer.parseInt(xy[0], 16), Integer.parseInt(xy[1], 16));
                    }
                    process.waitFor();
                    process.destroy();
                    cacheTime = System.currentTimeMillis();
                }

            } catch (Exception e) {
                System.err.println(e.getMessage());
            }

        }
    }

    private class Point {
        int x, y;
        Point toPoint;  // only if gesture is "swipe"
        Point() {
            this.x = 0;
            this.y = 0;
            this.toPoint = null;
        }
        Point(int x, int y) {
            this.x = x;
            this.y = y;
            this.toPoint = null;
        }
        Point(int x, int y, Point toPoint) {
            this.x = x;
            this.y = y;
            this.toPoint = toPoint;
        }
    }

    private class Action {
        Point touchAction;
        String commandAction;
        boolean isCustomCommandAction;
        Action() {
            this.touchAction = null;
            this.commandAction = null;
        }
        Action(Point touchAction) {
            this.touchAction = touchAction;
        }
        Action(String commandAction, boolean isCustomCommandAction) {
            this.commandAction = commandAction;
            this.isCustomCommandAction = isCustomCommandAction;
        }
    }

}

// clear && adb shell pm dump com.instagram.android | grep -A 1 MAIN | sed -n '2 p'