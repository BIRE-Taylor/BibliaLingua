package es.tjon.biblialingua.utils;

import android.content.*;
import android.text.*;
import android.text.method.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import es.tjon.biblialingua.utils.CustomLinkMovementMethod.*;

public class CustomLinkMovementMethod extends LinkMovementMethod
{

	private static Context movementContext;

	private static CustomLinkMovementMethod linkMovementMethod = new CustomLinkMovementMethod();

	private static CustomLinkMovementMethod.LinkListener mListener;

	public boolean onTouchEvent(android.widget.TextView widget, android.text.Spannable buffer, android.view.MotionEvent event)
	{
		int action = event.getAction();

		if (action == MotionEvent.ACTION_UP)
		{
			int x = (int) event.getX();
			int y = (int) event.getY();

			x -= widget.getTotalPaddingLeft();
			y -= widget.getTotalPaddingTop();

			x += widget.getScrollX();
			y += widget.getScrollY();

			Layout layout = widget.getLayout();
			int line = layout.getLineForVertical(y);
			int off = layout.getOffsetForHorizontal(line, x);

			URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);
			if (link.length != 0)
			{
				mListener.openUrl(link[0].getURL());
				return true;
			}
		}

		return super.onTouchEvent(widget, buffer, event);
	}

	public static android.text.method.MovementMethod getInstance(Context c, LinkListener listener)
	{
		movementContext = c;
		mListener = listener;
		return linkMovementMethod;
	}
	
	public interface LinkListener
	{
		public boolean openUrl(String url);
	}
}
