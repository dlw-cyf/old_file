### 								1、创建DataFrame的方式

#### 1.1、读取json格式的文件

* **spark.read.json(filePath)**---读取文件

* **spark.read.json(jsonDataset)**--读取jsonDataset类型

```scala
//spark 2.0+使用SparkSession
val spark = SparkSession.builder().master("local")
.appName("SqlTestScala").getOrCreate()
//读取json 
//val df = spark.read.format("json").load("./data/json")
val df: DataFrame = spark.read.json("./data/json")

//show默认显示前20行，可以通过传参指定行数
df.show()
//输出DataFrame的格式信息
df.printSchema()
```

#### 1.2、读取嵌套json格式文件

* **导入隐式转换包**
  * import org.apache.spark.sql.functions._
* 使用**explode()**函数
* 可以通过**column.field**获取到值

```scala
/**
 * json格式如下
 * {"name","zhangsan","score":100,"infos":{"age":20,"gender":"man"}}
 *  对于读取嵌套的json格式的数据，可以直接infos.列名来获取值
 * {"name":"zhangsan","age":18,"score":[{"yuwen":98,"yingyu":100},{"yuwen":98,"yingyu":78}]}
 *
 */
val spark = SparkSession.builder().appName("SQLTest").master("local").getOrCreate()

val frame = spark.read.json("./data/ArrayJson")

frame.printSchema()
//设置不折叠显示
frame.show(false)

//    frame.createOrReplaceTempView("infosView")
//    spark.sql("select name,infos.age,score,infos.gender from infosView").show()
//导入隐式转换包  -- 使用函数explode()
import org.apache.spark.sql.functions._
import spark.implicits._

val tranDF = frame.select(frame.col("name"),frame.col("age"),explode($"score"))
.toDF("name","age","allscores")

tranDF.show(100,false)
tranDF.printSchema()

tranDF.createOrReplaceTempView("tranDFView")
spark.sql("select name,age," +
          "allscores.yuwen as yuwen ,allscores.yingyu as yingyu from tranDFView").show()

spark.stop()
```

#### 1.3、读取csv文件

* csv文件**有列名**

  ```scala
  val spark = SparkSession.builder().appName("ReaderCSV")
  .master("local").getOrCreate()
  
  val csvDF: DataFrame = spark.read.format("csv")
  .option("header","true")//设置有csv文件有列名
  .option("inferSchema",true.toString)//设置类型自动推断--默认为String类型
  .load("H:/TIM文件/task1_5.csv")
  
  csvDF.printScheme()
  csvDF.show()
  spark.stop()
  ```

* csv文件**没有列名**--**自定义**

  ```scala
  val spark = SparkSession.builder().appName("CustomColCSV")
  .master("local").getOrCreate()
  spark.sparkContext.setLogLevel("error")
  
  //自定义schema
  val schema = StructType(
      List[StructField](
          StructField("product_type",StringType),
          StructField("price",DoubleType)
      )
  )
  
  val productTypeDF: DataFrame = spark.read.format("csv").option("header",false)
  .schema(schema).load("./data/task.csv")
  productTypeDF.show()
  ```
  
  

#### 1.4、读取RDD创建DataFrame

* **反射的方式**
  * 将RDD转换为**自定义类型的RDD**
  * **rdd.toDF()**

```scala
val spark = SparkSession.builder().appName("DF_RDD")
.master("local").getOrCreate()
//去除日志
spark.sparkContext.setLogLevel("ERROR")

import spark.implicits._
val lines: RDD[String] = spark.sparkContext.textFile("./data/people.txt")
val peopleRDD: RDD[Person] = lines.map(line => {
    val str: Array[String] = line.split(",")
    Person(str(0).toInt, str(1), str(2).toInt, str(3).toDouble)
})
val frame: DataFrame = peopleRDD.toDF()
frame.show()
```

* **动态创建schame**的方式
  * 创建**Row类型的RDD**
  * 使用**spark.createDataFrame(rowRDD,structType)**映射成DataFrame

```scala
val spark = SparkSession.builder().master("local")
.appName("CustomSchema").getOrCreate()
val lines: RDD[String] = spark.sparkContext.textFile("./data/custom_schema")
val line: RDD[Row] = lines.map(s => {
    val arr: Array[String] = s.split(" ")
    //将数据封装成一个Row对象
    Row(arr(0).toInt,arr(1),arr(2).toInt)
})

//自定义的schema
val schema: StructType = StructType(
    List(
        //true 代表不为空
        StructField("id", IntegerType, true),
        StructField("name", StringType, true),
        StructField("score", IntegerType, true)
    )
)

val frame: DataFrame = spark.createDataFrame(line,schema)
frame.printSchema()
frame.show()
spark.stop()
```

####  1.5、读取parquet文件

```scala
val df2: DataFrame = spark.read.parquet("./data/parquet")
df2.show()
```

#### 1.6、读取JDBC-MySQL

* **config("spark.sql.shuffle.partitions",1)**
  * 设置**执行sql语句**时spark底层的**partition的数量**，**默认为200个**
  * 当数据量少，也会有200个分区，**空分区也会产生task**，**连接，读取mysql，效率低**，所有进行手动**指定分区数**

```scala
val spark = SparkSession.builder().master("local").appName("readMySQL")
.config("spark.sql.shuffle.partitions",1)
.getOrCreate()
/**
* 读取mysql表第一种方式
*/
val propertis = new Properties()
propertis.setProperty("user","root")
propertis.setProperty("password","123456")
val person: DataFrame = spark.read.jdbc("jdbc:mysql://127.0.0.1:3306/spark","person",propertis)
person.show()
/**
* 读取mysql表的第二种方式
*/
val map = Map[String,String](
    "url"->"jdbc:mysql://127.0.0.1:3306/spark",
    "driver"->"com.mysql.jdbc.Driver",
    "user"->"root",
    "password"->"123456",
    "dbtable"->"score"//表名
)
val score: DataFrame = spark.read.format("jdbc").options(map).load()
score.show()
/**
* 读取mysql表的第三种方式
* 使用sql语句查询结果作为表参数传递
*/

val sqlTable: DataFrame = spark.read.jdbc("jdbc:mysql://127.0.0.1:3306/spark","(select person.id,person.name,person.age,score.score from person,score where person.id=score.id) T",propertis)
sqlTable.show()
```

#### 1.7、Spark操作Hive表

* enableHiveSupport（）开启器Hive支持

```scala
//enableHiveSupport--开启Hive支持
val spark = SparkSession.builder().appName("CreateDataFrameFromHive").master("local")
.enableHiveSupport().getOrCreate()

spark.sql("USE spark")
spark.sql("DROP TABLE IF EXISTS student_infos")
//在hive中创建student_infos表
spark.sql("CREATE TABLE IF NOT EXISTS student_infos (name STRING,age INT) row format delimited fields terminated by ',' ")
spark.sql("load data local inpath './data/student_infos' into table student_infos")

spark.sql("DROP TABLE IF EXISTS student_score")
//在hive中国创建student_score表
spark.sql("CREATE TABLE IF NOT EXISTS student_score (name STRING,score INT)" +
          "row format delimited fields terminated by ',' ")
spark.sql("load data local inpath './data/student_score' into table student_score")
val df = spark.sql("select si.name,si.age,ss.score from student_infos si , student_score ss where si.name = ss.name")
spark.sql("drop table if exists goo_student_infos")

/**
* 将结果写入到hive表中
*/
df.write.mode(SaveMode.Overwrite).saveAsTable("good_student_infos")

/**
* 将Hive中的某张表加载为DataFrame
*/
val frame: DataFrame = spark.table("good_student_infos")
frame.show()
spark.stop()
```



#### **注意：**

* 读取**json格式**的文件，json中的属性名，自动成为列，schema信息会**自动推断。**

* 读取**json格式**的文件，**列会按照ASCII排序**。

* 读取**嵌套格式的json**文件，使用**列名.field**

  ### 								2、保存DataFrame各种方式



#### SaveMode指定存储文件的保存模式

* **Overwrite**：覆盖
* **Append**：追加
* **ErrorIfExists**：如果存在就报错
* **Ignore**：如果存在就忽略

#### 2.1、保存为parquet文件格式

```scala
//SaveMode.Append 表示追加文件
df1.write.mode(SaveMode.Append).format("parquet").save("./data/parquet")
```

#### 2.1、保存DataFrame到jdbc-MySQL

```scala
//创建临时视图
person.createOrReplaceTempView("person")
score.createOrReplaceTempView("score")

val result: DataFrame = spark.sql("select person.id,person.name,person.age,score.score from person,score where person.id=score.id")
//result表可以不存在
result.write.mode(SaveMode.Overwrite).jdbc("jdbc:mysql://127.0.0.1:3306/spark",table="result",propertis)

```

#### 2.2、保存DataFrame为CSV

* 保存为csv也需要指定 header

```scala
//    productTypeDF.write.csv("./data/column")  //没有write header

productTypeDF.repartition(1).write.format("csv").option("header",true)
.option("delimiter",",").save("./data/column_csv")
```

#### 2.3、保存DataFrame为Hive

```scala
/**
* 将结果写入到hive表中
* 	指定的表会自动创建  如果表存在,则需要字段名相同
*/
df.write.mode(SaveMode.Overwrite).saveAsTable("good_student_infos")
```



### DataFrame_Transfrom

* DataFrame转换为RDD

  ```scala
  val spark = SparkSession.builder().master("local").appName("Transform").getOrCreate()
  
  val df = spark.read.json("./data/json")
  df.show()
  val rdd: RDD[Row] = df.rdd
  rdd.foreach(row=>{
      //通过getAs[T](cloumnName)
      val name = row.getAs[String]("name")
      val age = row.getAs[Long]("age")
      println(s"name = $name , age = $age")
  })
  ```


### 注册表

​	**想使用sql查询，首先要讲DataFrame注册成表**

* **临时表**：这张表**不在内存**中，也**不在磁盘**中，相当于一个**指针指向了源文件**，底层**操作spark job读取**源文件

  * 创建一个**当前会话**的临时表--**createOrReplaceTempView(viewName)**

    ```scala
    //在当前会话有效
    df.createOrReplaceTempView("hhh")
    //通过sql语句对注册的表进行查询
    val frame: DataFrame = spark.sql("select name , age from hhh where age > 18")
    ```

  * 创建**全局会话**临时表--**createOrReplaceGlobalTempView(viewName)**

    * 在会话中使用需要使用**global_tmp**关键字**指定**

    ```scala
    df.createOrReplaceGlobalTempView("ggg")
    //创建一个新的session会话
    val sparkSession01: SparkSession = spark.newSession()
    sparkSession01.sql("select name , age from global_temp.ggg where age > 18").show()
    ```


### DateSet

* DataSet与RDD的**区别：**
  * DataSet**内部序列化机制与RDD不同**，可以**不用反序列化成对象调用对象的方法**
  * DataSet是**强类型**的，**默认列名**是"**value**"
  * 操作上的方法比RDD多，RDD中有的算子在DataSet中都有

* DataSet的创建和使用

  ```scala
  val spark = SparkSession.builder().master("local").appName("WC_DataSet").getOrCreate()
  
  import spark.implicits._
  
  val linesDs: Dataset[String] = spark.read.textFile("./data/words")
  val words: Dataset[String] = linesDs.flatMap(_.split(" "))
  
  /**
       * 使用DataSet api 处理
       * 这里需要导入隐式转换
       *  spark.implicits._
       * 使用agg聚合中的聚合函数，这里也需要导入spark sql的函数
       *  import org.apache.spark.sql.functions._
       */
  //    import spark.implicits._
  //    import org.apache.spark.sql.functions._
  //
  //    val groupDS: RelationalGroupedDataset = words.groupBy($"value" as "word")
  //    val aggDs: DataFrame = groupDS.agg(count("*") as "totalCount")
  //    val result: Dataset[Row] = aggDs.sort($"totalCount" desc)
  //    result.show(100)
  
  /**
       * 使用sql语句处理
       * 这里默认 words中有个value列，withColumnRenamed 是给列重新命名
       */
  //    val frame: DataFrame = words.toDF()
  val frame: DataFrame = words.withColumnRenamed("value","word")
  frame.createOrReplaceTempView("WC")
  spark.sql("select word , count(word) as totalCount from WC group by word order by totalCount desc").show()
  //    spark.sql("select value , count(value) as totalCount from WC group by value order by totalCount desc").show()
  
  spark.stop()
  ```

  