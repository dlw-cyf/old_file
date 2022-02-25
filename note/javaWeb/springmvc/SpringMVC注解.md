##### @RequestMapping

* 设置请求路径映射

##### @RequestParam

* 属性
  * value：指定接收参数的名称
  * defaultValue：给参数一个默认值
  * required：改参数必须传参赋值

##### @PathVariable

* 获取请求路径中的占位符

```java
	@RequestMapping("/demo7/{id}/{name}")
	public String demo7(@PathVariable int id , @PathVariable String name){
		System.out.println(id+"-->"+name);
		return "/main.jsp";
	}
```

##### @ResponseBody

* 直接在方法只有@RequestMapping时，无论方法返回值是什么，都认为需要跳转
* 在方法上添加@ResponseBody（恒不跳转）
  * 如果返回值满足key-value形式(对象或map)
    * 把响应头设置为application/json;charset=utf-8
    * 把转换后的内容以输出流的形式响应给客户端
  * 如果返回值不满足key-value，例如返回值为String
    * 把响应头设置为text/html
    * 把返回值以流的形式直接输出
    * 如果返回值包含中文，会出现中文乱码
      * produces：设置响应头中Content-Type取值

```java
/**
 * produces设置响应的字符编码格式
 * @return
 */
@RequestMapping(value="demo12",produces="text/html;charset=utf-8")
@ResponseBody
public People demo12(){
	People p = new People();
	p.setAge(12);
	p.setName("张三");
	return p;
}
```

##### @RequestBody

* 作用：用于获取请求体内容。直接使用得到是 key=value&可以=value... 结构的数据
* 属性：
  * required：是否必须有去请求体
    * 默认是true

```java
	@RequestMapping("/testRequestBody")
	public String testRequestBody(@RequestBody String body){
		System.out.println(body);
		return "success";
	}
```

##### @RequestHeader

* 作用：用于获取请求头信息
* 属性：
  * value：提供消息头信息
  * required：是否必须有此请求头
  * 
* 注意：在实际开发中一般不怎么使用。

##### @CookieValue

* 作用：用于指定cookie名称的值传入控制器方法参数
* 属性:
  * value：指定cookie的名称
  * required：是否必须由此cookie

##### @ModelAttribute

* 作用：该注解是SpringMVC4.3版本之后新加入的。可以修饰方法和参数
  * 修饰方法：表示当前方法会在控制器的方法执行之前，先执行，它可以修饰没有返回值的方法，也可以修饰有具体返回值的方法
  * 修饰参数：获取指定的数据给参数赋值
* 属性：
  * value：用于获取数据的key，key可以是POJO的属性名称，也可以是map结构的key
* 用于有返回值的方法

```java
	@RequestMapping("/testModeAttribute")
	public String testModeAttribute(User user){
		System.out.println("testModeAttribute方法执行了");
		System.out.println(user);
		return "success";
	}

	/**
	 * 该方法会先执行
	 */
	@ModelAttribute
	public User showUser(String uname){
		System.out.println("showUser方法执行了");
		//通过用户名查询数据库(模拟)
		User user = new User();
		user.setUname(uname);
		user.setAge(20);
		user.setDate(new Date());
		System.out.println("查询的user："+user);
		return user;
	}
```

* 作用无返回值方法

```java
	@RequestMapping("/testModeAttribute")
	public String testModeAttribute(@ModelAttribute("abc") User user){
		System.out.println("testModeAttribute方法执行了");
		System.out.println(user);
		return "success";
	}
	
	@ModelAttribute
	public void showUser(String uname, Map<String,User> map){
		System.out.println("showUser方法执行了");
		//通过用户名查询数据库(模拟)
		User user = new User();
		user.setUname(uname);
		user.setAge(20);
		user.setDate(new Date());
		map.put("abc",user);
	}
```

##### @SessionAttributes

* 用于多次执行控制器方法之间的参数共享。
* 属性：
  * value：用于指定存入的属性名称
  * type：用于指定存入的数据类型

```java
@Controller
@RequestMapping("/anno")
@SessionAttributes(value = {"msg"}) //把msg=小戴存入到session域对象中
public class AnnoController {
    
	@RequestMapping("/testSessionAttributes")
	public String testSessionAttributes(Model model){
		System.out.println("testModeAttributes方法执行了");
        //将数据存入到Request作用域对象中
		model.addAttribute("msg","小戴");
		return "success";
	}
}
```

