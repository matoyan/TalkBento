package mobisocial.bento.test.talk.io;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;


import edu.stanford.junction.Junction;
import edu.stanford.junction.JunctionException;
import edu.stanford.junction.android.AndroidJunctionMaker;
import edu.stanford.junction.api.activity.JunctionActor;
import mobisocial.bento.test.util.AudioPlayer;
import mobisocial.bento.test.util.Base64;
import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.musubi.multiplayer.FeedRenderable;
import mobisocial.socialkit.obj.AppStateObj;
import mobisocial.socialkit.obj.MemObj;
import android.content.Context;
import android.net.Uri;
import android.util.Log;


public class DataManager {
	private static final String TAG = "DataManager";


	public static final String TYPE_TALKBENTO = "talkbento";
	public static final String TYPE_APP_STATE = "appstate";
	private static long ACCEPTABLE_LAG = 5*1000; // in ms


    private static DataManager sInstance;
    private Musubi mMusubi;

	private Map<String, AudioPlayer> datamap = new HashMap<String, AudioPlayer>();
	private Map<String, Long> lagmap = new HashMap<String, Long>();
	
	// ----------------------------------------------------------
	// Instance
	// ----------------------------------------------------------
	private DataManager() {
		// nothing to do
	}

	public static DataManager getInstance() {
		if (sInstance == null) {
			sInstance = new DataManager();
		}
		return sInstance;
	}

	public void init(Musubi musubi, Context context) {
		mMusubi = musubi;
	}
	
	public Musubi getMusubi() {
		return mMusubi;
	}
	
	public Uri postAppObj(DbFeed feed){
		try {
			JSONObject rootObj = new JSONObject();
			rootObj.put(Obj.FIELD_RENDER_TYPE, Obj.RENDER_LATEST);
			JSONObject out = new JSONObject(rootObj.toString());
						
			MemObj obj = new MemObj(TYPE_TALKBENTO, out, null);
			return feed.insert(obj);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void pushUpdate(DbFeed feed, String msg, String uuid){
		JSONObject state;
		JSONObject b;
		try {
			state = new JSONObject();
			state.put("uuid", uuid);
			b = new JSONObject(state.toString());

			FeedRenderable renderable = FeedRenderable.fromText(msg);
			AppStateObj aso = new AppStateObj(b, renderable);
			feed.postObj(aso);
		} catch (JSONException e) {
			Log.e(TAG, "Failed to post JSON", e);
		}
	}
	
	public String getJunctionHash(DbObj obj){
		try {
			JSONObject jso = obj.getJson();
			if(jso.has("uuid")){
					return jso.getString("uuid");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

    public Junction junctionForHash(JunctionActor actor, String uid)
            throws JunctionException {
    	        
        if(uid == null){
        	Log.e(TAG, "Failed to get hash for obj");
        	return null;
        }
        uid = uid.replace("^", "_").replace(":", "_");
        Uri uri = new Uri.Builder().scheme("junction")
                .authority("sb.openjunction.org")
                .appendPath("dbf-" + uid).build();
        Log.d(TAG, "JUNCTION: "+uri.toString());
        return AndroidJunctionMaker.bind(uri, actor);
    }
    public Junction junctionForObj(JunctionActor actor, DbObj obj)
            throws JunctionException {
    	
        String uid = obj.getUniversalHashString();
        
        // TODO remove this workaround
        if(uid == null){
        	obj = mMusubi.objForUri(obj.getUri());
        	uid = obj.getUniversalHashString();
        }
        
        if(uid == null){
        	Log.e(TAG, "Failed to get hash for obj");
        	return null;
        }
        uid = uid.replace("^", "_").replace(":", "_");
        Uri uri = new Uri.Builder().scheme("junction")
                .authority("sb.openjunction.org")
                .appendPath("dbf-" + uid).build();
        Log.d(TAG, "JUNCTION: "+uri.toString());
        return AndroidJunctionMaker.bind(uri, actor);
    }

    synchronized public boolean isValidLag(String sender, long time){
    	if(lagmap.containsKey(sender)){
    		long initLag = lagmap.get(sender);
    		long currentLag = Math.abs(System.currentTimeMillis() - time);
    		long diff = Math.abs(initLag-currentLag);
			Log.d(TAG, "timelag: "+diff+"ms");
    		if(diff > ACCEPTABLE_LAG){
    			Log.e(TAG, "too much timelag: "+diff+"ms");
    			return false;
    		}else{
    			return true;
    		}
    	}else{
    		long initLag = Math.abs(System.currentTimeMillis() - time);
    		Log.d(TAG, "initial Lag: "+initLag+"ms");
    		lagmap.put(sender, initLag);
    		return true;
    	}
    }
    
    synchronized public void addQueue(String sender, String base64string){
    	if(datamap.containsKey(sender)){
    		datamap.get(sender).add(Base64.decode(base64string));
    	}else{
    		Log.d("TAG", "start for play");
    		AudioPlayer audioPlayer = new AudioPlayer();
    		datamap.put(sender, audioPlayer);
    		audioPlayer.add(Base64.decode(base64string));
    		audioPlayer.play();
    	}
    }
    
    public void stopAllPlayers(){
    	for (String key : datamap.keySet()) {
    		AudioPlayer player = datamap.get(key);
        	player.stop();
    	}
        datamap.clear();
    }
    
    public void deleteQueue(String sender){
    	datamap.remove(sender);
    }
}
