package es.tjon.biblialingua.data;
import org.json.*;

public class Source
{
	
	//{"src":"","type":"application/vnd.apple.mpegurl","data-container":"hls","data-encodingbitspersec":"2088000","data-width":"640","data-height":"480","data-sizeinbytes":"8598079","data-durationms":"32949","data-alloweduses":null}
	public String src, type, container;
	public int encodingbitspersec, width, height, sizeinbytes, durationms;


	public static Source fromJSON(JSONObject json)
	{
		Source result = new Source();
		try
		{
			result.src = json.getString("src");
			result.type = json.getString("type");
			result.container = json.getString("data-container");
			result.encodingbitspersec = json.getInt("data-encodingbitspersecond");
			result.width = json.getInt("data-width");
			result.height = json.getInt("data-height");
			result.sizeinbytes = json.getInt("data-sizeinbytes");
			result.durationms = json.getInt("data-durationms");
		}
		catch (JSONException e)
		{}
		return result;
	}}
