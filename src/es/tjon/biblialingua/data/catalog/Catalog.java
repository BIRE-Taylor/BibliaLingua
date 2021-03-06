package es.tjon.biblialingua.data.catalog;
import com.mobandme.ada.*;
import com.mobandme.ada.annotations.*;
import es.tjon.biblialingua.database.*;
import java.util.*;
import com.mobandme.ada.exceptions.AdaFrameworkException;
import android.content.Context;

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

	public void setup(ApplicationDataContext adc,Context context)
	{
		for(Folder folder:folders)
		{
			folder.language=language;
			folder.catalog=this;
			folder.setStatus(STATUS_NEW);
			folder.setup(adc,context);
		}
		for(Book book:books)
		{
			book.language=language;
			book.catalog=this;
			book.setup(adc,context);
			book.setStatus(STATUS_NEW);
		}
	}

	public void update(Catalog catalog, ApplicationDataContext adc, Context context)
	{
		name = catalog.name;
		date_changed = catalog.date_changed;
		language = catalog.language;
		display_order = catalog.display_order;
		if(folders==null||folders.size()==0)
		{
				folders = new ArrayList<Folder>( adc.getFolders(language,0) );
		}
		System.out.println(folders.size()+" folders after");
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
					found.update(folder,adc, context);
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
				folder.language=language;
				folder.setStatus(STATUS_NEW);
				newFolders.add(folder);
				folder.setup(adc,context);
			}
			i++;
		}
		folders = newFolders;
		ArrayList<Book> newBooks = new ArrayList<Book>();
		i = 0;
		if(books==null||books.size()==0)
		{
			books = new ArrayList<Book>(Arrays.asList( adc.getBooks(language,0) ));
		}
		for (Book book : catalog.books)
		{
			book.language=language;
			Book found = null;
			for(Book oldBook: books)
			{
				if(oldBook.id==book.id)
				{
					found = oldBook;
					found.update(book,adc,context);
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
				book.setup(adc,context);
			}
			i++;
		}
		books = newBooks;
	}
}
