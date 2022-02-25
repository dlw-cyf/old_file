### 											 Class_And_Object

#### 共同点

1. 定义变量使用var，定义常量使用val---推荐使用**val**
   * 变量可变，常量不可变（类似于final）
   * 变量的类型会**自动推断**
2. 同一个scala文件中，class名称和object名称一样时
   * 这个类叫做这个对象的**伴生类**
   * 这个对象叫做这个类的**伴生对象**
   * 他们之间可以**互相访问私有变量**

#### object

1. scala中object相当于java中的单例

2. object中定义的**全是静态**的

3. object**不可以传参**--传参使用**apply**方法

```scala
object Lesson_ClassAndObj {
  
  def apply(i:Int):Unit = {
    println("Score is "+ i)
  }
  
  def main(args: Array[String]): Unit = {
    //object传入参数执行apply方法
    Lesson_ClassAndObj(1000)  
  }
}
```

#### class

1. 类中可以传参，传参一定要**指定类型**，有参数类型就有了默认构造。

2. 类中得属性默认有**set\get**方法   **私有属性只有get方法**

3. 类中重写构造是，构造的第一行必须**先调用默认的构造**

   ```scala
   class Person(xname:String,xage:Int){
     private val name = xname
     var age = xage
     var gender = 'M'
     
     def this(yname:String,yage:Int,ygender:Char){
       //先调用默认的构造
       this(yname,yage)
       this.gender = ygender
     }
   }
   ```

4. **new class**时，除了方法不执行[除了构造方法]，其他都执行

### 										Simple__Syntax

#### 条件语句（if)

* 语法和java相同

```scala
val age = 20
    if (age <= 20) {
        println("age <= 20")
    } else if (age > 20 && age <= 30) {
        println("20 < age <= 30")
    }else{
        println("age > 30")
    }
```

#### 循环语句（for——while）

* **while**语句

  ```scala
  var i = 1
      while (i < 5) {
      	i += 1
        println(i)
      }
  ```

* **for**语句

  1. 普通for

     ```scala
     /**
     *生成有序区间数组
     * until:左闭右开
     * to:左闭右闭
     */
     for(i <- 1 until 10){
         for(j <- 1 to i){
             //s:能在""中使用$引用变量
             print(s"$i * $j = "+i*j+"\t")
         }
         println()
     }
     ```

  2. 进阶for

     ```scala
     //多层for循环直接可以使用;隔开
     for(i <- 1 until 10;j <- 1 to i){
         print(s"$i * $j = "+i*j+"\t")
         if(i==j)
         	println()
     }
     ```

  3. 终极**for--if**

     ```scala
     //将if条件判断写入for循环结构中
     for(i <- 1 to 1000 if(i>500) if((i&1)==0)){
     	println(i)
     }
     ```

  4. 终极**for---if---yield**

     ```scala
     //将每次符合条件的i值
     val result = for(i <- 1 to 1000 if(i>500) if((i&1)==0))yield i
     println(result)
     ```

### 								Function_And_Method

#### 定义方法

1. **返回值**

   * 方法体最后返回值可以使用**return,**使用return时方法体的**返回值类型**一定要指定

   * 如果方法体重没有return 方法体的**返回值**可以**省略**,会自动推断

2. **参数**

   * 定义方法传入的参数一定要**指定类型**

3. **方法体**
   * 方法体可以一行搞定,那么方法体的**{}可以省略**
   * 省略方法名和方法体之间的 **=**，无论最后一行计算的结果是什么，**返回值都是unit**

#### 各种类型的方法

1. 参数有**默认值**的方法

   ```scala
   //如果没有传入参数使用参数默认值
   def fun(a:Int=10,b:Int=20)={
       a+b
   }
   println(fun(b=200))
   ```

2. **匿名函数**(**=>**:表示匿名函数)

   ```scala
   //将匿名函数赋值给一个变量
   def fun = (a:Int,b:Int) => {
   	a+b
   }
   println(fun(1,2))
   ```

3. **可变长参数**的方法----在参数类型后添加*****关键字

   ```scala
   def fun(s:Any*) = {
       s.foreach(println(_))
       s.foreach(println)
       for(elem <- s){
           println(elem)
       }
   }
   fun("Hello ","a","b","c",1,2)
   ```

4. **递归**方法

   * 递归方法要显示的**声明**函数的**返回值**类型

   ```scala
   def fun(num:Int):Int = {
       if(num==1) 1
       else{
           num*fun(num-1)
       }
   }
   println(fun(5))
   ```

5. **嵌套**方法

   ```scala
   def fun(num:Int) = {
       def fun1(a:Int):Int = {
           if(a==1)1 else a*fun1(a-1)
       }
       fun1(num)
   }
   
   println(fun(5))
   ```

6. **偏应用**函数

   *  **使用场景**：某些情况，方法中参数非常多，调用这个方法非常**频繁**，每次调用只有**固定**的某个参数变化，其他**参数不变**

   ```scala
   def showLog(date:Date,log:String) = {
       println(s"date is $date , log is $log")
   }
   val date = new Date()
   showLog(date,log="a")
   showLog(date,log="b")
   showLog(date,log="c")
   /**
   *使用偏应用函数
   * 实际上就是将一个方法转换为一个函数对象
   *   将不变的参数
   */
   def fun = showLog(date,_:String)
   
   fun("aaa")
   fun("bbb")
   fun("ccc")
   ```

7. **高阶**函数

   * 方法的**参数**是函数--**spark**中常用

     ```scala
     def fun(a:Int,b:Int) = {
     	a+b
     }
     
     def fun1(f:(Int,Int)=>Int,s:String):String = {
     	val i = f(100,200)
     	i+"#"+s
     }
     //一般和匿名函数一起使用
     val result = fun1( (a:Int,b:Int)=>{a*b} ,"scala")
     println(result)
     }
     ```

   * 方法的**返回值**是函数

     ```scala
     def fun(s: String) = {
     
         def fun1(s1: String, s2: String): String = {
             s1 + "~" + s2 + "#" + s
         }
         fun1 _
     }
     println(fun("a")("bb","cc"))
     ```

   * 方法的**参数**和**返回**都是函数

     ```scala
     def fun(f:(Int,Int)=>Int):(String,String)=>String = {
         val i = f(1,2)
     
         def fun1(s1:String,s2:String):String = {
             s1+"@"+s2+"*"+i
         }
         fun1
     }
     
     println(fun((a,b)=>{a+b})("hello","world"))
     ```

   * 柯里化函数

     ```scala
     def fun(a:Int,b:Int)(c:Int,d:Int) = {
         a+b+c+d
     }
     //是方法的返回值函数简化版
     println(fun(1,2)(3,4))
     ```

     