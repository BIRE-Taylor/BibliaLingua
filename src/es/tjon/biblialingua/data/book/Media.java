package es.tjon.biblialingua.data.book;
import com.mobandme.ada.*;
import com.mobandme.ada.annotations.*;

@Table(name="media")
public class Media extends Entity
{
	
	public static String TYPE_AUDIO_MP3 = "mp3";
	public static String TYPE_VIDEO_MP4 = "mp4";
	public static String TYPE_VIDEO_MP3U8 = "mp3u8";
	
	@TableField(name="id",datatype=DATATYPE_INTEGER)
	public int id;
	@TableField(name="type",datatype=DATATYPE_TEXT)
	public String type;
	@TableField(name="uri",datatype=DATATYPE_TEXT)
	public String uri;
	@TableField(name="link",datatype=DATATYPE_TEXT)
	public String link;
	@TableField(name="language_id",datatype=DATATYPE_INTEGER)
	public int language_id;
	@TableField(name="inline_id",datatype=DATATYPE_TEXT)
	public String inline_id;
	@TableField(name="size",datatype=DATATYPE_INTEGER)
	public int size;
	@TableField(name="width",datatype=DATATYPE_INTEGER)
	public int width;
	@TableField(name="height",datatype=DATATYPE_INTEGER)
	public int height;
	@TableField(name="name",datatype=DATATYPE_TEXT)
	public String name;
}
