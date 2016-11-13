package com.jyoti.screencapture;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Created by jyotisakhare on 13/11/16.
 */

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final int REQUEST_SCREEN_CAPTURE = 100;
    private static String STORE_DIRECTORY;
    private static int IMAGES_PRODUCED;
    private static final String SCREENCAP_NAME = "view";
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private static MediaProjection sMediaProjection;

    private MediaProjectionManager mProjectionManager;
    private ImageReader mImageReader;
    private Handler mHandler;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private OrientationChangeCallback mOrientationChangeCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createAccount();
        // call for the projection manager
        IMAGES_PRODUCED = ScreenShotImage.getImageCount() + 1;
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // start projection
        Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startProjection();
            }
        });

        // stop projection
        Button stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                stopProjection();
            }
        });

        // start capture handling thread
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mHandler = new Handler();
                Looper.loop();
            }
        }.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.close:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: ");
        if (requestCode == REQUEST_SCREEN_CAPTURE) {
            sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);

            if (sMediaProjection != null) {
                File externalFilesDir = getExternalFilesDir(null);
                if (externalFilesDir != null) {
                    STORE_DIRECTORY = externalFilesDir.getAbsolutePath() + "/screenshots/";
                    SharedPreferences preferences = getApplicationContext().getSharedPreferences(SSConstants.PREF_NAME,
                            Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(SSConstants.DIR_PATH, STORE_DIRECTORY);
                    editor.apply();
                    File storeDirectory = new File(STORE_DIRECTORY);
                    if (!storeDirectory.exists()) {
                        boolean success = storeDirectory.mkdirs();
                        if (!success) {
                            Log.e(TAG, "failed to create file storage directory.");
                            return;
                        }
                    }
                } else {
                    Log.e(TAG, "failed to create file storage directory, getExternalFilesDir is null.");
                    return;
                }

                // display metrics
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                mDensity = metrics.densityDpi;
                mDisplay = getWindowManager().getDefaultDisplay();

                // create virtual display depending on device width / height
                createVirtualDisplay();

                // register orientation change callback
                mOrientationChangeCallback = new OrientationChangeCallback(this);
                if (mOrientationChangeCallback.canDetectOrientation()) {
                    mOrientationChangeCallback.enable();
                }

                // register media projection stop callback
                sMediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
            }
        }
    }


    private void startProjection() {
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_SCREEN_CAPTURE);
    }

    private void stopProjection() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sMediaProjection != null) {
                    sMediaProjection.stop();
                }
            }
        });
    }


    private void createVirtualDisplay() {
        Point size = new Point();
        mDisplay.getSize(size);
        mWidth = size.x;
        mHeight = size.y;

        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = sMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mHandler);
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);
    }

    private void createAccount() {
        String application = "com.jyoti.screencapture";
        AccountManager am = AccountManager.get(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Account[] accounts = am.getAccountsByType(application);
        if (accounts.length == 0) {
            final Account account = new Account("account", application);
            AccountManager mAccountManager = AccountManager.get(this);
            mAccountManager.addAccountExplicitly(account, "password", null);
            ContentResolver.setSyncAutomatically(account, application, true);
            ContentResolver.addPeriodicSync(
                    account,
                    "com.jyoti.screencapture",
                    Bundle.EMPTY,
                    36000);
        }
    }


    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            FileOutputStream fos = null;
            Bitmap bitmap = null;

            try {
                image = mImageReader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;

                    // create bitmap
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    // write bitmap to a file
                    fos = new FileOutputStream(STORE_DIRECTORY + "/myscreen_" + IMAGES_PRODUCED + ".png");
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                    final File file = new File(STORE_DIRECTORY + "/myscreen_" + IMAGES_PRODUCED + ".png");
                    ScreenShotImage screenShotImage = new ScreenShotImage(file.getName(), new Date(), SyncStatus.NEVER_SYNCED.getCode());
                    screenShotImage.add();
                    SyncAdapter.uplodImage(screenShotImage, getApplicationContext(), STORE_DIRECTORY);
                    IMAGES_PRODUCED++;
                    Log.e(TAG, "captured image: " + file.getName() + " " + STORE_DIRECTORY + "/myscreen_");
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }

                if (bitmap != null) {
                    bitmap.recycle();
                }

                if (image != null) {
                    image.close();
                }
            }
        }
    }

    private class OrientationChangeCallback extends OrientationEventListener {
        public OrientationChangeCallback(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            synchronized (this) {
                final int rotation = mDisplay.getRotation();
                if (rotation != mRotation) {
                    mRotation = rotation;
                    try {
                        // clean up
                        if (mVirtualDisplay != null) mVirtualDisplay.release();
                        if (mImageReader != null)
                            mImageReader.setOnImageAvailableListener(null, null);

                        // re-create virtual display depending on device width / height
                        createVirtualDisplay();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class MediaProjectionStopCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
                    if (mOrientationChangeCallback != null) mOrientationChangeCallback.disable();
                    sMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
                }
            });
        }
    }

}
