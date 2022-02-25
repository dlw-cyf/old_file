#### 1. 创建数据库

```sql
create database databaseName;
```

#### 2. 创建表

##### 2.1 Create语句

```sql
create table tableName (col dataType)
partitioned by(col...) //分区
clustered by(col) into num buckets //分桶  num-->分桶的数量
row format delimited fields terminated by ','
collection items terminated by '-'	//集合的分隔方式
map keys terminated by ',';	//map的分隔方式
```

##### 2.2 as select_statement

* 有表结构和表信息

```sql
create table tableName as select_statement;
```

##### 2.3 like tableName

* 只有表结构

```sql
create table tableName like tableName;
```

#### 3. 静态分区

##### 3.1 添加分区

```sql
alert table tableName add partition(col=val);
```

##### 3.2 删除分区

```sql
alert table tableName drop pritition(col=val)
```

#### 4. 动态分区

##### 4.1 前提

* 开启动态分区（默认开启）

  ```shell
  set hive.exec.dynamic.partition = true;
  ```

* 进入非严格模式（默认为strict）

  * strict:至少有一个静态分区
  
  ```shell
  set hive.exec.dynamic.partition.mode = nonstrict;
  ```

##### 4.2 insert 方式加载数据

```sql
insert into table tableName partitioin (col) select_statement
```

##### 4.3 修复分区

```sql
msck repair table tableName
```

#### 5. 分桶

##### 5.1 前提

* 设置开启分桶模式

  ```shell
  set hive.enforce.bucketing = true
  ```

##### 5.2 create 方式创建分桶

```sql
create table if not exists u5(
id int,
name string,
age int
)
partitioned by(month int,day int)
clustered by(id) into 4 buckets
row format delimited fields terminated by ' ';
```

##### 5.3 分桶查询

```sql
select id, name, age from bucketdemo tablesample(bucket 2 out of 4 on age);
```

#### 6. 插入数据

##### 6.1 load关键字

```sql
load data local inpath 'path' into/overwrite table tableName (partition);
```

##### 6.2 from...insert关键字

```sql
form tableName
insert into/overwrite tableName select_statement
```

#### 7. 内部表和外部表

##### 内部表转换为外部表

```sql
alter table student set TBLPROPERTIES('EXTERNAL'='true')
```

##### 外部表转换为内部表

```sql
alert table student set TBLPROPERTIES('EXTERNAL'='false')
```



