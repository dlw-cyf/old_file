## matplotlib

#### 基本方法的使用

* plt.show()---->将图片进行显示

* plt.figure(num,figsize)---->创建画布

  ```python
  plt.figure(num=2,figsize=(8,5))
  #num:表示图形的编号或名称,数字代表编号,字符串代表名称
  #figsize:设置画布的尺寸,宽度,高度以英寸为单位
  ```

* plt.plot(x,y,label,color,linewith,linestyle)---->根据x,y函数方程画出一条直线图

  ```python
  l2, = plt.plot(x,y1,color='red',linewidth=1.0,linestyle='--',label='down')
  '''
  	x,y:函数方程的表达式
  		x = np.linspace(-3,3,50)
  		y1 = 2*x+1
  	linewith:设置线条的宽度
  	linestyle:
  		--:虚线
  	label:线的标签名
  注意:
  	接收的返回值后一定要加,
  '''
  ```

* plt.xlim(x,y)---->设置x坐标轴的范围

  ```python
  plt.xlim(-1,2)
  ```

* plt.xticks(ticks,label)---->将x的坐标使用ticks或者labels替换

  ```python
  new_ticks = np.linspace(-1,2,5)
  #替换x轴的ticks
  plt.xticks(new_ticks)
  #将y的ticks数字转换为对应的文字
  plt.yticks([-2,-1.8,-1,1.22,3],
             [r'$really\ bad$',r'$bad\ \alpha$',r'$normal$',r'$gond$',r'$really\ good$'])
  
  ```

* 设置坐标轴和位置

  1. 获取到所有的轴（top,bottom,left,right）

     ```python
     ax = plt.gca()
     #gca == get current axis
     ```

  2. 通过获取的轴,将right和top的轴设置为none

     ```python
     ax.spines['right'].set_color('none')
     ax.spines['top'].set_color('none')
     ```

  3. 谁知bottom轴为x轴,right轴为y轴

     ```python
     #设置bottom轴为x轴
     ax.xaxis.set_ticks_position('bottom')
     #设置left轴为y轴
     ax.yaxis.set_ticks_position('left')
     ```

  4. 设置bottom和left的位置(相对于left和bottom轴的位置)----->坐标原点（0,0）

     ```python
     #设置bottom轴对应left轴的位置为left==0
     ax.spines['bottom'].set_position(('data',0))
     #设置left轴对应bottom轴的位置为bottom==0
     ax.spines['left'].set_position(('data',0))
     ```

* plt.legend(handles,lablels,loc)---->为图片设置图例

  ```python
  l1, = plt.plot(x,y2,label='up')
  l2, = plt.plot(x,y1,color='red',linewidth=1.0,linestyle='--',label='down')
  #将l1,l2,的线条名替换为aa,bb
  plt.legend(handles=[l1,l2,],labels=['aa','bb'],loc='best')
  #handles:指定需要图例的线,对应plt.plot()的返回值
  #labels:指定线的图例名称
  #loc:指定图例的位置(best...)
  ```

#### 绘制各种图形的方法

 * 绘制子图

   * plt.**subplot**(nrows,ncols,index)

     ```python
     ax = plt.subplot(2,2,i)
     #为子图设置标题
     plt.title('%s月份各类商品销售额' % i)
     ```

* 绘制柱形图
  * plt.**bar**(x,height,width,facecolor,engecolor)

    ```python
    #width:每个柱形的宽度
    #facecolor:柱形的颜色
    #edgecolor:柱形边框颜色
    plt.bar(X,+Y1,facecolor="#9999ff",edgecolor='white',width=0.25)
    ```

* 绘制散点图

  * plt.**scatter**(x,y,s,c,alpha)

    ```python
    '''
    x,y:x轴和y轴对应的数据
    s:指定点的大小
    c:指定散点的颜色
    alpha:点的透明度
    '''
    plt.scatter(X,Y,s=75,c=T,alpha=0.5)
    ```

* 绘制饼图

  * plt.pie(x,explode,labels,autopct,shadow)

    ```python
    labels = ['娱乐','育儿','饮食','房贷','交通','其它']
    sizes = [2,5,12,70,2,9]
    explode = (0,0,0,0.1,0,0)
    plt.pie(sizes,explode=explode,labels=labels,autopct='%1.1f%%',shadow=False)
    ```

    * x:比例---如果sum>1会使用sum(x)归一化
    * labels:饼图外侧显示的说明文字
    * explode:每一块离开中心的距离
    * autopct:控制饼图内百分比设置