package com.yitong.alterbridge.activity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import com.yitong.alterbridge.R;
import com.yitong.alterbridge.jsbridge.WVJBWebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private WebView webView;
    private Button bt;
    private WVJBWebViewClient webViewClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        bt = findViewById(R.id.bt);

        webView.loadUrl("file:///android_asset/ExampleApp.html");
        webViewClient = new WVJBWebViewClient(webView);
        webView.setWebViewClient(webViewClient);

        //发送消息到前端并获取回调信息
        bt.setOnClickListener(this);

        //注册能被JS调用的插件
        webViewClient.registerHandler("AlterBridge", new WVJBWebViewClient.WVJBHandler() {
            @Override
            public void request(Object data, WVJBWebViewClient.WVJBResponseCallback callback) {
                try {
                    // SystemClock.sleep(3000);
                    Toast.makeText(MainActivity.this, data.toString(), Toast.LENGTH_LONG).show();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("Client", "Client Response Data");
                    callback.callback(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt:
                try {
                    //调用JS init方法注册的插件
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("DATA", "Client Call JS_InitHandler");
                    webViewClient.send(jsonObject, new WVJBWebViewClient.WVJBResponseCallback() {

                        @Override
                        public void callback(Object data) {
                            showAlerDialog("Response Data:" + data);
                        }
                    });
                    // 调用JS注册的插件JSHandler
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("DATA","Client Call JSHandler");
//                webViewClient.callHandler("JSHandler", jsonObject, new WVJBWebViewClient.WVJBResponseCallback() {
//                    @Override
//                    public void callback(Object data) {
//                        showAlerDialog("Response Data:"+data);
//                    }
//                });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode && webView.canGoBack()) {
            if (webView.getUrl().equals(this)) {
                finish();
            } else {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    //Dialog
    @SuppressLint("NewApi")
    private void showAlerDialog(String data) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Client Notice:")
                .setMessage(data)
                .setPositiveButton("Confirm", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextAppearance(R.style.MyCustomTabTextAppearance);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#FF4081"));
    }

}
