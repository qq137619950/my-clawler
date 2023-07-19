 MY-CRAWLER

**架构图：** 
![image](https://github.com/qq137619950/my-clawler/assets/22728004/785cc0d9-eb10-45f0-b41c-5476191c7b50)

**使用说明：** 

1. 修改配置文件
   在maven项目的resources目录下添加如下三个文件：
   ![image](https://github.com/qq137619950/my-clawler/assets/22728004/fc758002-c8ea-47f3-ae4f-0b9e88d5dd95)

（1）global.yaml 全局配置
![image](https://github.com/qq137619950/my-clawler/assets/22728004/f1c29b1b-3d8c-496a-96ba-159163283a73)
(2) proxy.yaml 代理池配置
(3) site.yaml 爬虫任务网址配置
![image](https://github.com/qq137619950/my-clawler/assets/22728004/a09e1e67-6ec6-49c9-a0fa-83be76e32311)

2. 执行
(1) 在site.yaml中的jobRoot配置的目录下，创建类，如：YoulaiArticleCrawler
然后继承AbsCommonCrawler，实现几个函数。
(2) 具体请参见源代码中idea.bios.jobs.example目录下的类文件，详情线上交流，用一下就明白了
(3) main函数中执行new CommonCrawlerStarter().run();即可

3. 过程检测
   部署一个爬虫任务之后，会通过所配置的微信群机器人发送消息，第一次发送时间为第一分钟，之后每隔一个小时发送一次。具体内容参见下图：
   ![image](https://github.com/qq137619950/my-clawler/assets/22728004/5ae024e8-0931-4fa4-a74a-962b7156e5b6)
   如果crawler没有速率，则判断为代理不通，会自动移除：
   ![image](https://github.com/qq137619950/my-clawler/assets/22728004/54f9844e-3918-420e-b01a-fbeb84b4ff92)

最佳实践
![image](https://github.com/qq137619950/my-clawler/assets/22728004/fabd8f2f-9327-42ae-8d84-fc0cee2a73d3)




