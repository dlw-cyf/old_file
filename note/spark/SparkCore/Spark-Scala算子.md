### transformations-转换算子--懒执行

* **延迟执行**----针对RDD的操作

* transformations是**某一类算子**（函数)

|                    Function                    |           arg=>return            |
| :--------------------------------------------: | :------------------------------: |
|     map(f:T => U)，flatMap(f:T => seq[U])      |         RDD[T] => RDD[U]         |
|                  **distinct**                  |   去重--底层使用wordCount实现    |
|              filter(f : T =>Bool)              |         RDD[T] => RDD[T]         |
|                  groupByKey()                  | RDD[(K , V) => RDD[(K , Seq[U)]] |
|       **reduceByKey(F : (V , V) => V)**        |   RDD[(K , V)] => RDD[(K ,V)]    |
|                  **sortBy()**                  |     通过key或者value进行排序     |
|                 **sortByKey**                  |           通过key排序            |
|                     sample                     |             随机抽样             |
| join，leftOutJoin，rightOutJoin，fullOuterJoin |   将两个RDD通过key进行连接合并   |
|                     union                      |        将两个RDD进行合并         |
|                 mapPartitions                  |   操作每个partition--返回iter    |
|                foreachPartition                |     操作partition后--不返回      |
|                  intersection                  |               交集               |
|                    subtract                    |               差集               |

* **sortBy(f:(T),ascending,numPartitions)**---通过f:(T)的返回进行排序

  ```scala
  //同过value值进行排序   f:(T) 通过放回的key或者value进行排序，ascending控制排序规则numPatition可设置分区个数
  rdd1.sortBy(_._2,false)
  ```

* **sample(withReplacement,fraction,seed)**---随机抽样

  ```scala
  // true表示有放回的抽样，fraction表示抽样的多少，send--时间种子
  val result: RDD[String] = lines.sample(true,0.1,100L)
  ```

* **mapPartitionsWithIndex(index,Iter)**---通过分区对数据进行操作

  ```scala
  //index:分区号(从0开始),iter:每个分区的迭代值
  val rdd2 = rdd1.mapPartitionsWithIndex((index,iter)=>{
      val list = new ListBuffer[String]()
      while(iter.hasNext){
          val one = iter.next()
          list.+=(s"rdd partition = [$index].varl = [$one]")
      }
      list.iterator
  })
  rdd2.foreach(println)
  ```

* **zip** --等同于python的zip函数，将**两个迭代变量合并在一起**

* **zipWithIndex**---和自己的**下标**压缩在一起-->**返回**一个**Tuple2**

* **cogroup**---**相同的key**作为一组

  ```scala
  //返回一个Tuple2:	key为RDD的key		value为nameRDD和scoreRDD的相同key的value的可迭代变量
  val result: RDD[(String, (Iterable[Int], Iterable[Int]))] = nameRDD.cogroup(scoreRDD)
  ```

* **repartition(numPartition)**--可以(**常用于)增多分区**，也可以减少分区---**宽依赖算子**，会产生shuffle

  ```scala
  //numPartition表示分区的数目
  val rdd3 = rdd2.repartition(4)
  ```

* **coalesce(numPartition,shuffle)**--常用与减少分区---窄依赖算子

  ```scala
  //numPartition:分区的数目	shuffle:分区时是否进行shuffle	若设为true等同于repartition 
  val rdd3 = rdd.coalesce(2,false)
  ```

  

### Action-行动算子

* Action也是一类**算子(函数)**
* 每一个**Action**都有一个**Job任务**

|       Function       |                   arg=>return                   |
| :------------------: | :---------------------------------------------: |
|       count()        |                 RDD[T] => Long                  |
|      collect()       |                RDD[T] => Seq[T]                 |
| reduce(f: (T,T) =>T) |                   RDD[T] => T                   |
|        first         |         获取第一条数据（take（1）实现）         |
|      take（n）       |                  获取前n条数据                  |
|      reduce(f)       | 将值传入f中，并将每次的放回结果作为下次的传入值 |

* **count()**和**collect()**会将**返回值**发送到**driver**
  
  * 避免在数据量时使用，避免driver内存不够
  
* **reduce（）**--将RDD的前两个value作为**形参传递**，将每次**返回的值作为下次的第一个参数**，第二个参数为**RDD的下一个value**

  ```scala
  val nums = Array(1,2,3,4,5,6,7,8,9)
  val numsRdd = sc.parallelize(nums,3)
  val reduce = numsRdd.reduce((a,b) => a+b)
  ```

* **countByKey**---一定要作用于k，v格式的RDD上

  ```scala
  // 统计key的个数，返回一个map  key为RDD的key值，value为相同可以的个数
  val key_count: collection.Map[String, Long] = rdd1.countByKey()
  ```

* **countByValue**---不一定要作用在k，v格式的RDD上

  ```scala
  //作用于一般的RDD上
  val rdd = sc.parallelize(List[Int](1,2,3,4,5,5))
  val count = rdd.countByValue()
  count.foreach(println)
  //作用于k,v格式的RDD上		将RDD的key和value整体作为返回map的key	value相同的key和value的个数
  val result: collection.Map[(String, Int), Long] = rdd1.countByValue()
  result.foreach(println)
  ```

  

### 持久化算子--懒执行

* **cache**

  * 默认将数据**存储在内存**中
  * cache() = persist() = persist(storageLevel.MEMORY_ONLY)

  ```scala
  //将数据持久化到内存
  rdd = rdd.cache()
  ```

* **persist**

  * 可以**手动指定**持久化级别

  * **常用**持久化级别

    * MEMORY_ONLY
    * MEMORY_ONLY_SER
    * MEMORY_AND_DISK
    * MEMORY_AND_DISK_SEQ

  * 注意事项

    1. **_2** : 表示有**副本**

    2. 尽量**少使用DISK_ONLY**级别

    3. 最小持久化**单位**是**partitioin**

    4. cache和persist之后可以直接**赋值给一个值,**下次直接使用这个值，就是使用的**持久化的数据**

    5. 如果采用第4条方式，后面**不能**紧跟**action**算子

       ```scala
       //这里为错误示例  此时放回的值不是需要存储的值,而是Action算子的求出的值
       rdd.persist(StorageLevel.MEMORY_ONLY).count()
       ```

    6. cache和persist数据,当**application执行完**之后会**自动清除数据**

### checkpoint

* 将数据直接**持久化到指定的目录**,当lineage计算非常复杂，可以尝试使用checkpoint，checkpoint还可以**切断RDD的依赖关系**

* **特殊场景使用**checkpoint，对RDD使用checkpoint要**慎用**

* checkpoint要指定目录，可以将数据持久化到指定目录中，当application执行完成之后，这个目录中的**数据不会被清除**

* checkpoint的执行流程

  1. 当sparkjon执行完成之后，Spark会从后**往前回溯**，找到**checkpointRDD做标记**
  2. 回溯完成之后，框架重新启动一个Job，计算**标记的RDD的数据**放入指定的checkpoint目录中
  3. 数据计算完成，放入目录之后，会**切断RDD的依赖关系**
  4. **优化**：对哪个RDD进行checkpoint，**最好先cache下**，这样回溯完成后再计算CheckpointRDD数据的时候可以直接在内存中拿到放到指定的目录中，防止**checkpoint二次计算**

  ```scala
  //指定checkpoint保存目录
  sc.setCheckpointDir("./data/ck")
  val rdd: RDD[String] = sc.textFile("./data/words")
  
  //使用checkpoint之前先cache(优化--避免checkpoint二次计算)
  rdd.cache()
  rdd.checkpoint()
  ```

  

### unpersist

* **删除**指定**RDD存储**在内存或者磁盘的**数据**

  ```scala
  //删除指定RDD存储的数据
  rdd.unpersist()
  ```

  