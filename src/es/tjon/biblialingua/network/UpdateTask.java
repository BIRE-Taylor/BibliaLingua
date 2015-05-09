package es.tjon.biblialingua.network;
import com.mobandme.ada.*;
import es.tjon.biblialingua.data.catalog.*;
import java.text.*;
import java.util.*;

import android.app.Service;
import com.android.volley.Response;
import com.mobandme.ada.exceptions.AdaFrameworkException;
import es.tjon.biblialingua.database.ApplicationDataContext;
import es.tjon.biblialingua.utils.Util;
import android.widget.Toast;
import com.android.volley.VolleyError;

public class UpdateTask implements Response.Listener<Object>, Response.ErrorListener
{

	@Override
	public void onErrorResponse( VolleyError error )
	{
		Toast.makeText( mContext, "Update failed " + mLanguage.name, Toast.LENGTH_SHORT ).show( );
		error.printStackTrace( );
		mContext.finishedUpdating( this );
	}


	private Catalog catalog;

	@Override
	public void onResponse( Object result )
	{
		if ( result != null )
		{
			if ( result instanceof CatalogModified )
			{
				onResponse( (CatalogModified)result );
			}
			else if ( result instanceof CatalogData )
			{
				onResponse( (CatalogData)result );
			}
		}
		else
		{
			System.out.println( "Result null" );
			mContext.finishedUpdating( this );
		}
	}

	private void onResponse( CatalogData result )
	{
		try
		{
			System.out.println( result );
			if ( result == null || !result.success )
			{
				mContext.finishedUpdating( this );
				System.out.println( "Fail " + mLanguage.name );
				return;
			}
			ApplicationDataContext adc = new ApplicationDataContext( mContext );
			ObjectSet<Catalog> cat ;
			try
			{
				cat = new ObjectSet<Catalog>( Catalog.class, adc );
			}
			catch (AdaFrameworkException e)
			{
				cat = adc.catalog;
			}
			result.catalog.language = mLanguage;
			cat.fill( );
			List<Catalog> cats =  cat.search( null, "c_name like '"+result.catalog.name+"'", null, null, null, null, null, null );
			if ( cats != null && cats.size( ) > 0 )
			{
				System.out.println("Catalog found");
				cat.clear( );
				cats.get( 0 ).update( result.catalog, adc , mContext);
				cats.get( 0 ).setStatus( Entity.STATUS_UPDATED );
				cat.add( cats.get( 0 ) );
			}
			else
			{
				System.out.println("Creating catalog");
				cat.clear( );
				result.catalog.setup( adc ,mContext);
				cat.add( result.catalog );
				result.catalog.setStatus( Entity.STATUS_NEW );
			}
			System.out.println( "Saving catalog " + mLanguage.name );
			cat.save( );
			cat.get( 0 ).saveAll( adc );
			adc.folders.save( );
			adc.books.save( );
		}
		catch (AdaFrameworkException e)
		{
			Toast.makeText( mContext, "Update failed " + mLanguage.name, Toast.LENGTH_SHORT ).show( );
			e.printStackTrace( );
		} finally
		{
			Toast.makeText( mContext, "Finish update " + mLanguage.name, Toast.LENGTH_SHORT ).show( );
			mContext.finishedUpdating( this );
		}
	}

	private void update( )
	{
		System.out.println( "Updating " + mLanguage.name );
		RestClient.query( mContext, this, this, new RestClient.ParameterSet( RestClient.Actions.QUERY_CATALOG, new RestClient.ParameterSet.Parameter( RestClient.Actions.Parameters.LANGUAGE_ID, mLanguage.id ) ) );
	}

	@Override
	public void onResponse( CatalogModified result )
	{
		if ( result != null )
		{
			try
			{
				catalog = new ApplicationDataContext( mContext ).getCatalog( mLanguage );
				SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd hh:mm:ss" );
				Date catDate = sdf.parse( catalog.date_changed );
				Date upDate = sdf.parse( result.catalog_modified );
				System.out.println( mLanguage.name+" "+catalog.date_changed + " " + result.catalog_modified );
				if ( catDate.before( upDate ) )
				{
					Toast.makeText( mContext, "Updating " + mLanguage.name, Toast.LENGTH_SHORT ).show( );
					update( );
				}
				else
				{
					System.out.println( "No update available" );
					mContext.finishedUpdating( this );
				}
			}
			catch (AdaFrameworkException e)
			{
				e.printStackTrace( );
			}
			catch (ParseException pe)
			{
				pe.printStackTrace( );
			}
		}
		else
		{
			System.out.println( "Result null" );
			mContext.finishedUpdating( this );
		}
	}

	private DownloadService mContext;

	private Language mLanguage;

	public UpdateTask( DownloadService context, Language language )
	{
		mContext = context;
		mLanguage = language;
		System.out.println( language );
	}

	public void check( )
	{
		RestClient.query( mContext, this, new RestClient.ParameterSet( RestClient.Actions.QUERY_CATALOG_MODIFIED, new RestClient.ParameterSet.Parameter( RestClient.Actions.Parameters.LANGUAGE_ID, mLanguage.id ) ) );
	}
}
