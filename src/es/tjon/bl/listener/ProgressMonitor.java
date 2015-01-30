package es.tjon.bl.listener;
import es.tjon.bl.data.catalog.*;

public interface ProgressMonitor
{

	public void notifyError(Book item);
	public void onProgress(Book book, int progress);
	public void onFinish(Book book);
}
