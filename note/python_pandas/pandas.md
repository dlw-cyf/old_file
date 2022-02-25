#### 1. 常用属性

* pd.dtypes
  * 获取每一列的类型
* pd.index
  * 获取每一行的索引
* pd.columns
  * 获取每一列的名字
* pd.values
  * 获取所有的值

#### 2. 切片和取值

##### 2.1 索引取值

* pd[column_value]：根据 列名 进行取值
* pd[startIndex：endIndex]：通过index进行切片（也可传入index的value）

##### 2.2 判断筛选

> pd[df.A > 8]   ：筛选A这列数据中大于8的各列数据

##### 2.3 纯标签筛选

> pd.loc[[index_value...]:,[columns_value...]]

##### 2.4 纯数字筛选

> pd.iloc[:,]
>
> 不连续的筛选：pd.iloc[[1,3,5],:]

##### 2.5 标签和数字筛选：ix

> pd.ix(:)