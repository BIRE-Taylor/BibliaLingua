package es.tjon.biblialingua.data;
import org.json.*;
import java.util.*;

public class Video
{
	public Integer id;
	public String title;
	public Integer index;
	public List<Source> sources;
	


	public static Video fromJSON(String string)
	{
		Video result = new Video();
		try
		{
			JSONObject obj = new JSONObject(string);
			try
			{
			result.id=obj.getInt("id");
			}
			catch(JSONException je)
			{}
			try
			{
			result.index=obj.getInt("index");
			}
			catch(JSONException je)
			{}
			result.title=obj.getString("title");
			JSONArray arr = obj.getJSONArray("sources");
			result.sources = new ArrayList<Source>();
			for (int i=0;i < arr.length();i++)
			{
				result.sources.add(Source.fromJSON(arr.getJSONObject(i)));
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return result;
	}}
