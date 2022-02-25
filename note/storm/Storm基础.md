#### Storm 编程模型

![storm编程模型](G:\学习笔记\storm\images\storm编程模型.png)

* **Topology**：Storm中运行的一个实时应用程序的名称。
* **spout**：在一个Topology中获取数据流的组件
  * 通常情况下，spout会从外部数据源中读取数据，然后转换为topology内部的数据源
* **bolt**：接收数据，执行处理的组件，用户可以在其中执行自己想要的操作。
* **Tuple**：一次消息传递的基本单元，理解为一组消息就是一个Tuple
  * 内部封装了一个list
* **stream**：表示数据的流向

#### Storm grouping分组

* **Shuffle group（随机分组）**：这种方式会随机分发 tuple 给bolt 的各个 task，每个bolt 实例接收到的相同数量的 tuple 。

* **Fields grouping（字段分组）**：根据指定字段的值进行分组。比如说，一个数据流根据“ word”字段进行分组，所有具有相同“ word ”字段值的 tuple 会路由到同一个 bolt 的 task 中。

* **All grouping（全复制分组）**：将所有的 tuple 复制后分发给所有 bolt task。每个订阅数据流的 task 都会接收到 tuple 的拷贝。
* **Globle group（全局分组）**：这种分组方式将**所有的 tuples 路由到唯一一个 task** 上，Storm 按照**最小的 task ID** 来选取接收数据的 task 。
  * 当使用全局分组方式时，设置 bolt 的 task 并发度是没有意义的（spout并发有意义），因为所有 tuple 都转发到同一个 task 上了
  * 所有的 tuple 都转发到一个 JVM 实例上，可能会引起  Storm 集群中某个 JVM 或者服务器出现性能瓶颈或崩溃。
* **None grouping（不分组）**：在功能上和随机分组相同，是为将来预留的。
* **Direct grouping（指向型分组）**：数据源会调用 emitDirect() 方法来判断一个 tuple 应该由哪个 Storm 组件来接收。只能在声明了是指向型的数据流上使用。
* **Local or shuffle group（本地或随机分组）**：和随机分组类似，但是，会将 tuple 分发给同一个 worker 内的bolt task （如果 worker 内有接收数据的 bolt task ）。其他情况下，采用随机分组的方式。取决于topology 的并发度，本地或随机分组可以减少网络传输，从而提高 topology 性能。

#### Storm的组件和架构

![storm架构图](G:\学习笔记\storm\images\storm架构图.png)

* **nimbus**：任务分配，对任务监控
* **supervisor**：当前物理机器上的管理者，接受nimbus分配的任务，启动自己的Worker。Worker数量是根据配置文件中指定的端口号来的。
* **Worker**：具体执行任务的组件，任务类型有两种，一个worker中可能存在多个spout任务和多个blot任务
  * spout任务：获取和分发数据
  * bolt任务：执行代码逻辑
* **Task**：worker中每一个spout/bolt的线程称为一个task，在**storm0.8之后**，Task不再与物理线程对应，不同spout/bolt 的task可能会共享一个物理线程，**该线程称为 executor**。

* **Zookeeper**：保存任务分配的信息3，心跳信息，元数据信息。

#### ack-fail机制

1. 需要ack-fail机制时，请为每个tuple生成一个messageID，这个messageID是用来标识你关心的tuple。当这个tuple被完全处理时，会调用spout的ack方法，否则调用fail。至于你的消息是否重发，完全由你自己处理。
2. 在spout又并发度的情况下，Storm会根据tuple最开始所属的spout taskID 通知相应的spoutTask。
3. 在流式计算中topology的bolt组件是可以配置多个的，在每个环节中，都需要bolt组件显示告诉strom框架，自己对于当前接受的这个tuple已经处理完成。