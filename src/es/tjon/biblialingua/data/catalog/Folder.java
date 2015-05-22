package es.tjon.biblialingua.data.catalog;
import com.mobandme.ada.*;
import com.mobandme.ada.annotations.*;
import es.tjon.biblialingua.database.*;
import java.util.*;
import com.mobandme.ada.exceptions.AdaFrameworkException;
import android.content.Context;
import android.util.Log;

@Table(name="Folder")
public class Folder extends Entity implements CatalogItem
{
	
	public static final String TAG = "es.tjon.biblialingua.data.catalog.Folder";

	public boolean contains(Book book)
	{
		if(books.contains(book))
			return true;
		return false;
	}

	@Override
	public String getCoverURL()
	{
		return cover_art;
	}


	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public int getId()
	{
		return id;
	}
	
	
	public ArrayList<Folder> folders;
	public ArrayList<Book> books;
	@TableField(name="f_name",datatype=DATATYPE_TEXT)
	public String name = null;
	@TableField(name="f_language",datatype=DATATYPE_ENTITY_LINK)
	public Language language = null;
	@TableField(name="f_display_order",datatype=DATATYPE_INTEGER)
	public int display_order = -1;
	@TableField(name="f_eng_name",datatype=DATATYPE_TEXT)
	public String eng_name = null;
	@TableField(name="f_download_all",datatype=DATATYPE_INTEGER)
	public int download_all = -1;
	@TableField(name="f_id",datatype=DATATYPE_INTEGER)
	public int id = -1;
	public int languageid = -1;
	@TableField(name="f_daysexpire",datatype=DATATYPE_INTEGER)
	public int daysexpire = -1;
	@TableField(name="f_catalog",datatype=DATATYPE_ENTITY_LINK)
	public Catalog catalog;
	@TableField(name="f_folder",datatype=DATATYPE_INTEGER)
	public int folder=0;
	@TableField(name="f_cover_art",datatype=DATATYPE_TEXT)
	public String cover_art=null;

	private boolean mSelected;

	public boolean isSelected()
	{
		return mSelected;
	}

	public void setSelected(boolean selected)
	{
		mSelected=selected;
	}

	public void saveAll(ApplicationDataContext adc)
	{
		adc.folders.addAll(folders);
		adc.books.addAll(books);
		for(Folder folder:folders)
		{
			folder.saveAll(adc);
		}
		if(books!=null&&books.size()>0)
		{
			for(Book book : books)
			{
				if(book.cover_art!=null&&!book.cover_art.isEmpty())
				{
					cover_art=book.cover_art;
					break;
				}
			}
		}
		if(cover_art==null||cover_art.isEmpty())
		{
			for(Folder folder : folders)
			{
				if(folder.cover_art!=null&&!folder.cover_art.isEmpty())
				{
					cover_art=folder.cover_art;
					break;
				}
			}
		}
	}

	public void setup(ApplicationDataContext adc,Context context)
	{
		int i = 0;
		for(Folder folder:folders)
		{
			folder.folder=this.id;
			folder.language=language;
			folder.setStatus(STATUS_NEW);
			folder.display_order=i;
			folder.setup(adc,context);
			i++;
		}
		for(Book book:books)
		{
			book.folder=this;
			book.language=language;
			book.display_order=i;
			book.setStatus(STATUS_NEW);
			book.setup(adc,context);
			i++;
		}
	}


	public void update(Folder folder, ApplicationDataContext adc, Context context)
	{
		name = folder.name;
		language = folder.language;
		display_order = folder.display_order;
		eng_name = folder.eng_name;
		daysexpire=folder.daysexpire;
		if(folders==null||folders.size()==0)
		{
			folders = adc.getFolders(language,getID());
		}
		ArrayList<Folder> newFolders = new ArrayList<Folder>();
		int i = 0;
		for (Folder newFolder : folder.folders)
		{
			newFolder.language=language;
			newFolder.display_order=i;
			Folder found = null;
			for(Folder oldFolder: folders)
			{
				if(oldFolder.id==newFolder.id)
				{
					found = oldFolder;
					found.language=language;
					found.update(newFolder,adc,context);
					break;
				}
			}
			if(found!=null)
			{
				folders.remove(found);
				found.setStatus(STATUS_UPDATED);
				newFolders.add(found);
			}
			else
			{
				newFolder.folder=this.id;
				newFolder.language=language;
				newFolder.setStatus(STATUS_NEW);
				newFolders.add(newFolder);
				newFolder.setup(adc,context);
			}
			i++;
		}
		for(Folder extra : folders)
		{
			extra.display_order=i;
			newFolders.add(extra);
			i++;
		}
		folders = newFolders;
		if(books==null||books.size()==0)
		{
			books = new ArrayList<Book>(adc.getBooks(language,getID()));
		}
		ArrayList<Book> newBooks = new ArrayList<Book>();
		for (Book book : folder.books)
		{
			book.display_order=i;
			book.language=language;
			Book found = null;
			for(Book oldBook: books)
			{
				if(oldBook.id==book.id)
				{
					found = oldBook;
					found.language=language;
					found.update(book,adc,context);
					break;
				}
			}
			if(found!=null)
			{
				books.remove(found);
				found.setStatus(STATUS_UPDATED);
				newBooks.add(found);
			}
			else
			{
				book.folder=this;
				book.language=language;
				book.setStatus(STATUS_NEW);
				newBooks.add(book);
				book.setup(adc,context);
			}
			i++;
		}
		for(Book extra : books)
		{
			extra.display_order=i;
			newBooks.add(extra);
			i++;
		}
		books = newBooks;
	}
}
