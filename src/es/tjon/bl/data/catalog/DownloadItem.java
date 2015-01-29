package es.tjon.bl.data.catalog;

import com.mobandme.ada.*;
import com.mobandme.ada.annotations.*;

@Table(name="DownloadItem")
public class DownloadItem extends Entity
{
	@TableField(name="book", datatype=DATATYPE_ENTITY_LINK)
	public Book item;
	@TableField(name="time", datatype=DATATYPE_LONG, unique=true)
	public long time;
	
	public DownloadItem()
	{}
	
	public DownloadItem(Book item)
	{
		this.item = item;
		time = System.currentTimeMillis();
	}
}
