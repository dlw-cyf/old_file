#### 1. 表空间和用户的创建以及用户授权

##### 1.1 创建表空间

```sql
create tablespace dlw
datafile 'D:\programmeApp\oracle\tablespace'
size 100m
autoextend on
next 10m;
```

##### 1.2 删除表空间

```sql
drop tablespace dlw;
```

##### 1.3 创建用户

```sql
--用户名称
create user dlw
--用户密码
identified by dlw
--指定用户所在tablespace
default tablespace dlw;
```

##### 1.4 给用户授权

* oracle数据库常用角色
  * connect：连接角色，基本角色
  * resource：开发者角色
  * dba：超级管理员角色

```sql
--给dlw用户授予dba角色
grant dba to dlw;
```

#### 2. 表的管理

##### 2.1 创建表

```sql
--创建一个person表
create table person(
       pid number(20),
       pname varchar2(10)
);
```

##### 2.2 修改表结构

* 2.2.1 添加一列

  ```sql
  --添加一列
  alter table person add (gender number(1),age number(10));
  ```

* 2.2.2 修改列类型

  ```sql
  --修改列类型
  alter table person modify gender char(1);
  ```

* 2.2.3 修改列的名称

  ```sql
  --修改列名称
  alter table person rename column gender to sex;
  ```

* 2.2.4 删除一列

  ```sql
  --删除一列
  alter table person drop column sex;
  ```

##### 2.3 增删改查

* 2.3.1 添加一条记录

  ```sql
  --添加一条记录
  insert into person (pid,pname,age) values (3, '六个' ,21);
  ```

* 2.3.2 修改一条记录

  ```sql
  --修改一条记录
  update person set pname = '小戴' where pid=2;
  commit;
  ```

* 2.3.3 删除表

  ```sql
  --三个删除
  --删除表中全部记录
  delete from person;
  --删除表结构
  drop table person;
  --先删除表，再创建表
  --在数据量大的情况下，尤其是有索引的情况下，
  --索引可以加快查询效率，但是会影响增删改效率。
  truncate table person;
  ```

* 2.3.4 删除记录

  ```sql
  delete from person where pid=1;
  ```

#### 序列

* 默认从1开始，依次递增，主要用来给主键赋值使用。
* dual：虚表，只是为了补全语法，没有任何意义。

```sql
create sequence s_person;

select s_person.nextval from dual;
```

* 使用序列插入数据

  ```sql
  insert into person (pid,pname,age) values (s_person.nextval, '小戴' ,21);
  ```

#### scount用户

* 密码：tiger

* 解锁scott用户

  ```
  alter user scott account unlock;
  ```

* 解锁scott用户的密码

  ```sql
  --解锁scott用户的密码【此句也可以用来重置密码】
  alter user scott identified by tiger;
  ```

  

#### oracle的分页查询

```sql
--第一种方式
select * from (
  select rownum rn ,tt.* from (
         select emp.* from emp order by sal desc
  ) tt where rownum<11
) where rn>5;

--第二种方式：使用此方式不能order by
select * from (
       select rownum rn ,emp.* from emp
) tt where tt.rn<11 and tt.rn>5;
```

