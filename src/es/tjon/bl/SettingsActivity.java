package es.tjon.bl;
import android.provider.*;
import android.preference.*;
import android.app.*;
import android.os.*;
import es.tjon.bl.fragment.*;

public class SettingsActivity extends BaseActivity
{

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.browse);
		
		getFragmentManager().beginTransaction()
		.replace(R.id.browse,new SettingsFragment())
		.commit();
	}
	
}
