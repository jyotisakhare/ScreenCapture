package com.jyoti.screencapture;

import android.util.Log;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.RealmClass;

/**
 * Created by jyotisakhare on 13/11/16.
 */
@RealmClass
public class ScreenShotImage extends RealmObject {
    private String imageName;
    private Date dateCreated;
    private int syncStatus;

    public ScreenShotImage(String imageName, Date dateCreated, int syncStatus) {
        this.imageName = imageName;
        this.dateCreated = dateCreated;
        this.syncStatus = syncStatus;
    }

    public ScreenShotImage() {
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        imageName = imageName;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public int getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(int syncStatus) {
        this.syncStatus = syncStatus;
    }

    public void add(){
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        ScreenShotImage image = realm.createObject(ScreenShotImage.class);
        image.setImageName(imageName);
        image.setDateCreated(dateCreated);
        image.setSyncStatus(syncStatus);
        realm.commitTransaction();
        realm.close();
    }

    public static void updateSyncStatus(String fileName, int syncStatus) {
        Log.d("notfound", "updateSyncStatus: " + fileName);
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        ScreenShotImage image = realm.where(ScreenShotImage.class).equalTo("imageName", fileName).findFirst();
        if (image != null) {
            image.setSyncStatus(syncStatus);
        }
        realm.commitTransaction();
        realm.close();
    }

    public static RealmResults<ScreenShotImage> getImagesToUpload() {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        RealmResults<ScreenShotImage> realmResults = realm.where(ScreenShotImage.class).equalTo("syncStatus", SyncStatus.NEVER_SYNCED.getCode()).findAll();
        realm.commitTransaction();
        return realmResults;
    }

    public static int getImageCount() {
        int count = 0;
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        RealmResults<ScreenShotImage> realmResults = realm.where(ScreenShotImage.class).findAll();
        count = realmResults.size();
        realm.commitTransaction();
        realm.close();
        return count;

    }

}
