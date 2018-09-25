package com.example.tomato.skymusic.utils;

import android.app.Activity;
import android.view.WindowManager;

import com.example.tomato.skymusic.activities.MainActivity;


/**
 * Created by IceMan on 12/10/2016.
 */

public class Common {
    public static String miliSecondToString(int totalTimeInSec) {
        int min = totalTimeInSec / 60;
        int sec = totalTimeInSec - min * 60;
        return String.format("%02d:%02d", min, sec);
    }
}
