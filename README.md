# AlterBridge
前端和Android客户端交互封装,前端和Android客户端方法的相互调用,打造一款Web App,适用于混合开发

# 使用方法
1.将AlterBridge.js.txt文件复制到assets目录下  
2.将类BridgeClient复制到项目中  
3.WebView使用BridgeClient对象,在这里Android客户端的框架基本完成  
4.客户端注册插件供前端调用  
```Java
        //注册能被JS调用的插件
        webViewClient.registerHandler("AlterBridge", new BridgeClient.BridgeHandler() {
            @Override
            public void request(Object data, BridgeClient.BridgeCallback callback) {
                try {
                    showAlerDialog(data.toString());
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("Client", "消息来源客户端");
                    callback.callback(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
```
5.前端页面AlterBridge初始化  
```javascript
function connectAlterBridge(callback) {
		if (window.WebViewJavascriptBridge) {
		    console.log('window已经包含WebViewJavascriptBridge对象');
			callback(WebViewJavascriptBridge);
		} else {
		    console.log('window不包含WebViewJavascriptBridge对象');
		    console.log('添加监听WebViewJavascriptBridgeReady');
			document.addEventListener('WebViewJavascriptBridgeReady', function() {
				callback(WebViewJavascriptBridge);
			}, false)
		}
	};
  
connectAlterBridge(function(bridge) {
		var uniqueId = 1
		bridge.init();
		});
```
6.前端注册插件  
```javascript
               <!-- 注册插件(客户端待调用) -->
		bridge.registerHandler('JSHandler', function(data, responseCallback) {
			log('客户端发送数据[Client -> Web]', data);
			var data = { 'Web':'消息来自前端' };
			log('前端回复数据[Client -> Web]', data);
			responseCallback(data);
```
 
7.前端调用客户端插件  
```javascript
   function() {
      <!--构建前端返回客户端数据-->
			var data = {"Web":"消息来源前端"};
			<!--调用客户端注册的AlterBridge方法-->
			bridge.callHandler('AlterBridge', data, function(response) {
        <!--处理客户端返回前端的数据-->
			});
   };
```
8.客户端调用前端插件  
```Java
                    //调用JS init方法注册的插件
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("Client", "消息来源客户端");
                    // 调用JS注册的插件JSHandler
                    webViewClient.sendData(jsonObject, new BridgeClient.BridgeCallback() {
                        @Override
                        public void callback(Object data) {
                            //处理前端传到客户端数据
                        }
                    }, "JSHandler");
```
# 效果展示
![image](https://github.com/DanielZhanggc/AlterBridge/blob/master/show.png)

# APK下载
![image](https://github.com/DanielZhanggc/AlterBridge/blob/master/apk_download.png)
