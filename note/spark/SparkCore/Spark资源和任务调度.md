## 1、资源调度

### 1.1、基于Standalone-client提交任务

* **提交任务命令**--基于**客户端**提交任务

  *  **client模式**(--**deploye-mode**的**默认值**)---bin目录执行

  > ./spark-submit **--master** spark://dlw-01:7077 --class +(完整包名加类名) +(jar包所在位置) +(num:传入的参数main方法中args)

  > ./spark-submit **--master** spark://dlw-01:7077 **--deploy-mode client** --class +(完整包名加类名) +(jar包所在位置) +(num:传入的参数main方法中args)

  * **提交任务原理**
    * 通过**standalone-client**模式提交任务，每个**spark application**都有自己**独立的Driver**，当客户端提交**多个**application，多个Driver需要负责**发送task**，**监控task执行**，**回收结果**，很容易造成客户端**网卡流量激增**问题，这种模式适用于**程序测试**，不适于生成环境，在客户端可以看到task的**执行**和**结果**。

### 1.2、基于standalone-cluster 模式提交任务

* **提交任务命令**--基于**客户端提交任务**

  * **cluster模式**---bin目录执行

  > ./spark-submit **--master** spark://dlw-01:7077 **--deploy-mode cluster** --class +(完整包名加类名) +(jar包所在位置) +(num:传入的参数main方法中args)

* **提交任务原理**

  spark 基于**standalone-cluster** 模式提交任务，当在客户端提交多个**Application**时，Driver是**随机**在某些**Worker节点启动**，客户端就没有网卡流量激增问题，将这种问题**分散到集群中**，在客户端**看不到task执行和结果**，要去**WebUI中查看**，这种模式适用于**生产环境**。

### 1.3、基于Yarn-client 模式提交任务

* **提交任务命令**--基于客户端提交任务

  * **client模式**

  > ./spark-submit --**master** yarn --class + (完整包名加类名) + (jar包所在位置) + (num:传入的参数main方法中args)

  > ./spark-submit **--master** **yarn** **--deploy-mode** **client** --class + (完整包名加类名) + (jar包所在位置) + (num:传入的参数main方法中args)

  > ./spark-submit --**master** **yarn-client** --class + (完整包名加类名) + (jar包所在位置) + (num:传入的参数main方法中args)

* 提交任务原理

  spark 基于**Yarn-client模式**提交任务，当Driver提交多个application时，会有**网卡流量激增**问题，这种模式不适于生成环境，在**客户端**可以看到**task的执行和结果**。

### 1.4、基于Yarn-cluster 模式提交任务

* **提交任务命令**--基于客户端提交任务

  * **cluster模式**

  > ./spark-submit --master yarn --deploy-mode **cluster** --class + (完整包名加类名) + (jar包所在位置) + (num:传入的参数main方法中args)

  > ./spark-submit --master **yarn-cluster** --class + (完整包名加类名) + (jar包所在位置) + (num:传入的参数main方法中args)

* **提交任务原理**

  Spark 基于**Yarn-cluster模式**提交任务，当有多个application提交时，每个application的**Driver（AM）**是分散到**集群中的NM中启动**，没有客户端网卡流量激增问题，将这种问题分散到集群中，在客户端看不到**task的执行和结果**，要去**WebUI**中查看，这个模式适用于生产环境

## 2. 任务调度

### 2.1、Spark任务调度流程

1. **DAGScheduler**拿到**DAG** ，按照RDD之间的**宽窄依赖关系**，**切割job划分stage**，将Stage以**taskSet**的形式提交给TaskScheduler 
2. **TaskScheduler** 遍历**TaskSet**，拿到一个个task，发送到**Woker**中的**Executor中**执行，**监控**task，**回收结果**
3. Executor将task放入**ThreadPool**中执行任务

## 3、粗粒度和细粒度资源申请原理

### 3.1、粗粒度资源申请：Spark

​	Application启动之前，首先将**所有的资源申请完毕**，如果**申请不到**，则一直处于**等待状态**，直到资源申请到为止，才会执行任务，这样**task执行**的时候不**需要自己申请资源**，加快了task的**执行效率**，task快了，job就快了，application执行也就快了，只有**最后一个task执行完毕**之后，才会**释放所有资源**。

* **优点**：执行**速度快**
* **缺点**：容易造成**资源不能充分利用**

### 3.2、细粒度资源申请：MapReduce

​	Application执行之前，**不会将所有task申请完毕**，task执行时，**自己申请资源，释放资源**，这样task**执行相对慢**，但是**集群资源可以充分利用**

* **优点**：集群**资源充分利用**
* **缺点**：application**执行相对慢**



