package es.tjon.biblialingua.utils;
import android.content.*;
import android.media.*;
import android.net.*;
import java.io.*;
import android.widget.*;
import android.media.MediaPlayer.*;
import android.os.*;

public class MediaUtil implements MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener ,MediaController.MediaPlayerControl, AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener
{
	
	private MediaPlayer mPlayer;
	
	private String mUri;

	private Context mContext;

	private int buffered = 0;

	private boolean mPrepared;

	private MediaController controller;

	private MediaPlayer.OnPreparedListener mPreparedListener;
	
	public MediaUtil(Context context)
	{
		mContext = context;
	}
	
	public boolean start(String url,MediaPlayer.OnCompletionListener completionListener , MediaPlayer.OnErrorListener listener, MediaPlayer.OnPreparedListener preparedListener)
	{
		if(mPlayer!=null)
			mPlayer.release();
		else
			mPlayer=new MediaPlayer();
		try
		{
			mPreparedListener=preparedListener;
			mPrepared=false;
			buffered=0;
			AudioManager audio=(AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
			audio.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
			mPlayer.reset();
			mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mPlayer.setOnBufferingUpdateListener(this);
			mPlayer.setOnErrorListener(listener==null?this:listener);
			mPlayer.setDataSource(url);
			mPlayer.setOnPreparedListener(this);
			mPlayer.setOnCompletionListener(completionListener==null?this:completionListener);
			mPlayer.setWakeMode(mContext,PowerManager.PARTIAL_WAKE_LOCK);
			mPlayer.prepareAsync();
			mUri=url;
		}
		catch (IllegalArgumentException e)
		{}
		catch (IllegalStateException e)
		{}
		catch (IOException e)
		{}
		catch (SecurityException e)
		{}
		return false;
	}
	
	public MediaController getController()
	{
		if(controller==null)
			controller = new MediaController(mContext);
		controller.setMediaPlayer(this);
		return controller;
	}

	@Override
	public void onPrepared(MediaPlayer player)
	{
		mPrepared=true;
		getController().setEnabled(true);
		player.start();
		if(mPreparedListener!=null)
			mPreparedListener.onPrepared(player);
	}

	@Override
	public void onCompletion(MediaPlayer p1)
	{
		p1.release();
		mPlayer=null;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer p1, int p2)
	{
		buffered = p2;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra)
	{
		System.err.println("Media Play Failed");
		return false;
	}

	@Override
	public void onAudioFocusChange(int p1)
	{
		switch(p1)
		{
			case AudioManager.AUDIOFOCUS_LOSS:
				mPlayer.release();
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				mPlayer.pause();
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				mPlayer.setVolume(0.1f,0.1f);
				break;
			case AudioManager.AUDIOFOCUS_GAIN:
				mPlayer.setVolume(1f,1f);
				mPlayer.start();
				break;
		}
	}

	@Override
	public void start()
	{
		if(mPlayer!=null&&mPrepared)
			mPlayer.start();
	}

	@Override
	public void pause()
	{
		if(mPlayer!=null&&mPrepared)
			mPlayer.pause();
	}
	
	public MediaPlayer getPlayer()
	{
		return mPlayer;
	}

	@Override
	public int getDuration()
	{
		if(mPlayer!=null)
			return mPlayer.getDuration();
		return 0;
	}

	@Override
	public int getCurrentPosition()
	{
		if(mPlayer!=null&&mPrepared)
			return mPlayer.getCurrentPosition();
		return 0;
	}

	@Override
	public void seekTo(int p1)
	{
		if(mPlayer!=null&&mPrepared)
			mPlayer.seekTo(p1);
	}

	@Override
	public boolean isPlaying()
	{
		if(mPlayer!=null&&mPrepared)
			return mPlayer.isPlaying();
		return false;
	}

	@Override
	public int getBufferPercentage()
	{
		if(mPlayer!=null&&mPrepared)
			return buffered;
		return 0;
	}

	@Override
	public boolean canPause()
	{
		return true;
	}

	@Override
	public boolean canSeekBackward()
	{
		return true;
	}

	@Override
	public boolean canSeekForward()
	{
		return true;
	}

	@Override
	public int getAudioSessionId()
	{
		if(mPlayer!=null)
			return mPlayer.getAudioSessionId();
		return 0;
	}
	
	public void release()
	{
		if(mPlayer!=null&&mPrepared)
			mPlayer.release();
			mPrepared=false;
	}
}
