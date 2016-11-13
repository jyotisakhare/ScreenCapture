package com.jyoti.screencapture;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;

import java.io.File;

import io.realm.RealmResults;

/**
 * Created by jyotisakhare on 13/11/16.
 */

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = SyncAdapter.class.getSimpleName();
    private Context context;
    public SyncAdapter(Context applicationContext, boolean b) {
        super(applicationContext, b);
        context = applicationContext;
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s,
                              ContentProviderClient contentProviderClient, SyncResult syncResult) {

        RealmResults<ScreenShotImage> screenShotImages = ScreenShotImage.getImagesToUpload();
        SharedPreferences pref = context.getSharedPreferences(SSConstants.PREF_NAME, Context.MODE_PRIVATE);
        String filePath = pref.getString(SSConstants.DIR_PATH,"");
        Log.d(TAG, "onPerformSync: ");
        for (ScreenShotImage screenshotIMage :
                screenShotImages) {
            uplodImage(screenshotIMage, context, filePath);
        }

    }

    public static void uplodImage(final ScreenShotImage image, Context context, String filePath) {

        final File file = new File(filePath+"/"+image.getImageName());
        if (file == null) {
            Log.d(TAG, "uplodImage: not found");
            return;
        }

        AmazonS3 s3 = ScreenCaptureApplication.getS3Client();
        TransferUtility transferUtility = new TransferUtility(s3, context);
        Log.d(TAG, "uplodImage: "+filePath+"/"+image.getImageName() );
        TransferObserver observer = transferUtility.upload(
                SSConstants.BUCKET_NAME,     /* The bucket to upload to */
                image.getImageName(),    /* The key for the uploaded object */
                file        /* The file where the data to upload exists */
        );

        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d(TAG, "onStateChanged: " + state.toString());
                if (state == TransferState.COMPLETED) {
                    ScreenShotImage.updateSyncStatus(image.getImageName(), SyncStatus.SYNCED.getCode());
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d(TAG, "onProgressChanged: " + bytesCurrent + " " + bytesTotal);
            }

            @Override
            public void onError(int id, Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
