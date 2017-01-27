package com.umpquariversoftware.metronome.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class contentProvider extends ContentProvider {
    public contentProvider() {
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        switch (uri.getPathSegments().get(0)){
            case dbContract.ComponentTable.TABLE_NAME:{
                // delete from component table
                Log.e("contentProvider","Matched component URI via case/switch");
            }
            case dbContract.KitTable.TABLE_NAME:{
                // delete from kit table
                Log.e("contentProvider","Matched kit URI via case/switch");
            }
            case dbContract.PatternTable.TABLE_NAME:{
                // delete from pattern table
                Log.e("contentProvider","Matched pattern URI via case/switch");
            }
            case dbContract.JamTable.TABLE_NAME:{
                // delete from jam table
                Log.e("contentProvider","Matched jam URI via case/switch");
            }
        }

        Log.e("contentProvider","delete() called with uri: " + uri.toString());
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        Log.e("contentProvider","getType() called with uri: " + uri.toString());
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        switch (uri.getPathSegments().get(0)){
            case dbContract.ComponentTable.TABLE_NAME:{
                // insert to component table
                Log.e("contentProvider","Matched component URI via case/switch");
            }
            case dbContract.KitTable.TABLE_NAME:{
                // insert to kit table
                Log.e("contentProvider","Matched kit URI via case/switch");
            }
            case dbContract.PatternTable.TABLE_NAME:{
                // insert to pattern table
                Log.e("contentProvider","Matched pattern URI via case/switch");
            }
            case dbContract.JamTable.TABLE_NAME:{
                // insert to jam table
                Log.e("contentProvider","Matched jam URI via case/switch");
            }
        }
        Log.e("contentProvider", "insert() sees these contentValues: " + values.toString());
        return null;
    }

    @Override
    public boolean onCreate() {
        Log.e("contentProvider","onCreate called");
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        switch (uri.getPathSegments().get(0)){
            case dbContract.ComponentTable.TABLE_NAME:{
                // query component table
                Log.e("contentProvider","Matched component URI via case/switch");
            }
            case dbContract.KitTable.TABLE_NAME:{
                // query kit table
                Log.e("contentProvider","Matched kit URI via case/switch");
            }
            case dbContract.PatternTable.TABLE_NAME:{
                // query pattern table
                Log.e("contentProvider","Matched pattern URI via case/switch");
            }
            case dbContract.JamTable.TABLE_NAME:{
                // query jam table
                Log.e("contentProvider","Matched jam URI via case/switch");
            }
        }

        Log.e("contentProvider","query() called with uri: " + uri.toString());
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        switch (uri.getPathSegments().get(0)){
            case dbContract.ComponentTable.TABLE_NAME:{
                // update component table
                Log.e("contentProvider","Matched component URI via case/switch");
            }
            case dbContract.KitTable.TABLE_NAME:{
                // update kit table
                Log.e("contentProvider","Matched kit URI via case/switch");
            }
            case dbContract.PatternTable.TABLE_NAME:{
                // update pattern table
                Log.e("contentProvider","Matched pattern URI via case/switch");
            }
            case dbContract.JamTable.TABLE_NAME:{
                // update jam table
                Log.e("contentProvider","Matched jam URI via case/switch");
            }
        }

        Log.e("contentProvider","update() called with uri: " + uri.toString());
        return 0;
    }
}
