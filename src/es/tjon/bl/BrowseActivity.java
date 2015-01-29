package es.tjon.bl;
import android.os.*;
import es.tjon.bl.fragment.*;
import es.tjon.bl.data.book.*;
import com.mobandme.ada.exceptions.*;
import android.view.*;
import android.widget.RadioGroup.*;
import es.tjon.bl.data.catalog.*;
import android.view.ViewGroup.*;
import android.hardware.display.*;
import android.graphics.*;
import android.widget.*;
import java.util.*;
import android.view.GestureDetector.*;
import es.tjon.bl.utils.*;
import android.content.res.*;
import android.preference.*;
import es.tjon.bl.listener.*;
import android.content.*;
import android.support.v4.app.*;

public class BrowseActivity extends BaseActivity implements BrowseFragment.BookController
{
	public static final String BOOK_ID="es.tjon.sl.bookid";
	public static final String BOOK_URI = "NodeUri";
	private static final String BOOKBROWSE = "BookBrowseFragment";
	

	public String mUri=null;

	private BrowseFragment mContentBrowse;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
			setContentView(R.layout.browse);
		setTitle("Browse");
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		if (mContentBrowse == null)
		{
			mContentBrowse=new BrowseFragment();
			getSupportFragmentManager().beginTransaction()
				.replace(R.id.browse, mContentBrowse, BOOKBROWSE).commit();
		}
		if (savedInstanceState == null && getIntent() != null)
		{
			savedInstanceState = getIntent().getExtras();
		}
		mUri = savedInstanceState.getString(BOOK_URI);
		mContentBrowse.start(this,savedInstanceState);
		super.onPostCreate(savedInstanceState);
	
	}

	@Override
	public void onAttachFragment(Fragment fragment)
	{
		mContentBrowse = (BrowseFragment) fragment;
		super.onAttachFragment(fragment);
	}

	@Override
	public void onBackPressed()
	{
		mContentBrowse.onBackPressed();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putString(BOOK_URI, mUri);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onNodeSelected(Node node)
	{
		if (node == null)
			return;
		if (node.content == null)
		{
			mNode = node;
			mUri = mNode.uri;
			getActionBar().setTitle(node.title);
			mContentBrowse.open(node);
			return;
		}
		Intent i = new Intent();
		i.setClass(this,BookViewActivity.class);
		i.putExtra(BookViewActivity.KEY_URI,node.uri);
		startActivity(i);
		return;
	}
	
}
