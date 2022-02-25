#### 1. Eclipse的使用

##### 1.1 创建项目

* 选择target runtime,否则出现新建jsp报错
* 如果忘记选择，右击项目--> build path --> configure path --> 选项卡 library --> add library --> server runtime

![创建项目](G:\学习笔记\javaWeb\mybatis\image\创建项目.png)

##### 1.2 Eclipse和tomcat

* Eclipse默认会自己下载所需tomcat最简结构

#### 2. MVC的开发模式

* M:Model 
  * 实体类和业务和dao
* V:view
  * 视图，jsp
* C：Controller 
  * 控制器,servlet
  * 作用：视图和逻辑分离

##### MVC适用场景：大型项目开发

#### 3. Eclipse和jstl

##### 3.1 注意

* 在eclipse中默认没有jstl的jar
* 需要自己手动导入

##### 3.2 在jsp中使用jstl

* 导入jar

* 使用jsp指令引入jstl类库

  * 指定编码可以通过下列操作一次指定

    > window --> preferences --> web --> JSP Files --> encoding -->

  ```jsp
  <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
  ```

![jsp和jstl](G:\学习笔记\javaWeb\mybatis\image\jsp和jstl.png)