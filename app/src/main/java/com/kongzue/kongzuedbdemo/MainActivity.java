package com.kongzue.kongzuedbdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.kongzue.kongzuedb.DB;
import com.kongzue.kongzuedb.DBData;
import com.kongzue.kongzuedb.util.BaseUtil;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    private static final String dbName = "normal";          //数据库名
    private static final String tableName = "user";         //表名
    
    private Button btnAdd;
    private Button btnPrintAll;
    private Button btnFind18;
    private Button btnDelete22;
    private Button btnUpdate18;
    private Button btnFindCount22;
    private Button btnDeleteAll;
    private Button btnCreateTable2;
    
    DB db;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    
        btnAdd = findViewById(R.id.btn_add);
        btnPrintAll = findViewById(R.id.btn_printAll);
        btnFind18 = findViewById(R.id.btn_find18);
        btnDelete22 = findViewById(R.id.btn_delete22);
        btnUpdate18 = findViewById(R.id.btn_update18);
        btnFindCount22 = findViewById(R.id.btn_findCount22);
        btnDeleteAll = findViewById(R.id.btn_deleteAll);
        btnCreateTable2 = findViewById(R.id.btn_createTable2);
        
        BaseUtil.DEBUGMODE = true;                     //是否开启日志输出模式，主要用于打印sql执行语句
        
        db = new DB(this, dbName);    //准备数据库
        
        //添加5条数据
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int errorNum = 0;
                
                DBData dbData = new DBData(tableName);
                dbData.set("name", "张三").set("age", 18).set("phone", 18513000000l);
                if (!db.add(dbData, false)) errorNum++;
                dbData.set("name", "李四").set("age", 22).set("phone", 13772656666l);
                if (!db.add(dbData, false)) errorNum++;
                dbData.set("name", "王五").set("age", 20).set("phone", 15555555555l);
                if (!db.add(dbData, false)) errorNum++;
                dbData.set("name", "赵六").set("age", 18).set("phone", 18513012345l);
                if (!db.add(dbData, false)) errorNum++;
                dbData.set("name", "钱七").set("age", 22).set("phone", 13313012345l);
                if (!db.add(dbData, false)) errorNum++;
                
                toast("执行完毕，错误" + errorNum + "个");
            }
        });
        
        btnPrintAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<DBData> allData = db.findAll(tableName);
                for (DBData data : allData) {
                    log(data);
                }
                
                toast("执行完毕，请在Logcat查看结果");
            }
        });
        
        btnFind18.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<DBData> dataList = db.find(new DBData("user").set("age", 18));
                for (DBData data : dataList) {
                    log(data);
                }
                
                toast("执行完毕，请在Logcat查看结果");
            }
        });
        
        btnDelete22.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean errorFlag = db.deleteFind(new DBData("user").set("age", 22));
                
                toast("执行完毕，结果：" + (errorFlag ? "成功" : "失败"));
            }
        });
        
        btnUpdate18.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int errorNum = 0;
                List<DBData> dataList = db.find(new DBData("user").set("age", 18));
                for (DBData data : dataList) {
                    data.set("age", 22);
                    if (!db.update(data)) {
                        errorNum++;
                    }
                }
                
                toast("执行完毕，错误" + errorNum + "个");
            }
        });
    
        btnFindCount22.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long count = db.getCount("user",new DBData("user").set("age", 22));
    
                toast("执行完毕，查询到" + count + "个");
            }
        });
        
        btnDeleteAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean errorFlag= db.deleteAll(tableName);
                toast("执行完毕，结果：" + (errorFlag ? "成功" : "失败"));
            }
        });
    
        btnCreateTable2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DBData dbData = new DBData("table2");
                dbData.set("name", "张三").set("age", 18).set("phone", 18513000000l);
                db.createNewTable(dbData);
            }
        });
    }
    
    public void log(Object o) {
        Log.i("DB>>>", o.toString());
    }
    
    public void toast(Object o) {
        Toast.makeText(this, o.toString(), Toast.LENGTH_SHORT).show();
        log("TOAST: " + o.toString());
    }
    
    @Override
    protected void onDestroy() {
        if (db != null) db.closeDB();
        super.onDestroy();
    }
}
