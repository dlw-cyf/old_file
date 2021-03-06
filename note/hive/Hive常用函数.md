####  1. 内置运算符

##### 1.1 关系运算符

| 运算符        | 类型             | 说明                                                         |
| :------------ | :--------------- | ------------------------------------------------------------ |
| A = B\|A == B | 所有原始数据类型 | 如果A与B相等,返回TRUE,否则返回FALSE                          |
| A <> B\|A!=B  | 所有原始数据类型 | 如果A不等于B返回TRUE,否则返回FALSE。如果A或B值为”NULL”，结果返回”NULL”。 |
| A < B         | 原始数据类型     | 如果A小于B返回TRUE,否则返回FALSE。如果A或B值为”NULL”，结果返回”NULL”。 |
| A <= B        | 原始数据类型     | 如果A小于等于B返回TRUE,否则返回FALSE。如果A或B值为”NULL”，结果返回”NULL”。 |
| A > B         | 原始数据类型     | 如果A大于B返回TRUE,否则返回FALSE。如果A或B值为”NULL”，结果返回”NULL”。 |
| A >= B        | 原始数据类型     | 如果A大于等于B返回TRUE,否则返回FALSE。如果A或B值为”NULL”，结果返回”NULL”。 |
| A IS NULL     | 所有类型         | 如果A值为”NULL”，返回TRUE,否则返回FALSE。                    |
| A IS NOT NULL | 所有类型         | 如果A值不为”NULL”，返回TRUE,否则返回FALSE。                  |
| A LIKE B      | 字符串           | 如果A或B值为”NULL”，结果返回”NULL”。通过sql匹配，'_' 代表一个字符，'%' 代表多个字符 |
| A RLIKE B     | 字符串           | 如果A或B值为”NULL”，结果返回”NULL”。支持java中的正则匹配     |

##### 1.2 算数运算符

| 运算符 | 类型         | 说明                                                         |
| ------ | ------------ | ------------------------------------------------------------ |
| A + B  | 所有数字类型 | A和B相加。结果的与操作数值有共同类型。例如每一个整数是一个浮点数，浮点数包含整数。所以，一个浮点数和一个整数相加结果也是一个浮点数。 |
| A - B  | 所有数字类型 | A和B相减。结果的与操作数值有共同类型。                       |
| A * B  | 所有数字类型 | A和B相乘，结果的与操作数值有共同类型。需要说明的是，如果乘法造成溢出，将选择更高的类型。 |
| A / B  | 所有数字类型 | A和B相除，结果是一个double（双精度）类型的结果。             |
| A % B  | 所有数字类型 | A除以B余数与操作数值有共同类型。                             |
| A & B  | 所有数字类型 | 运算符查看两个参数的二进制表示法的值，并执行按位”与”操作。两个表达式的一位均为1时，则结果的该位为 1。否则，结果的该位为 0。 |
| A \| B | 所有数字类型 | 运算符查看两个参数的二进制表示法的值，并执行按位”或”操作。只要任一表达式的一位为 1，则结果的该位为 1。否则，结果的该位为 0 |
| A ^ B  | 所有数字类型 | 运算符查看两个参数的二进制表示法的值，并执行按位”异或”操作。当且仅当只有一个表达式的某位上为 1 时，结果的该位才为 1。否则结果的该位为 0。 |
| ~ A    | 所有数字类型 | 对一个表达式执行按位”非”（取反）。                           |

##### 1.3 逻辑运算符

| 运算符  | 类型   | 说明                                                         |
| ------- | ------ | ------------------------------------------------------------ |
| A AND B | 布尔值 | A和B同时正确时,返回TRUE,否则FALSE。如果A或B值为NULL，返回NULL。 |
| A && B  | 布尔值 | 与”A AND B”相同                                              |
| A OR B  | 布尔值 | A或B正确,或两者同时正确返返回TRUE,否则FALSE。如果A和B值同时为NULL，返回NULL。 |
| A \| B  | 布尔值 | 与”A OR B”相同                                               |
| NOT A   | 布尔值 | 如果A为NULL或错误的时候返回TURE，否则返回FALSE。             |
| ! A     | 布尔值 | 与”NOT A”相同                                                |

##### 1.4 复杂类型函数

| 函数   | 类型                            | 说明                                                        |
| ------ | ------------------------------- | ----------------------------------------------------------- |
| map    | (key1, value1, key2, value2, …) | 通过指定的键/值对，创建一个map。                            |
| struct | (val1, val2, val3, …)           | 通过指定的字段值，创建一个结构。结构字段名称将COL1，COL2，… |
| array  | (val1, val2, …)                 | 通过指定的元素，创建一个数组。                              |

##### 1.5 对复杂类型函数操作

| 函数   | 类型                   | 说明                                                         |
| ------ | ---------------------- | ------------------------------------------------------------ |
| A[n]   | A是一个数组，n为int型  | 返回数组A的第n个元素，第一个元素的索引为0。如果A数组为['foo','bar']，则A[0]返回’foo’和A[1]返回”bar”。 |
| M[key] | M是Map<K,  V>，关键K型 | 返回关键值对应的值，例如mapM为 \{‘f’ ->  ‘foo’, ‘b’ -> ‘bar’, ‘all’ -> ‘foobar’\}，则M['all']  返回’foobar’。 |
| S.x    | S为struct              | 返回结构x字符串在结构S中的存储位置。如 foobar \{int foo, int bar\} foobar.foo的领域中存储的整数。 |

#### 2. 内置函数

##### 2.1 数学函数

| 返回类型   | 函数                                              | 说明                                                         |
| ---------- | ------------------------------------------------- | ------------------------------------------------------------ |
| BIGINT     | round(double a)                                   | 四舍五入                                                     |
| DOUBLE     | round(double a, int d)                            | 小数部分d位之后数字四舍五入                                  |
| BIGINT     | floor(double a)                                   | 对给定数据进行向下舍入最接近的整数。                         |
| BIGINT     | ceil(double a), ceiling(double a)                 | 将参数向上舍入为最接近的整数。                               |
| DOUBLE     | rand(), rand(int seed)                            | 返回大于或等于0且小于1的平均分布随机数（依重新计算而变）     |
| DOUBLE     | exp(double a)                                     | 返回e的n次方                                                 |
| DOUBLE     | ln(double a)                                      | 返回给定数值的自然对数                                       |
| DOUBLE     | log10(double a)                                   | 返回给定数值的以10为底自然对数                               |
| DOUBLE     | log2(double a)                                    | 返回给定数值的以2为底自然对数                                |
| DOUBLE     | log(double base, double a)                        | 返回给定底数及指数返回自然对数                               |
| DOUBLE     | pow(double a, double p) power(double a, double p) | 返回某数的乘幂                                               |
| DOUBLE     | sqrt(double a)                                    | 返回数值的平方根                                             |
| STRING     | bin(BIGINT a)                                     | 返回二进制格式                                               |
| STRING     | hex(BIGINT a) hex(string a)                       | 将整数或字符转换为十六进制格式                               |
| STRING     | unhex(string a)                                   | 十六进制字符转换由数字表示的字符。                           |
| STRING     | conv(BIGINT num, int from_base, int to_base)      | 将 指定数值，由原来的度量体系转换为指定的试题体系。例如CONV(‘a’,16,2),返回 |
| DOUBLE     | abs(double a)                                     | 取绝对值                                                     |
| INT,DOUBLE | pmod(int a, int b) pmod(double a, double b)       | 返回a除b的余数的绝对值                                       |
| DOBLE      | sin(double a)                                     | 返回给定角度的正弦值                                         |
| DOBLE      | asin(double a)                                    | 返回x的反正弦，即是X。如果X是在-1到1的正弦值，返回NULL。     |
| DOBLE      | cos(double a)                                     | 返回余弦                                                     |
| DOBLE      | acos(double a)                                    | 返回X的反余弦，即余弦是X，，如果-1<= A <= 1，否则返回null.   |
| INT,DOBLE  | positive(int a) positive(double a)                | 返回A的值，例如positive(2)，返回2。                          |
| INT,DOBLE  | negative(int a) negative(double a)                | 返回A的相反数，例如negative(2),返回-2。                      |

##### 2.2 收集函数

| 返回类型 | 函数            | 说明                      |
| -------- | --------------- | ------------------------- |
| INT      | size(Map<K.V>)  | 返回的map类型的元素的数量 |
| INT      | size(Array<T’>) | 返回数组类型的元素数量    |

##### 2.3 类型转换函数

| 返回类型    | 函数                | 说明                                                         |
| ----------- | ------------------- | ------------------------------------------------------------ |
| 指定 “type” | cast(expr as  type) | 类型转换。例如将字符”1″转换为整数:cast(’1′ as bigint)，如果转换失败返回NULL。 |

##### 2.4 日期函数

| 返回类型 | 函数                                                | 说明                                                         |
| -------- | --------------------------------------------------- | ------------------------------------------------------------ |
| string   | **from_unixtime**(bigint unixtime[, string format]) | UNIX_TIMESTAMP参数表示返回一个值’YYYY- MM – DD HH：MM：SS’或YYYYMMDDHHMMSS.uuuuuu格式，这取决于是否是在一个字符串或数字语境中使用的功能。该值表示在当前的时区。 |
| bigint   | unix_timestamp()                                    | 如果不带参数的调用，返回一个Unix时间戳（从’1970-  01 – 0100:00:00′到现在的UTC秒数）为无符号整数。 |
| bigint   | unix_timestamp(string date)                         | 指定日期参数调用UNIX_TIMESTAMP（），它返回参数值’1970-  01 – 0100:00:00′到指定日期的秒数。 |
| bigint   | **unix_timestamp**(string date, string pattern)     | 指定时间输入格式，返回到1970年秒数：unix_timestamp(’2009-03-20′, ‘yyyy-MM-dd’) = 1237532400 |
| string   | to_date(string timestamp)                           | 返回时间中的年月日： to_date(“1970-01-01 00:00:00″) =  “1970-01-01″ |
| string   | to_dates(string date)                               | 给定一个日期date，返回一个天数（0年以来的天数）              |
| int      | year(string date)                                   | 返回指定时间的年份，范围在1000到9999，或为”零”日期的0。      |
| int      | month(string date)                                  | 返回指定时间的月份，范围为1至12月，或0一个月的一部分，如’0000-00-00′或’2008-00-00′的日期。 |
| int      | day(string date) dayofmonth(date)                   | 返回指定时间的日期                                           |
| int      | hour(string date)                                   | 返回指定时间的小时，范围为0到23。                            |
| int      | minute(string date)                                 | 返回指定时间的分钟，范围为0到59。                            |
| int      | second(string date)                                 | 返回指定时间的秒，范围为0到59。                              |
| int      | weekofyear(string date)                             | 返回指定日期所在一年中的星期号，范围为0到53。                |
| int      | datediff(string enddate, string startdate)          | 两个时间参数的日期之差。                                     |
| string   | date_add(string startdate, int days)                | 给定时间，在此基础上加上指定的时间段。                       |
| string   | date_sub(string startdate, int days)                | 给定时间，在此基础上减去指定的时间段。                       |

##### 2.5 条件函数

| 返回类型 | 函数                                                       | 说明                                                         |
| -------- | ---------------------------------------------------------- | ------------------------------------------------------------ |
| T        | if(boolean testCondition, T valueTrue, T valueFalseOrNull) | 判断是否满足条件，如果满足返回一个值，如果不满足则返回另一个值。 |
| T        | COALESCE(T v1, T v2, …)                                    | 返回一组数据中，第一个不为NULL的值，如果均为NULL,返回NULL。  |
| T        | CASE a WHEN b THEN c [WHEN d THEN e]* [ELSE f] END         | 当a=b时,返回c；当a=d时，返回e，否则返回f。                   |
| T        | CASE WHEN a THEN b [WHEN c THEN d]* [ELSE e] END           | 当值为a时返回b,当值为c时返回d。否则返回e。                   |

##### 2.6 字符函数

| 返回类型 | 函数                                                         | 说明                                                         |
| -------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| int      | length(string A)                                             | 返回字符串的长度                                             |
| string   | reverse(string A)                                            | 返回倒序字符串                                               |
| string   | concat(string A, string B…)                                  | 连接多个字符串，合并为一个字符串，可以接受任意数量的输入字符串 |
| string   | concat_ws(string SEP, string A, string B…)                   | 链接多个字符串，字符串之间以指定的分隔符分开。               |
| string   | substr(string A, int start) substring(string A, int  start)  | 从文本字符串中指定的起始位置后的字符。                       |
| string   | substr(string A, int start, int len) substring(string  A, int start, int len) | 从文本字符串中指定的位置指定长度的字符。                     |
| string   | upper(string A) ucase(string A)                              | 将文本字符串转换成字母全部大写形式                           |
| string   | lower(string A) lcase(string A)                              | 将文本字符串转换成字母全部小写形式                           |
| string   | trim(string A)                                               | 删除字符串两端的空格，字符之间的空格保留                     |
| string   | ltrim(string A)                                              | 删除字符串左边的空格，其他的空格保留                         |
| string   | rtrim(string A)                                              | 删除字符串右边的空格，其他的空格保留                         |
| string   | regexp_replace(string A, string B, string C)                 | 字符串A中的B字符被C字符替代                                  |
| string   | regexp_extract(string subject, string pattern, int index)    | 通过下标返回正则表达式指定的部分。                           |
| string   | parse_url(string urlString, string partToExtract [, string keyToExtract]) | 返回URL指定的部分。                                          |
| string   | get_json_object(string json_string, string path)             |                                                              |
| string   | space(int n)                                                 | 返回指定数量的空格                                           |
| string   | repeat(string str, int n)                                    | 重复N次字符串                                                |
| int      | ascii(string str)                                            | 返回字符串中首字符的数字值                                   |
| array    | split(string str, string pat)                                | 将字符串转换为数组。                                         |
| int      | find_in_set(string str, string strList)                      | 返回字符串str第一次在strlist出现的位置。如果任一参数为NULL,返回NULL；如果第一个参数包含逗号，返回0。 |

#### 1. 内置的聚合函数（UDAF）

| 返回类型 | 函数                                                         | 说明                           |
| -------- | ------------------------------------------------------------ | ------------------------------ |
| bigint   | count(*) , count(expr), count(DISTINCT expr[, expr_.,  expr_.]) | 返回记录条数。                 |
| double   | sum(col), sum(DISTINCT col)                                  | 求和                           |
| double   | avg(col), avg(DISTINCT col)                                  | 求平均值                       |
| double   | min(col)                                                     | 返回指定列中最小值             |
| double   | max(col)                                                     | 返回指定列中最大值             |
| double   | var_pop(col)                                                 | 返回指定列的方差               |
| double   | var_samp(col)                                                | 返回指定列的样本方差           |
| double   | stddev_pop(col)                                              | 返回指定列的偏差               |
| double   | stddev_samp(col)                                             | 返回指定列的样本偏差           |
| double   | covar_pop(col1, col2)                                        | 返回组内两个数字列的总体协方差 |
| double   | covar_samp(col1, col2)                                       | 返回组内两个数字列的样本协方差 |
| double   | corr(col1, col2)                                             | 返回两列数值的相关系数         |
| array    | collect_set(col)                                             | 返回无重复记录                 |

