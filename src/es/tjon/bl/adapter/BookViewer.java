package es.tjon.bl.adapter;
import android.text.*;
import android.webkit.*;
import es.tjon.bl.*;
import es.tjon.bl.fragment.*;
import java.util.*;
import android.view.*;
import android.view.View.*;
import es.tjon.bl.listener.*;
import android.app.*;
import android.content.*;
import android.util.*;
import org.restlet.engine.header.*;
import android.graphics.*;
import android.widget.*;

public class BookViewer extends WebView
{
	
	private static final String TAG = "es.tjon.bl.adapter.BookViewer";

	private BookInterface mActivity;
	private boolean mScrolled=false;

	public NavigableMap<Float,String> rcaOffsetMap;
	public NavigableMap<Float,String> uriOffsetMap;
	public NavigableMap<String,Float> uriOffsetLookupMap;
	
	Toast toast;
	
	public BookViewer(BaseActivity context)
	{
		this(context, null);
	}
	
	public BookViewer(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		toast = Toast.makeText(context,"",Toast.LENGTH_SHORT);
		mActivity=(BookInterface) context;
		getSettings().setJavaScriptEnabled(true);
		String cs = ((BaseActivity)context).getColorScheme();
		if(cs.equalsIgnoreCase("Night"))
			setBackgroundColor(Color.BLACK);
		if(cs.equalsIgnoreCase("Sepia"))
			setBackgroundColor(Color.rgb(250,230,175));
		setWebChromeClient(new WebChromeClient()
		{
			public boolean onConsoleMessage(ConsoleMessage message)
			{
				Log.d(TAG,message.message());
				return true;
			}
		});
		addJavascriptInterface(new ContentJsInterface(this),"mainInterface");
	}

	public void onFinishRender(String string)
	{
		((Activity)getContext()).runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					BookViewer.this.loadUrl("javascript:ldssa.main.getOffsetsForRcaItems();ldssa.main.getOffsetsForUris();");
				}
			
		});
	}
	
	
	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt)
	{
		if(mScrolled)
		{
			mScrolled=false;
			return;
		}
		if(Math.abs(t-oldt)>0&&getContentHeight()!=0&&getWidth()!=0)
		{
			if(t==0)
			{
				mActivity.scrollTo(null,0,0);
				super.onScrollChanged(l,t,oldl,oldt);
				return;
			}
			if(uriOffsetMap!=null)
			{
				double center = ((double)t)/(getContentHeight()*2.0-getHeight());
				if(center>1)
					center=1;
				if(center<0)
					center=0;
				
				center*=getHeight();
				if(center>getHeight())
					center=getHeight();
				center=(int)center;
				final double POWER = 4;
				if(center/getHeight()<0.5)
				{
					double pre = center;
					center = Math.pow(2.0*center/getHeight(),1.0/POWER)*getHeight()/2.0;
				}
				else
				{
					center = (1.5-Math.pow(1-(2.0*(center/getHeight()-0.5)),1/POWER))*getHeight();
				}
				center=(int)center;
				double centerT=(t+center);
				Map.Entry<Float,String> top = uriOffsetMap.floorEntry((float)(centerT));
				Map.Entry<Float,String> bottom = uriOffsetMap.ceilingEntry((float)(centerT+1));
				String url=null;
				float topScroll=0;
				if(top!=null)
				{
					url= top.getValue();
					topScroll=top.getKey();
				}
				float bottomScroll = getContentHeight()*2;
				if(bottom!=null)
				{
					bottomScroll = bottom.getKey();
				}
				float scroll = ((float)(centerT)-topScroll)/(bottomScroll-topScroll);
				mActivity.scrollTo(url,center,scroll);
			}
			
		
			
		}
		super.onScrollChanged(l, t, oldl, oldt);
	}

	public void scrollTo(double center, double y)
	{
		int t = (int)(y*getContentHeight()-center);
		if(t<0)
			t=0;
		if(t>getContentHeight()*2)
			t=getContentHeight()*2;
		mScrolled=true;
		setScrollY(t);
	}
	
	public void scrollTo(String uri, Double center, float scroll)
	{
		if(uriOffsetMap==null)
			return;
		float top = 0;
		if(uri!=null)
			top = uriOffsetLookupMap.floorEntry(uri).getValue();
		Float bottom = uriOffsetMap.ceilingKey(top+1);
		if(bottom==null||bottom<=0)
			bottom=new Float(getContentHeight()*2);
		double t = top+((bottom-top)*scroll);
		if(t<0)
			t=0;
		mScrolled=true;
		setScrollY((int)(t-center));
	}
	
	public void requestScrollToUri(String uri, boolean highlight) {
        uri = uri.replace(BookFragment.BASE_URL,"");
		String[] parts = uri.split("\\.", 2);
        if(parts.length<2)
			return;
        String verse = parts[1];
        int end = verse.indexOf(35);
        if (end > 0) {
            verse = verse.substring(0, end);
        }
        String[] refParts = verse.split("\\.|,");
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        if (refParts.length > 0) {
            for (int i = 0; i < refParts.length; ++i) {
                getVersesToBeHighlighted(set, refParts[i]);
            }
        }
        if(set.isEmpty())
			return;
        String[] verses = new String[set.size()];
        set.toArray(verses);
        uri = parts[0] + "." + verses[0];
		String script = "javascript:ldssa.main.scrollToElementUri('" + uri + "')";
        if (verses != null && highlight) {
            script = "javascript:ldssa.main.scrollToElementUri('" + uri + "', '" + TextUtils.join(",", verses) + "')";
        }
        loadUrl(script);
    }

	private void getVersesToBeHighlighted(Set<String> set, String reference) {
        String[] arrstring = reference.split("-");
        if (arrstring.length > 1) {
            int start=0;
            int end = 0;
            try {
                start = Integer.parseInt(arrstring[0]);
                end = Integer.parseInt(arrstring[1]);
            }
            catch (Exception e) {
				e.printStackTrace();
            }
            if (start <= 0) return;
            if (end <= 0) return;
            {
                for (int i = start; i <= end; ++i) {
                    set.add(String.valueOf((i)));
                }
                return;
            }
        }
        set.add(reference);
    }
	
	public String findFirstRCAItem()
	{
		if(rcaOffsetMap==null)
			return null;
		float top = ((float)this.getScrollY() / getDevicePixelRatio(getContext()));
		if(rcaOffsetMap.ceilingEntry(top)==null)
			return null;
		return rcaOffsetMap.ceilingEntry(top).getValue();
	}

	private float getDevicePixelRatio(Context context)
	{
        return context.getResources().getDisplayMetrics().density;
	}
	
}
