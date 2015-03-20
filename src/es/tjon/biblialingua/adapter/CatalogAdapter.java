package es.tjon.biblialingua.adapter;

import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.android.volley.toolbox.*;
import es.tjon.biblialingua.*;
import es.tjon.biblialingua.data.catalog.*;
import es.tjon.biblialingua.network.*;
import es.tjon.biblialingua.utils.*;
import java.util.*;

public class CatalogAdapter extends BaseAdapter
{
	private BaseActivity mContext;

	public BaseActivity getContext()
	{
		return mContext;
	}

    public CatalogAdapter(BaseActivity c, Folder folder)
	{
        mContext = c;
		setFolder(folder);
    }

	public void setFolder(Folder folder)
	{
		if (Looper.getMainLooper().equals(Looper.myLooper()))
		{
			new AsyncTask<Folder,Object,Object>()
			{
				@Override
				protected Object doInBackground(Folder[] folder)
				{
					setFolder(folder[0]);
					return null;
				}
			}.execute(folder);
			return;
		}
		List<Folder> folders;
		List<Book> books;
		if (folder == null)
		{
			folders = mContext.getAppDataContext().getFolders(mContext.getPrimaryLanguage(), 0, mContext.getHideNotDownloaded());
			books = mContext.getAppDataContext().getBooks(mContext.getPrimaryLanguage(), 0, mContext.getHideNotDownloaded());
			update(folders,books);
			return;
		}
		folders = mContext.getAppDataContext().getFolders(mContext.getPrimaryLanguage(), folder.getID(),mContext.getHideNotDownloaded());
		books = mContext.getAppDataContext().getBooks(mContext.getPrimaryLanguage(), folder.getID(),mContext.getHideNotDownloaded());
		update(folders, books);
	}

	private void update(List<Folder> folders, List<Book> books)
	{
		if (!Looper.getMainLooper().equals(Looper.myLooper()))
		{
			getContext().runOnUiThread(new Runnable()
			{

				private List<Folder> folders;
				private List<Book> books;
				
				public Runnable setup(List<Folder> folders, List<Book> books)
				{
					this.folders=folders;
					this.books=books;
					return this;
				}

				@Override
				public void run()
				{
					update(folders,books);
				}


			}.setup(folders, books));
			return;
		}
		mFolders=folders;
		mBooks=books;
		notifyDataSetChanged();
	}

	@Override
	public void notifyDataSetChanged()
	{
		if (!Looper.getMainLooper().equals(Looper.myLooper()))
		{
			getContext().runOnUiThread(
				new Runnable()
				{

					@Override
					public void run()
					{
						notifyDataSetChanged();
					}

				});
			return;
		}
		super.notifyDataSetChanged();
	}

    public int getCount()
	{
		int count=0;
		count += mFolders != null ?mFolders.size(): 0;
		count += mBooks != null ?mBooks.size(): 0;
        return count;
    }

    public CatalogItem getItem(int position)
	{

		if (mFolders != null && position < mFolders.size())
			return mFolders.get(position);
		position = position - (mFolders != null ?mFolders.size(): 0);
		if (mBooks != null && mBooks.size() > position)
			return mBooks.get(position);
        return null;
    }

    public long getItemId(int position)
	{
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent)
	{
        RelativeLayout itemLayout;
        if (convertView == null)
		{  // if it's not recycled, initialize some attributes
            itemLayout = (RelativeLayout) ((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.catalog_item, null);
        }
		else
		{
            itemLayout = (RelativeLayout) convertView;
        }
		itemLayout.setTag(getItem(position));
		itemLayout.setBackgroundColor(Color.argb(getItem(position).isSelected()?25:0,255,255,255));
		ImageView mImageView = (ImageView)itemLayout.findViewById(R.id.item_cover);
        mImageView.setImageResource(R.drawable.cover_default_list);
		if (getItem(position) instanceof Book)
		{
			Book item = (Book) getItem(position);
			if (!mContext.getBookUtil().doesExist(item))
			{
				mImageView.setImageAlpha(100);
			}
			else
			{
				mImageView.setImageAlpha(255);
			}

		}
		else
		{
			mImageView.setImageAlpha(255);
		}
		ImageLoader mImageLoader;
		mImageLoader = VolleySingleton.getInstance(mContext).getImageLoader();
		String imageUrl=null;
		imageUrl = ImageUtil.getImageUrl(getItem(position).getCoverURL(), ImageUtil.LARGE_SIZE);
		if (imageUrl != null)
			mImageLoader.get(imageUrl, ImageLoader.getImageListener(mImageView, R.drawable.cover_default_list, R.drawable.cover_default_list));
		TextView text = ((TextView)itemLayout.findViewById(R.id.item_title));
		text.setText(getItem(position).getName());
		text.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        return itemLayout;
    }

    private List<Folder> mFolders = null;
	private List<Book> mBooks = null;
}
