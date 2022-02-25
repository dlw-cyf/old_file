#### 排序

* order by

  * 对输出做全局排序，因此只有一个reducer，会导致输入数据规模较大时，需要较长的计算时间

* sort by

  * 不是全局排序，其在数据进入reducer前完成排序，只能保证局部有序（每个reducer出来的数据是有序的）。
  * 设置map和reduce个数

  ```mssql
  set mapred.reduce.tasks=5;
  set mapred.map.tasks=5;
  ```

  * 优势

    * 执行了局部排序之后可以为接下去的全局排序提高不少的效率（其实就是做一次归并排序就可以做到全局排序了）

    ```sql
    select * from(select name,Company from haha distribute by Company sort by Company)t order by Company
    ```

* distribute by

  * 根据指定的字段将数据分到不同的reducer
  * 分发算法是hash散列，一般和 sort by 排序一起使用

* cluster by (默认排序规则为desc)

  * 等同于distribute by 和 sort by 结合
  * 以下两条语句等效

  ```sql
  select cid , price from orders DISTRIBUTE BY cid SORT BY cid ;
  select cid , price from orders CLUSTER BY cid ;
  ```

  * 注意
    * 被 cluster by 指定的列只能按照降序进行排列，不能指定 desc 和 asc 。

#### 多表连接

* union all

  * 相同字段数和相同字段类型的合并。
  * 根据不同的规则筛选出来的数据需要合并到一个表格中，这时候就需要用到hive中的union all操作。

  ```sql
  select id from test1 
  union all
  select id from test2
  ```

* join

  * 无字段联合 join

  ```sql
  select n.namea as name from (
      select nameA from db_phone where nameB='杨力谋' 
      union all 
      select nameB from db_phone where nameA='杨力谋'
  ) n group by n.namea;
  ```

#### 自定义hive函数

* 继承UDF类
* evaluate

```java
public String evaluate(final String incomeStr) {
		double income = 0;
		if(incomeStr.endsWith("元以上")){
			income = Double.parseDouble(incomeStr.substring(0, incomeStr.indexOf("元")))*1.5;
		}else if(incomeStr.endsWith("元以下")){
			income = Double.parseDouble(incomeStr.substring(0, incomeStr.indexOf("元")))/2;
		}else if(incomeStr.endsWith("元")){
			String preIncome = incomeStr.substring(0, incomeStr.indexOf("～"));
			String sufIncom = incomeStr.substring(incomeStr.indexOf("～")+1, incomeStr.lastIndexOf("元"));
			income = (Double.parseDouble(preIncome)+Double.parseDouble(sufIncom))/2;
		}
		return String.valueOf(income);
	}
```

* 添加 jar 包至hive

```
add jar /home/CustomUDF.jar;
```

* 显示所有添加的 jar

```
list jars;
```

* 删除指定jar

```sql
delete jar /home/CustomUDF.jar;
```

* 创建临时方法

```sql
create tempoary function funName as "包名+类名";
```

#### hive常见命令

> 关闭hive保留字
>
> ​	set hive.support.sqlll.reserved.keywords = false;
>
> 动态分区
>
> ​	开启动态分区（默认开启）
>
> ​		set hive.exec.dynamic.partition = true;
>
> ​	关闭严格模式（严格模式下至少有一个静态分区）
>
> ​		set hive.exec.dynamic.partition.mode = nostrict;
>
> 设置reduce的个数
>
> ​	set hive.mapred.reduce.tasks = 3;
>
> 添加自定义函数jar
>
> ​	add jar 'path'；
>
> 创建临时函数
>
> ​	create temporary function 'functioinName' as '包名+类名'

##### 读取json数据

1. add jar

2. 创建表格是设置序列化方式

   ```shell
   ROW FORMAT SERDE 'org.openx.data.jsonserde.JsonSerDe'
   ```

   