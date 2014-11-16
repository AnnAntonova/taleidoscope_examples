package ru.fantaversum.taleidoscope;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.UiLifecycleHelper;
import com.facebook.internal.Utility;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;
import com.google.android.gms.plus.PlusShare;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKSdkListener;
import com.vk.sdk.VKUIHelper;
import com.vk.sdk.api.VKError;
import com.vk.sdk.dialogs.VKCaptchaDialog;
import com.vk.sdk.dialogs.VKShareDialog;

abstract public class SocialNetworkActivity extends ActionBarActivity {
	//https://developers.facebook.com/docs/android/getting-started/
	//https://developers.google.com/+/mobile/android/
	//https://vk.com/dev/android_sdk
	
	private static final int GOOGLEPLUS_REQUEST_CODE = 1001;

	// ========== FaceBook vars
	
	private UiLifecycleHelper fbUIHelper;
	
	// ========== Vkontakte vars
	
	private String appId = "1234567"; // Need to change to real app_id
	private static String vkTokenKey = "VK_ACCESS_TOKEN";
	private static String[] vkScope = new String[]{VKScope.WALL};
	private final VKSdkListener vkSdkListener = new VKSdkListener() {
		@Override
	    public void onCaptchaError(VKError captchaError) {
	    	new VKCaptchaDialog(captchaError).show();
		}
	    @Override
	    public void onTokenExpired(VKAccessToken expiredToken) {
	    	VKSdk.authorize(vkScope, true, false);
	    }
	    @Override
	    public void onAccessDenied(VKError authorizationError) {
	    	new AlertDialog.Builder(SocialNetworkActivity.this)
	        	.setMessage(authorizationError.errorMessage)
	            .show();
	    }
	    @Override
	    public void onReceiveNewToken(VKAccessToken newToken) {
	    	newToken.saveTokenToSharedPreferences(getApplicationContext(), vkTokenKey);
	    }
	 };
	 
	// ========== Activity LifeCycle
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// ========== FaceBook Init
		fbUIHelper = new UiLifecycleHelper(this, null);
	    fbUIHelper.onCreate(savedInstanceState);
	    
		// ========== Vkontakte Init
        VKUIHelper.onCreate(this);
	    VKSdk.initialize(vkSdkListener, appId, VKAccessToken.tokenFromSharedPreferences(this, vkTokenKey));

		// ========== For generate KeyHash for Facebook
		// ========== KeyHash is unique for each working PC with IDE!
/*		try {
	        PackageInfo info = getPackageManager().getPackageInfo("ru.fantaversum.taleidoscope", PackageManager.GET_SIGNATURES);
	        for (Signature signature : info.signatures) {
	            MessageDigest md = MessageDigest.getInstance("SHA");
	            md.update(signature.toByteArray());
	            Log.i("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
			}
	    } catch (Exception e) {} 
*/		
		// ========== For generate FingerPrint for Vkontakte
//	    Log.i("FingerPrint:", VKUtil.getCertificateFingerprint(this, this.getPackageName())[0]);  
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
		// ========== FaceBook
	    fbUIHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
	    	//Listener for Facebook-client if installed
	    	@Override
	    	public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
	    		toastMessage("Запись не опубликована");
	    	}

	    	@Override
	    	public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
	    		toastMessage("Если вы сами не отменили команду, то запись опубликована");
	    	}
	    });
		// ========== Vkontakte
	    VKUIHelper.onActivityResult(this, requestCode, resultCode, data); 
		// ========== Google+
	    if ((requestCode == GOOGLEPLUS_REQUEST_CODE) && (resultCode == -1)) {
	    	//Do something if success
		}
	} 
	
	@Override
	protected void onResume() {
	    super.onResume();
	    fbUIHelper.onResume();
	    VKUIHelper.onResume(this); 
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    fbUIHelper.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() {
	    super.onPause();
	    fbUIHelper.onPause();
	}

	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    fbUIHelper.onDestroy();
	    VKUIHelper.onDestroy(this); 
	}
	
	// ========== FaceBook Sharing
	
	/**
	* Publish link in FaceBook
	* @param name - title of block
	* @param caption - text on bottom of block
	* @param description - description of link (between title and caption)
	* @param link - http:// etc
	* @param pictureLink - http:// etc - link on image in web
	*/
	public final void facebookPublish(String name, String caption, String description, String link, String pictureLink) {
  	  if (FacebookDialog.canPresentShareDialog(getApplicationContext(), FacebookDialog.ShareDialogFeature.SHARE_DIALOG)) {
  		  //Facebook-client is installed
  		  FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(this)
	    	.setName(name)
	    	.setCaption(caption)
	    	.setDescription(description)
	    	.setLink(link)
	    	.setPicture(pictureLink)
	    	.build();
  		  fbUIHelper.trackPendingDialogCall(shareDialog.present());	    	  
	  } else {
		  //Facebook-client is not installed – use web-dialog
		  Bundle params = new Bundle();
		  params.putString("name", name);
		  params.putString("caption", caption);
		  params.putString("description", description);
		  params.putString("link", link);
		  params.putString("picture", pictureLink);
		  WebDialog feedDialog = new WebDialog.FeedDialogBuilder(this, Utility.getMetadataApplicationId(this), params)
		  	.setOnCompleteListener(new OnCompleteListener() {
		  		//Listener for web-dialog
		  		@Override
		        public void onComplete(Bundle values, FacebookException error) {
		  			if ((values != null) && (values.getString("post_id") != null) && (error == null)) {
		  				toastMessage("Запись опубликована");
		  			} else {
		  				toastMessage("Запись не опубликована");
		  			};
		  		};
		  	})
		  	.build();
		  feedDialog.show();
	  }
	}

	// ========== Google+ Sharing
	
	/**
	* Publish link in Google+
	* @param text - message about link (may be changed or deleted by user)
	* @param link - http:// etc
	*/
	public final void googleplusPublish(String text, String link) {
		Intent shareIntent = new PlusShare.Builder(this)
          .setType("text/plain")
          .setText(text)
          .setContentUrl(Uri.parse(link))
          .getIntent();
		startActivityForResult(shareIntent, GOOGLEPLUS_REQUEST_CODE);
	}
	
	// ========== Vkontakte Sharing
	
	/**
	* Publish link in Vkontakte
	* @param message - message about link (may be changed or deleted by user)
	* @param link - http:// etc
	* @param linkName - title of link - not published (don't know why...)
	*/
	public final void vkontaktePublish(String message, String link, String linkName) {
		VKAccessToken token = VKAccessToken.tokenFromSharedPreferences(this, vkTokenKey);
		if ((token == null) || token.isExpired()) {
			VKSdk.authorize(vkScope, true, false);
			toastMessage("Требуется авторизация. После нее повторите попытку публикации");
		} else {
			new VKShareDialog()
	        .setText(message)
	        .setAttachmentLink(linkName, link)
	        .setShareDialogListener(new VKShareDialog.VKShareDialogListener() {
	            @Override
	            public void onVkShareComplete(int postId) {
	            	toastMessage("Запись опубликована");
	            }
	            @Override
	            public void onVkShareCancel() {
	            	toastMessage("Запись не опубликована");
	            }
	        }).show(getSupportFragmentManager(), "VK_SHARE_DIALOG");    
		} 
	}
	
	// ========== Utility
	
	private void toastMessage(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}
}
