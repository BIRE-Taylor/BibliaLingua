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
		
		setContentView(R.layout.main);
		
		getSupportFragmentManager().beginTransaction()
		.replace(R.id.content,new SettingsFragment())
		.commit();
	}
	
}
