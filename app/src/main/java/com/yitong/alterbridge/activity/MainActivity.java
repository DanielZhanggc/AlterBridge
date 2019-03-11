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
import com.yitong.alterbridge.jsbridge.BridgeClient;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private WebView webView;
    private Button nativeBt;
    private BridgeClient webViewClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        webView = findViewById(R.id.webView);
        nativeBt = findViewById(R.id.nativeBt);
        webView.loadUrl("file:///android_asset/ExampleApp.html");
        nativeBt.setOnClickListener(this);

        webViewClient = new BridgeClient(webView);
        webView.setWebViewClient(webViewClient);

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

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.nativeBt:
                try {
                    //调用JS init方法注册的插件
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("Client", "消息来源客户端");
                    // 调用JS注册的插件JSHandler
                    webViewClient.sendData(jsonObject, new BridgeClient.BridgeCallback() {
                        @Override
                        public void callback(Object data) {
                            showAlerDialog("Web:" + data);
                        }
                    }, "JSHandler");
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
                .setTitle("前端消息:")
                .setMessage(data)
                .setPositiveButton("确定", null)
                .setCancelable(false)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextAppearance(R.style.MyCustomTabTextAppearance);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#FF4081"));
    }

}
