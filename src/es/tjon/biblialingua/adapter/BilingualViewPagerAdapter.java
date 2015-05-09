package es.tjon.biblialingua.adapter;

import android.support.v4.view.*;
import android.view.*;
import java.util.*;

public class BilingualViewPagerAdapter extends PagerAdapter
{
	
	ArrayList mAvailableViews;
	ArrayList mUnavailableViews;

	@Override
	public int getCount()
	{
		// TODO: Implement this method
		return 0;
	}

	@Override
	public boolean isViewFromObject(View p1, Object p2)
	{
		// TODO: Implement this method
		return false;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position)
	{
		// TODO: Implement this method
		return super.instantiateItem(container, position);
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object)
	{
		// TODO: Implement this method
		super.destroyItem(container, position, object);
	}

	@Override
	public int getItemPosition(Object object)
	{
		// TODO: Implement this method
		return super.getItemPosition(object);
	}

	@Override
	public void setPrimaryItem(ViewGroup container, int position, Object object)
	{
		// TODO: Implement this method
		super.setPrimaryItem(container, position, object);
	}
	
}
