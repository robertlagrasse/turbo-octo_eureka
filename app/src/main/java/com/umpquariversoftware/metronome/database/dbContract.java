package com.umpquariversoftware.metronome.database;

import android.net.Uri;
import android.provider.BaseColumns;

import com.umpquariversoftware.metronome.elements.Pattern;

/**
 * Created by robert on 1/26/17.
 */

public class dbContract {
    /**
     * This section defines all things Uri related for the contract provider.
     * Any call to the CP should used one of the build*Uri() methods to define the Uri.
     */

    public static final Uri CONTENT_AUTHORITY = Uri.parse("content://com.umpquariversoftware.metronome");

    public static Uri buildComponentUri(){
        return CONTENT_AUTHORITY.buildUpon().appendPath(ComponentTable.TABLE_NAME).build();
    }
    public static Uri buildKitUri(){
        return CONTENT_AUTHORITY.buildUpon().appendPath(KitTable.TABLE_NAME).build();
    }
    public static Uri buildPatternUri(){
        return CONTENT_AUTHORITY.buildUpon().appendPath(PatternTable.TABLE_NAME).build();
    }
    public static Uri buildJamUri(){
        return CONTENT_AUTHORITY.buildUpon().appendPath(JamTable.TABLE_NAME).build();
    }

    /**
     * This section defines the tables in the database, and each table's associated columns.
     */

    public static final class ComponentTable implements BaseColumns {
        public static final String TABLE_NAME = "components";
        public static final String ID = "_id";
        public static final String NAME = "name";
        public static final String RESOURCE = "resource";
        public static final String HEXID = "hexid";
    }

    public static final class KitTable implements BaseColumns {
        public static final String TABLE_NAME = "kit";
        public static final String ID = "_id";
        public static final String NAME = "name";
    }

    public static final class PatternTable implements BaseColumns {
        public static final String TABLE_NAME = "pattern";
        public static final String ID = "_id";
        public static final String NAME = "name";
    }

    public static final class JamTable implements BaseColumns {
        public static final String TABLE_NAME = "jam";
        public static final String ID = "_id";
        public static final String NAME = "name";
    }
}
