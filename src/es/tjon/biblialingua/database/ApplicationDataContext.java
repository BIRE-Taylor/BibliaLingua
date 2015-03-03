package es.tjon.biblialingua.database;
import android.content.*;
import com.mobandme.ada.*;
import com.mobandme.ada.exceptions.*;
import es.tjon.biblialingua.data.catalog.*;
import java.util.*;

import com.mobandme.ada.Entity;
import android.database.sqlite.*;
import es.tjon.biblialingua.*;
import android.os.*;
import java.io.*;
import android.content.res.*;
import es.tjon.biblialingua.utils.*;
import es.tjon.biblialingua.network.DownloadService;
import android.util.Log;

public class ApplicationDataContext extends ObjectContext
{
	private static final String TAG = "es.tjon.biblialingua.database.ApplicationDataContext";
	
	public ObjectSet<DownloadItem> downloadQueue;
	
	public ObjectSet<Language> languages;

	public ObjectSet<Catalog> catalog;
	
	public ObjectSet<Folder> folders;
	
	public ObjectSet<Book> books;
	
	public static BaseActivity context;

	private Context mContext;
	
	public ApplicationDataContext(Context context) throws AdaFrameworkException
	{
		super(context);
		
		mContext = context;

		downloadQueue = new ObjectSet<DownloadItem>( DownloadItem.class, this );
		
		languages = new ObjectSet<Language>(Language.class, this);
		
		catalog = new ObjectSet<Catalog>(Catalog.class, this);
		
		folders = new ObjectSet<Folder>(Folder.class, this);
		
		books = new ObjectSet<Book>(Book.class,  this);
		
	}

	@Override
	protected void onCreate(SQLiteDatabase pDataBase) throws AdaFrameworkException
	{
		restoreFromAssets("database.db",pDataBase);
		super.onCreate(pDataBase);
	}
	
	

	public static void initialize(BaseActivity context)
	{
		ApplicationDataContext.context=context;
		File f = new File(context.getExternalFilesDir("books"),"scriptures/");
		if(f.exists())
		{
			context.fileInitialized();
			return;
		}
		else
		{
			copyFiles();
		}
	}

	private static void copyFiles()
	{
		if(Looper.getMainLooper().equals(Looper.myLooper()))
		{
			new AsyncTask()
			{
				@Override
				protected Object doInBackground(Object[] p1)
				{
					copyFiles();
					return null;
				}
				protected void onPostExecute(Object object)
				{
					context.fileInitialized();
				}
			}.execute();
			return;
		}
		copyAssetFolder(context.getAssets(),"books",context.getExternalFilesDir("books").getAbsolutePath());
	}
	
	private static boolean copyAssetFolder(AssetManager assetManager,
										   String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            new File(toPath).mkdirs();
            boolean res = true;
            for (String file : files)
                if (file.contains("."))
                    res &= copyAsset(assetManager, 
									 fromAssetPath + "/" + file,
									 toPath + "/" + file);
                else 
                    res &= copyAssetFolder(assetManager, 
										   fromAssetPath + "/" + file,
										   toPath + "/" + file);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean copyAsset(AssetManager assetManager,
									 String fromAssetPath, String toPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
			in = assetManager.open(fromAssetPath);
			new File(toPath).createNewFile();
			out = new FileOutputStream(toPath);
			copyFile(in, out);
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
			return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
			out.write(buffer, 0, read);
        }
		}
	
	private static void copy(InputStream in, OutputStream out)
	throws IOException
    {
        byte[] buffer = new byte[1000];
        int len;
        while((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }

	public void queueProcessing(Book item)
	{
		// TODO: Implement this method
	}
	
	public void queueUpdate(Book item)
	{
		DownloadItem newDI = new DownloadItem(item);
		newDI.setStatus(DownloadItem.STATUS_NEW);
		try
		{
			if(!hasDownload(newDI))
			{
				downloadQueue.save(newDI);
			}
			Intent i = new Intent(mContext, DownloadService.class);
			i.putExtra(DownloadService.QUEUE_ITEM_ID,item.getID());
			mContext.startService(i);
		}
		catch (AdaFrameworkException e)
		{
			e.printStackTrace();
		}
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
			Log.d(TAG,language==null?"Language NULL":language.name);
			catalog.fill("ID IN(SELECT Catalog_ID FROM LINK_Catalog_c_language_Language WHERE Language_ID=?)", new String[]{language.getID().toString()}, null);
			if(catalog==null||catalog.size()==0)
				return null;
			return catalog.get(0);
		}
		catch (Exception e)
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
