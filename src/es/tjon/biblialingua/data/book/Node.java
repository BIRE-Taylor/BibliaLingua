package es.tjon.biblialingua.data.book;
import android.content.*;
import android.os.*;
import com.mobandme.ada.*;
import com.mobandme.ada.annotations.*;
import es.tjon.biblialingua.*;
import es.tjon.biblialingua.utils.*;

import com.mobandme.ada.Entity;

@Table(name="node")
public class Node extends Entity implements Comparable<Node>, Parcelable
{

	@Override
	public int compareTo(Node node)
	{
		if(equals(node))
			return 0;
		return uri.compareTo(node.uri);
	}
	
	public boolean equals(Object o)
	{
		if(o!=null&&o instanceof Node)
			return uri.equals(((Node)o).uri);
		else return false;
	}
	
	@Override
	public Long getID()
	{
		return id;
	}
	
	@TableField(name="id", datatype=DATATYPE_INTEGER)
	public long id;
	
	@TableField(name="doc_version", datatype=DATATYPE_INTEGER)
	public int doc_version;
	
	@TableField(name="parent_id", datatype=DATATYPE_LONG)
	public long parent_id;
	
	@TableField(name="book_id", datatype=DATATYPE_INTEGER)
	public int book_id;
	
	@TableField(name="content_id", datatype=DATATYPE_INTEGER)
	public int content_id;
	
	@TableField(name="language_id", datatype=DATATYPE_INTEGER)
	public int language_id;
	
	@TableField(name="display_order", datatype=DATATYPE_INTEGER)
	public int display_order;
	
	@TableField(name="title", datatype=DATATYPE_STRING)
	public String title;
	
	@TableField(name="subtitle", datatype=DATATYPE_STRING)
	public String subtitle;
	
	@TableField(name="short_title", datatype=DATATYPE_STRING)
	public String short_title;
	
	@TableField(name="section_name", datatype=DATATYPE_STRING)
	public String section_name;
	
	@TableField(name="uri", datatype=DATATYPE_STRING)
	public String uri;
	
	@TableField(name="content", datatype=DATATYPE_STRING)
	public String content;

	public String getContent()
	{
		return content;
	}
	
	public static String staticGenerateHtmlText(Context context, String string, String title, String content, String css, Integer n, boolean hideFootnotes) {
        String contentFinal;
        StringBuilder stringBuilder = new StringBuilder();
        boolean black = ((BaseActivity)context).getColorScheme().equals("Night");
        boolean sepia = ((BaseActivity)context).getColorScheme().equals("Sepia");
        boolean serif = true; //SettingsActivity.getIsSerifFontFromPrefs(context);
        //SettingsActivity.getIsHideFootnotesFromPrefs(context);
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML Basic 1.1//EN\"\n    \"http://www.w3.org/TR/xhtml-basic/xhtml-basic11.dtd\">");
        stringBuilder.append("\n<html>\n<head>\n<title>");
        stringBuilder.append(title);
        stringBuilder.append("</title>");
        stringBuilder.append("\n<meta charset=\"UTF-8\">");
        stringBuilder.append("\n<meta name=\"viewport\" content=\"initial-scale=1.0,maximum-scale=1.0\"/>");
        stringBuilder.append("\n<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/style_base.css\"/>");
        stringBuilder.append("\n<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/style_screenPresentation.css\"/>");
        stringBuilder.append("\n<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/style_androidCustom.css\"/>");
        if (serif) {
            stringBuilder.append("\n<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/style_androidSerif.css\"/>");
        } else {
            stringBuilder.append("\n<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/style_androidSansSerif.css\"/>");
        }
        if (css!=null&&!css.isEmpty()) {
            stringBuilder.append("\n<style type=\"text/css\">\n/*<![CDATA[*/");
            stringBuilder.append(css);
            stringBuilder.append("/*]]>*/\n</style>\n");
        }
        stringBuilder.append("\n<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/style_androidOverrides.css\"/>");
        stringBuilder.append("\n<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/style_finishRender.css\"/>");
        stringBuilder.append("\n<script type=\"text/javascript\" src=\"file:///android_asset/javascript/ldssa.finish_render.js\" ></script>");
        String color = black ? "night" : (sepia ? "sepia" : "default");
        String footnotes = hideFootnotes ? "hidden" : "visible";
        stringBuilder.append("\n</head><body scheme=\"").append(color).append("\"");
        stringBuilder.append("page-numbers=\"visible\" summaries=\"visible\" footnotes=\"").append(footnotes).append("\">");
        if (content==null||content.isEmpty()) {
            contentFinal = title;
        } else {
            String imageBase = ImageUtil.getBaseImageUrl();
            content = content.replaceAll("__ORIGIN_PATH__", imageBase);
            String poster = Build.VERSION.SDK_INT >= 14 ? "<video poster=\"file:///android_asset/poster.png\">" : "<video poster=\"https://dl.dropboxusercontent.com/u/4053974/poster.png\">";
            contentFinal = content.replaceAll("<video[^>]*>", poster).replaceAll("<source ", "<source-no-preload ");
        }
        stringBuilder.append(contentFinal);
        stringBuilder.append("\n<div id=\"transitionElement\"></div>");
        stringBuilder.append("\n<script type=\"text/javascript\" src=\"file:///android_asset/javascript/json2.js\" ></script>");
        stringBuilder.append("\n<script type=\"text/javascript\" src=\"file:///android_asset/javascript/lds.selection.js\" ></script>");
        stringBuilder.append("\n<script type=\"text/javascript\" src=\"file:///android_asset/javascript/ldssa.main.js\" ></script>");
        if (black) {
            stringBuilder.append("\n<script type=\"text/javascript\">\n<!--\nldssa.main.setNightMode(true);\n// -->\n</script>\n");
        }
        stringBuilder.append("</body></html>");
        return stringBuilder.toString();
    }

    /*
     * Enabled aggressive block sorting
     */
    public static String staticGenerateHtmlText(Context context,String[] csss, Node bookNode, boolean hideFootnotes) {
        if (context == null) {
            return null;
        }
        String css = null;
        if (bookNode != null) {
            String[] arrstring = csss;
            if (arrstring != null) {
                StringBuilder stringBuilder = new StringBuilder();
                int n2 = arrstring.length;
                for (int i = 0; i < n2; ++i) {
                    stringBuilder.append(arrstring[i]);
                    stringBuilder.append("\n");
                }
                css = stringBuilder.toString();
            }
        }
		else
			return null;
        return staticGenerateHtmlText(context, bookNode.uri, bookNode.title, bookNode.content, css, bookNode.language_id, hideFootnotes);
	}
	
	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags)
	{
		parcel.writeLong(id);
		parcel.writeInt(doc_version);
		parcel.writeLong(parent_id);
		parcel.writeInt(book_id);
		parcel.writeInt(content_id);
		parcel.writeInt(language_id);
		parcel.writeInt(display_order);
		parcel.writeString(title);
		parcel.writeString(subtitle);
		parcel.writeString(short_title);
		parcel.writeString(section_name);
		parcel.writeString(uri);
		parcel.writeString(content);
	}
	
	static final Parcelable.Creator<Node> CREATOR = new Parcelable.Creator<Node>()
	{

		@Override
		public Node createFromParcel(Parcel parcel)
		{
			Node node = new Node();
			node.id = parcel.readLong();
			node.doc_version = parcel.readInt();
			node.parent_id = parcel.readLong();
			node.book_id = parcel.readInt();
			node.content_id = parcel.readInt();
			node.language_id = parcel.readInt();
			node.display_order = parcel.readInt();
			node.title = parcel.readString();
			node.subtitle = parcel.readString();
			node.short_title = parcel.readString();
			node.section_name = parcel.readString();
			node.uri = parcel.readString();
			node.content = parcel.readString();
			return node;
		}

		@Override
		public Node[] newArray(int count)
		{
			return new Node[count];
		}
		
	};
}
