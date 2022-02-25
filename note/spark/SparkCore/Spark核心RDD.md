### 											Spark核心RDD

* RDD --> **弹性分布式**的数据集

#### RDD的五大特性

1. RDD是由一系列**partition**组成
2. **算子**（函数）**作用在RDD的partition上**
3. RDD之间有**依赖关系**
4. **分区器**是作用在**k，v格式**的RDD上

5. partition是提供**数据计算**的**最佳位置**,利于数据处理的本地化。
   * 计算移动，数据不移动

#### 问题

1. Spark读取HDFS中数据的方法 **textFile** 底层是**调用的MR读写HDFS文件**的方法，首先会split，每个split对应一个block，**每个split对应生成RDD的每个parttion**
2. 什么是**K，V格式的RDD**
   * RDD中的数据是一个个的**tuple2数据**，那么这个RDD就是K,V格式的RDD
3. 哪里体现了**RDD的弹性**？
   * RDD之间有**依赖关系**
   * RDD的**partition可多可少**
4. 哪里体现RDD的**分布式**？
   * RDD的parition是分布在**多个节点**上的

* **注意**：**RDD**和**partition**中实际是**不存储数据**的

### 创建RDD方式

1. testFile("filepath")

   ```scala
   //文件路径
   var rdd: RDD[String] = sc.textFile("./data/persistData.txt")
   ```

2. parallelize(seq[T])

   ```scala
   //seq[T]  可迭代的数据		partition:表示分区数
   val rdd = sc.parallelize(Array[String]("a","b","c"),partition)
   ```

3. makeRDD(seq[T])

   ```scala
   //seq[T] 可迭代数据
   val rdd: RDD[Int] = sc.makeRDD(Array[Int](1,2,3))
   ```

### 窄依赖

* **父RDD**与**子RDD** partition之间的关系是**一对一**
* **父RDD**与**子RDD** partition之间的关系是**多对一**

### 宽依赖

* **父RDD**与**子RDD** partition之间的关系是**一对多**
* 宽依赖过程**一定有shuffle**
* 涉及到**数据的传输**