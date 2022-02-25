##### parameterType属性

* 控制参数类型

* **#{}**获取参数

  * 通过占位符方式赋值
  * 通过索引获取参数内容
    * #{0}表示第一个参数
  * 如果只有一个参数(基本数据类型或者String)，#{}内可以为任意

  ```xml
  <mapper namespace="a.b">
  	<select id="selById" resultType="com.dlw.pojo.Flower" parameterType="int">
  		select * from flower where id = #{0}
  	</select>
  </mapper>
  ```

* ${}获取参数

  * 不能通过索引获取内容
    * ${3} 就是表示 3
  * 获取到参数内容，通过字符串拼接成sql

* sqlsession的selectList()和selectOne()的第二个参数和selectMap()的第三个参数都表示方法的参数

* 示例

  ```java
  Flower flower = session.selectOne("a.b.selById",2);
  		System.out.println(flower);
  ```

  