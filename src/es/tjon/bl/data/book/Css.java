package es.tjon.bl.data.book;
import com.mobandme.ada.*;
import com.mobandme.ada.annotations.*;

@Table(name="css")
public class Css extends Entity
{
	@TableField(name="_id", datatype=DATATYPE_LONG)
	public Long _id=null;
	@TableField(name="css", datatype=DATATYPE_STRING)
	public String css=null;
	@TableField(name="_file", datatype=DATATYPE_STRING)
	public String _file=null;

	@Override
	public String toString()
	{
		return css;
	}
	
	
}
