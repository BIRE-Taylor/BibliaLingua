package es.tjon.biblialingua.listener;
import es.tjon.biblialingua.adapter.*;
import android.webkit.*;
import android.widget.*;
import android.util.*;

public class EnhancedJsInterface {
	
	private static final String TAG = "es.tjon.biblialingua.listener.EnhancedJSInterface";
    protected BookViewer view;

    public EnhancedJsInterface(BookViewer enhancedWebView) {
        this.view = enhancedWebView;
    }

    @JavascriptInterface
    public void jsConsoleLog(String string) {
        Log.v(TAG,string);
    }

    @JavascriptInterface
    public void jsFinishedRendering(String string) {
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

