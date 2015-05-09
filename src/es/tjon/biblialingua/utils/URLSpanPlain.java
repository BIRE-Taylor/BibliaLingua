package es.tjon.biblialingua.utils;
import android.text.style.*;
import android.text.*;

public class URLSpanPlain extends URLSpan
{
	public URLSpanPlain(String url)
	{
		super(url);
	}

	@Override
	public void updateDrawState(TextPaint ds)
	{
		super.updateDrawState(ds);
		ds.setUnderlineText(false);
	}
}
