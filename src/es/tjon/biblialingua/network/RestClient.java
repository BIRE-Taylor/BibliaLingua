package es.tjon.biblialingua.network;
import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import java.io.*;

import android.content.Context;
import es.tjon.biblialingua.data.*;
import java.util.*;
import es.tjon.biblialingua.data.catalog.*;
import org.json.*;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import javax.xml.transform.*;
import com.android.volley.*;
import org.xml.sax.helpers.*;

public class RestClient<T1> implements Response.ErrorListener
{

	@Override
	public void onErrorResponse( VolleyError error )
	{
		Log.e(TAG,error.getMessage(),error);
	}
	
	public static final String TAG="es.tjon.biblialingua.network.RestClient";


	private final String BASEURL = "http://tech.lds.org/glweb/";
	public class Actions
	{
		private Actions( )
		{}

		public static final String QUERY_LANGUAGES = "languages.query";
		public static final String QUERY_PLATFORMS = "platforms.query";
		public static final String QUERY_CATALOG = "catalog.query";
		public static final String QUERY_CATALOG_MODIFIED = "catalog.query.modified";
		public static final String QUERY_CATALOG_FOLDER = "catalog.query.folder";
		public static final String BOOK_VERSIONS = "book.versions";
		//http://tech.lds.org/glweb/?action=book.versions&languageid=1&platformid=1&lastdate=2010-10-22&format=json
		public class Parameters
		{
			private Parameters( )
			{}

			public static final String LANGUAGE_ID = "languageid";
			public static final String PLATFORM_ID = "platformid";
			public static final String LASTDATE = "lastdate";
		}
	}

	public static class ParameterSet extends JSONObject
	{

		public ParameterSet( )
		{
		}

		public ParameterSet( String action )
		{
			this.addParameter( new Parameter( "action", action ) );
			this.addParameter(new Parameter("format","json"));
		}

		public ParameterSet( String action, Parameter...params )
		{
			this( action );
			for ( Parameter param : params )
				addParameter( param );
		}

		public static class Parameter
		{

			String key;
			String value;

			public Parameter( String key, String value )
			{
				this.key = key;
				this.value = value;
			}

			public Parameter( String key, int value )
			{
				this.key = key;
				this.value = Integer.toString( value );
			}

			public String getKey( )
			{
				return key;
			}

			public String getValue( )
			{
				return value;
			}
		}

		public void addParameter( Parameter parameter )
		{
			try
			{
				this.put( parameter.getKey( ), parameter.getValue( ) );
			} catch (JSONException e)
			{
				Log.e( TAG, e.getMessage( ) );
			}
		}

	}
	
	public static void query( Context context, Response.Listener callback, ParameterSet params )
	{
		query(context,callback,new RestClient(),params);
	}

	public static void query( Context context, Response.Listener callback, Response.ErrorListener errorHandler, ParameterSet params )
	{
		try
		{
			String action = params.getString( "action" );
			params.addParameter(new ParameterSet.Parameter( Actions.Parameters.PLATFORM_ID, "17" ));
			if ( action == Actions.QUERY_LANGUAGES )
			{
				new RestClient<LanguageData>( ).fetch( context, LanguageData.class, callback, errorHandler, params );
				return;
			}
			if ( action == Actions.QUERY_CATALOG )
			{
				new RestClient<CatalogData>( ).fetch( context, CatalogData.class, callback, errorHandler, params );
				return;
			}
			if ( action == Actions.QUERY_CATALOG_MODIFIED )
			{
				new RestClient<CatalogModified>( ).fetch( context, CatalogModified.class, callback, errorHandler,  params );
				return;
			}
		} catch (JSONException e)
		{}
	}

	public void fetch( Context c, Class<T1> result, Response.Listener<T1> callback, Response.ErrorListener errorHandler, ParameterSet params )
	{
		GsonObjectRequest<T1> request = new GsonObjectRequest<T1>(GsonObjectRequest.Method.GET,BASEURL,params,callback,errorHandler,result);
		request.setRetryPolicy(new DefaultRetryPolicy(10000,3,1));
		VolleySingleton.getInstance(c).addToRequestQueue(request);
	}

}
