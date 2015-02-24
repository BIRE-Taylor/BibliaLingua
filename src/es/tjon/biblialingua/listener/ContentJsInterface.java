package es.tjon.biblialingua.listener;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.text.ClipboardManager;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import es.tjon.biblialingua.adapter.*;
import es.tjon.biblialingua.data.book.*;
import java.util.concurrent.*;
import java.util.*;
import android.util.*;

public class ContentJsInterface
extends EnhancedJsInterface {
//    private static final int MAX_TTS_TRIES = 10;
//    private long lastImageTap = 0;
//    private String lastSelection = null;
//    private long lastVideoTap = 0;
//    private Runnable queueTtsTextRunnable;
//    private int ttsTriesToStart = 0;
    private BookViewer view;
	
	private static final String TAG = "es.tjon.biblialingua.listener.ContentJSInterface";

    public ContentJsInterface(BookViewer contentWebView) {
        super(contentWebView);
        this.view = contentWebView;
    }
	
	@JavascriptInterface
    public void jsReportRcaOffsets(String offsets) {
		try
		{
			JSONArray offsetArray = new JSONArray(offsets);
			NavigableMap<Float,String> offsetMap = new ConcurrentSkipListMap<Float,String>();
			for(int i=0;i<offsetArray.length();i++)
			{
				JSONObject offset = offsetArray.getJSONObject(i);
				if(!offset.getString("id").contains("file://"))
					offsetMap.put(Float.parseFloat(offset.getString("top"))+(Float.parseFloat(offset.getString("left"))/1000),offset.getString("id"));
			}
			view.rcaOffsetMap=offsetMap;
		}
		catch (JSONException e)
		{e.printStackTrace();}
    }
	
	@JavascriptInterface
    public void jsReportUriOffsets(String string) {
		Log.i(TAG,"Report offsets");
        JSONArray jSONArray;
        NavigableMap<Float,String> sortedMap = new ConcurrentSkipListMap<Float,String>();
		NavigableMap<String,Float> reverseMap = new ConcurrentSkipListMap<String,Float>();
        try {
            jSONArray = new JSONArray(string);
			for (int i = 0; i < jSONArray.length(); ++i) {
				JSONObject jSONObject = jSONArray.getJSONObject(i);
				sortedMap.put(Float.valueOf(jSONObject.getInt("top")), jSONObject.getString("id"));
				reverseMap.put(jSONObject.getString("id"),Float.valueOf(jSONObject.getInt("top")));
			}
			view.mCount+=1;
			view.uriOffsetMap=sortedMap;
			view.uriOffsetLookupMap=reverseMap;
        }
        catch (Exception e) {
            Log.e(TAG, "Unable to parse results of reportUriOffsets:" + string, e);
            return;
        }
        
    }

//    private String fixHeadUri(String string) {
//        String string2 = Annotation.blockLevelIdentifierFromURI(string);
//        BookNode bookNode = this.view.getBookNode();
//        if (bookNode != null && "head".equals((Object)string2)) {
//            string = bookNode.getUri();
//        }
//        return string;
//    }
//
//    private Runnable getQueueTtsTextRunnable() {
//        if (this.queueTtsTextRunnable == null) {
//            this.queueTtsTextRunnable = new QueueTtsTextRunnable(this.view, this);
//        }
//        return this.queueTtsTextRunnable;
//    }
//
//    /*
//     * Enabled aggressive block sorting
//     */
//    private void processAnnotationText(String string, String string2, String string3) {
//        int n = Integer.parseInt((String)string3);
//        String string4 = string2;
//        if (GLStringUtil.isEmpty((CharSequence)string4) && this.lastSelection != null) {
//            string4 = this.lastSelection;
//        }
//        if (n == 100) {
//            Context context = this.view.getContext().getApplicationContext();
//            ((ClipboardManager)context.getSystemService("clipboard")).setText((CharSequence)string4);
//            String string5 = context.getString(2131624024);
//            String string6 = string5 + ": \n\n" + string4;
//            Toast.makeText((Context)this.view.getContext(), (CharSequence)string6, (int)1).show();
//        } else if (n == 101) {
//            super.sendToSocial(string, string4);
//        }
//        this.view.endSelectMode();
//    }
//
//    /*
//     * WARNING - Removed back jump from a try to a catch block - possible behaviour change.
//     * Enabled aggressive block sorting
//     * Enabled unnecessary exception pruning
//     * Converted monitor instructions to comments
//     * Lifted jumps to return sites
//     */
//    private void processHighlightData(String string) {
//        int n;
//        Double d;
//        Double d2;
//        boolean bl;
//        Double d3;
//        Double d4;
//        ArrayList arrayList;
//        Double d5;
//        JSONArray jSONArray;
//        Annotation annotation;
//        float f = EnhancedWebView.getDevicePixelRatio(this.view.getContext());
//        try {
//            int n2;
//            JSONObject jSONObject = new JSONObject(string);
//            String string2 = jSONObject.getString("annotationId");
//            if (string2 == null || "null".equals((Object)string2)) {
//                string2 = "";
//            }
//            if ((annotation = this.view.getAnnotationForId(string2)) == null) {
//                annotation = new Annotation();
//                annotation.setHighlightsDirty(true);
//                annotation.setHighlightAnalytics(1);
//                annotation.setId(string2);
//                annotation.setAnnotationAnalytics(1);
//                this.view.setActiveAnnotation(annotation, false);
//            }
//            arrayList = new ArrayList();
//            JSONArray jSONArray2 = jSONObject.getJSONArray("highlights");
//            d = null;
//            d2 = null;
//            d5 = null;
//            d4 = null;
//            d3 = null;
//            boolean bl2 = true;
//            for (int i = 0; i < (n2 = jSONArray2.length()); ++i) {
//                Highlight highlight;
//                JSONObject jSONObject2 = jSONArray2.getJSONObject(i).getJSONObject("offsets");
//                String string3 = jSONObject2.getString("uri");
//                GLLog.dev(TAG, "Annotation: " + string2 + " - " + string3);
//                boolean bl3 = Annotation.isValidBlockLevelUri(string3);
//                if (bl2) {
//                    bl2 = bl3;
//                }
//                if ((highlight = annotation.getHighlightForUri(string3)) == null) {
//                    highlight = new Highlight(jSONObject2);
//                } else {
//                    highlight.updateValues(jSONObject2);
//                }
//                arrayList.add((Object)highlight);
//            }
//            jSONArray = jSONObject.getJSONArray("rects");
//            GLLog.dev(TAG, jSONArray.toString());
//            bl = true;
//            n = 0;
//        }
//        catch (JSONException var4_20) {
//            GLLog.e(TAG, "JSON Exception", (Throwable)var4_20);
//            return;
//        }
//        do {
//            int n3;
//            if (n < (n3 = jSONArray.length())) {
//                JSONObject jSONObject = jSONArray.getJSONObject(n);
//                if (jSONObject != null) {
//                    double d6 = jSONObject.getDouble("bottom");
//                    double d7 = jSONObject.getDouble("height");
//                    if (d2 == null || d6 <= d2) {
//                        d2 = d6;
//                        d = jSONObject.getDouble("left");
//                    }
//                    if (d4 == null || d6 >= d4) {
//                        d4 = d6;
//                        d5 = jSONObject.getDouble("right");
//                    }
//                    if (d3 == null || d3 <= d7) {
//                        d3 = d7;
//                    }
//                    if (bl) {
//                        Object object;
//                        annotation.setOffsetTop(jSONObject.getInt("top"));
//                        annotation.setOffsetLeft(jSONObject.getInt("left"));
//                        SortedMap sortedMap = Collections.synchronizedSortedMap((SortedMap)((SortedMap)this.view.rcaOffsetMap));
//                        Object object2 = object = this.view.rcaMapLock;
//                        // MONITORENTER : object2
//                        sortedMap.put((Object)Float.valueOf((float)annotation.getPositionScore()), (Object)annotation.getId());
//                        // MONITOREXIT : object2
//                        {
//                            this.view.reloadRelatedDelayed(500);
//                            bl = false;
//                        }
//                    }
//                }
//            } else {
//                annotation.setHighlights((List<Highlight>)arrayList);
//                if (!annotation.equals((Object)this.view.getActiveAnnotation())) return;
//                this.view.getControls();
//                if (this.view.leftHandle != null && d != null) {
//                    double d8 = 0.15000000596046448 * d3 * (double)f + (double)this.view.getPaddingTop();
//                    double d9 = (double)this.view.leftHandle.getWidth() / 2.0;
//                    int n4 = (int)(d * (double)f - d9);
//                    int n5 = (int)(d2 * (double)f - d8);
//                    this.view.updateHandle(this.view.leftHandle, n4, n5, 0);
//                }
//                if (this.view.rightHandle != null && d5 != null) {
//                    double d10 = 0.15000000596046448 * d3 * (double)f + (double)this.view.getPaddingTop();
//                    double d11 = (double)this.view.rightHandle.getWidth() / 2.0;
//                    int n6 = (int)(d5 * (double)f - d11);
//                    int n7 = (int)(d4 * (double)f - d10);
//                    this.view.updateHandle(this.view.rightHandle, n6, n7, 0);
//                }
//                if (this.view.listener == null) return;
//                this.view.listener.selectionOccurred(annotation);
//                return;
//            }
//            ++n;
//        } while (true);
//    }
//
//    /*
//     * Enabled aggressive block sorting
//     */
//    private void queueTtsText(long l) {
//        this.view.handler.removeCallbacks(super.getQueueTtsTextRunnable());
//        if (TtsManager.getInstanceIfExists() == null) return;
//        {
//            this.ttsTriesToStart = 1 + this.ttsTriesToStart;
//            if (this.ttsTriesToStart < 10) {
//                this.view.handler.postDelayed(super.getQueueTtsTextRunnable(), l);
//                return;
//            } else {
//                if (this.view.listener == null) return;
//                {
//                    GLLog.dev(TAG, "TTS failed to start speaking. Aborting...");
//                    this.view.listener.resetAfterTtsFinished();
//                    return;
//                }
//            }
//        }
//    }
//
//    /*
//     * Enabled aggressive block sorting
//     */
//    private void sendToSocial(String string, final String string2) {
//        boolean bl = "undefined".equals((Object)string);
//        String string3 = null;
//        if (bl) return;
//        String[] arrstring = string.split("\\|");
//        TreeSet treeSet = new TreeSet();
//        for (String string4 : arrstring) {
//            if (GLStringUtil.isEmpty((CharSequence)string4)) continue;
//            treeSet.add((Object)new Uri(string4));
//        }
//        if (treeSet.size() > 1) {
//            string3 = Link.createOneUri(treeSet);
//        } else {
//            boolean bl2 = treeSet.isEmpty();
//            string3 = null;
//            if (bl2) return;
//            string3 = ((Uri)treeSet.first()).getUri();
//        }
//        if (string3 != null) {
//            final String string5 = string3;
//            if (this.view.bookNode != null) {
//                this.view.bookNode.getLanguageId();
//            }
//            this.view.handler.postDelayed((Runnable)new Runnable(){
//
//                public void run() {
//                    if (ContentJsInterface.access$200((ContentJsInterface)ContentJsInterface.this).listener == null) {
//                        return;
//                    }
//                    ShareUtil.showShareDialog((Activity)ContentJsInterface.access$200((ContentJsInterface)ContentJsInterface.this).listener.getContentActivity(), "text/plain", string5, null, string2);
//                }
//            }, 300);
//        }
//    }
//
//    @JavascriptInterface
//    @Override
//    public void jsFinishedRendering(String string) {
//        boolean bl = this.view.isLoaded();
//        super.jsFinishedRendering(string);
//        if (!bl) {
//            Log.d(TAG,"Executing Post Load Actions: " + (Object)this.view);
//            this.view.runHtmlCustomizations();
//            this.view.requestOffsets();
//            if (this.view.listener != null) {
//                this.view.listener.webViewFinishedLoading((EnhancedWebView)this.view);
//            }
//            return;
//        }
//        this.view.restoreSavedScrollPosition();
//    }
//
//    @JavascriptInterface
//    public void jsReportAnnotationPressed(String string) {
//        Annotation annotation;
//        Annotation annotation2 = this.view.getActiveAnnotation();
//        if ("null".equalsIgnoreCase(string)) {
//            string = null;
//        }
//        if ((annotation = this.view.getAnnotationForId(string)) != null) {
//            this.view.isInSelectMode = true;
//            this.view.processAnnotationForDisplay(annotation);
//            this.view.setActiveAnnotation(annotation, false);
//        }
//        if (!((annotation2 == null || annotation != null) && (annotation2 == null || annotation.getId().equals((Object)annotation2.getId())))) {
//            this.view.saveExistingAnnotation(annotation2);
//        }
//    }
//
//    @JavascriptInterface
//    public void jsReportAnnotationRibbonTapped(String string, String string2) {
//        Annotation annotation;
//        if (this.view.listener != null && string != null && string2 != null && (annotation = this.view.getAnnotationForId(string)).getBookmark() != null) {
//            this.view.ribbonTapped(annotation, string2);
//        }
//    }
//
//    @JavascriptInterface
//    public void jsReportAnnotationStickyTapped(String string, String string2) {
//        if ("null".equalsIgnoreCase(string)) {
//            string = null;
//        }
//        final Annotation annotation = this.view.getActiveAnnotation();
//        final Annotation annotation2 = this.view.getAnnotationForId(string);
//        final boolean bl = "true".equalsIgnoreCase(string2);
//        GLLog.dev(TAG, "Sticky tapped: " + string + " - " + string2);
//        this.view.handler.post((Runnable)new Runnable(){
//
//            public void run() {
//                if (!((annotation == null || annotation2 != null) && (annotation == null || annotation2.getId().equals((Object)annotation.getId())))) {
//                    ContentJsInterface.this.view.saveExistingAnnotation(annotation);
//                }
//                ContentJsInterface.this.view.endSelectMode(bl, true);
//                ContentJsInterface.this.view.selectAnnotation(annotation2, true);
//                if (ContentJsInterface.access$200((ContentJsInterface)ContentJsInterface.this).listener != null) {
//                    ContentJsInterface.access$200((ContentJsInterface)ContentJsInterface.this).listener.selectedSticky(annotation2);
//                }
//            }
//        });
//    }
//
//    @JavascriptInterface
//    public void jsReportAnnotationText(final String string, final String string2, final String string3) {
//        this.view.handler.post((Runnable)new Runnable(){
//
//            public void run() {
//                ContentJsInterface.this.processAnnotationText(string, string2, string3);
//            }
//        });
//    }
//
//    @JavascriptInterface
//    public void jsReportBookMarkRibbonUri(String string, String string2) {
//        this.view.ribbonElementTop = Float.valueOf((String)string2).floatValue() * EnhancedWebView.getDevicePixelRatio(this.view.getContext());
//        String string3 = super.fixHeadUri(string);
//        this.view.updateRibbonReference(string3);
//    }
//
//    @JavascriptInterface
//    public void jsReportCurrentAnnotationText(String string) {
//        this.lastSelection = string;
//    }
//
//    @JavascriptInterface
//    public void jsReportDocumentHeadingImage(String string) {
//        GLLog.dev(TAG, "Document heading image: " + string);
//        BookNode bookNode = this.view.getBookNode();
//        if (bookNode != null) {
//            bookNode.setImageUrl(string);
//        }
//    }
//
//    @JavascriptInterface
//    public void jsReportHighlightData(final String string) {
//        this.view.handler.post((Runnable)new Runnable(){
//
//            public void run() {
//                ContentJsInterface.this.processHighlightData(string);
//            }
//        });
//    }
//
//    @JavascriptInterface
//    public void jsReportImageInfo(String string) {
//        long l = System.currentTimeMillis();
//        if (l - this.lastImageTap > 1000) {
//            this.lastImageTap = l;
//            if (this.view.listener != null) {
//                this.view.listener.inlineImageTapped(string);
//            }
//            return;
//        }
//        GLLog.dev(TAG, "Ignoring image tap");
//    }
//
//    @JavascriptInterface
//    public void jsReportInlineVideoInfo(String string) {
//        if (this.view.listener != null) {
//            this.view.listener.inlineVideosInfo(this.view.bookNode, string);
//        }
//    }
//
//    @JavascriptInterface
//    public void jsReportInvalidUri(String string) {
//        GLLog.w(TAG, "Invalid URI: " + string);
//    }
//
//    /*
//     * Exception decompiling
//     */
//    @JavascriptInterface
//    public void jsReportPageOffsets(String var1) {
//        // This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
//        // org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [1[TRYBLOCK]], but top level block is 9[FORLOOP]
//        // org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:392)
//        // org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:444)
//        // org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:2783)
//        // org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:764)
//        // org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:215)
//        // org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:160)
//        // org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:71)
//        // org.benf.cfr.reader.entities.Method.analyse(Method.java:357)
//        // org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:718)
//        // org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:650)
//        // org.benf.cfr.reader.Main.doJar(Main.java:109)
//        // com.njlabs.showjava.AppProcessActivity$4.run(AppProcessActivity.java:415)
//        // java.lang.Thread.run(Thread.java:818)
//        throw new IllegalStateException("Decompilation failed");
//    }
//    @JavascriptInterface
//    public void jsReportSelectionProblem() {
//        GLLog.w(TAG, "Fine-grained text selection not supported on this device");
//        this.view.handleSelectionProblem();
//    }
//
//    /*
//     * Exception decompiling
//     */
//    @JavascriptInterface
//    public void jsReportTtsText(String var1) {
//        // This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
//        // java.util.ConcurrentModificationException
//        // java.util.LinkedList$ReverseLinkIterator.next(LinkedList.java:217)
//        // org.benf.cfr.reader.bytecode.analysis.structured.statement.Block.extractLabelledBlocks(Block.java:212)
//        // org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement$LabelledBlockExtractor.transform(Op04StructuredStatement.java:483)
//        // org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.transform(Op04StructuredStatement.java:600)
//        // org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.insertLabelledBlocks(Op04StructuredStatement.java:610)
//        // org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:774)
//        // org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:215)
//        // org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:160)
//        // org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:71)
//        // org.benf.cfr.reader.entities.Method.analyse(Method.java:357)
//        // org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:718)
//        // org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:650)
//        // org.benf.cfr.reader.Main.doJar(Main.java:109)
//        // com.njlabs.showjava.AppProcessActivity$4.run(AppProcessActivity.java:415)
//        // java.lang.Thread.run(Thread.java:818)
//        throw new IllegalStateException("Decompilation failed");
//    }
//
//    /*
//     * Enabled aggressive block sorting
//     * Enabled unnecessary exception pruning
//     */
//    @JavascriptInterface
//    public void jsReportTtsTextForPid(String string) {
//        Object object;
//        SortedMap sortedMap = Collections.synchronizedSortedMap((SortedMap)((SortedMap)this.view.pidByPosMap));
//        Map map = Collections.synchronizedMap(this.view.textByPidMap);
//        Object object2 = object = this.view.textMapLock;
//        synchronized (object2) {
//            try {
//                JSONObject jSONObject = new JSONObject(string);
//                String string2 = jSONObject.getString("pid");
//                String string3 = jSONObject.getString("text");
//                Point point = new Point(jSONObject.getInt("left"), jSONObject.getInt("top"));
//                StringBuilder stringBuilder = new StringBuilder().append("").append(point.y).append(".");
//                Object[] arrobject = new Object[]{point.x};
//                Float f = Float.valueOf((float)Float.parseFloat((String)stringBuilder.append(String.format((String)"%05d", (Object[])arrobject)).toString()));
//                map.put((Object)string2, (Object)string3);
//                sortedMap.put((Object)f, (Object)string2);
//            }
//            catch (Exception var7_13) {
//                GLLog.e(TAG, "Unable to parse results of ttsArray:" + string, (Throwable)var7_13);
//            }
//            return;
//        }
//    }
//
//    /*
//     * Enabled aggressive block sorting
//     * Enabled unnecessary exception pruning
//     * Converted monitor instructions to comments
//     * Lifted jumps to return sites
//     */
//    @JavascriptInterface
//    public void jsReportUriOffsets(String string) {
//        JSONArray jSONArray;
//        Object object;
//        SortedMap sortedMap = Collections.synchronizedSortedMap((SortedMap)((SortedMap)this.view.uriOffsetMap));
//        Object object2 = object = this.view.uriMapLock;
//        // MONITORENTER : object2
//        sortedMap.clear();
//        try {
//            jSONArray = new JSONArray(string);
//        }
//        catch (Exception var7_8) {
//            GLLog.e(TAG, "Unable to parse results of reportUriOffsets:" + string, (Throwable)var7_8);
//            return;
//        }
//        for (int i = 0; i < jSONArray.length(); ++i) {
//            JSONObject jSONObject = jSONArray.getJSONObject(i);
//            sortedMap.put((Object)Float.valueOf((float)jSONObject.getInt("top")), (Object)jSONObject.getString("id"));
//        }
//        // MONITOREXIT : object2
//    }
//
//    @JavascriptInterface
//    public void jsReportVideoTapped(String string) {
//        long l = System.currentTimeMillis();
//        if (l - this.lastVideoTap > 1000) {
//            this.lastVideoTap = l;
//            if (this.view.listener != null) {
//                this.view.listener.inlineVideoTapped(this.view.bookNode, string);
//            }
//            return;
//        }
//        GLLog.dev(TAG, "Ignoring video tap");
//    }
//
//    /*
//     * Failed to analyse overrides
//     */
//    private static class QueueTtsTextRunnable
//    implements Runnable {
//        private ContentJsInterface jsInterface;
//        private ContentWebView view;
//
//        public QueueTtsTextRunnable(ContentWebView contentWebView, ContentJsInterface contentJsInterface) {
//            this.view = contentWebView;
//            this.jsInterface = contentJsInterface;
//        }
//
//        /*
//         * Enabled aggressive block sorting
//         * Enabled unnecessary exception pruning
//         */
//        public void run() {
//            ContentActivity contentActivity;
//            Object object;
//            if (this.view.listener == null || (contentActivity = this.view.listener.getContentActivity()) == null || contentActivity.isDestroyed()) {
//                return;
//            }
//            TtsManager ttsManager = TtsManager.getInstanceIfExists();
//            if (!(ttsManager != null && ttsManager.isReady())) {
//                GLLog.dev(TAG, "TTS not ready. Retrying in 100ms...");
//                this.jsInterface.queueTtsText(100);
//                return;
//            }
//            SortedMap sortedMap = Collections.synchronizedSortedMap((SortedMap)((SortedMap)this.view.pidByPosMap));
//            Map map = Collections.synchronizedMap(this.view.textByPidMap);
//            Object object2 = object = this.view.textMapLock;
//            synchronized (object2) {
//                boolean bl = true;
//                String string = this.view.getFirstVisiblePidItem();
//                Iterator iterator = sortedMap.values().iterator();
//                while (iterator.hasNext()) {
//                    String string2 = (String)iterator.next();
//                    String string3 = (String)map.get((Object)string2);
//                    if (string != null && !string2.equals((Object)string) || GLStringUtil.isEmpty((CharSequence)string3)) continue;
//                    ttsManager.queue(string3, string2);
//                    string = null;
//                    if (!bl) continue;
//                    this.view.listener.textToSpeechIsSpeakingPid(string2);
//                    bl = false;
//                    string = null;
//                }
//                return;
//            }
//        }
//    }
//
}

