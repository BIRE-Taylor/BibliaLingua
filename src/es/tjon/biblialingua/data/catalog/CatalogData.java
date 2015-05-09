package es.tjon.biblialingua.data.catalog;
import android.os.*;
import com.mobandme.ada.*;

import com.android.volley.Response;
import com.mobandme.ada.exceptions.AdaFrameworkException;
import es.tjon.biblialingua.BaseActivity;
import es.tjon.biblialingua.data.catalog.CatalogData;
import es.tjon.biblialingua.database.ApplicationDataContext;
import es.tjon.biblialingua.network.RestClient;
import java.util.List;
import com.android.volley.*;

public class CatalogData implements Response.Listener<CatalogData>, Response.ErrorListener
{
	
	public Catalog catalog;
	public boolean success;

	private static BaseActivity context;

	private static Language language;

	private static int errorCount = 0;

	public static void initialize(BaseActivity context)
	{
		CatalogData.context=context;
		runInit();
	}
	
	private static void runInit()
	{
		if (Looper.getMainLooper().equals(Looper.myLooper()))
		{
			new AsyncTask(){

				@Override
				protected Object doInBackground(Object[] p1)
				{
					runInit();
					return null;
				}


			}.execute();
			return;
		}
		ApplicationDataContext adc = context.getAppDataContext();
		if(adc.getCatalog(context.getPrimaryLanguage())==null)
		{
			context.setLoadingMessage("Downloading Primary Catalog... Try "+(errorCount+1));
			language = context.getPrimaryLanguage();
			CatalogData cd = new CatalogData();
			RestClient.query(context, cd, cd, new RestClient.ParameterSet(RestClient.Actions.QUERY_CATALOG, new RestClient.ParameterSet.Parameter(RestClient.Actions.Parameters.LANGUAGE_ID,language.id)));
			return;
		}
		if(adc.getCatalog(context.getSecondaryLanguage())==null)
		{
			context.setLoadingMessage("Downloading Secondary Catalog... Try "+(errorCount+1));
			language = context.getSecondaryLanguage();
			RestClient.query(context, new CatalogData(), new RestClient.ParameterSet(RestClient.Actions.QUERY_CATALOG, new RestClient.ParameterSet.Parameter(RestClient.Actions.Parameters.LANGUAGE_ID,language.id)));
			return;
		}
		context.setLoadingMessage("Catalog Initialized...");
		context.catalogInitialized();
	}

	@Override
	public void onResponse(CatalogData result)
	{
		System.out.println("Catalog Result");
		context.setLoadingMessage("Saving catalog...");
		if(result==null||!result.success)
		{
			if(errorCount>3)
			{
				context.finish();
				return;
			}
			errorCount++;
			runInit();
			return;
		}
		errorCount=0;
		ApplicationDataContext adc = context.getAppDataContext();
		ObjectSet<Catalog> cat ;
		try
		{
			cat = new ObjectSet<Catalog>(Catalog.class, adc);
		}
		catch (AdaFrameworkException e)
		{
			cat=adc.catalog;
		}
		try
		{
			result.catalog.language = language;
			cat.fill();
			List<Catalog> cats =  cat.search(null,"c_name=?",new String[]{result.catalog.name},null,null,null,null,null);
			if(cats!=null&&cats.size()>0)
			{
				cat.clear();
				if(cats.get(0).date_changed.equals(result.catalog.date_changed))
				{
					runInit();
					return;
				}
				cats.get(0).update(result.catalog,adc,context);
				cats.get(0).setStatus(Entity.STATUS_UPDATED);
				cat.add(cats.get(0));
			}
			else
			{
				cat.clear();
				result.catalog.setup(adc,context);
				cat.add(result.catalog);
				result.catalog.setStatus(Entity.STATUS_NEW);
			}
			cat.save();
			cat.get(0).saveAll(adc);
			adc.folders.save();
			adc.books.save();
			context.setLastUpdate(language,System.currentTimeMillis());
		}
		catch (AdaFrameworkException e)
		{
			e.printStackTrace();
		}
		runInit();
	}
	
	@Override
	public void onErrorResponse(VolleyError error)
	{
		error.printStackTrace();
		if(errorCount>3)
		{
			context.finish();
			return;
		}
		errorCount++;
		runInit();
	}
}
