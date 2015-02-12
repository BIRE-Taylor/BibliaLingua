package es.tjon.biblialingua.database;
import com.mobandme.ada.*;
import android.content.*;
import es.tjon.biblialingua.data.catalog.*;
import es.tjon.biblialingua.data.book.*;
import com.mobandme.ada.exceptions.*;
import es.tjon.biblialingua.utils.*;
import java.util.*;

public class BookDataContext extends ObjectContext
{
	private Book mBook;
	
	public ObjectSet<Node> nodes;
	public ObjectSet<Css> css;
	public ObjectSet<Reference> refs;
	
	public BookDataContext(Context context, Book book)
	{
			
		super(context, BookUtil.getFile(book,context).getAbsolutePath(),1001);
		mBook = book;
		try
		{
			nodes = new ObjectSet<Node>(Node.class, this);
			css = new ObjectSet<Css>(Css.class, this);
			refs = new ObjectSet<Reference>(Reference.class, this);
		}
		catch (AdaFrameworkException e)
		{
			e.printStackTrace();
		}
	}

	public CharSequence getUri()
	{
		return mBook.gl_uri;
	}
	
	public List<Reference> getRefsByUrl(String uri)
	{
		try
		{
			while(uri.contains("."))
				uri = uri.substring(0,uri.lastIndexOf("."));
			List<Reference> refsFound = refs.search(true, new String[]{"_id","uri","node_id","ref_name","link_name","ref"}, "uri=?", new String[]{uri}, "_id", null, null, null, null);
			if(refsFound!=null&&refsFound.size()>0)
			{
				return refsFound;
			}
			else
			{
				if((refsFound==null||refsFound.isEmpty())&&!uri.isEmpty())
				{
					return getRefsByUrl( uri.substring(0,uri.lastIndexOf("/")));
				}
				return null;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public Node getNodeByUrl(String uri)
	{
		if(uri==null)
			return null;
		try
		{
			while(uri.contains("."))
				uri = uri.substring(0,uri.lastIndexOf("."));
			//List<Node> nodesFound = nodes.search(true, "instr(?,uri)", new String[]{uri}, "LENGTH(uri) DESC", null, null, null, 1);
			List<Node> nodesFound = nodes.search(true, "uri=?", new String[]{uri}, "LENGTH(uri) DESC", null, null, null, 1);
			if(nodesFound!=null&&nodesFound.size()>0)
			{
				return nodesFound.get(0);
			}
			else
			{
				if((nodesFound==null||nodesFound.isEmpty())&&!uri.isEmpty())
				{
					return getNodeByUrl( uri.substring(0,uri.lastIndexOf("/")));
				}
				return null;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public String[] getCss()
	{
		String[] csss=null;
		try
		{
			List<Css> result = css.search(true,new String[]{"css"},null,null,"css ASC",null,null,null,null);
			csss=new String[result==null?0:result.size()];
			int i=0;
			if(result==null)
				return csss;
			for(Css one: result)
			{
				csss[i]=one.toString();
				i++;
			}
		}
		catch (AdaFrameworkException e)
		{}
		return csss;
	}
}
