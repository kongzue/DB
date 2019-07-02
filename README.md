# KongzueDB
简单易用的SQLite封装

<a href="https://github.com/kongzue/DB/">
<img src="https://img.shields.io/badge/Kongzue%20DB-1.0.8-green.svg" alt="Kongzue DB">
</a>
<a href="https://bintray.com/myzchh/maven/KongzueDB/1.0.8/link">
<img src="https://img.shields.io/badge/Maven-1.0.8-blue.svg" alt="Maven">
</a>
<a href="http://www.apache.org/licenses/LICENSE-2.0">
<img src="https://img.shields.io/badge/License-Apache%202.0-red.svg" alt="License">
</a>
<a href="http://www.kongzue.com">
<img src="https://img.shields.io/badge/Homepage-Kongzue.com-brightgreen.svg" alt="Homepage">
</a>

Demo预览图如下：
![KongzueDB](https://github.com/kongzue/Res/raw/master/app/src/main/res/mipmap-xxxhdpi/db.jpg)

## 使用前的约定与须知
- KongzueDB 是对 Android 中 SQLite 的封装，因简化存储方式以及采用自生成 SQL 语句的方式，仅可用于轻量需求的场景，如果需要多表联合查询等重度场景，以及对数据库中存储的数据类型有严格限定要求的项目，请勿使用本框架；

- 因为使用了 Map 与数据表对应的形式，数据源（DBData）与数据表中的键值对是对应的，请保证数据源（DBData）中不要存储无意义的键值，可能造成操作失败；

- 本框架目的是解决 SQLite 上手难度的问题，对其增删改查进行了进一步的封装，使其更好用更易用，流程更轻松，但本质上依然是在数据库中进行操作，请注意至少具备数据库、表、项，以及数据库的基本常识的情况下进行使用；

- 本框架会对插入的数据中额外的插入一个名为“_id”的自增键，请勿以任何方式对其进行修改。

## Maven仓库或Gradle的引用方式
Maven仓库：
```
<dependency>
  <groupId>com.kongzue.kongzuedb</groupId>
  <artifactId>kongzuedb</artifactId>
  <version>1.0.8</version>
  <type>pom</type>
</dependency>
```
Gradle：
在dependencies{}中添加引用：
```
implementation 'com.kongzue.kongzuedb:kongzuedb:1.0.8'
```

## 使用方法：

#### 创建数据库以及初始化表

使用下边的语句初始化数据库，dbName 为数据库名
```
DB db = new DB(this, dbName);    //准备数据库
```

要使用表中的增删改查等功能，请先初始化数据库，方法有两种：

1) 增加一个数据以根据数据的键自动创建表：
```
//准备一个数据，tableName 为表名
DBData dbData = new DBData(tableName);
dbData.set("name", "张三")
      .set("age", 18)
      .set("phone", 18513000000l);
//添加到数据库，第二个参数为是否允许重复
db.add(dbData, false);
```
此时，若没有这个表，数据库会自动根据数据 dbData 的键，自动创建一个名为 tableName 的表；

2) 使用创建方法创建一个表：
```
//创建表，tableName 为表名，所传入的 DBData 为数据样本
db.createNewTable(new DBData(tableName)
                                       .set("name", "")
                                       .set("age", 0)
                                       .set("phone", 0)
                 );
```

这里推荐使用第一种方法，直接创建数据表

3) 判断一张表是否存在
```
isHaveTable(tableName)
```

#### 添加数据
```
//准备一个数据，tableName 为表名
DBData dbData = new DBData(tableName);
dbData.set("name", "张三")
      .set("age", 18)
      .set("phone", 18513000000l);
//添加到数据库，第二个参数为是否允许重复
boolean flag = db.add(dbData, false);
//flag用于判断操作是否成功，成功返回 true，否则返回 false
```

#### 删除数据
删除已有的数据
```
//删除已有的数据，只能对已存在的数据（使用find查询出来的数据）进行修改，返回 boolean 的值以判断是否删除成功
db.delete(DBData);
```

按条件查找并删除数据：
```
//删除一个未知的数据，会根据查询条件 conditions 进行查询，并对查询到的结果进行删除，返回 boolean 的值以判断是否删除成功
db.deleteFind(new DBData("user").set("age", 22));
```

清空数据表：
```
//清空一个数据库，tableName 为要清空的数据库表名，返回 boolean 的值以判断是否删除成功，此操作会自动将自增键恢复到 0
db.deleteAll(tableName);
```

#### 修改数据
```
//修改已有的数据，只能对已存在的数据（使用find查询出来的数据）进行修改，返回 boolean 的值以判断是否修改成功
db.update(DBData);
```

#### 查询数据
查询所有数据：
```
List<DBData> allData = db.findAll(tableName);
for (DBData data : allData) {
    //data即查询出的数据
    log(data);
}
```

依据条件查询：
```
//传入的 DBData 为数据条件
List<DBData> dataList = db.find(new DBData("user").set("age", 18));
for (DBData data : dataList) {
    log(data);
}
```

正反序排序查询

在使用 findAll() 或 find() 方法时，可以额外的增加一个参数：
```
//sort 可选值为 DB.SORT_NORMAL 或 DB.SORT_BACK，默认为 DB.SORT_NORMAL，若使用 DB.SORT_BACK 则可以输出倒序的结果
db.findAll(tableName, SORT_NORMAL);
db.find(new DBData("user").set("age", 18), SORT_NORMAL);
```

#### 额外方法
使用 getCount 获取数据表中数据的数量：
```
long count = db.getCount("user");
```

使用 getCount 获取数据表中符合某个条件的数据的数量：
```
//会根据查询条件 conditions 进行查询，并返回数量
long count = db.getCount("user",new DBData("user").set("age", 22));
```

创建新表的方法：
```
//创建表，tableName 为表名，所传入的 DBData 为数据样本
db.createNewTable(new DBData(tableName)
                                       .set("name", "")
                                       .set("age", 0)
                                       .set("phone", 0)
                 );
```

更新一个已有的旧表：
```
//更新表，tableName 为表名，newKeys 为 List<String> 的需要增加的新键
db.updateTable(tableName, newKeys);
```

## 开源协议
```
   Copyright KongzueDB

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

## 更新日志：
v1.0.8:
- DB 新增 findWithoutId(...) 方法，可实现排除“_id”字段的查询；
- 修复已知 bug；

v1.0.4:
- 修复了无法创建多张表的bug；
- 新增 isHaveTable(tableName) 用于检测表是否存在；
- 对所有创建查询指针 Cursor 进行了异常判断，以保证在表不存在或其他情况时不会造成崩溃；

v1.0.1:
- 修复了一些bug；

v1.0.0:
- 首次发布；