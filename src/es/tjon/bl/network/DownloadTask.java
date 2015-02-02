package es.tjon.bl.network;

import android.app.*;
import android.content.*;
import android.util.*;
import es.tjon.bl.data.catalog.*;
import es.tjon.bl.network.*;
import es.tjon.bl.utils.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import es.tjon.bl.listener.*;

public class DownloadTask implements Callable<Pair<Book,Boolean>>
{

	Service context;
	Book mItem;

	public DownloadTask(Service c, Book item)
	{
		context = c;
		mItem = item;
	}

	public String getName()
	{
		return mItem.name;
	}

	@Override
	public Pair<Book,Boolean> call()
	{
		try
		{
			File temp = downloadFile();
			if (temp != null)
			{
				try
				{
					BookUtil.getDir(mItem,context).mkdirs();
					File book = BookUtil.getFile(mItem, context);
					ZLib.decompressFile(temp, book);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					return null;
				}
			}
			else
			{
				context.stopForeground(true);
				context.stopSelf();
				return null;
			}
			if(temp!=null)
				temp.delete();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		((ProgressMonitor)context).onFinish(mItem);
		return new Pair<Book,Boolean>(mItem, false);
	}

	File downloadFile() throws IOException
	{
		int bytesWritten = 0;
		URL url = new URL(mItem.url);
		HttpURLConnection conn=null;
		BufferedInputStream in=null;
		try
		{
			conn = (HttpURLConnection) url.openConnection();
			in = new BufferedInputStream(url.openStream());
		}
		catch(UnknownHostException e)
		{
			((ProgressMonitor)context).notifyError(mItem);
			return null;
		}
		File tempFile = File.createTempFile(mItem.name, null, context.getFilesDir());
		OutputStream out = new FileOutputStream(tempFile);
		byte[] TEMP = new byte[1024];
		int size = conn.getContentLength();
		int read=0;
		while ((read = in.read(TEMP)) != -1)
		{
			out.write(TEMP, 0, read);
			bytesWritten += read;
			try
			{
				publishProgress(bytesWritten * 100.0 / size);
			}catch(Exception e)
			{
				e.printStackTrace(System.err);
			}
		}
		out.flush();
		out.close();
		in.close();
		conn.disconnect();
		return tempFile;
	}

	private void publishProgress(double progress)
	{
		((ProgressMonitor)context).onProgress(mItem, (int)progress);
	}


}
