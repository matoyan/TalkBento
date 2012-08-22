package mobisocial.bento.test.util;

import mobisocial.bento.test.talk.R;
import mobisocial.bento.test.talk.io.DataManager;
import mobisocial.socialkit.musubi.Musubi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;

public class InitialHelper {
    public interface OnInitCompleteListener {
        public void onInitCompleted();
    }
    
	private Activity mActivity = null;
	private OnInitCompleteListener mListener;
	
	public InitialHelper(final Activity activity, OnInitCompleteListener listener) {
		mActivity = activity;
		mListener = listener;
	}
	
	public Musubi initMusubiInstance(boolean readDecks) {
		// Check if Musubi is installed
		boolean bInstalled = false;
		try {
			bInstalled = Musubi.isMusubiInstalled(mActivity);
		} catch (Exception e) {
			// be quiet
			bInstalled = false;
		}
		if (!bInstalled) {
			goMusubiMarket();
			return null;
		}
		
		// Intent
		Intent intent = mActivity.getIntent();

		// create Musubi Instance
		Musubi musubi = Musubi.forIntent(mActivity, intent);

		// Check if this activity launched from apps feed
		if (musubi == null) {
			// go to market
			goMarket();
			mActivity.finish();
			return null;
		}
		
		// get version code
		int versionCode = 0;
		try {
			PackageInfo packageInfo = mActivity.getPackageManager().getPackageInfo(
					"mobisocial.bento.talk", PackageManager.GET_META_DATA);
			versionCode = packageInfo.versionCode;
		} catch (NameNotFoundException e) {
		    e.printStackTrace();
		}
		
		new InitAsyncTask(mActivity, musubi, versionCode).execute();
		return musubi;
	}
	
	public void goMusubiMarket() {
		AlertDialog.Builder marketDialog = new AlertDialog.Builder(mActivity)
				.setTitle(R.string.market_dialog_title)
				.setMessage(R.string.market_dialog_text)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setCancelable(true)
				.setPositiveButton(mActivity.getResources().getString(R.string.market_dialog_yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Go to Android Market
						mActivity.startActivity(Musubi.getMarketIntent());
						mActivity.finish();
					}
				})
				.setNegativeButton(mActivity.getResources().getString(R.string.market_dialog_no),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								mActivity.finish();
							}
						});
		marketDialog.create().show();
	}


	public void goMarket() {
		// Go to Market
		Uri uri = Uri.parse("market://details?id=mobisocial.bento.talk");
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		mActivity.startActivity(intent);
		mActivity.finish();
	}

	private class InitAsyncTask extends AsyncTask<Void, Void, Boolean> {
		private DataManager mManager = DataManager.getInstance();
		private Context mContext;
		private ProgressDialog mProgressDialog = null;
		private Musubi mMusubi;
		
		public InitAsyncTask(Context context, Musubi musubi, int versionCode) {
			mContext = context;
			mMusubi  = musubi;
		}

		@Override
		protected void onPreExecute() {
			// show progress dialog
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setMessage(mContext.getString(R.string.home_loading));
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			mManager.init(mMusubi, mContext);
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			try {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				
				// callback
				mListener.onInitCompleted();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
		
}
