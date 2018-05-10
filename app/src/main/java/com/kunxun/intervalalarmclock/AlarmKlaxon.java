package com.kunxun.intervalalarmclock;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;

/**
 * Manages alarms and vibe. Runs as a service so that it can continue to play
 * if another activity overrides the AlarmAlert dialog.
 */
public class AlarmKlaxon extends Service {

    /**
     * Play alarm up to 10 minutes before silencing
     */
    // Get value from SettingActivity
    private static final String ALARM_TIMEOUT = "10";

    private static final long[] sVibratePattern = new long[]{500, 500};

    private boolean mPlaying = false;
    private Vibrator mVibrator;
    private MediaPlayer mMediaPlayer;
    private Alarm mCurrentAlarm;
    private long mStartTime;
    private TelephonyManager mTelephonyManager;
    private int mInitialCallState;
    private AudioManager mAudioManager = null;
    private boolean mCurrentState = true;

    // Volume suggested by media team for in-call alarms.
    private static final float IN_CALL_VOLUME = 0.125f;

    // Internal message
    private static final int KILLER = 1;
    private static final int FOCUS_CHANGE = 2;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case KILLER:
                    sendKillBroadcast((Alarm) msg.obj);
                    stopSelf();
                    break;
                case FOCUS_CHANGE:
                    switch (msg.arg1) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            if (!mPlaying && mMediaPlayer != null) {
                                stop();
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if (!mPlaying && mMediaPlayer != null) {
                                mMediaPlayer.pause();
                                mCurrentState = false;
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            if (mPlaying && !mCurrentState) {
                                play(mCurrentAlarm);
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            // The user might already be in a call when the alarm fires. When we register
            // onCallStateChanged, we get the initial in-call state which kills the alarm.
            // Check against the initial call state so we don't kill the alarm during the call.
            if (state != TelephonyManager.CALL_STATE_IDLE && state != mInitialCallState) {
                sendKillBroadcast(mCurrentAlarm);
                stopSelf();
            }
        }
    };

    private AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    mHandler.obtainMessage(FOCUS_CHANGE, focusChange, 0).sendToTarget();
                }
            };

    @Override
    public void onCreate() {
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // Listen for incoming call to kill the alarm
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        assert mTelephonyManager != null;
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        AlertWakeLock.acquireCpuWakeLock(this);
    }

    @Override
    public void onDestroy() {
        stop();

        // Stop listening for incoming calls.
        mTelephonyManager.listen(mPhoneStateListener, 0);
        AlertWakeLock.releaseCpuLock();
        mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // No intent, tell the system not to restart us.
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        final Alarm alarm = intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
        if (alarm == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (mCurrentAlarm != null) {
            sendKillBroadcast(mCurrentAlarm);
        }

        play(alarm);
        mCurrentAlarm = alarm;

        // Record the initial call state here so that the new alarm has the newest state.
        mInitialCallState = mTelephonyManager.getCallState();
        return START_STICKY;
    }

    private void sendKillBroadcast(Alarm alarm) {
        long millis = System.currentTimeMillis() - mStartTime;
        int minutes = (int) Math.round(millis / 60000.0);
        Intent alarmKilled = new Intent(Alarms.ALARM_KILLED);
        alarmKilled.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        alarmKilled.putExtra(Alarms.ALARM_KILLED_TIMEOUT, minutes);
        sendBroadcast(alarmKilled);
    }

    private void play(Alarm alarm) {
        // Stop() checks to see if we are ready playing.
        mAudioManager.requestAudioFocus(mAudioFocusChangeListener, AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        stop();
        if (!alarm.silent) {
            Uri alert = alarm.alert;
            // Fall back on the default alarm if the database does not have an alarm stored.
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }

            // Reuse mMediaPlayer instead of creating a new one and/or use RingtoneManager.
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mp.stop();
                    mp.release();
                    mMediaPlayer = null;
                    return true;
                }
            });

            try {
                // Check if we are in a call. If we are, use the in-call alarm resource at a low
                // volume to not disrupt the call
                if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                    mMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                    setDataSourceFromResource(getResources(), mMediaPlayer,
                            R.raw.in_call_alarm);
                } else {
                    mMediaPlayer.setDataSource(this, alert);
                }
                startAlarm(mMediaPlayer);
            } catch (Exception ex) {
                // The alert may be on the sd card which could be busy right now. Use the fallback ringtone.
                try {
                    // Must rest the media player to clear the error state.
                    mMediaPlayer.reset();
                    setDataSourceFromResource(getResources(), mMediaPlayer, R.raw.fallbackring);
                    startAlarm(mMediaPlayer);
                } catch (Exception ex2) {
                    Log.v("Lily","Failed to play fallback ringtong " + ex2);
                }
            }
        }

        // Start the vibrate after everything is ok with the media player
        if (alarm.vibrate) {
            mVibrator.vibrate(sVibratePattern, 0);
        } else {
            mVibrator.cancel();
        }

        enableKiller(alarm);
        mPlaying = true;
        mStartTime = System.currentTimeMillis();
    }

    /**
     * Stops alarm audio and disables alarm if it not snoozed and not
     * repeating
     */
    public void stop() {
        if (mPlaying) {
            mPlaying = false;

            Intent alarmDone = new Intent(Alarms.ALARM_DONE_ACTION);
            sendBroadcast(alarmDone);

            // Stop audio playing
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            // Stop the vibrate
            mVibrator.cancel();
        }
        disabledKiller();
    }

    // Do the common stuff when starting the alarm
    private void startAlarm(MediaPlayer player)
            throws IOException, IllegalArgumentException, IllegalStateException {
        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // Do not play alarms if stream volume is 0 (typically because ringer mode is silent).
        assert audioManager != null;
        if (audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) != 0) {
            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.setLooping(true);
            player.prepare();
            player.start();
        }
    }

    private void setDataSourceFromResource(Resources resource, MediaPlayer player, int res)
            throws IOException {
        AssetFileDescriptor afd = resource.openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        }
    }

    /**
     * Kills alarm audio after ALARM_TIMEOUT_SECONDS, so the alarm
     * won't run all day.
     * <p>
     * This just cancels the audio, but leaves the notification
     * popped, so the user will know that the alarm tripped.
     */

    private void enableKiller(Alarm alarm) {
        String timeOut = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingActivity.KEY_ALERT_TIMEOUT, ALARM_TIMEOUT);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(KILLER, alarm), 1000 * Integer.parseInt(timeOut) * 60);
    }

    private void disabledKiller() {
        mHandler.removeMessages(KILLER);
    }

}
