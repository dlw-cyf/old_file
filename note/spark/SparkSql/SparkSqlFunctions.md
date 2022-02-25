#### 1. DataFrame.withcolumn

* **新增**一列

#### 2. DataFrame.withcolumnRename

* **修改**某列的名字

#### 3. unix_timestamp(s:Column,p:String)

* 将指定格式的时间字符串转换为时间戳

#### 4. date_format(dateExpr:column,format:String)

* 将指定**Date类型**的列，转换为**指定的时间格式**

  ```scala
  val formatDateDF = monthDF.withColumn("format_date",date_format($"_date","yyyy年"))
  ```

#### 5. from_unixtime（ut:Column,f:String）

* unxi时间戳转换为字符串格式

#### 6. to_date(e:column,format:String)

* 将某一列**字符串格式**转换为**日期格式**

* column：指定某一类

* format：字符串格式

  * yyyy年MM月dd日

* 转换后的日期格式为**yyyy-MM-dd**

  ```scala
  val dateDF = cityDF.withColumn("_date",to_date($"_date","yyyy年MM月dd日"))
  ```

#### 7. month(e:column)

* 获取日期对象的**月份**

  ```scala
  val monthDF = dateDF.withColumn("month",month($"_date"))
  ```

#### 8. quarter(e:column)

* 获取日期对象的季度

#### 9. hour(e:Column)

* 获取日期对象的小时值

#### 10. minute(e:Column)

* 获取日期对象的分钟值

#### 11. second(e:Column)

* 获取日期对象的秒值

#### 12. dayofmonth(e:Column)

* 日期在一月中的天数

* 支持的数据类型

  * date：日期类型
  * timestamp：时间戳类型
  * String--只支持yyyy-MM-dd格式的字符串

  ```scala
  cityDF.withColumn("dayofmonth_",dayofmonth($"_date")).show()
  ```

#### 13. dayofyear(e:Column)

* 日期在一年中的天数
* 支持的数据类型
  * date：日期类型
  * timestamp：时间戳类型
  * String--只支持yyyy-MM-dd格式的字符串

#### 14. weekofyear(e:Column)

* 日期在一年中的周数
* 支持的数据类型
  * date：日期类型
  * timestamp：时间戳类型
  * String--只支持yyyy-MM-dd格式的字符串

#### 15. last_day(e:Column)

* 指定日期的最后一天