#### 启动flume配置文件

* 启动前查看端口是否被占用
  * netstat -tunlp | grep 44445

> /home/flume-1.8.0/bin/flume-ng agent \
>
> --conf /home/flume-1.8.0/conf \
>
> --name a1 \
>
> --conf-file /home/flume-1.8.0/jobconf/flume-telnet.conf  \
>
> -Dflume.root.logger==info,console

* 通过telnet往指定端口写入数据
  * telnet 192.168.80.10 44445