package com.yitong.alterbridge.jsbridge;

import android.annotation.SuppressLint;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressLint({"SetJavaScriptEnabled", "NewApi"})
public class BridgeClient extends WebViewClient {

    //URL特定标识
    private static final String kTag = "WVJB";
    private static final String kInterface = kTag + "Interface";
    private static final String kCustomProtocolScheme = "wvjbscheme";
    private static final String kQueueHasMessage = "__WVJB_QUEUE_MESSAGE__";

    protected WebView webView;
    //存储注册插件以及回调
    private Map<String, BridgeHandler> bridgeHandlers = null;
    private Map<String, BridgeCallback> bridgeCallbacks = null;
    //生成唯一标识ID
    private long uniqueId = 0;
    //适配Android4.4之前版本
    private MyJavascriptInterface myInterface = new MyJavascriptInterface();

    public BridgeClient(WebView webView) {
        this.webView = webView;
        this.webView.getSettings().setJavaScriptEnabled(true);
        this.webView.addJavascriptInterface(myInterface, kInterface);
        this.bridgeCallbacks = new HashMap<String, BridgeCallback>();
        this.bridgeHandlers = new HashMap<String, BridgeHandler>();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith(kCustomProtocolScheme)) {
            if (url.indexOf(kQueueHasMessage) > 0) {
                flushMessageQueue();
            }
            return true;
        }
        return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        try {
            InputStream is = webView.getContext().getAssets().open("AlterBridge.js.txt");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String js = new String(buffer);
            executeJavascript(js, null);
            super.onPageFinished(view, url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //拦截特定标识的URL后执行js方法
    private void flushMessageQueue() {
        String script = "WebViewJavascriptBridge._fetchQueue()";
        executeJavascript(script, new JavascriptCallback() {
            public void onReceiveValue(String messageQueueString) {
                if (messageQueueString == null || messageQueueString.length() == 0) {
                    return;
                }
                processQueueMessage(messageQueueString);
            }
        });
    }

    //前端 --[数据]--> 客户端 --[数据]--> 前端{交互的关键}
    private void processQueueMessage(String messageQueueString) {
        try {
            JSONArray messages = new JSONArray(messageQueueString);
            for (int i = 0; i < messages.length(); i++) {
                JSONObject jo = messages.getJSONObject(i);
                BridgeMessage message = getBridgeMessage(jo);
                if (message.responseId != null) {
                    BridgeCallback responseCallback = bridgeCallbacks.remove(message.responseId);
                    if (responseCallback != null) {
                        responseCallback.callback(message.responseData);
                    }
                } else {
                    BridgeCallback responseCallback = null;
                    if (message.callbackId != null) {
                        final String callbackId = message.callbackId;
                        responseCallback = new BridgeCallback() {
                            @Override
                            public void callback(Object data) {
                                BridgeMessage msg = new BridgeMessage();
                                msg.responseId = callbackId;
                                msg.responseData = data;
                                dispatchMessage(msg);
                            }
                        };
                    }
                    BridgeHandler handler = null;
                    if (message.handlerName != null) {
                        handler = bridgeHandlers.get(message.handlerName);
                    }
                    if (handler != null) {
                        handler.request(message.data, responseCallback);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //调用前端插件
    public void sendData(Object data, BridgeCallback responseCallback, String handlerName) {
        if (data == null && (handlerName == null || handlerName.length() == 0)) {
            return;
        }
        BridgeMessage message = new BridgeMessage();
        if (data != null) {
            message.data = data;
        }
        if (responseCallback != null) {
            String callbackId = "objc_cb_" + (++uniqueId);
            bridgeCallbacks.put(callbackId, responseCallback);
            message.callbackId = callbackId;
        }
        if (handlerName != null) {
            message.handlerName = handlerName;
        }
        dispatchMessage(message);
    }

    //去反斜杠并执行JS方法
    private void dispatchMessage(BridgeMessage message) {
        String messageJSON = getJSONObject(message).toString()
                .replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"")
                .replaceAll("\'", "\\\\\'").replaceAll("\n", "\\\\\n")
                .replaceAll("\r", "\\\\\r").replaceAll("\f", "\\\\\f");
        executeJavascript("WebViewJavascriptBridge._handleMessageFromObjC('" + messageJSON + "');", null);
    }

    //用于执行JS方法
    private void executeJavascript(String script, final JavascriptCallback callback) {
        //Android4.4以后WebView支持回调
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(script, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    if (callback != null) {
                        if (value != null && value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1).replaceAll("\\\\", "");
                        }
                        callback.onReceiveValue(value);
                    }
                }
            });
        } else {
            //Android4.4之前只能将回调保存起来 Map的key随着callback增加加1即可
            if (callback != null) {
                myInterface.addCallback(++uniqueId + "", callback);
                webView.loadUrl("javascript:window." + kInterface + ".onResultForScript(" + uniqueId + "," + script + ")");
            } else {
                webView.loadUrl("javascript:" + script);
            }
        }
    }

    //客户端注册插件
    public void registerHandler(String handlerName, BridgeHandler handler) {
        if (handlerName == null || handlerName.length() == 0 || handler == null) {
            return;
        }
        bridgeHandlers.put(handlerName, handler);
    }

    private class BridgeMessage {
        Object data = null;
        String callbackId = null;
        String handlerName = null;
        String responseId = null;
        Object responseData = null;
    }

    public interface JavascriptCallback {
        void onReceiveValue(String value);
    }

    public interface BridgeCallback {
        void callback(Object data);
    }

    public interface BridgeHandler {
        void request(Object data, BridgeCallback callback);
    }

    private class MyJavascriptInterface {
        Map<String, JavascriptCallback> map = new HashMap<String, JavascriptCallback>();

        void addCallback(String key, JavascriptCallback callback) {
            map.put(key, callback);
        }

        @JavascriptInterface
        public void onResultForScript(String key, String value) {
            JavascriptCallback callback = map.remove(key);
            if (callback != null)
                callback.onReceiveValue(value);
        }
    }

    //将json转化成BridgeMessage对象
    private BridgeMessage getBridgeMessage(JSONObject jo) {
        BridgeMessage message = new BridgeMessage();
        try {
            if (jo.has("callbackId")) {
                message.callbackId = jo.getString("callbackId");
            }
            if (jo.has("data")) {
                message.data = jo.get("data");
            }
            if (jo.has("handlerName")) {
                message.handlerName = jo.getString("handlerName");
            }
            if (jo.has("responseId")) {
                message.responseId = jo.getString("responseId");
            }
            if (jo.has("responseData")) {
                message.responseData = jo.get("responseData");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return message;
    }

    //将BridgeMessage对象转化成Json
    private JSONObject getJSONObject(BridgeMessage message) {
        JSONObject jo = new JSONObject();
        try {
            if (message.callbackId != null) {
                jo.put("callbackId", message.callbackId);
            }
            if (message.data != null) {
                jo.put("data", message.data);
            }
            if (message.handlerName != null) {
                jo.put("handlerName", message.handlerName);
            }
            if (message.responseId != null) {
                jo.put("responseId", message.responseId);
            }
            if (message.responseData != null) {
                jo.put("responseData", message.responseData);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo;
    }

}
