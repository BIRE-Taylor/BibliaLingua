package es.tjon.biblialingua.network;

import com.android.volley.*;
import com.android.volley.Response.*;
import com.android.volley.toolbox.*;
import com.google.gson.*;
import java.io.*;
import org.json.*;
import java.util.Map;
import java.net.URLEncoder;
import java.util.Iterator;

/**
 * A request for retrieving a T response body at a given URL, allowing for an
 * optional {@link JSONObject} to be passed in as part of the request body.
 */
public class GsonObjectRequest<T> extends JsonRequest<T>
{

	Class<T> mResultClass;

	private JSONObject mParams;

    /**
     * Creates a new request.
     * @param method the HTTP method to use
     * @param url URL to fetch the Object from
     * @param jsonRequest A {@link JSONObject} to post with the request. Null is allowed and
     *   indicates no parameters will be posted along with request.
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public GsonObjectRequest( int method, String url, JSONObject jsonRequest,
							 Listener<T> listener, ErrorListener errorListener, Class<T> resultClass )
	{
        super( method, url, ( method==Method.GET||jsonRequest == null ) ? null : jsonRequest.toString( ), listener,
			  errorListener );
		mResultClass = resultClass;
		if(method==Method.GET&&jsonRequest != null)
		{
			mParams = jsonRequest;
		}
		setShouldCache(false);
    }

    /**
     * Constructor which defaults to <code>GET</code> if <code>jsonRequest</code> is
     * <code>null</code>, <code>POST</code> otherwise.
     *
     * @see #GsonObjectRequest(int, String, JSONObject, Listener, ErrorListener, Class<T1>)
     */
    public GsonObjectRequest( String url, JSONObject jsonRequest, Listener<T> listener,
							 ErrorListener errorListener, Class<T> resultClass )
	{
        this( jsonRequest == null ? Method.GET : Method.POST, url, jsonRequest,
			 listener, errorListener, resultClass );
    }

	@Override
	public String getUrl( )
	{
		StringBuilder encodedParams = new StringBuilder();
		if(mParams!=null)
		{
			Iterator<String> keys = mParams.keys( );
			String key;
			encodedParams.append( '?' );
            while (keys.hasNext()) {
				try
				{
				key=keys.next();
                encodedParams.append( key );
                encodedParams.append('=');
                encodedParams.append( mParams.getString( key ) );
                encodedParams.append('&');
				} catch( JSONException e )
				{
					encodedParams.append('&');
				}
            }
       }
		System.out.println(super.getUrl( )+encodedParams.toString());
		return super.getUrl( )+encodedParams.toString();
	}

    @Override
    protected Response<T> parseNetworkResponse( NetworkResponse response )
	{
        try
		{
            String jsonString =
                new String( response.data, HttpHeaderParser.parseCharset( response.headers ) );
			System.out.println( jsonString );
            return Response.success( new Gson( ).fromJson( jsonString, mResultClass ),
									HttpHeaderParser.parseCacheHeaders( response ) );
        } catch (UnsupportedEncodingException e)
		{
			e.printStackTrace( );
            return Response.error( new ParseError( e ) );
        }
    }
}
