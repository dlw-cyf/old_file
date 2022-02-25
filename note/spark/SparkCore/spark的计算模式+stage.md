### Spark 处理数据的模式

* **pipeline**：**管道处理模式**

* **stage**的**并行度**由谁决定
* 由stage中**finalRDD**的**partition的个数决定**
  
* 管道中的数据何时落地？

  1. **shuffle write** 时落地
  2. 对RDD进行**持久化**时落地

* 如何提交stage的并行度---指定partition的数据

  1. **reduceByKey**（Functioin,numpartition）
     * 设置下一个stage中**第一个RDD**的partition个数，影响stage的**finalRDD**的partition数，从而提高stage的并行度
  2. **join**（xxx,numpartition）
     * 默认与多的父RDD的partition数相同，可以手动指定

  3. **distinct**（numPartition）
     * 内部使用了reduceByKey，也可以指定partition数量