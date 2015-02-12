package es.tjon.biblialingua.listener;
import es.tjon.biblialingua.data.catalog.*;

public interface ProgressMonitor
{

	public void notifyError(Book item);
	public void onProgress(Book book, int progress);
	public void onFinish(Book book);
}
