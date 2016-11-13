package com.jyoti.screencapture;

import android.app.Application;
import android.content.Context;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import io.realm.Realm;

/**
 * Created by jyotisakhare on 13/11/16.
 */

public class ScreenCaptureApplication extends Application {

    private static Context mContext;

    public static CognitoCachingCredentialsProvider getCredentials() {
        // Initialize the Amazon Cognito credentials provider
        System.setProperty(SDKGlobalConfiguration.ENFORCE_S3_SIGV4_SYSTEM_PROPERTY, "true");

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                mContext,
                SSConstants.IDENTITY_POOL_ID,
                Regions.US_WEST_2 // Region
        );

        return credentialsProvider;
    }

    public static AmazonS3 getS3Client(){
        AmazonS3 s3 = new AmazonS3Client(ScreenCaptureApplication.getCredentials());
        s3.setEndpoint(SSConstants.END_POINT);
        return  s3;
    }
    public void onCreate() {
        mContext = getApplicationContext();
        Realm.init(mContext);
    }
}
