package es.tjon.biblialingua.adapter;

import android.os.*;
import android.view.*;
import android.widget.*;
import es.tjon.biblialingua.*;
import es.tjon.biblialingua.data.catalog.*;

import android.content.Context;
import android.graphics.Color;
import com.android.volley.toolbox.ImageLoader;
import es.tjon.biblialingua.network.VolleySingleton;
import es.tjon.biblialingua.utils.ImageUtil;

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
		Folder[] folders;
		Book[] books;
		if (folder == null)
		{
			folders = mContext.getAppDataContext().getFolders(mContext.getPrimaryLanguage(), 0);
			books = mContext.getAppDataContext().getBooks(mContext.getPrimaryLanguage(), 0);
			update(folders,books);
			return;
		}
		folders = mContext.getAppDataContext().getFolders(mContext.getPrimaryLanguage(), folder.getID());
		books = mContext.getAppDataContext().getBooks(mContext.getPrimaryLanguage(), folder.getID());
		update(folders, books);
	}

	private void update(Folder[] folders, Book[] books)
	{
		if (!Looper.getMainLooper().equals(Looper.myLooper()))
		{
			getContext().runOnUiThread(new Runnable()
			{

				private Folder[] folders;
				private Book[] books;
				
				public Runnable setup(Folder[] folders, Book[] books)
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
		count += mFolders != null ?mFolders.length: 0;
		count += mBooks != null ?mBooks.length: 0;
        return count;
    }

    public CatalogItem getItem(int position)
	{

		if (mFolders != null && position < mFolders.length)
			return mFolders[position];
		position = position - (mFolders != null ?mFolders.length: 0);
		if (mBooks != null && mBooks.length > position)
			return mBooks[position];
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

    private Folder[] mFolders = null;
	private Book[] mBooks = null;
}
