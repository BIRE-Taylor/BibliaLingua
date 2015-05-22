package es.tjon.biblialingua;
import android.app.*;
import com.instabug.library.*;

public class BaseApplication extends Application
{

	@Override
	public void onCreate()
	{
		Instabug.initialize(this, "478d5eb450cec994ac1eb0fe615fcad2");
		super.onCreate();
	}
	
}
