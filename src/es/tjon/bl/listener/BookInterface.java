package es.tjon.bl.listener;
import android.app.*;
import es.tjon.bl.*;

public abstract class BookInterface  extends BaseActivity
{

	public abstract int getTopUiHeight();

	public abstract boolean openUrl(String url);
	public abstract void scrollTo(double center, double scroll);
	public abstract String getUri();
}
