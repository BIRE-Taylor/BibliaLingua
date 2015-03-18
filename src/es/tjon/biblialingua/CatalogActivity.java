package es.tjon.biblialingua;

import android.os.*;
import android.view.*;
import android.widget.*;
import es.tjon.biblialingua.data.catalog.*;

import android.content.Intent;
import android.graphics.Color;
import android.widget.AdapterView.OnItemClickListener;
import es.tjon.biblialingua.adapter.CatalogAdapter;
import es.tjon.biblialingua.listener.ProgressMonitor;
import java.util.ArrayList;

public class CatalogActivity extends BaseActivity implements ProgressMonitor
{

	@Override
	public void onProgress(Book book, int progress)
	{
	}


	@Override
	public void notifyError(Book item)
	{}


	private static final String FOLDER_ID = "folderid";

	@Override
	public void onFinish(Book book)
	{
		if (currentFolder != null && currentFolder.contains(book))
			((CatalogAdapter)gridview.getAdapter()).notifyDataSetChanged();
	}

	@Override
	public boolean onNavigateUp()
	{
		if(currentFolder!=null)
			setFolder(null);
		return true;
	}

	Folder currentFolder = null;
	GridView gridview=null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		int folder=0;
		if (savedInstanceState != null)
		{
			folder = savedInstanceState.getInt(FOLDER_ID, 0);

			currentFolder=getAppDataContext().getFolder(folder);
		}
		setContentView(R.layout.catalog);
		getActionBar().setTitle("Library");
		gridview = (GridView) findViewById(R.id.catalog);
		gridview.setAdapter(new CatalogAdapter(this,currentFolder));
		gridview.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
		gridview.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener()
			{
				ArrayList<Integer> selected;

				private MenuItem download;

				private MenuItem delete;

				@Override
				public boolean onCreateActionMode( ActionMode p1, Menu menu )
				{
					selected = new ArrayList<Integer>();
					download = menu.add("Download");
					delete = menu.add("Delete");
					return true;
				}

				@Override
				public boolean onPrepareActionMode( ActionMode p1, Menu menu )
				{
					return false;
				}

				@Override
				public boolean onActionItemClicked( ActionMode p1, MenuItem item )
				{
					if(item.equals(download))
					{
						downloadAll(selected);
						p1.finish();
						Toast.makeText(CatalogActivity.this,"Items Requested",Toast.LENGTH_SHORT).show();
						return true;
					}
					if(item.equals(delete))
					{
						deleteAll(selected);
						p1.finish();
						Toast.makeText(CatalogActivity.this,"Items Deleted",Toast.LENGTH_SHORT).show();
						setFolder(currentFolder);
						return true;
					}
					return false;
				}

				@Override
				public void onDestroyActionMode( ActionMode p1 )
				{
					for(Integer position : selected)
					{
						if(gridview.findViewWithTag(gridview.getItemAtPosition(position))==null)
							return;
						gridview.findViewWithTag(gridview.getItemAtPosition(position)).setBackgroundColor(Color.argb(0,255,255,255));
						((CatalogItem)gridview.getItemAtPosition(position)).setSelected(false);
					}
				}

				@Override
				public void onItemCheckedStateChanged( ActionMode cab, int position, long id, boolean state )
				{
					((CatalogItem)gridview.getItemAtPosition(position)).setSelected(state);
					if(state)
					{
						gridview.findViewWithTag(gridview.getItemAtPosition(position)).setBackgroundColor(Color.argb(25,255,255,255));
						selected.add(position);
					}
					else
					{
						gridview.findViewWithTag(gridview.getItemAtPosition(position)).setBackgroundColor(Color.argb(0,255,255,255));
						selected.remove((Integer)position);
					}
					cab.setTitle(selected.size()+" item"+(selected.size()==1?"":"s")+" selected");
				}

			});
		gridview.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View v, int position, long id)
				{
					Object item = parent.getItemAtPosition(position);
					if (item instanceof Folder)
					{
						setFolder((Folder) item);
						gridview.smoothScrollToPosition(0);
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
			int folderId = savedInstanceState.getInt(FOLDER_ID, -1);
			if (folderId != -1)
				setFolder(getAppDataContext().getFolder(folderId));
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

	public void downloadAll(ArrayList<Integer> positions)
	{
		ArrayList<CatalogItem> items = new ArrayList<CatalogItem>();
		for(int position : positions)
		{
			items.add((CatalogItem)gridview.getItemAtPosition(position));
		}
		getBookUtil().requestAll(items);
	}

	public void deleteAll(ArrayList<Integer> positions)
	{
		ArrayList<CatalogItem> items = new ArrayList<CatalogItem>();
		for(int position : positions)
		{
			items.add((CatalogItem)gridview.getItemAtPosition(position));
		}
		getBookUtil().deleteAll(items);
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
