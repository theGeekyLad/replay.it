package com.thegeekylad.madautomate;

public class Action {
    public Point touchAction;
    public String commandAction;
    public boolean isCustomCommandAction;
    public Action() {
        this.touchAction = null;
        this.commandAction = null;
    }
    public Action(Point touchAction) {
        this.touchAction = touchAction;
    }
    public Action(String commandAction, boolean isCustomCommandAction) {
        this.commandAction = commandAction;
        this.isCustomCommandAction = isCustomCommandAction;
    }
}