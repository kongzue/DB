package com.kongzue.kongzuedb.util;

import android.util.Log;

/**
 * Author: @Kongzue
 * Github: https://github.com/kongzue/
 * Homepage: http://kongzue.com/
 * Mail: myzcxhh@live.cn
 * CreateTime: 2018/8/25 02:48
 */
public class BaseUtil {
    
    public static boolean DEBUGMODE = false;
    
    public boolean isNull(String s) {
        if (s == null || s.trim().isEmpty() || s.equals("null")) {
            return true;
        }
        return false;
    }
    
    public void log(Object o) {
        if (DEBUGMODE)Log.i("DB>>>", o.toString());
    }
    
    public void error(Object o) {
        if (DEBUGMODE)Log.e("DB>>>", o.toString());
    }
    
}
