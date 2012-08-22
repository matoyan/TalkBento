package mobisocial.bento.test.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;

public class AudioPlayer{
	
	private static final int SOUND_RATE = 8000; // 8000 or 44100
	private static final String TAG = "AudioPlayer";
	private ArrayList<byte[]> datalist;
	private boolean playing = false;
	
	public AudioPlayer(){
		datalist = new ArrayList<byte[]>();
	}
	
	public void add(byte[] data){
		datalist.add(data);
	}
	
	public void stop(){
		playing = false;
	}
	
	public void play(){
		new MyPlayer().execute();
	}
		
	class MyPlayer extends AsyncTask<Void, Void, Exception> {

		@Override
		protected void onPreExecute() {
			playing = true;
		}
		
		@Override
		protected Exception doInBackground(Void... params) {
			android.os.Process.setThreadPriority(
			          android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

			int bufferSizeInBytes = android.media.AudioTrack.getMinBufferSize(
					SOUND_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
			AudioTrack audioTrack = new AudioTrack(
					AudioManager.STREAM_VOICE_CALL,
					SOUND_RATE,
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					bufferSizeInBytes*10,
					AudioTrack.MODE_STREAM);
			audioTrack.play();
			
			while(playing){
				if(datalist.size()>0){
					byte[] data = datalist.remove(0);
					short[] audio = new short[data.length/2];
					ByteArrayInputStream bais = new ByteArrayInputStream(data);
					BufferedInputStream bis = new BufferedInputStream(bais);
					DataInputStream dis = new DataInputStream(bis);
					try {
						int i=0;
						while(dis.available()>0){
							audio[i] = dis.readShort();
							i++;
						}
						bais.close();
						bis.close();
						dis.close();
						audioTrack.write(audio, 0, audio.length);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}else{
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			audioTrack.stop();
			return null;
		}
		
		@Override
		protected void onPostExecute(Exception result) {
			Log.d(TAG, "Playing Finished");
		}	
	}
}
