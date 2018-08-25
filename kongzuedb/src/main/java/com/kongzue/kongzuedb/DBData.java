package com.kongzue.kongzuedb;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kongzue.kongzuedb.util.BaseUtil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Author: @Kongzue
 * Github: https://github.com/kongzue/
 * Homepage: http://kongzue.com/
 * Mail: myzcxhh@live.cn
 * CreateTime: 2018/8/23 00:15
 */
public class DBData extends BaseUtil {
    
    private TreeMap<String, String> data;
    private String tableName;
    
    public DBData(@NonNull String tableName) {
        this.tableName = tableName;
    }
    
    @NonNull
    public DBData set(String key, Serializable value) {
        if (data == null) data = new TreeMap<>();
        data.put(key, value + "");
        return this;
    }
    
    public int getInt(String key) {
        int value = Integer.parseInt(data.get(key));
        return value;
    }
    
    public String getString(String key) {
        String value = data.get(key);
        return value;
    }
    
    public boolean getBoolean(String key) {
        boolean value = Boolean.parseBoolean(data.get(key));
        return value;
    }
    
    public double getDouble(String key) {
        double value = Double.parseDouble(data.get(key));
        return value;
    }
    
    public byte getByte(String key) {
        byte value = Byte.parseByte(data.get(key));
        return value;
    }
    
    public short getShort(String key) {
        short value = Short.parseShort(data.get(key));
        return value;
    }
    
    public long getLong(String key) {
        long value = Long.parseLong(data.get(key));
        return value;
    }
    
    public float getFloat(String key) {
        float value = Float.parseFloat(data.get(key));
        return value;
    }
    
    public String getTableName(){
        if (isNull(tableName)){
            error("表名tableName不能为空");
            return null;
        }
        return tableName;
    }
    
    public DBData setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
    
    public TreeMap<String, String> getData() {
        return data;
    }
    
    @Override
    public String toString() {
        return data.toString();
    }
    
}
