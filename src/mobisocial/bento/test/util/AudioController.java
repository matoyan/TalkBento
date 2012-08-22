package mobisocial.bento.test.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class AudioController{
	
	private static final int SOUND_RATE = 8000; // or 44100
	private static final String TAG = "AudioController";
	private boolean recording = false;
	
	public AudioController(){
	}
	
	public void startRecording(){
		if(recording){
			Log.d(TAG, "Already recording");
		}else{
			recording = true;
			new MyRecorder().execute();
		}
	}
	
	public void stopRecording(){
		recording = false;
	}
	
	class MyRecorder extends AsyncTask<Void, Void, Exception> {

		@Override
		protected void onPreExecute() {
			Log.d(TAG, "Recording Start");
		}
		
		@Override
		protected Exception doInBackground(Void... params) {
			
			try {
				int bufferSizeRecord = 
						AudioRecord.getMinBufferSize(
								SOUND_RATE, 
								AudioFormat.CHANNEL_IN_MONO, 
								AudioFormat.ENCODING_PCM_16BIT);
				AudioRecord audioRecord = 
						new AudioRecord(
								MediaRecorder.AudioSource.MIC,SOUND_RATE,
								AudioFormat.CHANNEL_IN_MONO,
								AudioFormat.ENCODING_PCM_16BIT,
								bufferSizeRecord);
				
				short[] bufferRecord = new short[bufferSizeRecord];
				
				File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/soundTest.pcm");
				if(file.exists()){
					 file.delete();
				}
				file.createNewFile();
			
				OutputStream os = new FileOutputStream(file);
				BufferedOutputStream bos = new BufferedOutputStream(os);
				DataOutputStream dos = new DataOutputStream(bos);
				
				audioRecord.startRecording();

				
				while(recording){
					int bufferReadResult = audioRecord.read(bufferRecord,0,bufferSizeRecord);
					for(int i = 0 ; i < bufferReadResult ; i++){
						dos.writeShort(bufferRecord[i]);
					}
					Log.d(TAG, dos.size()+"bytes");
				}

			} catch (IOException e) {
				e.printStackTrace();
				return e;
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Exception result) {
			Log.d(TAG, "Recording Stopped");
		}	
	}
	
	class MyPlayer extends AsyncTask<Void, Void, Exception> {

		@Override
		protected void onPreExecute() {
			Log.d(TAG, "Playing Start");
		}
		
		@Override
		protected Exception doInBackground(Void... params) {
			
			// test play
			try {
				File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/soundTest.pcm");
				InputStream is = new FileInputStream(file);
				BufferedInputStream bis = new BufferedInputStream(is);
				DataInputStream dis = new DataInputStream(bis);
				int audioLength = (int)(file.length()/2);
				short[] audio = new short[audioLength];
				
				int i = 0;
				while(dis.available() > 0){
					audio[i] = dis.readShort();
					i++;
				}
				dis.close();
				
				int bufferSizeInBytes = android.media.AudioTrack.getMinBufferSize(
						SOUND_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
				AudioTrack audioTrack = new AudioTrack(
						AudioManager.STREAM_MUSIC,
						SOUND_RATE,
						AudioFormat.CHANNEL_OUT_MONO,
						AudioFormat.ENCODING_PCM_16BIT,
						bufferSizeInBytes,
						AudioTrack.MODE_STREAM);
				audioTrack.play();
				
				audioTrack.write(audio, 0 ,audioLength);
				audioTrack.stop();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Exception result) {
			Log.d(TAG, "Playing Finished");
		}	
	}
}
