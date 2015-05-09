package es.tjon.biblialingua;
import android.os.*;
import android.widget.*;
import android.widget.AdapterView.*;
import es.tjon.biblialingua.adapter.*;
import android.view.*;

public class OsisActivity extends BaseActivity
{

	private GridView gridview;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.catalog);
		getActionBar().setTitle("OSIS Library");
		gridview = (GridView) findViewById(R.id.catalog);
		gridview.setAdapter(new OsisAdapter(this));
		gridview.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View v, int position, long id)
				{
					
				}
			});
	}
	
	
}
