package cn.redcdn.hvs.im.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import cn.redcdn.hvs.MedicalApplication;
import java.io.File;

/**
 * Designed by guoyx on 4/9/17.
 */

public class AudioManagerHelper {

    private AudioManager manager;
    private MediaPlayer mMediaPlayer;
    private Context context;

    public AudioManagerHelper(){
        context = MedicalApplication.getContext();
        manager = ((AudioManager) context.getSystemService(
            Context.AUDIO_SERVICE));
    }

    public void enableReceiver(String audioPath){
        manager.setSpeakerphoneOn(false);
        manager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        // playNewAudio(audioPath);
    }

    public void enableSpeaker(){
        manager.setSpeakerphoneOn(false);
        manager.setMode(AudioManager.MODE_NORMAL);
    }

    public void playNewAudio(String audioPath){
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
        mMediaPlayer = MediaPlayer.create(context,
            Uri.fromFile(new File(audioPath)));
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.start();
    }
}
