package es.tjon.biblialingua.fragment;
import android.support.v4.app.*;
import android.view.*;
import android.widget.*;
import android.os.*;
import es.tjon.biblialingua.adapter.HeaderListView.*;
import es.tjon.biblialingua.adapter.*;

public class HeaderListFragment extends ListFragment
{

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		HeaderListView listView = new HeaderListView(getActivity());
		listView.setId(android.R.id.list);
		listView.setPinnedHeaderView(LayoutInflater.from(getActivity()).inflate(es.tjon.biblialingua.R.layout.list_item_header, listView, false));
	    listView.setDividerHeight(0);
		return listView;
	}

	@Override
	public void setListAdapter(HeaderAdapter adapter)
	{
		super.setListAdapter(adapter);
	}
	
}
