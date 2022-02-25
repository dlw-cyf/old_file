#### 文件上传的必要前提

1. form表单的enctype取值必须是：multipart/form-data
   * 表单enctype的默认值是：application/x-www-form-urlencoded

2. method 属性取值必须是post
3. 提供一个文件选择域<input type="file">

#### 借助第三方组件实现文件上传

```xml
	<dependency>
      <groupId>commons-io</groupId>
      <artifactId>commons-io</artifactId>
      <version>2.4</version>
    </dependency>
    <dependency>
      <groupId>commons-fileupload</groupId>
      <artifactId>commons-fileupload</artifactId>
      <version>1.3.1</version>
    </dependency>
```

#### 配置文件解析器对象

```xml
	<!--配置文件解析器对象，要求id名称必须是multipartResolver-->
    <bean id="multipartResolver" class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
        <property name="maxUploadSize" value="10485760"></property>
    </bean>
```

