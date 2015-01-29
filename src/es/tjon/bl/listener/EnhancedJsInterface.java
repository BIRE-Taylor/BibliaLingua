package es.tjon.bl.listener;
import es.tjon.bl.adapter.*;
import android.webkit.*;
import android.widget.*;
import android.util.*;

public class EnhancedJsInterface {
	
	private static final String TAG = "es.tjon.bl.listener.EnhancedJSInterface";
    protected BookViewer view;

    public EnhancedJsInterface(BookViewer enhancedWebView) {
        this.view = enhancedWebView;
    }

    @JavascriptInterface
    public void jsConsoleLog(String string) {
        Log.d(TAG,string);
    }

    @JavascriptInterface
    public void jsFinishedRendering(String string) {
        this.view.onFinishRender(string);
    }

    @JavascriptInterface
    public void jsShowToast(String string) {
        this.jsShowToast(string, 0);
    }

    @JavascriptInterface
    public void jsShowToast(String string, int n) {
        Toast.makeText(view.getContext(),string,Toast.LENGTH_SHORT).show();
    }
}

