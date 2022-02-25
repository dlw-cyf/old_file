#### 九大内置对象

| 名称        | 类型                | 含义             | 获取方式                    |
| ----------- | ------------------- | ---------------- | --------------------------- |
| request     | HttpServletRquest   | 封装所有请求信息 | 方法参数                    |
| response    | HttpServletResponse | 封装所有响应信息 | 方法参数                    |
| session     | HttpSession         | 封装所有会话信息 | request.getSession()        |
| application | ServletContext      | 所有信息         | request.getServletContext() |
| out         | PrintWriter         | 输出对象         | response.getWriter()        |
| exception   | Exception           | 异常对象         |                             |
| page        | Object              | 当前页面对象     |                             |
| pageContext | PageContext         | 获取其他对象     |                             |
| config      | ServletConfig       | 配置信息         |                             |

#### 四大作用域

1. page
   * 在当前页面不会重新实例化。
2. request
   * 在一次请求中同一个对象，下次请求重新实例化一个request对象。
3. session
   * 一次会话
   * 只要客户端Cookie中传递的Jsessionid不变，Session不会重新实例化（不超过默认时间）。
   * 实际有效时间
     * 浏览器关闭。Cookie失效。
     * 默认时间，在时间范围内无任何交互，在tomcat的 web.xml 中配置
4. application
   * 只有在 tomacat 启动项目时实例化，关闭tomcat时销毁 application

#### SpringMVC作用域传值的几种方式

1. 使用原生servlet API

```java
@RequestMapping("demo")
	public String demo(HttpServletRequest abc,HttpSession sessionParam){
		abc.setAttribute("req", "req的值");
		HttpSession session = abc.getSession();
		session.setAttribute("session", "session的值");
		sessionParam.setAttribute("sessionParam", "sessionParam的值");
		
		ServletContext application = abc.getServletContext();
		application.setAttribute("application", "application的值");
		return "index";
	}
```

2. 使用map集合

   * 把map中内容放在request作用域中
   * spring会对map集合通过BindingAwareModeMap进行实例化

   ```java
   	/**
   	 * 把map中内容放在request作用域中
   	 * spring会对map集合通过BindingAwareModeMap进行实例化
   	 * @param map
   	 * @return
   	 */
   	@RequestMapping("demo2")
   	public String demo2(Map<String,Object> map){
   		map.put("map", "map的值");
   		return "index";
   	}
   ```

3. 使用SpringMVC中Model接口

   * 把内容最终放到  request  作用域中。

   ```java
   	@RequestMapping("demo3")
   	public String demo3(Model model){
   		model.addAttribute("model", "model的值");
   		return "index";
   	}
   ```

4. 使用SpringMVC中ModeAndView类

   ```java
   	@RequestMapping("demo4")
   	public ModelAndView demo4(){
   		//参数,跳转视图
   		ModelAndView mav = new ModelAndView("index");
   		mav.addObject("mav", "mav的值");
   		return mav;
   	}
   ```

   

