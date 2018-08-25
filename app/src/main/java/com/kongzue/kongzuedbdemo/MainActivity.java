package com.kongzue.kongzuedbdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.kongzue.kongzuedb.DB;
import com.kongzue.kongzuedb.DBData;
import com.kongzue.kongzuedb.util.BaseUtil;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        BaseUtil.DEBUGMODE = true;
        
        DB db = new DB(this, "normal");
        DBData dbData = new DBData("user");
        dbData.set("name", "张三").set("age", 18).set("phone", 18513000000l);
        db.add(dbData, false);
        dbData.set("name", "李四").set("age", 22).set("phone", 13772656666l);
        db.add(dbData, false);
        dbData.set("name", "王五").set("age", 20).set("phone", 15555555555l);
        db.add(dbData, false);

        log("查询全表_______________");
        List<DBData> allData = db.findAll("user");
        for (DBData data : allData) {
            log(data);
        }
        log("_______________");


        log("查询age=18的所有数据_______________");
        List<DBData> dataList = db.find(new DBData("user").set("age", 18));
        for (DBData data : dataList) {
            log(data);
        }
        log("_______________");

        log("删除age=22的数据：" + db.deleteFind(new DBData("user").set("age",22)));

        log("查询全表_______________");
        allData = db.findAll("user");
        for (DBData data : allData) {
            log(data);
        }
        log("_______________");
    
        db.closeDB();
    }
    
    public void log(Object o) {
        Log.i("DB>>>", o.toString());
    }
}
