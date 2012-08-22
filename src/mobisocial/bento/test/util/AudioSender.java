package mobisocial.bento.test.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.junction.Junction;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

public class AudioSender{
	
	private static final int SOUND_RATE = 8000; // 8000 or 44100
	private static final int TH_SIZE = 8*1024;
	private static final int TH_WAIT = 8;

	private static final String TAG = "AudioSender";
	private static boolean recording = false;
	private static Junction mJunction;
	
	public AudioSender(Junction junction){
		mJunction = junction;
	}
	
	public void startRecording(){
		if(recording){
			Log.d(TAG, "Already in recording");
		}else{
			recording = true;
			new MyRecorder().execute();
		}
	}
	
	public void stopRecording(){
		recording = false;
	}
	
	public boolean isRecording(){
		return recording;
	}
	
	class MyRecorder extends AsyncTask<Void, Void, Exception> {

		@Override
		protected void onPreExecute() {
			Log.d(TAG, "Recording Start");
		}
		
		@Override
		protected Exception doInBackground(Void... params) {
			android.os.Process.setThreadPriority(
			          android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

			try {
				int source = MediaRecorder.AudioSource.MIC;
				if(Build.VERSION.SDK_INT >= 11){
					source = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
				}
				int bufferSizeRecord = 
						AudioRecord.getMinBufferSize(
								SOUND_RATE, 
								AudioFormat.CHANNEL_IN_MONO, 
								AudioFormat.ENCODING_PCM_16BIT);
				AudioRecord audioRecord = 
						new AudioRecord(
								source,
								SOUND_RATE,
								AudioFormat.CHANNEL_IN_MONO,
								AudioFormat.ENCODING_PCM_16BIT,
								bufferSizeRecord);
				
				short[] bufferRecord = new short[bufferSizeRecord];
				
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				BufferedOutputStream bos = new BufferedOutputStream(os);
				DataOutputStream dos = new DataOutputStream(bos);
				
				audioRecord.startRecording();

				int count = 0;
				while(recording){
					int bufferReadResult = audioRecord.read(bufferRecord,0,bufferSizeRecord);
					for(int i = 0 ; i < bufferReadResult ; i++){
						dos.writeShort(bufferRecord[i]);
						if((count > TH_WAIT && os.size()>0) || os.size() > TH_SIZE){
							Log.d(TAG, os.size()+"bytes");
							JSONObject jso = new JSONObject();
							try {
								jso.put("data", Base64.encodeToString(os.toByteArray(), false));
								jso.put("time", System.currentTimeMillis());
								mJunction.sendMessageToSession(jso);
							} catch (JSONException e) {
								e.printStackTrace();
							}
							count = 0;
							os.reset();
						}
					}
					count++;
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
}
