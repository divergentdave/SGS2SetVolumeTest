package dnc.setvolumetest;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements
		OnAudioFocusChangeListener, OnCompletionListener, OnPreparedListener
{
	private class SingleFileButtonClickListener implements View.OnClickListener
	{
		private int resourceId;

		public SingleFileButtonClickListener(int id)
		{
			this.resourceId = id;
		}

		@Override
		public void onClick(View v)
		{
			currentResource = resourceId;
			startMusic();
		}
	}

	private class PanButtonClickListener implements View.OnClickListener
	{
		private float left, right;

		public PanButtonClickListener(float left, float right)
		{
			this.left = left;
			this.right = right;
		}

		@Override
		public void onClick(View v)
		{
			currentLeftLevel = left;
			currentRightLevel = right;
			if (mediaPlayer != null)
			{
				mediaPlayer.setVolume(left, right);
			}
			else
			{
				Toast.makeText(MainActivity.this,
						R.string.startAFileFirstError, Toast.LENGTH_LONG)
						.show();
			}
		}
	}

	private MediaPlayer mediaPlayer;
	private AudioManager audioManager;
	private float currentLeftLevel, currentRightLevel;
	private boolean haveAudioFocus;
	private int currentResource;
	private boolean headphonesNag;
	private AlertDialog headphonesAlertDialog;

	public MainActivity()
	{
		haveAudioFocus = false;
		currentLeftLevel = 1.0f;
		currentRightLevel = 1.0f;
		headphonesNag = true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button aButton = (Button)findViewById(R.id.aButton);
		Button bButton = (Button)findViewById(R.id.bButton);
		Button leftButton = (Button)findViewById(R.id.leftButton);
		Button centerButton = (Button)findViewById(R.id.centerButton);
		Button rightButton = (Button)findViewById(R.id.rightButton);

		aButton.setOnClickListener(new SingleFileButtonClickListener(
				R.raw.short_tone));
		bButton.setOnClickListener(new SingleFileButtonClickListener(
				R.raw.long_tone));
		leftButton.setOnClickListener(new PanButtonClickListener(1.0f, 0.0f));
		centerButton.setOnClickListener(new PanButtonClickListener(1.0f, 1.0f));
		rightButton.setOnClickListener(new PanButtonClickListener(0.0f, 1.0f));
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onStart()
	{
		super.onStart();
		if (audioManager == null)
			audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
		if (headphonesNag)
		{
			if (!audioManager.isBluetoothA2dpOn()
					&& !audioManager.isWiredHeadsetOn())
			{
				headphonesAlertDialog();
			}
			headphonesNag = false;
		}
	}

	@Override
	protected void onPause()
	{
		stopMusic();
		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		if (headphonesAlertDialog != null)
		{
			headphonesAlertDialog.dismiss();
			headphonesAlertDialog = null;
		}
		super.onDestroy();
	}

	private void startMusic()
	{
		if (mediaPlayer != null && mediaPlayer.isPlaying())
		{
			stopMusic();
		}
		if (haveAudioFocus)
		{
			initializeMediaPlayer();
			playSong();
		}
		else
		{
			int result = audioManager.requestAudioFocus(this,
					AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
			switch (result)
			{
				case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
					initializeMediaPlayer();
					playSong();
					break;
				case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
				default:
					Toast.makeText(this, R.string.audioFocusError,
							Toast.LENGTH_LONG).show();
					break;
			}
		}
	}

	private void initializeMediaPlayer()
	{
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setVolume(currentLeftLevel, currentRightLevel);
	}

	private void stopMusic()
	{
		if (mediaPlayer != null)
		{
			if (mediaPlayer.isPlaying())
			{
				mediaPlayer.stop();
			}
			mediaPlayer.release();
			mediaPlayer = null;
		}
		if (audioManager != null)
		{
			audioManager.abandonAudioFocus(this);
		}
	}

	private void playSong()
	{
		try
		{
			AssetFileDescriptor afd = getResources().openRawResourceFd(
					currentResource);
			if (afd == null)
			{
				errorToast();
			}
			else
			{
				mediaPlayer.setDataSource(afd.getFileDescriptor(),
						afd.getStartOffset(), afd.getLength());
				afd.close();
				mediaPlayer.prepareAsync();
			}
		}
		catch (IOException e)
		{
			errorToast();
		}
	}

	private void errorToast()
	{
		Toast.makeText(this, R.string.playError, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onAudioFocusChange(int type)
	{
		switch (type)
		{
			case AudioManager.AUDIOFOCUS_GAIN:
				haveAudioFocus = true;
				if (!mediaPlayer.isPlaying())
				{
					mediaPlayer.start();
				}
				break;
			case AudioManager.AUDIOFOCUS_LOSS:
				haveAudioFocus = false;
				stopMusic();
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				haveAudioFocus = false;
				if (mediaPlayer.isPlaying())
				{
					mediaPlayer.pause();
				}
				break;
		}
	}

	@Override
	public void onPrepared(MediaPlayer arg0)
	{
		mediaPlayer.start();
	}

	@Override
	public void onCompletion(MediaPlayer arg0)
	{
		stopMusic();
		initializeMediaPlayer();
		playSong();
	}

	private void headphonesAlertDialog()
	{
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setCancelable(false);
		b.setMessage(R.string.needHeadphones);
		b.setPositiveButton(R.string.okay, new DismissDialogListener());
		headphonesAlertDialog = b.create();
		headphonesAlertDialog.show();
	}
}
