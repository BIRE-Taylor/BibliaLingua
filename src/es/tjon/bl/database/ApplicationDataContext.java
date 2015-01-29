package es.tjon.bl.database;
import android.content.*;
import com.mobandme.ada.*;
import com.mobandme.ada.exceptions.*;
import es.tjon.bl.data.catalog.*;
import java.util.*;

import com.mobandme.ada.Entity;
import android.database.sqlite.*;

public class ApplicationDataContext extends ObjectContext
{
	public ObjectSet<DownloadItem> downloadQueue;
	
	public ObjectSet<Language> languages;

	public ObjectSet<Catalog> catalog;
	
	public ObjectSet<Folder> folders;
	
	public ObjectSet<Book> books;
	
	public ApplicationDataContext(Context context) throws AdaFrameworkException
	{
		super(context);
		
		downloadQueue = new ObjectSet<DownloadItem>(DownloadItem.class, this);
		
		languages = new ObjectSet<Language>(Language.class, this);
		
		catalog = new ObjectSet<Catalog>(Catalog.class, this);
		
		folders = new ObjectSet<Folder>(Folder.class, this);
		
		books = new ObjectSet<Book>(Book.class,  this);
		
	}

	public boolean hasDownload(DownloadItem item)
	{
		try
		{
			downloadQueue.fill("ID IN(SELECT DownloadItem_ID FROM LINK_DownloadItem_book_Book WHERE Book_ID=?)", new String[]{"" + item.item.getID()}, "time");
			if(downloadQueue.size()>0)
				return true;
		}
		catch (AdaFrameworkException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	public void downloadComplete(Book item)
	{
		try
		{
			downloadQueue.fill("ID IN(SELECT DownloadItem_ID FROM LINK_DownloadItem_book_Book WHERE Book_ID=?)", new String[]{"" + item.getID()}, "time");
			for(DownloadItem di : downloadQueue)
			{
				di.setStatus(Entity.STATUS_DELETED);
			}
			downloadQueue.save();
		}
		catch (AdaFrameworkException e)
		{}
	}
	
	public Catalog getCatalog(Language language)
	{
		try
		{
			catalog.fill("ID IN(SELECT Catalog_ID FROM LINK_Catalog_c_language_Language WHERE Language_ID=?)", new String[]{language.getID().toString()}, null);
			return catalog.get(0);
		}
		catch (AdaFrameworkException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public Language getLanguage(String languageId)
	{
		try
		{
			List<Language> langs = languages.search(null, "l_id=?", new String[]{languageId}, null, null, null, null, null);
			if(langs!=null&&langs.size()>0)
				return langs.get(0);
		}
		catch (AdaFrameworkException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public Book[] getBooks(Language language, long parentId)
	{
		if(language==null||parentId<0)
			return null;
		books.clear();
		try
		{
			books.fill("ID IN(SELECT Book_ID FROM LINK_Book_b_folder_Folder WHERE Folder_ID=?) AND ID IN(SELECT Book_ID FROM LINK_Book_b_language_Language WHERE Language_ID=?)", new String[]{new Long(parentId).toString(),language.getID().toString()}, "b_display_order");
			
		}
		catch (AdaFrameworkException e)
		{
			e.printStackTrace();
			return null;
		}
		return books.toArray(new Book[books.size()]);
	}
	
	public Book getBook(Language language, String uri)
	{
		if(uri==null||language==null)
			return null;
		books.clear();
		try
		{
			//books.fill(" instr(?,b_gl_uri) AND ID IN(SELECT Book_ID FROM LINK_Book_b_language_Language WHERE Language_ID=?)", new String[]{uri,language.getID().toString()}, "LENGTH(b_gl_uri) DESC");
			books.fill(" b_gl_uri=? AND ID IN(SELECT Book_ID FROM LINK_Book_b_language_Language WHERE Language_ID=?)", new String[]{uri,language.getID().toString()}, "LENGTH(b_gl_uri) DESC");
			if(books.isEmpty()&&!uri.isEmpty())
			{
				int i = uri.lastIndexOf("/");
				if(i<0)
					return null;
				return getBook(language, uri.substring(0,i));
			}
		}
		catch (AdaFrameworkException e)
		{
			e.printStackTrace();
			return null;
		}
		if(books==null||books.size()<1)
			return null;
		return books.get(0);
	}

	public Folder[] getFolders(Language language, long parentId)
	{
		if(language==null||parentId<0)
			return null;
		folders.clear();
		try
		{
			folders.fill("f_folder=? AND ID IN(SELECT Folder_ID FROM LINK_Folder_f_language_Language WHERE Language_ID=?)", new String[]{new Long(parentId).toString(),language.getID().toString()}, "f_display_order");
		}
		catch (AdaFrameworkException e)
		{
			e.printStackTrace();
			return null;
		}
		return folders.toArray(new Folder[folders.size()]);
	}
	
	public Folder getFolder(int folder)
	{
		try
		{
			List<Folder> search = folders.search(null, "f_id=?", new String[]{new Integer(folder).toString()}, null, null, null, null, null);
			if(search!=null&&search.size()>0)
				return search.get(0);
		}
		catch (AdaFrameworkException e)
		{}
		return null;
	}
}