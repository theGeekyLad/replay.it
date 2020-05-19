package com.thegeekylad.madautomate;

public class Point {
    public int x, y;
    public Point toPoint;  // only if gesture is "swipe"
    public Point() {
        this.x = 0;
        this.y = 0;
        this.toPoint = null;
    }
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
        this.toPoint = null;
    }
    public Point(int x, int y, Point toPoint) {
        this.x = x;
        this.y = y;
        this.toPoint = toPoint;
    }
}
