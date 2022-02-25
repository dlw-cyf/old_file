### scrapy登录网页实现

* get请求

  ```
  scrapy.Request(url,callback)
  ```

* post请求

  ```python
  scrapy.FormRequst(url,formdata,callback)
  ```

* 识别验证码

  * 通过解析器xpath或者css获取图片链接

  * 保存图片到本地，并打开图片

    ```
    from urllib import request
    			request.urlretrieve(file_url,file_name)
    from PIL import Image
    			img = Image.open(image_path)
    			img.show()
    ```

  * 手动输入验证码，提交请求（post）

### python的数据库操作--pymysql

 * 初始化连接mysql——获取游标

   ```python
   #连接数据库
   self.conn = pymysql.connect(
   	host = '127.0.0.1',
   	port = 3306,
   	db = 'databaseName',
   	user = 'root',
   	password = '123456',
   	charset = 'utf8'
   )
   #获得游标
   self.cur = self.conn.cursor()
   ```

* 操作数据库

  ```python
  #sql语句
  sql = 'insert into tableName({columns})values({values})'.format(column=...,values=					(['%s']*len(values)))
  #使用游标执行语句
  self.cur.execute(sql,values)
  ```

