package es.tjon.bl;

import android.os.*;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.*;
import es.tjon.bl.adapter.*;
import es.tjon.bl.data.catalog.*;
import es.tjon.bl.network.*;
import android.content.*;

public class CatalogActivity extends BaseActivity implements BookDownloadService.ProgressMonitor
{

	private static final String FOLDER_ID = "folderid";

	@Override
	public void onProgress(Book book, int progress)
	{

	}

	@Override
	public void onFinish(Book book)
	{
		if (currentFolder != null && currentFolder.contains(book))
			((CatalogAdapter)gridview.getAdapter()).notifyDataSetChanged();
	}


	Folder currentFolder = null;
	GridView gridview=null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.catalog);
		getActionBar().setTitle("Library");
		gridview = (GridView) findViewById(R.id.catalog);
		gridview.setAdapter(new CatalogAdapter(this));

		gridview.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View v, int position, long id)
				{
					Object item = parent.getItemAtPosition(position);
					if (item instanceof Folder)
					{
						setFolder((Folder) item);
						return;
					}
					if (item instanceof Book)
					{
						openBook((Book)item);
					}
				}
			});
		if (savedInstanceState != null)
		{
			((CatalogAdapter)gridview.getAdapter()).notifyDataSetInvalidated();
			int folder = savedInstanceState.getInt(FOLDER_ID, -1);
			if (folder != -1)
				setFolder(getAppDataContext().getFolder(folder));
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
	}

	private void setFolder(Folder folder)
	{
		currentFolder = folder;
		((CatalogAdapter)gridview.getAdapter()).setFolder(currentFolder);
		if (currentFolder == null)
			getActionBar().setTitle("Library");
		else
			getActionBar().setTitle(currentFolder.name);
	}

	public void openBook(Book item)
	{
		if (getBookUtil().doesExist(item))
		{
			new AsyncTask()
			{
				@Override
				protected Object doInBackground(Object[] p1)
				{
					Book item = getAppDataContext().getBook(getSecondaryLanguage(), ((Book)p1[0]).gl_uri);
					if (!getBookUtil().doesExist(item))
					{
						getBookUtil().requestBook(item, CatalogActivity.this);
					}
					return null;
				}

			}.execute(item);
			Intent i = new Intent(this, BrowseActivity.class);
			i.putExtra(BrowseActivity.BOOK_URI, item.gl_uri);
			startActivity(i);
		}
		else
		{
			Toast.makeText(this, "Downloading book", Toast.LENGTH_SHORT).show();
			getBookUtil().requestBook(item, this);

		}
	}

	@Override
	public void onBackPressed()
	{
		if (currentFolder == null)
		{
			exit();
			return;
		}
		else if (currentFolder.folder < 1)
		{
			setFolder(null);
			((CatalogAdapter)gridview.getAdapter()).setFolder(null);
			return;
		}
		else
		{
			setFolder(getAppDataContext().getFolder(currentFolder.folder));
			return;
		}
	}

	@Override
	protected void onDestroy()
	{
		getBookUtil().removeMonitor(this);
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		if (currentFolder != null)
			outState.putInt(FOLDER_ID, currentFolder.id);
		super.onSaveInstanceState(outState);
	}

}
