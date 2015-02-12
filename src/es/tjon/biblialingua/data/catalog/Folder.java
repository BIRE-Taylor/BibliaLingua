package es.tjon.biblialingua.data.catalog;
import com.mobandme.ada.*;
import com.mobandme.ada.annotations.*;
import es.tjon.biblialingua.database.*;
import java.util.*;
import com.google.gson.*;

@Table(name="Folder")
public class Folder extends Entity implements CatalogItem
{

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

	public void setup(ApplicationDataContext adc)
	{
		for(Folder folder:folders)
		{
			folder.folder=this.id;
			folder.language=language;
			folder.setStatus(STATUS_NEW);
			folder.setup(adc);
		}
		for(Book book:books)
		{
			book.folder=this;
			book.language=language;
			book.setStatus(STATUS_NEW);
			book.setup(adc);
		}
	}


	public void update(Folder folder, ApplicationDataContext adc)
	{
		name = folder.name;
		language = folder.language;
		display_order = folder.display_order;
		eng_name = folder.eng_name;
		daysexpire=folder.daysexpire;
		ArrayList<Folder> newFolders = new ArrayList<Folder>();
		int i = 0;
		for (Folder newFolder : folder.folders)
		{
			newFolder.language=language;
			Folder found = null;
			for(Folder oldFolder: folders)
			{
				if(oldFolder.id==newFolder.id)
				{
					found = oldFolder;
					found.update(newFolder,adc);
					break;
				}
			}
			if(found!=null)
			{
				found.setStatus(STATUS_UPDATED);
				newFolders.add(found);
			}
			else
			{
				newFolder.folder=this.id;
				newFolder.setStatus(STATUS_NEW);
				newFolders.add(newFolder);
				newFolder.setup(adc);
			}
			i++;
		}
		folders = newFolders;
		ArrayList<Book> newBooks = new ArrayList<Book>();
		i = 0;
		for (Book book : folder.books)
		{
			book.language=language;
			Book found = null;
			for(Book oldBook: books)
			{
				if(oldBook.id==book.id)
				{
					found = oldBook;
					found.update(book,adc);
					break;
				}
			}
			if(found!=null)
			{
				found.setStatus(STATUS_UPDATED);
				newBooks.add(found);
			}
			else
			{
				book.folder=this;
				book.setStatus(STATUS_NEW);
				newBooks.add(book);
				book.setup(adc);
			}
			i++;
		}
		books = newBooks;
	}
}
