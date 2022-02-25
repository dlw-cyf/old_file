### trait

1. trait可以包含**抽象方法**和**具体方法**

   * class中必须**实现trait**中的**抽象方法**

   ```scala
   trait IsEqu{
     def isEqu(o:Any):Boolean
     def isNoEqu(o:Any):Boolean = !isEqu(o)
   }
   
   class Point(xx:Int, xy:Int) extends IsEqu {
     val x = xx
     val y = xy
   
     def isEqu(o: Any): Boolean = {
       //isInstanceOf[Point]  	判断o是否为Point类型
       //asInstanceOf[Point]	获取将o转换为Point类型对象
       o.isInstanceOf[Point] && o.asInstanceOf[Point].x == this.x
     }
   }
   
   object Lesson_Trait02 {
     def main(args: Array[String]): Unit = {
       val p1 = new Point(1,2)
       val p2 = new Point(1,3)
       
       println(p1.isNoEqu(p2))
     }
   }
   ```

   

2. trait可以包含**抽象字段**和**具体字段**

   ```scala
   trait Field{
     //抽象字段
     var str:String
     //具体字段
     var str_ = "It's me"
   }
   
   class Test() extends Field {
     var str: String = "重写抽象字段"
   }
   ```

   

3. 多个trait**共同实现**使用**with**关键字

   ```scala
   trait Read{
     def read(name:String) ={
       println(s"$name is reading ...")
     }
   }
   
   trait Listen{
     def listen(name:String) = {
       println(s"$name is Listen ...")
     }
   }
   //使用extends关键字和with关键字实现多个trait
   class Human() extends Read with Listen{
     
   }
   
   object Lesson_Trait01 {
     def main(args: Array[String]): Unit = {
       val h = new Human()
       h.read("张三")
       h.listen("李四")
     }
   }
   ```

   

4. trait构造器是**无参构造**,不能定义带参的构造:**不能传参**

### Match 模式匹配

* 模式匹配可以**匹配值**和**类型**
* 匹配过程中会有**数值的转换**
  * 例如：1.0 匹配 1
* **从上玩下**匹配，匹配上终止匹配
* **match{case...}**表示**一行语句**，可以**省略**模式匹配外部定义方法的{}
* case **_ 下划线**表示什么都匹配不上,放在**最后一行**----类似于java **switch语句**的default

```scala
object Lesson_Match {
  
  def main(args: Array[String]): Unit = {
    val tp = (1,1.0,"abc",'a',true)
    
    val iter = tp.productIterator
    iter.foreach(MatchTest)
  }
  
  def MatchTest(o:Any) = {
    o match {
      //value值得匹配
      case 1 => println("value is 1")
      //类型匹配
      case i:Int => println(s"type is Int ,value = $i")
      case d:Double => println(s"type is Double , value = $d")
      case s:String => println(s"type is String ,value = $s")
      case 'a' => println("value is a")
      //匹配所有   类似于java的default
      case _ =>println("no match ...")
    }
  }
}
```

### partialFunction  偏函数

* 偏函数只能匹配一个值，匹配上返回一个值

* 偏函数不能通过（）进行传参

* 需要指定类型 PartialFunction[A,B]

  * A : 匹配的类型
  * B : 返回值的类型

  ```scala
  object Lesson_PartialFun {
    def MyTest:PartialFunction[String,Int] = {
      case "abc" => 3
      case "a" => 1
      case _ => 200
    }
    
    def main(args: Array[String]): Unit = {
      val result = MyTest("abc")
      println(result)
    }
  }
  ```

  

### case  样例类

1. 样例类和普通类的区别

   * 默认实现**序列化接口**
   * 默认自动重写**toString equest hashCode**方法
   * 默认实现**set/get**方法 默认变量使用**val修饰**
     * **val修饰的变量**没有**set方法**
   * 不需要**new**可以直接**生成对象**

2. 创建一个样例类

   ```scala
   //这种写法,并且顺带给该类定义了一个构造函数,此时该类没有空参数
   case class Demo2(var name:String,age:Int){
   
   }
   
   object Demo4 {
     def main(args: Array[String]): Unit = { 
         val p1 = new Demo2("zhangsan", 19)
         val p2 = Demo2("zhangsan", 19)
         //返回值为true  重写了equest方法
         println(p1.equals(p2))
         println(p1)
     }
   }
   ```

   