### Array类型

1. **创建**Array

   ```scala
   //创建一个有默认值得array  使用new关键字
   val arr1 = new Array[Int](3)
   arr1(0) = 100
   arr1(1) = 200
   arr1(2) = 300
   //创建一个带value的array
   val arr = Array[String]("a","b","c","d")
   //创建一个二维Array (num)-->表示有row
   val array = new Array[Array[Int]](3)
   ```

2. **遍历**Array

   ```scala
   //使用匿名函数
   array.foreach(arr=>{arr.foreach(println})
   //使用进阶for
   for(arr <- array ; elem <- arr){
    println(elem)
   }          
   ```

3. **常用方法**

   ```scala
   //数组的合并
   val arrays = Array.concat(arr,arr1)
   array.foreach(println)
   //数组的填充创建
   Array.fill(5)("hello").foreach(println)
   ```

4. **可变**的Array（默认的Array是不可变类型）

   ```scala
   import scala.collection.mutable.ArrayBuffer
   	//导包,使用ArrayBuffer创建一个Array对象为可变类型
   	val arr = ArrayBuffer[Int](1,2,3)
   	arr.+=(4)
       arr.+=:(100)
       arr.append(7,8,9)
   ```

### List类型

1. **创建**List

   ```scala
   val list = List[String]("hello","scala","spark")
   /**
   *创建一个可变的List
   * 1.导包 2.通过ListBuffer创建
   */
   import scala.collection.mutable.ListBuffer
   val list = ListBuffer[Int](1,2,3,4)
   list.append(5,6,7)
   list.+=(8)
   list.foreach(println)
   ```

2. **遍历**List

   ```scala
   //匿名函数遍历
   list.foreach(println)
   ```

3. **常用方法**

   ```scala
   //1. filter函数   过滤掉函数中不符合条件的值
   val result = list.filter(s=>{
       "hello scala".equals(s)
   })
   result.foreach(println)
   
   //2. count函数	统计符合函数中条件的值
   val result = list.count(s=>{
       s.length < 4 
   })
   println(result)
   
   //2. map函数和flatMap函数 	类似于python的map函数
   // map是一对多 flatMap是一对一(相当于在map中的值进行再一次加工)
   val result = list.flatMap(s=>{s.split(" ")})
   result.foreach(println)
   val result = list.map(s=>{
       s.split(" ")
   })
   ```

### Set类型

1. 创建Set

   ```scala
   import scala.collection.mutable
   import scala.collection.immutable
   
   //可变set 通过包指定Set为可变
   val set_mu = mutable.Set[Int](1)
   set_.+=(100)
   set_.foreach(println)
   //不可变Set 默认为不可变Set,如果需要和可变Set同时使用需要指定为不可变
   val set = immutable.Set[Int](1,2,3,4)
   ```

2. **常用方法**

   ```scala
   //intersect方法	求两个Set的并集---方法一
   val result = set.intersect(set1)
   println(result)
   //diff方法		求两个Set的差集---方法一
   val result = set.diff(set1)
   println(result)
   
   // & 表示并集		求两个Set的并集---方法二
   val result = set & set1
   // &~ 表示差集		求两个Set的差集---方法二
   ```

### Map类型

1. **创建**Map

   ```scala
   import scala.collection.mutable
   import scala.collection.immutable
   
   //创建一个可变类型的Map
   val map_mu = mutable.Map[String,Int]("a"->10,"b"->200,("c",300),("c",400))
   //创键不可变类型的Map  默认是不可变
   val map = immutable.Map[String,Int]("b"->2,("c",3))
   ```

2. **遍历**Map

   ```scala
   for(elem <- map){
       //输出的结果为一个二元元组类型
   	println(elem)
   }
   map.foreach(println)
   //通过keys取所有的value
   val keys = map.keys
   keys.foreach(key =>
   	val value = map.get(key).get
       println(s"key = $key , value = $value")
   )
   //获取所有的value
   val values = map.values
   ```

3. **常用方法**

   ```scala
   //get()值时防止出现异常		如果key不存在,则放回getOrElse中给定的值
   map.get("key").getOrElse("no value")
   
   /** 
   *map的合并
   *	1. ++ 将后面的map丢进前面的map--key相同后面的value覆盖前面的
   *	2. ++: 将前面的map丢进后面的map--key相同前面的value覆盖后面的
   */
   val result01 = map01.++(map02)
   val result = map01.++:(map02)
   
   //filter方法	过滤值
    val result_map = map.filter(tp =>{
        //tp为元组类型,可以通过_num进行取值
        val key = tp._1
        val value = tp._2
        value == 200
    })
   result_map.foreach(println)
   ```

### Tuple类型

1. **创建**tuple

   ```scala
   val tuple1 = new Tuple1("hello")
   val tuple2 = new Tuple2("a",100)
   val tuple3 = new Tuple3(1,true,'C')
   val tuple4 = Tuple4(1,3.4,"abc",false)
   val tuple6 = (1,2,3,4,"abc")
   //元组可以通过_num进行取值
   println(tuple3._2)
   
   //二元元组可以通过swap交换两个值得位置
   println(tuple2.swap)
   ```

2. **遍历**tuple

   ```scala
   // 将tuple转换为一个迭代对象
   val iterator = tuple6.productIterator
   //通过匿名函数遍历
   iterator.foreach(println)
   
   //循环遍历取值
   while(iterator.hasNext){
       println(iterator.next())
   }
   ```

   

