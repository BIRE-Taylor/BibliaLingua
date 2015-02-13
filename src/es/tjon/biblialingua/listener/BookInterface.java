package es.tjon.biblialingua.listener;
import android.app.*;
import es.tjon.biblialingua.*;

public abstract class BookInterface  extends BaseActivity
{

	public abstract void scrollTo(double center, double contentHeight);


	public abstract int getTopUiHeight();

	public abstract boolean openUrl(String url);
	public abstract void scrollTo(String url, double center, float scroll);
	public abstract String getUri();
}
