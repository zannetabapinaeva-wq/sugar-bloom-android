package app.sugarbloom.diary;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private static final int PICK_FILE = 100;
    private static final String LOCAL_HOST = "appassets.androidplatform.net";
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        setContentView(web);
        WebView.setWebContentsDebuggingEnabled(false);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(true);
        web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.addJavascriptInterface(new DownloadBridge(), "AndroidDownloads");
        web.addJavascriptInterface(new NotificationBridge(), "AndroidNotifications");
        WebViewAssetLoader loader = new WebViewAssetLoader.Builder().addPathHandler("/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (LOCAL_HOST.equals(request.getUrl().getHost())) return loader.shouldInterceptRequest(request.getUrl());
                return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",java.util.Collections.emptyMap(),new java.io.ByteArrayInputStream(new byte[0]));
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                return !("https".equals(uri.getScheme()) && LOCAL_HOST.equals(uri.getHost()));
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/json", "text/plain"});
                try {startActivityForResult(intent,PICK_FILE);return true;}catch(Exception ex){fileCallback.onReceiveValue(null);fileCallback=null;return false;}
            }
        });
        if (state != null) web.restoreState(state); else web.loadUrl("https://"+LOCAL_HOST+"/index.html");
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("diary","Напоминания Sugar Bloom",NotificationManager.IMPORTANCE_DEFAULT));
    }
    @Override protected void onActivityResult(int request,int result,Intent intent) {
        super.onActivityResult(request,result,intent);
        if(request==PICK_FILE && fileCallback!=null){fileCallback.onReceiveValue(result==RESULT_OK&&intent!=null&&intent.getData()!=null?new Uri[]{intent.getData()}:null);fileCallback=null;}
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);web.saveState(out);}
    @Override public void onBackPressed(){web.evaluateJavascript("document.querySelector('[aria-label=\"Закрыть\"]')?.click()",null);}
    @Override protected void onDestroy(){if(fileCallback!=null)fileCallback.onReceiveValue(null);web.destroy();super.onDestroy();}
    final class DownloadBridge {
        @JavascriptInterface public void saveBase64(String encoded,String requestedName,String requestedType){
            try {
                if(encoded.length()>50_000_000)throw new IllegalArgumentException("Too large");
                String name=requestedName.replaceAll("[^a-zA-Z0-9._-]","_");
                if(!name.startsWith("sugar-bloom"))throw new IllegalArgumentException("Invalid name");
                String type=name.endsWith(".pdf")?"application/pdf":name.endsWith(".csv")?"text/csv":"application/json";
                ContentValues values=new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME,name);
                values.put(MediaStore.Downloads.MIME_TYPE,type);
                values.put(MediaStore.Downloads.RELATIVE_PATH,"Download/SugarBloom");
                values.put(MediaStore.Downloads.IS_PENDING,1);
                Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
                if(uri==null)throw new IllegalStateException("Download unavailable");
                try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IllegalStateException();out.write(Base64.decode(encoded,Base64.DEFAULT));}
                catch(Exception ex){getContentResolver().delete(uri,null,null);throw ex;}
                values.clear();values.put(MediaStore.Downloads.IS_PENDING,0);getContentResolver().update(uri,values,null,null);
                runOnUiThread(()->Toast.makeText(MainActivity.this,"Сохранено в Загрузки / SugarBloom",Toast.LENGTH_LONG).show());
            }catch(Exception ex){runOnUiThread(()->Toast.makeText(MainActivity.this,"Не удалось сохранить файл",Toast.LENGTH_LONG).show());}
        }
    }
    final class NotificationBridge {
        @JavascriptInterface public void requestPermission(){runOnUiThread(()->{if(Build.VERSION.SDK_INT>=33)MainActivity.this.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},101);});}
        @JavascriptInterface public void notify(String body){
            if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
            Notification notification=new Notification.Builder(MainActivity.this,"diary").setContentTitle("Sugar Bloom 💙").setContentText(body).setSmallIcon(app.sugarbloom.diary.R.drawable.ic_bloom).setAutoCancel(true).build();
            getSystemService(NotificationManager.class).notify(1,notification);
        }
    }
}
