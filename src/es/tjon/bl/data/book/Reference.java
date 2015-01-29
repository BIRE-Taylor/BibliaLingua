package es.tjon.bl.data.book;

import com.mobandme.ada.*;
import com.mobandme.ada.annotations.*;
import android.text.*;

@Table(name="ref")
public class Reference extends Entity
{
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
		html+="&nbsp;";
		html+=link_name;
		html+="</span>&nbsp;&dash;&nbsp;";
		html+=ref;
		return html;
	}

	public Spanned getSpan()
	{
		return Html.fromHtml(getHtml());
	}
}
