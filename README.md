# AlterBridge
用于Web-Native交互解决方案

# 原理  
## 引入JS文件 ##
在html加载完成后走入onPageFinished()方法，调用excuteJavascript方法执行AlterBridge.js.txt的js方法

## 客户端注册插件 ##
BridgeClient对象调用registerHandler方法将BridgeHandler以key-value的形式存入BridgeClient的成员变量bridgeHandlers中

## 前端注册插件 ##


## 前端调用客户端插件 ##
1.调用shouldOverrideUrlLoading()方法[API],拦截URL,分析URL特征-->
2.调用flushMessage()方法,[1]调用js方法_fetchQueue(),[2]定义JavascriptCallback对象，在这里分成两条线-->
3.调用excuteJavascript方法,判断Android版本是否超过19，[3]调用webView.evaluateJavascript(方法){API},[4]{暂不研究}-->

4.[3]调用[2]中定义的JavascriptCallback对象的方法{不为null},json字符串去反斜杠-->
5.调用processQueueMessage()方法,将json格式字符串转换成BridgeMessage对象,创建BridgeCallback对象[5]
通过message对象的callbackId从bridgeHandlers取BridgeHandler对象,调用其方法{在注册时已经重写}
如果重写的方法调用了[5]的方法,整合数据,重复3步骤,开始调用JS方法_handleMessageFromObjC()-->

## 客户端调用前端插件 ##
调用sendData()方法,创建BridgeMessage对象,重复3,调用js方法_handleMessageFromObjC()

# 效果展示

# APK下载
