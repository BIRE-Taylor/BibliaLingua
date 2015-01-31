package es.tjon.bl.listener;
import android.app.*;
import es.tjon.bl.*;

public abstract class BookInterface  extends BaseActivity
{


	public abstract int getTopUiHeight();

	public abstract boolean openUrl(String url);
	public abstract void scrollTo(String url, double center, float scroll);
	public abstract String getUri();
}
