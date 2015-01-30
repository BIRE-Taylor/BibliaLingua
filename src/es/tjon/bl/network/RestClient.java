package es.tjon.bl.network;
import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import com.google.gson.*;
import java.io.*;
import org.json.*;
import org.restlet.*;
import org.restlet.data.*;
import org.restlet.ext.json.*;
import org.restlet.resource.*;

import android.content.Context;
import es.tjon.bl.data.*;
import java.util.*;
import es.tjon.bl.data.catalog.*;

public class RestClient<T1> extends AsyncTask
{
	
	private final String BASEURL = "http://tech.lds.org/glweb/";
	public class Actions
	{
		private Actions(){}
		
		public static final String QUERY_LANGUAGES = "languages.query";
		public static final String QUERY_PLATFORMS = "platforms.query";
		public static final String QUERY_CATALOG = "catalog.query";
		public static final String QUERY_CATALOG_MODIFIED = "catalog.query.modified";
		public static final String QUERY_CATALOG_FOLDER = "catalog.query.folder";
		public static final String BOOK_VERSIONS = "book.versions";
		//http://tech.lds.org/glweb/?action=book.versions&languageid=1&platformid=1&lastdate=2010-10-22&format=json
		public class Parameters
		{
			private Parameters(){}
			
			public static final String LANGUAGE_ID = "languageid";
			public static final String PLATFORM_ID = "platformid";
			public static final String LASTDATE = "lastdate";
		}
	}
	
	public static void query(Context context, OnFinishListener callback, String action, Parameter... params)
	{
		if(action==Actions.QUERY_LANGUAGES)
		{
			new RestClient<LanguageData>().fetch(context, LanguageData.class, callback, new Parameter("action",Actions.QUERY_LANGUAGES));
			return;
		}
		if(action==Actions.QUERY_CATALOG)
		{
			new RestClient<CatalogData>().fetch(context, CatalogData.class, callback,params, new Parameter("action",Actions.QUERY_CATALOG),new Parameter(Actions.Parameters.PLATFORM_ID,"17"));
			return;
		}
	}
	
	/**
	 *  fetch
	 *  fetches object via rest api and converts it to java object.
	 */
	public void fetch(Context context, Class<T1> resultClass, OnFinishListener<T1> listener, Parameter... params)
	{
		ClientResource cr = new ClientResource(BASEURL);
		cr.addQueryParameter("format","json");
		for (Parameter param : params)
		{
			cr.addQueryParameter(param);
		}
		
		execute(cr, context, resultClass, listener);
		
	}
	
	/**
	 *  fetch
	 *  fetches object via rest api and converts it to java object.
	 */
	public void fetch(Context context, Class<T1> resultClass, OnFinishListener<T1> listener, Parameter[] paramArray, Parameter... params)
	{
		ClientResource cr = new ClientResource(BASEURL);
		cr.addQueryParameter("format","json");
		for (Parameter param : params)
		{
			cr.addQueryParameter(param);
		}
		for (Parameter param : paramArray)
		{
			cr.addQueryParameter(param);
		}
		
		execute(cr, context, resultClass, listener);

	}
	
	/**
	*  doInBackground
	*  pulls request from the internet
	*  params
	*  0 ClientResource		Rest request
	*  1 Context			Application context
	*  2 Class				Output class
	*  3 OnFinishListenee	Listener to call when finished
	*/
	@Override
	protected Object doInBackground(Object[] params)
	{
		ClientResource request = (ClientResource)params[0];
		request.setOnResponse(new Uniform() {

							private Object[] params;

							public Uniform setup(Object[] params)
							{
								this.params = params;
								return this;
							}
							
							public void handle(Request request, Response response)
							{
								if(response.getStatus().isError())
								{
									response.getStatus().getThrowable().printStackTrace();
									return;
								}
								// Get the representation as an JsonRepresentation
								try
								{
									
									String json = response.getEntityAsText();
									
									JsonRepresentation rep = new JsonRepresentation(json);
									JSONObject object = rep.getJsonObject();
									RestClient.this.onFinish((Context)params[1], object, (Class)params[2], (OnFinishListener<T1>)params[3]);
								}
								catch (JSONException e)
								{
									e.printStackTrace();
								}
							}
						}.setup(params));
		request.get(MediaType.APPLICATION_JSON);
		return null;
	}
	
	/**
	*	onFinish
	*	Processes result JSON into desired object and calls listener.
	*/
	private void onFinish(Context context, JSONObject result, Class<T1> resultClass, OnFinishListener<T1> listener)
	{
		T1 object = new Gson().fromJson(result.toString(), resultClass);
		((Activity)context).runOnUiThread(new Runnable()
										  {

											  private T1 object;

											  private RestClient.OnFinishListener<T1> listener;

											  public Runnable setup(T1 result, OnFinishListener<T1> listener)
											  {
												  this.object = result;
												  this.listener = listener;
												  return this;
											  }

											  @Override
											  public void run()
											  {
												  listener.onFinish(object);
											  }


										  }.setup(object, listener));
	}

	public interface OnFinishListener<T1>
	{
		public abstract void onFinish(T1 result);
	}
}
