package com.example.faceconnect;

import android.graphics.Rect;
import android.graphics.RectF;

public class BoxWithText { //creating a class for boxes with text
    public String text;  //text which is to be displayed alongside box
    public Rect rect;    //rectangular box around the face

    public BoxWithText(String text, Rect rect) {  //constructor
        this.text = text;
        this.rect = rect;
    }

    public BoxWithText(String displayName, RectF boundingBox) {
        this.text = displayName;
        this.rect = new Rect();
        boundingBox.round(rect);
    }
}
