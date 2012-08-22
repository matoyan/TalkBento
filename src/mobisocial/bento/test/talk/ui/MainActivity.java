package mobisocial.bento.test.talk.ui;

import org.json.JSONException;
import org.json.JSONObject;


import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import edu.stanford.junction.Junction;
import edu.stanford.junction.JunctionException;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;

import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.FeedObserver;
import mobisocial.socialkit.musubi.Musubi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import mobisocial.bento.test.talk.R;
import mobisocial.bento.test.talk.io.DataManager;
import mobisocial.bento.test.util.AudioSender;
import mobisocial.bento.test.util.InitialHelper;
import mobisocial.bento.test.util.InitialHelper.OnInitCompleteListener;

public class MainActivity extends SherlockFragmentActivity {

	private static final String TAG = "MainActivity";
	private static DataManager mManager = DataManager.getInstance();
	private AudioSender audioSender;
	private Junction mJunction;
	private Musubi mMusubi;
	private boolean isOnline = false;
	private DbObj parent;
	private String myname;
	private String uuid = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        uuid = null;
        parent = null;
        
        // create Musubi Instance
        InitialHelper initHelper = new InitialHelper(this, mInitCompleteListener);
		initHelper.initMusubiInstance(true);
		
	}

	@Override
	protected void onDestroy() {
		disconnect();
		super.onDestroy();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
//		getSupportMenuInflater().inflate(R.menu.menu_items, menu);

		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
//		if (item.getItemId() == R.id.menu_add) {
//			return true;
//		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
//		if(requestCode == REQUEST_PICK){
//			if(resultCode == RESULT_OK){
//			}
//        }
	}
	
	private OnInitCompleteListener mInitCompleteListener = new OnInitCompleteListener() {
		@Override
		public void onInitCompleted() {
			setupButtons();
			
			mMusubi = mManager.getMusubi();
			if(mMusubi.getFeed()!=null){
	    		myname = mMusubi.userForLocalDevice(mMusubi.getFeed().getUri()).getName();
	    		
				parent = mMusubi.getFeed().getLatestObj(DataManager.TYPE_TALKBENTO);
				DbObj obj = mMusubi.getFeed().getLatestObj(DataManager.TYPE_APP_STATE);
				if(obj != null){
					uuid = mManager.getJunctionHash(obj);
				}
				
				if(uuid == null || parent == null){
					if(parent == null){
						// send parent and generate new
						Uri objUri = mManager.postAppObj(mMusubi.getFeed());
						parent = mMusubi.objForUri(objUri);
					
						if(uuid == null){
							// TODO remove
							while(uuid == null){
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								parent = mMusubi.objForUri(objUri);
								uuid = parent.getUniversalHashString();
							}
						}
					}else{
						uuid = parent.getUniversalHashString();
					}
					mManager.pushUpdate(parent.getSubfeed(), myname+" has joined @TalkBento", uuid);
				}
			}else{
				Log.d(TAG, "FALIED TO GET FEED");
			}
			connect(uuid);
   		}			
	};
	
	private void connect(String uuid){
		Log.d(TAG, "Connecting to uuid="+String.valueOf(uuid));
		if(uuid!=null){
    		try {
				mJunction = mManager.junctionForHash(mStatusUpdater, uuid);
			} catch (JunctionException e1) {
				Log.d(TAG, "Failed to connect Junction");
				e1.printStackTrace();
			}
		}
		if(mJunction!=null){
        	audioSender = new AudioSender(mJunction);
        	audioSender.startRecording();
        	isOnline = true;
		}else{
			isOnline = false;
		}
		updateDisplay();
	}
		
	private JunctionActor mStatusUpdater = new JunctionActor() {
        @Override
        public void onMessageReceived(MessageHeader header, final JSONObject json) {
        	Log.d(TAG, "Message Received");
			final boolean mymessage = mJunction.getActor().getActorID().equals(header.getSender());
			
			if(!mymessage){
				final String sender = header.getSender();
    			try {
    				if(mManager.isValidLag(sender, json.getLong("time"))){
    					mManager.addQueue(sender, json.getString("data"));
    				}else{
    					Log.e(TAG, "Could not find Time parameter in Junction message");
    				}
    			} catch (JSONException e) {
    				Log.e(TAG,"Cannot read position from Junciton");
    			}
			}
        }

    };

	// ----------------------------------------------------------
	// Musubi
	// ----------------------------------------------------------
	private final FeedObserver mStateObserver = new FeedObserver() {
		@Override
		public void onUpdate(DbObj obj) {
			// do something
		}
	};

	private void disconnect(){
		if(uuid!=null){
			mManager.pushUpdate(parent.getSubfeed(), myname+" is leaving...", uuid);
		}
    	if(audioSender!=null){
    		audioSender.stopRecording();
    		audioSender = null;
    	}
    	if(mJunction!=null){
    		mJunction.disconnect();
    		mJunction = null;
    	}
		mManager.stopAllPlayers();
		isOnline=false;
		updateDisplay();
	}
	
	private void setupButtons(){
        Button inbtn = (Button)findViewById(R.id.toggleTalk);
        inbtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	if(isOnline){
            		disconnect();
            	}else{
            		if(uuid!=null){
    					mManager.pushUpdate(parent.getSubfeed(), myname+" has joined @TalkBento", uuid);
            			connect(uuid);
            		}else{
            			askMusubi();
            		}
            	}
            }
        });
	}
	
	private void updateDisplay(){
		ImageView icon = (ImageView)findViewById(R.id.stateIcon);
        Button inbtn = (Button)findViewById(R.id.toggleTalk);
		if(isOnline){
			icon.setImageResource(R.drawable.online);
			inbtn.setText("Disconnect");
		}else{
			icon.setImageResource(R.drawable.offline);
			inbtn.setText("Connect");
		}
	}

	private void askMusubi(){
		new AlertDialog.Builder(MainActivity.this)
		.setTitle("Launch from Musubi")
		.setIcon(android.R.drawable.ic_dialog_info)
		.setCancelable(false)
		.setMessage("Please launch from Musubi to go online.")
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					// Launching Musubi
	                Intent intent = new Intent(Intent.ACTION_MAIN);
	                intent.setClassName("mobisocial.musubi", "mobisocial.musubi.ui.FeedListActivity"); 
	                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
	                startActivity(intent);
	                finish();
				} catch (Exception e) {
					e.printStackTrace();
					finish();
				}
			}
		})
		.setNegativeButton("Cancel", null)
        .show();
	}
}