package es.tjon.biblialingua.data.book;

import com.mobandme.ada.*;
import com.mobandme.ada.annotations.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import es.tjon.biblialingua.utils.*;

@Table(name="ref")
public class Reference extends Entity
{
	private static final String TAG = "es.tjon.biblialingua.data.book.Reference";
	
	@TableField(name="_id", datatype=DATATYPE_INTEGER)
	public int _id;
	@TableField(name="uri", datatype=DATATYPE_STRING)
	public String uri;
	@TableField(name="node_id", datatype=DATATYPE_INTEGER)
	public int node_id;
	@TableField(name="ref_name", datatype=DATATYPE_STRING)
	public String ref_name;
	@TableField(name="link_name", datatype=DATATYPE_STRING)
	public String link_name;
	@TableField(name="ref", datatype=DATATYPE_STRING)
	public String ref;

	public String getHtml()
	{
		String html="<span>";
		html+=ref_name;
		if(!ref_name.contains(link_name))
			html+="&nbsp;"+link_name;
		html+="</span>&nbsp;&nbsp;";
		html+=ref;
		return html;
	}

	public Spanned getSpan()
	{
		SpannableStringBuilder builder = SpannableStringBuilder.valueOf(Html.fromHtml(getHtml()));
		for(URLSpan uSpan : builder.getSpans(0,builder.length()-1,URLSpan.class))
		{
			URLSpanPlain newSpan =  new URLSpanPlain( uSpan.getURL());
			int start = builder.getSpanStart(uSpan);
			int end = builder.getSpanEnd(uSpan);
			builder.removeSpan(uSpan);
			builder.setSpan(newSpan,start,end,0);
		}
		return builder;
	}
}
