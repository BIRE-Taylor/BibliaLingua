package es.tjon.biblialingua.data.catalog;
import com.mobandme.ada.*;
import com.mobandme.ada.annotations.*;
import es.tjon.biblialingua.database.*;
import java.util.*;

@Table(name="Catalog")
public class Catalog extends Entity
{
	public ArrayList<Folder> folders;
	public ArrayList<Book> books;
	@TableField(name="c_name",datatype=DATATYPE_TEXT)
	public String name = null;
	@TableField(name="c_date_changed",datatype=DATATYPE_STRING)
	public String date_changed= null;
	@TableField(name="c_language",datatype=DATATYPE_ENTITY_LINK)
	public Language language = null;
	@TableField(name="c_display_order",datatype=DATATYPE_INTEGER)
	public int display_order = -1;

	public void saveAll(ApplicationDataContext adc)
	{
		adc.folders.addAll(folders);
		adc.books.addAll(books);
		for(Folder folder:folders)
		{
			folder.saveAll(adc);
		}
	}

	public void setup(ApplicationDataContext adc)
	{
		for(Folder folder:folders)
		{
			folder.language=language;
			folder.catalog=this;
			folder.setStatus(STATUS_NEW);
			folder.setup(adc);
		}
		for(Book book:books)
		{
			book.language=language;
			book.catalog=this;
			book.setup(adc);
			book.setStatus(STATUS_NEW);
		}
	}

	public void update(Catalog catalog, ApplicationDataContext adc)
	{
		name = catalog.name;
		date_changed = catalog.date_changed;
		language = catalog.language;
		display_order = catalog.display_order;
		ArrayList<Folder> newFolders = new ArrayList<Folder>();
		int i = 0;
		for (Folder folder : catalog.folders)
		{
			folder.language=language;
			Folder found = null;
			if(folders!=null)
			for(Folder oldFolder: folders)
			{
				if(oldFolder.id==folder.id)
				{
					found = oldFolder;
					found.update(folder,adc);
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
				folder.catalog=this;
				folder.setStatus(STATUS_NEW);
				newFolders.add(folder);
				folder.setup(adc);
			}
			i++;
		}
		folders = newFolders;
		ArrayList<Book> newBooks = new ArrayList<Book>();
		i = 0;
		for (Book book : catalog.books)
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
				book.setStatus(STATUS_UPDATED);
				newBooks.add(found);
			}
			else
			{
				book.catalog=this;
				book.setStatus(STATUS_NEW);
				newBooks.add(book);
				book.setup(adc);
			}
			i++;
		}
		books = newBooks;
	}
}
