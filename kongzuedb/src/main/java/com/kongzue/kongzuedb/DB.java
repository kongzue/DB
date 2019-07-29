package com.kongzue.kongzuedb;

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
import java.util.regex.Pattern;

/**
 * Author: @Kongzue
 * Github: https://github.com/kongzue/
 * Homepage: http://kongzue.com/
 * Mail: myzcxhh@live.cn
 * CreateTime: 2018/8/23 00:14
 */
public class DB extends BaseUtil {
    
    public static final int SORT_NORMAL = 0;       //排序原则：正序
    public static final int SORT_BACK = 1;       //排序原则：倒序
    
    private Context context;
    private SqlliteHelper helper;
    private SQLiteDatabase db;
    
    private String dbName;
    private int dbVersion = 0;
    private String createTableSQLCommand;
    private String updateTableSQLCommand;
    private String newTableSQLCommand;
    
    /**
     * 构造函数
     *
     * @param context 上下文索引，可直接传 activity.this
     * @param dbName  数据库名称
     */
    public DB(Context context, String dbName) {
        this.dbName = dbName;
        this.context = context;
        dbVersion = Preferences.getInstance().getInt(context, "KongzueDB", dbName + ".version");
    }
    
    /**
     * 在已有的表中插入一个新数据，注意，如果一开始没有创建新表也无所谓，会按照 data 中的表名自动创建
     *
     * @param data              数据源
     * @param isAllowRepetition 是否允许重复插入
     * @return 是否插入成功
     */
    public boolean add(DBData data, boolean isAllowRepetition) {
        try {
            String tableName = data.getTableName();
            if (!isHaveTable(data.getTableName())) {
                createNewTable(data);
            }
            if (!isAllowRepetition) {
                List<DBData> queryResult = findWithoutId(data);
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
                    if (!key.equals("_id")) {
                        sql = sql + "\'" + key + "\'" + " ,";
                    }
                }
                if (sql.endsWith(",")) sql = sql.substring(0, sql.length() - 1);
                sql = sql + ") VALUES (";
                for (String key : set) {
                    if (!key.equals("_id")) {
                        String value = data.getData().get(key);
                        sql = sql + "\'" + value + "\'" + " ,";
                    }
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
     * 查询，仅返回是否存在此条件的数据
     *
     * @param conditions 样板条件
     * @return 是否存在样板条件数据
     */
    public boolean isHave(DBData conditions) {
        List<DBData> result = findWithoutId(conditions);
        if (result != null && !result.isEmpty()) return true;
        return false;
    }
    
    /**
     * 查找，但排除_id字段
     *
     * @param conditions 样板条件
     * @return 查询结果
     */
    public List<DBData> findWithoutId(DBData conditions) {
        if (dbVersion == 0) {
            error("数据库不存在，请先通过add()或createNewTable()来创建一个数据库");
            return new ArrayList<>();
        }
        if (db == null) {
            helper = new SqlliteHelper(context, dbName, dbVersion);
            db = helper.getWritableDatabase();
        }
        
        List<DBData> list = new ArrayList<DBData>();
        Cursor c;
        try {
            c = getQueryCursorWithoutId(conditions);
        } catch (Exception e) {
            return list;
        }
        while (c.moveToNext()) {
            DBData data = new DBData(conditions.getTableName());
            for (int i = 0; i < c.getColumnCount(); i++) {
                String key = c.getColumnName(i);
                data.set(key, c.getString(c.getColumnIndex(key)));
            }
            list.add(data);
        }
        c.close();
        return list;
    }
    
    /**
     * 修改已有的数据
     *
     * @param data 要修改的数据
     * @return 是否修改成功
     */
    public boolean update(DBData data) {
        if (data.getInt("_id") == 0) {
            error("只能对已存在的数据（使用find查询出来的数据）进行修改");
            return false;
        }
        if (dbVersion == 0) {
            error("数据库不存在，请先通过add()或createNewTable()来创建一个数据库");
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
                if (!key.equals("_id")) {
                    String value = data.getData().get(key);
                    sql = sql + " " + key + " = \'" + value + "\' ,";
                }
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
     * 删除一个已有的数据
     *
     * @param data 要删除的数据
     * @return 是否删除成功
     */
    public boolean delete(DBData data) {
        if (data.getInt("_id") == 0) {
            error("只能对已存在的数据（使用find查询出来的数据）进行删除");
            return false;
        }
        if (dbVersion == 0) {
            error("数据库不存在，请先通过add()或createNewTable()来创建一个数据库");
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
     * 删除一个未知的数据，会根据查询条件 conditions 进行查询，并对查询到的结果进行删除
     *
     * @param conditions 要删除的数据的查询条件
     * @return 是否删除成功
     */
    public boolean deleteFind(DBData conditions) {
        if (dbVersion == 0) {
            error("数据库不存在，请先通过add()或createNewTable()来创建一个数据库");
            return false;
        }
        if (db == null) {
            helper = new SqlliteHelper(context, dbName, dbVersion);
            db = helper.getWritableDatabase();
        }
        List<DBData> dataList = find(conditions);
        if (dataList.size() == 0) {
            error("没有找到任何符合删除条件的目标数据");
            return false;
        }
        for (DBData dbData : dataList) {
            boolean result = delete(dbData);
            if (!result) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 清空一个数据表
     *
     * @param tableName 要清空的数据库表名
     * @return 是否清空成功
     */
    public boolean deleteAll(String tableName) {
        if (dbVersion == 0) {
            error("数据库不存在，请先通过add()或createNewTable()来创建一个数据库");
            return false;
        }
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
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();    //结束事务
        }
        return true;
    }
    
    /**
     * 全表查询
     *
     * @param tableName 表名
     * @param sort      排序方法
     * @return 查询结果，请以 list.size()==0 来判断没有结果
     */
    public List<DBData> findAll(String tableName, int sort) {
        if (dbVersion == 0) {
            error("数据库不存在，请先通过add()或createNewTable()来创建一个数据库");
            return new ArrayList<>();
        }
        if (db == null) {
            helper = new SqlliteHelper(context, dbName, dbVersion);
            db = helper.getWritableDatabase();
        }
        
        List<DBData> list = new ArrayList<DBData>();
        Cursor c;
        try {
            c = getQueryCursor(tableName);
        } catch (Exception e) {
            return list;
        }
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
     * 条件查询
     *
     * @param tableName 表名
     * @param conditions 查询条件，例如：  TABLE_COLUMN_NAME like 'ABC'
     * @return 查询结果，请以 list.size()==0 来判断没有结果
     */
    public List<DBData> findConditions(String tableName, String conditions) {
        if (dbVersion == 0) {
            error("数据库不存在，请先通过add()或createNewTable()来创建一个数据库");
            return new ArrayList<>();
        }
        if (db == null) {
            helper = new SqlliteHelper(context, dbName, dbVersion);
            db = helper.getWritableDatabase();
        }
        
        List<DBData> list = new ArrayList<DBData>();
        Cursor c;
        try {
            c = getQueryCursor(tableName,conditions);
        } catch (Exception e) {
            return list;
        }
        while (c.moveToNext()) {
            DBData data = new DBData(tableName);
            for (int i = 0; i < c.getColumnCount(); i++) {
                String key = c.getColumnName(i);
                data.set(key, c.getString(c.getColumnIndex(key)));
            }
            list.add(data);
        }
        c.close();
        return list;
    }
    
    /**
     * 全表分页查询
     *
     * @param tableName 表名
     * @param start     开始索引
     * @param count     数量
     * @param sort      排序方法
     * @return 查询结果，请以 list.size()==0 来判断没有结果
     */
    public List<DBData> findSub(String tableName, int start, int count, int sort) {
        if (dbVersion == 0) {
            error("数据库不存在，请先通过add()或createNewTable()来创建一个数据库");
            return new ArrayList<>();
        }
        if (db == null) {
            helper = new SqlliteHelper(context, dbName, dbVersion);
            db = helper.getWritableDatabase();
        }
        
        List<DBData> list = new ArrayList<DBData>();
        Cursor c;
        try {
            c = getQueryCursor(tableName, start, count);
        } catch (Exception e) {
            return list;
        }
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
    
    public List<DBData> findSub(String tableName, int start, int count) {
        return findSub(tableName, start, count, SORT_NORMAL);
    }
    
    public List<DBData> findSub(String tableName, int start, int count, String sortName, int sort) {
        if (dbVersion == 0) {
            error("数据库不存在，请先通过add()或createNewTable()来创建一个数据库");
            return new ArrayList<>();
        }
        if (db == null) {
            helper = new SqlliteHelper(context, dbName, dbVersion);
            db = helper.getWritableDatabase();
        }
        
        List<DBData> list = new ArrayList<DBData>();
        Cursor c;
        try {
            c = getQueryCursor(tableName, start, count, sortName, sort);
        } catch (Exception e) {
            return list;
        }
        while (c.moveToNext()) {
            DBData data = new DBData(tableName);
            for (int i = 0; i < c.getColumnCount(); i++) {
                String key = c.getColumnName(i);
                String value = c.getString(c.getColumnIndex(key));
                
                data.set(key, value);
            }
            list.add(data);
        }
        c.close();
        return list;
    }
    
    /**
     * @param tableName 表名
     * @return 查询结果，请以 list.size()==0 来判断没有结果
     * 全表正序查询，会以查询条件中的键值对作为条件进行查询
     */
    public List<DBData> findAll(String tableName) {
        return findAll(tableName, SORT_NORMAL);
    }
    
    /**
     * 按条件查询，会以查询条件中的键值对作为条件进行查询
     *
     * @param conditions 查询条件
     * @return 查询结果，请以 list.size()==0 来判断没有结果
     */
    public List<DBData> find(DBData conditions, int sort) {
        if (dbVersion == 0) {
            error("数据库不存在，请先通过add()或createNewTable()来创建一个数据库");
            return new ArrayList<>();
        }
        if (db == null) {
            helper = new SqlliteHelper(context, dbName, dbVersion);
            db = helper.getWritableDatabase();
        }
        
        List<DBData> list = new ArrayList<DBData>();
        Cursor c;
        try {
            c = getQueryCursor(conditions);
        } catch (Exception e) {
            return list;
        }
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
     * 正序按条件查询，会以查询条件中的键值对作为条件进行查询
     *
     * @param conditions 查询条件
     * @return 查询结果，请以 list.size()==0 来判断没有结果
     */
    public List<DBData> find(DBData conditions) {
        return find(conditions, SORT_NORMAL);
    }
    
    //查询指针
    private Cursor getQueryCursor(String tableName) {
        String sql = "SELECT * FROM " + tableName;
        log("SQL.exec: " + sql);
        Cursor c = db.rawQuery(sql, null);
        return c;
    }
    
    //条件查询指针
    private Cursor getQueryCursor(String tableName,String conditions) {
        String sql = "SELECT * FROM " + tableName +" WHERE " + conditions;
        log("SQL.exec: " + sql);
        Cursor c = db.rawQuery(sql, null);
        return c;
    }
    
    //查询指针（分页）
    private Cursor getQueryCursor(String tableName, int start, int count) {
        String sql = "SELECT * FROM " + tableName + " limit " + start + "," + count;
        log("SQL.exec: " + sql);
        Cursor c = db.rawQuery(sql, null);
        return c;
    }
    
    //查询指针（分页） + 指定排序
    private Cursor getQueryCursor(String tableName, int start, int count, String sortName, int sort) {
        String sql = "SELECT * FROM " + tableName + " ORDER BY CAST(" + sortName + " AS REAL) " + (sort == SORT_NORMAL ? "ASC" : "DESC") +  " limit " + start + "," + count ;
        log("SQL.exec: " + sql);
        Cursor c = db.rawQuery(sql, null);
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
    
    private Cursor getQueryCursorWithoutId(DBData conditions) {
        String sql = "SELECT * FROM " + conditions.getTableName() + " where ";
        Set<String> set = conditions.getData().keySet();
        for (String key : set) {
            if (!key.equals("_id")) {
                String value = conditions.getData().get(key);
                sql = sql + " " + key + " = \'" + value + "\' AND";
            }
        }
        if (sql.endsWith("AND"))
            sql = sql.substring(0, sql.length() - 3);
        log("SQL.exec: " + sql);
        Cursor c = db.rawQuery(sql, null);
        return c;
    }
    
    
    /**
     * 更新一个已有的旧表
     *
     * @param tableName 表名
     * @param newKeys   新增的列名
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
        Preferences.getInstance().set(context, "KongzueDB", dbName + ".version", dbVersion);
        closeDB();
        helper = new SqlliteHelper(context, dbName, dbVersion);
        db = helper.getWritableDatabase();
    }
    
    /**
     * 查询一个表的内容数量
     *
     * @param tableName 表名
     * @return 查询数量结果
     */
    public long getCount(String tableName) {
        return getCount(tableName, null);
    }
    
    /**
     * 按条件查询一个表的内容数量
     *
     * @param tableName  表名
     * @param conditions 查询条件
     * @return 查询数量结果
     */
    public long getCount(String tableName, DBData conditions) {
        if (dbVersion == 0) {
            error("数据库不存在，请先通过add()或createNewTable()来创建一个数据库");
            return 0;
        }
        if (db == null) {
            helper = new SqlliteHelper(context, dbName, dbVersion);
            db = helper.getWritableDatabase();
        }
        long count = 0;
        db.beginTransaction();
        try {
            String sql = "select count(*) from " + tableName;
            
            if (conditions != null) {
                sql = sql + " where ";
                Set<String> set = conditions.getData().keySet();
                for (String key : set) {
                    String value = conditions.getData().get(key);
                    sql = sql + " " + key + " = \'" + value + "\' ,";
                }
                if (sql.endsWith(","))
                    sql = sql.substring(0, sql.length() - 1);
            }
            
            log("SQL.exec: " + sql);
            
            Cursor cursor = db.rawQuery(sql, null);
            cursor.moveToFirst();
            count = cursor.getLong(0);
            cursor.close();
            
            db.setTransactionSuccessful();  //设置事务成功完成
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            db.endTransaction();    //结束事务
        }
        return count;
    }
    
    public boolean isHaveTable(String tableName) {
        if (dbVersion == 0) {
            error("数据库不存在，请先通过add()或createNewTable()来创建一个数据库");
            return false;
        }
        if (db == null) {
            helper = new SqlliteHelper(context, dbName, dbVersion);
            db = helper.getWritableDatabase();
        }
        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("select name from sqlite_master where type='table';", null);
            while (c.moveToNext()) {
                //遍历出表名
                if (c.getString(0).equals(tableName)) {
                    return true;
                }
            }
            c.close();
            
            db.setTransactionSuccessful();  //设置事务成功完成
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();    //结束事务
        }
        return false;
    }
    
    /**
     * 创建一个新表，按照 data 作为样板进行创建，data.getTableName() 作为表名，data中所有键作为列进行创建
     *
     * @param data 数据源范例
     */
    public void createNewTable(DBData data) {
        if (isHaveTable(data.getTableName())) {
            error("错误：已存在表" + data.getTableName());
            return;
        }
        
        newTableSQLCommand = "CREATE TABLE IF NOT EXISTS " + data.getTableName() + " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, ";
        Set<String> set = data.getData().keySet();
        for (String key : set) {
            String value = data.getData().get(key);
            if (!key.equals("_id")) {
                newTableSQLCommand = newTableSQLCommand + " " + key + " TEXT,";
            }
        }
        
        if (newTableSQLCommand.endsWith(","))
            newTableSQLCommand = newTableSQLCommand.substring(0, newTableSQLCommand.length() - 1);
        
        newTableSQLCommand = newTableSQLCommand + ")";
        
        log("SQL.exec: " + newTableSQLCommand);
        
        if (dbVersion == 0) {
            createTableSQLCommand = newTableSQLCommand;
            newTableSQLCommand = null;
        }
        dbVersion++;
        Preferences.getInstance().set(context, "KongzueDB", dbName + ".version", dbVersion);
        closeDB();
        helper = new SqlliteHelper(context, dbName, dbVersion);
        db = helper.getWritableDatabase();
    }
    
    private boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("-?[0-9]*");
        return pattern.matcher(str).matches();
    }
    
    private boolean isDecimal(String str) {
        Pattern pattern = Pattern.compile("-?[0-9]*.?[0-9]*");
        return pattern.matcher(str).matches();
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
    
    private DBData parameter;
    
    public DB addParameter(String key, String value) {
        if (parameter == null) parameter = new DBData("");
        parameter.set(key, value);
        return this;
    }
    
    /**
     * 通过addParameter(key,value)添加查询条件进行条件查询
     *
     * @param tableName 表名
     */
    public List<DBData> find(String tableName) {
        if (parameter != null) {
            parameter.setTableName(tableName);
            List<DBData> result = find(parameter);
            parameter = null;
            return result;
        }
        error("请先通过addParameter(key,value)添加查询条件");
        return null;
    }
    
    /**
     * 通过addParameter(key,value)添加查询条件进行查询，仅返回是否存在此条件的数据
     *
     * @param tableName 表名
     * @return 是否存在样板条件数据
     */
    public boolean isHave(String tableName) {
        if (parameter == null) {
            error("请先通过addParameter(key,value)添加查询条件");
            return false;
        }
        List<DBData> result = findWithoutId(parameter);
        parameter = null;
        if (result != null && !result.isEmpty()) return true;
        return false;
    }
}
