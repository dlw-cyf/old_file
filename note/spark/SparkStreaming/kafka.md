#### 									kafka概念

> kafka是**分布式消息系统**，默认消息是**存储磁盘**，默认**保存7天**。

##### **producer**：生产者

* 两种机制--**轮循**，**key的hash**
  * **轮循**：如果**key是null**，就是轮循
  * **key的hash**：如果**key是非null**，按照key的hash

##### **broker**：kafka集群节点

* broker之间**没有主从关系**，依赖**zookeeper协调**
* broker负责消息的**读写和存储**，每个**broker**可以**管理多个parition**

##### **topic**：一类消息

* **一类**(**消息**/**消息队列**)---例如：日志类消息...

* 每个topic是由**多个partition组成**--**挺高并行度**，partition数量在创建时指定	

##### **partition**

* 组成topic的单元，**直接接触磁盘**，消息是**append**到每个partition上的

* 每个partition内部消息是**强有序**的。**FIFO**
* partition可以有**副本**，**创建topic**是可以**指定**

##### consumer:消费者

* 每个consumer都有自己的**消费者组**
* 每个消费者组在**消费同一个topic**时，这个topic中的**数据**只能被**消费一次**
* 不同的消费者组之间互不影响
* 版本差异
  * kafka**0.8之前** consumer 在自己的**zookeeper**中**维护消费者offset**
  * kafka**0.8之后** consumer的offset是通过**kafka集群来维护**的

##### zookeeper

* 存储元数据信息
  * broker，topic，partition...
* kafka0.8之前还可以存储消费者offset

#### kafka常用命令

##### 创建kafka topic

> bin/**kafka-topics.sh** **--zookeeper** node1:2181... --create --topic topicName **--partitions** 30 **--replication-factor** 2
>
> * **partitions**：分区数，
>   * 控制topic将分片成多少个log。可以显示指定，如果不指定则会使用broker(server.properties)中的num.partitions配置的数量
>   * 虽然增加分区数可以提高kafka集群的吞吐量，但是过多的分区数或者是单台服务器的分区数过多，会增加不可用及延迟的风险，因为多的分区数，就意味着要打开更多的文件句柄，增加点到点的延时，增加客户端的内存消耗
>   *  分区数也限制了consumer的并行度，即限制了并行consumer消息的线程数不能大于分区数 
>   *  分区数也限制了producer发送消息是指定的分区。如创建topic时分区设置为1，producer发送消息时通过自定义的分区方法指定分区为2或以上的数都会出错的；这种情况可以通过alter –partitions 来增加分区数。 
> * **replication-factor**副本
>   *  replication factor 控制消息保存在几个broker(服务器)上，一般情况下等于broker的个数。 
>   *  如果没有在创建时显示指定或通过API向一个不存在的topic生产消息时会使用broker(server.properties)中的default.replication.factor配置的数量 

##### 查看所有topic列表

> bin/kafka-topics.sh --zookeeper node1:2181... --list

##### 查看指定topic信息

> bin/kafka-topics.sh --zookeeper node1:2181.. --describe  --topic topicName

##### 控制台向topic生产数据

> bin/kafka-console-producer.sh --broker-list node1:9092 --topic topicName

##### 控制台消费topic的数据

> bin/kafka-console-consumer.sh --zookeeper node1:2181... --topic topicName --from-beginning

##### 增加topic分区数

> bin/kafka-topis.sh --zookeeper node1:2181 --alter --topic topicName --partitions 10

#### SparkStreaming+kafka Receiver 模式

![SparkStreaming+kafka Receiver 模式](G:\学习笔记\spark\SparkStreaming\SparkStreaming+kafka\SparkStreaming+kafka Receiver 模式.jpg)

#### SparkStreaming+kafka Direct模式

![SparkStreaming + kafka Direct 模式](G:\学习笔记\spark\SparkStreaming\SparkStreaming+kafka\SparkStreaming + kafka Direct 模式.jpg)

#### SparkStreaming+Kafka：生产数据

1. new Properties()对象
   * 设置bootstrap.servers：告诉客户端kafka服务器在哪
     * node1::9092...Kafka的节点加端口号
   * 设置key.serializer和value.serializer：
     * key和value的序列化格式
   * 设置acks--主要用来做应答
     * 0：客户端将数据发送到kafka集群，不会等待集群的应答
     * 1：客户端将数据发送到kafka集群，会等待leader的应答，leader不会等待follower应答
     * -1|all：客户端发送数据到kafka集群，leader会等待follower应答
2. 创建生成数据的客户端
   * new KafkaProducer[String,String]（Properties对象）
3. 创建生产记录对象：new ProducerRecord[String,String]（topic,partition,key,value）
   * topic：指定记录要存储到那个主题
   * partition：指定记录要存储的分区
   * key：记录的key
     * 如果没有指定分区编号，就会安装key.hash % 分区的数量来决定存储的位置
     * 如果key没有传，那么会轮询
   * value：记录的value（这个值一般放我们想发送到kafka里面的内容）
4. 通过send方法使用客户端发送数据
   * producerClient.send(producerRecord)

```scala
def main(args: Array[String]): Unit = {
    val props = new Properties()
    //告诉客户端，kafka服务器在哪里
    props.setProperty("bootstrap.servers","node1:9092,node2:9092,node3:9092")
    //设置key 和 value 的序列化方式
    props.setProperty("key.serializer",classOf[StringSerializer].getName)
    props.setProperty("value.serializer",classOf[StringSerializer].getName)
    props.setProperty("acks","1")

    //生成数据的客户端
    val producerClient: KafkaProducer[String, String] = new KafkaProducer[String,String](props)
    while(true){
      Thread.sleep(100)
      val wordIndex: Int = new Random().nextInt(26)
      val assiCOde = (wordIndex+97).asInstanceOf[Char]
      val word = String.valueOf(assiCOde)

//      logger.info("{}",word)
      println(word)
      val record = new ProducerRecord[String,String]("wordcount",word,word)
      producerClient.send(record)
    }
  }
```

#### SparkStreaming+Kafka：消费数据

1. 通过Map设置kafkaParams
   * bootStrap.servers：指定kafka集群位置
   * key.deserializer：key的反序列化格式
   * value.deserializer：value的反序列化格式
   * group.id：消费者组
   * auto.offset.resrt：当没有初识的offset，或者当前的offset不存在，如何处理数据
     * earliest：自动重置偏移量为最小偏移量
     * latest：自动重置偏移量为最大偏移量【默认】
     * none：如果没有找到以前的offset，抛出异常
   * enable.auto.commit：是否自动提交offset至kafka集群
     * false：不自动向kafka中保存消费者offset，需要处理完数据之后，异步的手动提交
     * true【默认】
2. 创建获取数据的InputDStream
   * KafkaUtils.createDirectStream[String,String]（StreamingContext对象,PreferConsistent，subscribe[String,String]（topic，kafkaParams））

3. 手动提交offset
   * 通过InputStream对象.foreachRDD()提交

```scala
def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
    conf.setAppName("WordCountKafka").setMaster("local").set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

    val ssc = new StreamingContext(conf,Durations.seconds(3))

    val kafkaParams = Map[String,Object](
        "bootstrap.servers"->"node1:9092,node2:9092,node3:9092",
        "key.deserializer"->classOf[StringDeserializer],
        "value.deserializer"->classOf[StringDeserializer],
        "group.id"->"consumer_word_count",
        "auto.offset.reset"->"earliest",
        "enable.auto.commit"->(false:java.lang.Boolean)
    )

    val topics = Array("wordcount")
    //获取数据
    val stream: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream[String, String](
        ssc,
        PreferConsistent,
        Subscribe[String, String](topics, kafkaParams)
    )

    ssc.sparkContext.setCheckpointDir("./data/word_count_kafka")
    val result: DStream[(String, Int)] = stream.map(record => (record.value(), 1)).updateStateByKey((currentValues: Seq[Int], preValue: Option[Int]) => {
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

    stream.foreachRDD(rdd=>{

        val offsetRanges: Array[OffsetRange] = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
        stream.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)
    })

    ssc.start()
    ssc.awaitTermination()
    ssc.stop()
}
```

