package com.kongzue.kongzuedb;

import android.support.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.kongzue.kongzuedb.util.BaseUtil;
import com.kongzue.kongzuedb.util.Preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Author: @Kongzue
 * Github: https://github.com/kongzue/
 * Homepage: http://kongzue.com/
 * Mail: myzcxhh@live.cn
 * CreateTime: 2018/8/23 00:14
 */
public class DB extends BaseUtil {
    
    public final int SORT_NORMAL = 0;       //排序原则：正序
    public final int SORT_BACK = 1;       //排序原则：倒序
    
    private Context context;
    private SqlliteHelper helper;
    private SQLiteDatabase db;
    
    private String dbName;
    private int dbVersion = 0;
    private String createTableSQLCommand;
    private String updateTableSQLCommand;
    private String newTableSQLCommand;
    
    /**
     * @param context 上下文索引，可直接传 activity.this
     * @param dbName  数据库名称
     *                构造函数
     */
    public DB(@NonNull Context context, @NonNull String dbName) {
        this.dbName = dbName;
        this.context = context;
        dbVersion = Preferences.getInstance().getInt(context, "KongzueDB", "dbName.version");
    }
    
    /**
     * @param data              数据源
     * @param isAllowRepetition 是否允许重复插入
     * @return 是否插入成功
     * 在已有的表中插入一个新数据，注意，如果一开始没有创建新表也无所谓，会按照 data 中的表名自动创建
     */
    public boolean add(@NonNull DBData data, boolean isAllowRepetition) {
        try {
            String tableName = data.getTableName();
            if (helper == null) {
                //如果没初始化，现在初始化
                createTableSQLCommand = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, ";
                
                Set<String> set = data.getData().keySet();
                
                if (set.size() == 0) {
                    error("Data中没有任何数据");
                    return false;
                }
                
                for (String key : set) {
                    String value = data.getData().get(key);
                    if (!isNull(key) && !isNull(value)) {
                        createTableSQLCommand = createTableSQLCommand + " " + key + " VARCHAR,";
                    }
                }
                
                if (createTableSQLCommand.endsWith(","))
                    createTableSQLCommand = createTableSQLCommand.substring(0, createTableSQLCommand.length() - 1);
                
                createTableSQLCommand = createTableSQLCommand + ")";
                
                log("SQL.exec: " + createTableSQLCommand);
                
                if (dbVersion == 0) dbVersion = 1;
                Preferences.getInstance().set(context, "KongzueDB", "dbName.version", dbVersion);
                closeDB();
                helper = new SqlliteHelper(context, dbName, dbVersion);
                db = helper.getWritableDatabase();
            }
            if (!isAllowRepetition) {
                List<DBData> queryResult = find(data);
                if (queryResult.size() != 0) {
                    error("已重复：" + data.toString());
                    return false;
                }
            }
            
            db.beginTransaction();
            try {
                String sql = "INSERT INTO " + tableName + " (";
                Set<String> set = data.getData().keySet();
                for (String key : set) {
                    sql = sql + "\'" + key + "\'" + " ,";
                }
                if (sql.endsWith(",")) sql = sql.substring(0, sql.length() - 1);
                sql = sql + ") VALUES (";
                for (String key : set) {
                    String value = data.getData().get(key);
                    sql = sql + "\'" + value + "\'" + " ,";
                }
                if (sql.endsWith(",")) sql = sql.substring(0, sql.length() - 1);
                sql = sql + ")";
                
                log("SQL.exec: " + sql);
                
                db.execSQL(sql);
                db.setTransactionSuccessful();  //设置事务成功完成
                
                return true;
            } catch (Exception e) {
                return false;
            } finally {
                db.endTransaction();    //结束事务
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * @param data 要修改的数据
     * @return 是否修改成功
     * 修改已有的数据
     */
    public boolean update(@NonNull DBData data) {
        if (data.getInt("_id") == 0) {
            error("只能对已存在的数据（使用find查询出来的数据）进行修改");
            return false;
        }
        if (db == null) {
            helper = new SqlliteHelper(context, dbName, dbVersion);
            db = helper.getWritableDatabase();
        }
        db.beginTransaction();
        try {
            String sql = "update " + data.getTableName() + " set ";
            
            Set<String> set = data.getData().keySet();
            for (String key : set) {
                String value = data.getData().get(key);
                sql = sql + " " + key + " = \'" + value + "\' ,";
            }
            if (sql.endsWith(","))
                sql = sql.substring(0, sql.length() - 1);
            
            sql = sql + " where _id=\"" + data.getInt("_id") + "\"";
            
            log("SQL.exec: " + sql);
            db.execSQL(sql);
            db.setTransactionSuccessful();  //设置事务成功完成
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();    //结束事务
        }
        return true;
    }
    
    /**
     * @param data 要删除的数据
     * @return 是否删除成功
     * 删除一个已有的数据
     */
    public boolean delete(@NonNull DBData data) {
        if (data.getInt("_id") == 0) {
            error("只能对已存在的数据（使用find查询出来的数据）进行删除");
            return false;
        }
        if (db == null) {
            helper = new SqlliteHelper(context, dbName, dbVersion);
            db = helper.getWritableDatabase();
        }
        db.beginTransaction();
        try {
            String sql = "delete from " + data.getTableName() + " where _id=\'" + data.getInt("_id") + "\'";
            log("SQL.exec: " + sql);
            db.execSQL(sql);
            db.setTransactionSuccessful();  //设置事务成功完成
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();    //结束事务
        }
        return true;
    }
    
    /**
     * @param conditions 要删除的数据的查询条件
     * @return 是否删除成功
     * 删除一个未知的数据，会根据查询条件 conditions 进行查询，并对查询到的结果进行删除
     */
    public boolean deleteFind(@NonNull DBData conditions) {
        if (db == null) {
            helper = new SqlliteHelper(context, dbName, dbVersion);
            db = helper.getWritableDatabase();
        }
        List<DBData> dataList = find(conditions);
        if (dataList.size()==0){
            error("没有找到任何符合删除条件的目标数据");
            return false;
        }
        for (DBData dbData:dataList){
            boolean result = delete(dbData);
            if (!result){
                return false;
            }
        }
        return true;
    }
    
    /**
     * @param tableName 要清空的数据库表名
     * @return 是否清空成功
     * 清空一个数据库
     */
    public void deleteAll(@NonNull String tableName) {
        if (db == null) {
            helper = new SqlliteHelper(context, dbName, dbVersion);
            db = helper.getWritableDatabase();
        }
        db.beginTransaction();
        try {
            String sql = "delete from " + tableName;
            log("SQL.exec: " + sql);
            db.execSQL(sql);
            db.execSQL("update sqlite_sequence set seq=0 where name='" + tableName + "';");          //将自增键初始化为0
            db.setTransactionSuccessful();  //设置事务成功完成
        } finally {
            db.endTransaction();    //结束事务
        }
    }
    
    /**
     * @param tableName 表名
     * @param sort      排序方法
     * @return 查询结果，请以 list.size()==0 来判断没有结果
     * 全表查询，会以查询条件中的键值对作为条件进行查询
     */
    public List<DBData> findAll(@NonNull String tableName, int sort) {
        if (db == null) {
            helper = new SqlliteHelper(context, dbName, dbVersion);
            db = helper.getWritableDatabase();
        }
        
        List<DBData> list = new ArrayList<DBData>();
        Cursor c = getQueryCursor(tableName);
        while (c.moveToNext()) {
            DBData data = new DBData(tableName);
            for (int i = 0; i < c.getColumnCount(); i++) {
                String key = c.getColumnName(i);
                data.set(key, c.getString(c.getColumnIndex(key)));
            }
            if (sort == SORT_NORMAL) {
                list.add(data);
            } else if (sort == SORT_BACK) {
                list.add(0, data);
            }
        }
        c.close();
        return list;
    }
    
    /**
     * @param tableName 表名
     * @return 查询结果，请以 list.size()==0 来判断没有结果
     * 全表正序查询，会以查询条件中的键值对作为条件进行查询
     */
    public List<DBData> findAll(@NonNull String tableName) {
        return findAll(tableName, SORT_NORMAL);
    }
    
    /**
     * @param conditions 查询条件
     * @return 查询结果，请以 list.size()==0 来判断没有结果
     * 按条件查询，会以查询条件中的键值对作为条件进行查询
     */
    public List<DBData> find(@NonNull DBData conditions, int sort) {
        if (db == null) {
            helper = new SqlliteHelper(context, dbName, dbVersion);
            db = helper.getWritableDatabase();
        }
        
        List<DBData> list = new ArrayList<DBData>();
        Cursor c = getQueryCursor(conditions);
        while (c.moveToNext()) {
            DBData data = new DBData(conditions.getTableName());
            for (int i = 0; i < c.getColumnCount(); i++) {
                String key = c.getColumnName(i);
                data.set(key, c.getString(c.getColumnIndex(key)));
            }
            if (sort == SORT_NORMAL) {
                list.add(data);
            } else if (sort == SORT_BACK) {
                list.add(0, data);
            }
        }
        c.close();
        return list;
    }
    
    /**
     * @param conditions 查询条件
     * @return 查询结果，请以 list.size()==0 来判断没有结果
     * 正序按条件查询，会以查询条件中的键值对作为条件进行查询
     */
    public List<DBData> find(@NonNull DBData conditions) {
        return find(conditions, SORT_NORMAL);
    }
    
    //查询指针
    private Cursor getQueryCursor(String tableName) {
        String sql = "SELECT * FROM " + tableName;
        Cursor c = db.rawQuery(sql, null);
        log("SQL.exec: " + sql);
        return c;
    }
    
    //查询指针
    private Cursor getQueryCursor(DBData conditions) {
        String sql = "SELECT * FROM " + conditions.getTableName() + " where ";
        Set<String> set = conditions.getData().keySet();
        for (String key : set) {
            String value = conditions.getData().get(key);
            sql = sql + " " + key + " = \'" + value + "\' AND";
        }
        if (sql.endsWith("AND"))
            sql = sql.substring(0, sql.length() - 3);
        log("SQL.exec: " + sql);
        Cursor c = db.rawQuery(sql, null);
        return c;
    }
    
    /**
     * @param tableName 表名
     * @param newKeys   新增的列名
     *                  更新一个已有的旧表
     */
    public void updateTable(String tableName, List<String> newKeys) {
        updateTableSQLCommand = "ALTER TABLE " + tableName + " ADD ";
        for (String key : newKeys) {
            updateTableSQLCommand = updateTableSQLCommand + " " + key + " VARCHAR,";
        }
        
        if (updateTableSQLCommand.endsWith(","))
            updateTableSQLCommand = updateTableSQLCommand.substring(0, updateTableSQLCommand.length() - 1);
        
        log("SQL.exec: " + updateTableSQLCommand);
        
        dbVersion++;
        Preferences.getInstance().set(context, "KongzueDB", "dbName.version", dbVersion);
        closeDB();
        helper = new SqlliteHelper(context, dbName, dbVersion);
        db = helper.getWritableDatabase();
    }
    
    /**
     * @param data 数据源范例
     *             创建一个新表，按照 data 作为样板进行创建，data.getTableName() 作为表名，data中所有键作为列进行创建
     */
    public void createNewTable(DBData data) {
        newTableSQLCommand = "CREATE TABLE IF NOT EXISTS " + data.getTableName() + " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, ";
        Set<String> set = data.getData().keySet();
        for (String key : set) {
            newTableSQLCommand = newTableSQLCommand + " " + key + " VARCHAR,";
        }
        
        if (newTableSQLCommand.endsWith(","))
            newTableSQLCommand = newTableSQLCommand.substring(0, newTableSQLCommand.length() - 1);
        
        log("SQL.exec: " + newTableSQLCommand);
        
        if (dbVersion == 0) {
            createTableSQLCommand = newTableSQLCommand;
            newTableSQLCommand = null;
        }
        dbVersion++;
        Preferences.getInstance().set(context, "KongzueDB", "dbName.version", dbVersion);
        closeDB();
        helper = new SqlliteHelper(context, dbName, dbVersion);
        db = helper.getWritableDatabase();
    }
    
    private class SqlliteHelper extends SQLiteOpenHelper {
        
        public SqlliteHelper(Context context, String dbName, int dbVersion) {
            //CursorFactory设置为null,使用默认值
            super(context, dbName + ".db", null, dbVersion);
        }
        
        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(createTableSQLCommand);
        }
        
        //数据库升级用
        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            if (oldVersion != dbVersion) {
                if (!isNull(updateTableSQLCommand)) sqLiteDatabase.execSQL(updateTableSQLCommand);
                if (!isNull(newTableSQLCommand)) sqLiteDatabase.execSQL(newTableSQLCommand);
            }
        }
    }
    
    //数据库关闭
    public void closeDB() {
        if (db != null) db.close();
    }
    
}
