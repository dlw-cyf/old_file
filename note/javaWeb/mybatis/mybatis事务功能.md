##### 概念复习

* 功能：从应用程序角度出发，软件具有哪些功能
* 业务：完成功能时的逻辑，对应Service中一个方法
* 事务：从数据库角度出发，完成业务时需要执行的SQL集合，统称一个事务

##### mybatis中的事务

1. mybatis默认关闭了JDBCder自动提交功能

2. 每个SqlSession默认都是不自动提交事务

3. session.commit()提交事务
4. openSession(true);自动提交

