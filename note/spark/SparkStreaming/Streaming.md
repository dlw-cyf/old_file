SparkStreaming执行流程

<img src="G:\学习笔记\spark\SparkStreaming\SparkStreaming\SparkStreaming接收数据原理.jpg" alt="SparkStreaming接收数据原理"  />

#### 运行SparkStreaming

1. 需要**设置local[2]**，因为一个线程是**读取数据**，一个线程是**处理数据**

2. 创建StreamingContext两种方式

   * 通过**SparkConf()对象创建**---这种方式**不能创建SparkContext()**对象，创建StreamingContext会自动创建，**一个application中不能有两个SparkContext对象**

     ```scala
     val conf = new SparkConf()
     conf.setAppName("StreamingTest")
     //设置程序线程数
     conf.setMaster("local[2]")
     //Durations.seconds() 设置处理批次时间间隔
     val ssc = new StreamingContext(conf,Durations.seconds(5))
     ```

   * 通过**SparkContext()对象创建**

     ```scala
     val conf = new SparkConf()
     conf.setAppName("StreamingTest")
     conf.setMaster("local[2]")
     val sc = new SparkContext(conf)
     val ssc = new StreamingContext(sc,Durations.seconds(5))
     ```

3. Durations 批次间隔时间的设置需要根据**集群的资源情况**以及监控每个job的执行时间来**调节出最佳时间**

4. SparkStreaming所有业务处理完成之后需要有一个**output operator操作**

5. StreamingContext.start() streaming框架启动之后是不能再次添加业务逻辑的

6. StreamingContext.stop()无参的stop方法会**将SparkContext一同关闭**，**默认为true**，false表示不关闭SparkContext对象

7. StreamingContext**.stop()停止**之后是不能**再调用start() 方法**

8. **监控端口**

   * **nc -lk 9999**


#### SparkStreaming 函数

* **updateStateByKey**（updateFunc:（Seq[V]，Option[S]）=> Options[S]）

  * 根据key**更新状态** 需要**设置checkpoint来保存状态**
  * 默认key的状态在**内存中有一份**，在**checkpoint目录中有一份**
  * 多久会将内存中的数据（每一个key所对应的的状态）写入到磁盘上一份呢？
    * 如果**batchInterval小于10s**，那么**10s**会将**内存中的数据写入到磁盘**一份
    * 如果batchInterval**大于10s**，那么以**batchInterval的时间为准**

  ```scala
  //需要保存上一次的状态  需要持久化数据
  ssc.checkpoint("./data/streamingCheckPoint")
  
  //首先通过key分组，currentValues表示当前batchInterval的value preValue表示之前转态的value
  val result: DStream[(String, Int)] = pairwords.updateStateByKey((currentValues: Seq[Int], preValue: Option[Int]) => {
      var totalValues = 0
      if (!preValue.isEmpty) {
          totalValues += preValue.get
      }
      for (value <- currentValues) {
          totalValues += value
      }
      Option(totalValues)
  })
  result.print()
  ```

* **reduceByKeyAndWindow**  窗口操作
  
  * 每隔窗口滑动时间计算窗口长度内的数据，按照指定的方式处理
  
  ```scala
  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
  Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)
  val conf = new SparkConf()
  conf.setAppName("WindowOperator").setMaster("local[2]")
  
  val  ssc = new StreamingContext(conf,Durations.seconds(5))
  //    ssc.sparkContext.setLogLevel("ERROR")
  
  //nc -lk 9999   在虚拟机开启端口
  val lines = ssc.socketTextStream("node4",9999)
  val pairRDD = lines.flatMap(_.split(" ")).map((_,1))
  //获取指定时间段的DStream 可以通过自己的逻辑进行处理
  //    val ds: DStream[(String, Int)] = pairRDD.window(Durations.seconds(15),Durations.seconds(5))
  //    ds.print()
  	/**
       * 窗口操作普通的机智
       *
       * 滑动窗口和窗口长度是
       */
  //(v1:Int,v2:Int)=>{v1+v2}
  //    val windowsPrint: DStream[(String, Int)] = pairRDD.reduceByKeyAndWindow((v1:Int, v2:Int)=>{v1+v2},Durations.seconds(15),Durations.seconds(5))
  //    windowsPrint.print()
  	
  	/**
       * 窗口操作优化的机制
       *    需要通过checkpoint保存上一次的结果
       */
  ssc.checkpoint("./data/streamingWindowCheckPoint")
  val windowsPrint = pairRDD.reduceByKeyAndWindow(_+_,_-_,Durations.seconds(15),Durations.seconds(5))
  windowsPrint.print()
  
  ssc.start()
  ssc.awaitTermination()
  ssc.stop()
  ```
* **foreachRDD()**

  * foreachRDD是DStream中**outputOperator类算子**
  * foreachRDD可以遍历的到**DStream中的RDD**，可以在这个算子内对RDD使用**RDD的transformation类算子进行转化**，但是一定要使用**RDD的Action类算子触发执行**。
  * foreachRDD可以得到DStream中的RDD，在这个算子内，**RDD算子外执行的代码是在Driver端执行的**
  
  ```scala
    val pairDS: DStream[(String, Int)] = lines.flatMap(_.split(" "))
    .map((_,1)).reduceByKey(_+_)
    pairDS.foreachRDD(rdd=>{
        println("*************Driver 执行***************")
        rdd.filter(one=>{
            println("----------executor 执行------------")
            true
        }).count()
    })
  ```
  
  

* **saveAsTextFiles(prefix,[suffix])**---将SparkStreaming处理的结果保存在指定的目录中

  * 将此DStream的内容另**存为文本文件**，每批次数据产生的**文件名称格式**基于prefix和suffix
    * 例如 **prefix + 时间戳 + suffix**
  * saveAsTextFile是调用saveAsHadoopFile实现的
  * spark中**普通rdd可以直接用saveAsTextFile(path)**的方式，保存到本地，但是**DStream只有saveAsTextFile()方法，参数只有prefix，suffix，不能传入路径，保存的路径为checkpoint指定的目录**

  ```scala
  /**
  *SparkStreaming 监控一个目录数据时
  *	1.这个目录下已经存在的文件不会被监控到，可以监控增加的文件
  *	2.增加的文件必须是原子性产生。不能在文件中追加数据
  *	3.这些文件的格式必须相同，如:统一为文本文件
  */
  val result: DStream[(String, Int)] = pairwords.updateStateByKey((currentValues: Seq[Int], preValue: Option[Int]) => {
      var totalValues = 0
      if (!preValue.isEmpty) {
          totalValues += preValue.get
      }
      for (value <- currentValues) {
          totalValues += value
      }
      Option(totalValues)
  })
  //保存的多级目录就直接写在前缀中
  result.saveAsTextFiles("./data/prefix","suffix")
  ```

* **transform**

  * 将DStream**转换为RDD**，进行RDD的操作
  * **必须返回一个RDD**：返回的RDD会被**封装成一个DStream**
  * transform内，**RDD算子外**的代码会在**Driver端执行**，**RDD算子内**的代码会在**Executor端执行**

  ```scala
  def main(args: Array[String]): Unit = {
      //不打印日志
      Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
      Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)
      val conf = new SparkConf()
      conf.setMaster("local[2]").setAppName("TranfromAndForeachRDD")
      val ssc = new StreamingContext(conf,Durations.seconds(5))
      val sc = ssc.sparkContext
  
      val lines: ReceiverInputDStream[String] = ssc.socketTextStream("node4",9999)
      //设置黑名单
      val blackList = List[String]("zhangsan","lisi")
      //将黑名单转换为("张三",true) 类型的RDD
      val blackRDD = sc.parallelize(blackList).map((_,true))
  
      lines.map(line=>(line.split(" ")(1),line)).transform(pairRDD=>{
          println("------------我在Driver运行呢---------------")
          pairRDD.leftOuterJoin(blackRDD).filter(one=>{
              println("*******************我在Executor运行哦***********************")
              one._2._2.getOrElse(false) != true
          }).map(_._2._1)
      }).print()
  
      ssc.start()
      ssc.awaitTermination()
  
      ssc.stop()
  }
  ```

  

#### SparkStreaming--DriverHA

1. 提交application的时候 添加 **--supervise** 选项 如果Driver挂掉会**自动启动一个Driver**

2. **代码层面恢复**Driver---**StreamingContext.getOrCreate()**

   * 这个方法首先会从checkDir目录中获取StreamingContext
     * StreamingContext是**序列化存储在checkPoint目录**中，恢复时会**尝试反序列化**
     * 如果使用修改过的class可能会导致错误，此时需要更**换checkpoint目录**或者**删除checkpoint目录中的数据**
   * 若**不能**从checkpoint目录中**获取StreamingContext**，就会通过之后的**CreateStreamingContext这个方法**创建。

3. Driver HA主要用到 当**停止SparkStreaming**，**再次启动**时，SparkStreaming可以**接着上次消费的数据继续消费**

   ```scala
   val checkDir = "./data/StreamingHA"
   def main(args: Array[String]): Unit = {
       //代码层控制DriverHA
       val ssc = StreamingContext.getOrCreate(checkDir,CreateSteamingContext)
   
       ssc.start()
       ssc.awaitTermination()
       ssc.stop()
   }
   
   def CreateSteamingContext()= {
       val conf = new SparkConf()
       conf.setMaster("local[2]").setAppName("StreamingHA")
       val ssc: StreamingContext = new StreamingContext(conf,Durations.seconds(5))
       /**
        * 默认checkpoint 存储:
        *    1.配置信息
        *    2.DStream操作逻辑
        *    3.job的执行进度
        *    4.offset---kafka
        */
       ssc.checkpoint(checkDir)
   
       val linesDS: ReceiverInputDStream[String] = ssc.socketTextStream("node4",9999)
       val sc = ssc.sparkContext
       //模拟黑名单
       val blackList = List[String]("zhangsan","lisi")
       val broadBlackRDD = sc.broadcast(blackList)
   
       linesDS.map(line => {
           val arr = line.split(" ")
           (arr(1), arr)
       }).filter(pairDS=>{
           if (broadBlackRDD.value.contains(pairDS._1))
           	false
           else
           	true
       }).flatMap(_._2).map((_,1)).updateStateByKey((currentValues:Seq[Int],preValue:Option[Int])=>{
           var totalValues = 0
           //      if (!preValue.isEmpty){
           totalValues += preValue.getOrElse(0)
           //      }
           for(value <- currentValues){
               totalValues+=value
           }
           Option(totalValues)
       }).print()
   
       ssc
   }
   ```

   