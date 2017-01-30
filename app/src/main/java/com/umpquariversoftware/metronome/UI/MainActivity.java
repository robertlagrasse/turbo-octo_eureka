package com.umpquariversoftware.metronome.UI;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.umpquariversoftware.metronome.R;
import com.umpquariversoftware.metronome.database.dbContract;
import com.umpquariversoftware.metronome.elements.Beat;
import com.umpquariversoftware.metronome.elements.Component;
import com.umpquariversoftware.metronome.elements.Jam;
import com.umpquariversoftware.metronome.elements.Kit;
import com.umpquariversoftware.metronome.elements.Pattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/** OVERVIEW
 *
 * The user interface for metronome allows the user to build their own Jam.
 * A Jam consists of three parts - all user selectable on the main screen
 *
 * Tempo (Set by seekbar)
 * Kit (Selected via recyclerview)
 * Pattern (Selected via recyclerview)
 *
 * Kits consist of 8 components. A component is a sound.
 * A pattern is a sequence of beats. A beat is an array of 8 boolean values. These boolean values
 * will correlate with the components in each kit.
 *
 * For every tick of the timer, the app will cycle through the pattern. The beat in the pattern
 * will determine which components sound on that tick.
 *
 * I chose the data structures here very specifically. Beats consist of 8 binary values, which
 * correspond to the 8 components in a kit. Any beat can be represented as a two digit hex value.
 * These values can the chained together to create patterns of arbitrary length, with the complete
 * information for any beat only taking up a single byte in the database. This facilitates both
 * efficient DB storage, and easy sharing between users.
 *
 * Component values have associated 2 Digit Hexidecimal values in the database HEXID
 * All components will be supplied with the software. No user supplied sounds will be allowed.
 * Essentially, this allows me to employ the same identification and sharing technique.
 *
 * Users can pass all of the information necessary to share their Jam in the space of a tweet.
 *
 */


public class MainActivity extends AppCompatActivity {
    String TAG = "COUNTER";
    Timer mTimer = new Timer();
    Jam mJam = new Jam();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTimer = null;
        int lastLoadedJamID;

        if(savedInstanceState == null){
            /**
             *  If this is the first time the app's been run, we'll need to do some
             *  basic setup. The database tables will be created and populated,
             *  and the default Jam will be selected.
             */
            createComponentsTable();
            createKitTable();
            createPatternTable();
            createJamTable();
            lastLoadedJamID = 1;
        } else {
            /**
             * We've been here before. Load the last jam we worked with
             */
            lastLoadedJamID = savedInstanceState.getInt(String.valueOf(R.string.jamID));
        }

        mJam = buildJamFromDB(lastLoadedJamID);

        /**
         *
         * Populate the UI
         *
         * TODO: This is all of the recyclerview stuff I have to figure out and build.
         *
         */

        // Display Information about the current Kit
        showKit(mJam.getKit());

        // Display a graph of the current pattern
        graphPattern(mJam.getPattern());

        // setup Tempo Bar
        setupTempoBar();

        // Setup user controls
        setupStartStopFAB();
        // setupSaveFab();
        // setupFavoriteFab();
    }

    public void setupStartStopFAB(){
        FloatingActionButton startstop = new FloatingActionButton(this);
        startstop = (FloatingActionButton) findViewById(R.id.startStopButton);
        startstop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mTimer==null){
                    mTimer = tempoTimerStart();  }
                else {
                    tempoTimerStop();
                }
            }
        });
    }

/**
    public void setupSaveFab(){
        FloatingActionButton saveButton = new FloatingActionButton(this);
        saveButton = (FloatingActionButton) findViewById(R.id.saveJamButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Pattern Check
                // Save if Unique
                Toast.makeText(getApplicationContext(), "save", Toast.LENGTH_LONG).show();
            }
        });
    }
*/

/**
    public void setupFavoriteFab(){
        FloatingActionButton favoriteButton = new FloatingActionButton(this);
        favoriteButton = (FloatingActionButton) findViewById(R.id.favoriteJamButton);
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "favorite", Toast.LENGTH_LONG).show();
            }
        });
    }
*/

    public void setupTempoBar(){
        int tempo = mJam.getTempo();
        SeekBar tempoBar = (SeekBar) findViewById(R.id.tempoBar);
        tempoBar.setProgress(tempo-30);
        tempoBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mJam.setTempo(i+30);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.e("onStopTrackingTouch", "mJam.getTempo: " + mJam.getTempo());
                tempoTimerStop();
                mTimer = tempoTimerStart();
            }
        });
    }

    public Timer tempoTimerStart(){

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

                                      SoundPoolPlayer sound = new SoundPoolPlayer(getApplicationContext(), mJam.getKit());
                                      int position = 0;

                                      @Override
                                      public void run() {
                                          Beat beat = new Beat();
                                          if (position == mJam.getPattern().getLength()) {
                                              position = 0;
                                          }
                                          beat = mJam.getPattern().getBeat(position);

                                          // Iterate through each of the 8 components in the beat
                                          // Play it if marked true
                                          for(int x=0;x<8;++x){
                                              if(beat.getPosition(x)){
                                                  sound.playShortResource(mJam.getKit().getComponents().get(x).getResource());
                                              }
                                          }

                                          position++;
                                      }

                                  },
                //Set how long before to startButton calling the TimerTask (in milliseconds)
                0,
                //Set the amount of time between each execution (in milliseconds)
                mJam.getInterval());
        return timer;
    }

    public void tempoTimerStop(){
        mTimer.cancel();
        mTimer.purge();
        mTimer = null;
    }

    public class SoundPoolPlayer {
        private SoundPool mShortPlayer= null;
        private HashMap mSounds = new HashMap();

        public SoundPoolPlayer(Context pContext)
        {
            // setup Soundpool
            this.mShortPlayer = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);

            mSounds.put(R.raw.bass, this.mShortPlayer.load(pContext, R.raw.bass,1));
            mSounds.put(R.raw.hihat, this.mShortPlayer.load(pContext, R.raw.hihat,1));
            mSounds.put(R.raw.snare, this.mShortPlayer.load(pContext, R.raw.snare,1));
            mSounds.put(R.raw.tom, this.mShortPlayer.load(pContext, R.raw.tom,1));

//            mSounds.put(R.raw.button1.wav, this.mShortPlayer.load(pContext, R.raw.button1.wav, 1));
//            mSounds.put(R.raw.button3.wav, this.mShortPlayer.load(pContext, R.raw.button3.wav, 1));
        }

        public SoundPoolPlayer(Context pContext, Kit kit)
        {
            // setup Soundpool
            this.mShortPlayer = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);

            // Get components from kit
            ArrayList<Component> components = kit.getComponents();

            // Iterate through components, extract resource ID, add to soundpool
            for(int x=0;x<components.size();x++){
                mSounds.put(components.get(x).getResource(), this.mShortPlayer.load(pContext, components.get(x).getResource(),1));
            }
        }

        public void playShortResource(int piResource) {
            int iSoundId = (Integer) mSounds.get(piResource);
            this.mShortPlayer.play(iSoundId, 0.99f, 0.99f, 0, 0, 1);
        }

        // Cleanup
        public void release() {
            // Cleanup
            this.mShortPlayer.release();
            this.mShortPlayer = null;
        }
    }

    private Jam buildTestJam(){
        /**
         * buildTestJam()
         *
         * Build Some Individual Beats
         * Put Beats into a Pattern
         *
         * Build Some Components
         * Put up to 8 components into a Kit
         *
         * Build a Jam
         * Add the Kit, Pattern, and the Tempo
         * */
        // Instantiate Beats
        Beat beat1 = new Beat();
        Beat beat2 = new Beat();
        Beat beat3 = new Beat();
        Beat beat4 = new Beat();
        Beat beat5 = new Beat();
        Beat beat6 = new Beat();
        Beat beat7 = new Beat();
        Beat beat8 = new Beat();


        beat3.setSECOND(true);
        beat7.setSECOND(true);
        beat8.setSECOND(true);
        beat4.setEIGHTH(true);


        // Instantiate Pattern, add beat
        Pattern pattern = new Pattern();
        pattern.addBeat(beat1);
        pattern.addBeat(beat2);
        pattern.addBeat(beat3);
        pattern.addBeat(beat4);
        pattern.addBeat(beat5);
        pattern.addBeat(beat6);
        pattern.addBeat(beat7);
        pattern.addBeat(beat8);
        pattern.setName("Default Pattern");

        // Build components and assemble kit from signature
        Kit kit = new Kit("slick kit name", "05040609080A060C", this);

        // Instantiate Jam - Add Pattern, Kit, Tempo
        Jam jam = new Jam();
        jam.setKit(kit);
        jam.setPattern(pattern);
        jam.setTempo(120);
        jam.setName("Default Jam");

        return jam;
    }

    private Jam buildDefaultJam(){
        /**
         * buildTestJam()
         *
         * Build Some Individual Beats
         * Put Beats into a Pattern
         *
         * Build Some Components
         * Put up to 8 components into a Kit
         *
         * Build a Jam
         * Add the Kit, Pattern, and the Tempo
         * */
        // Instantiate Beats
        Beat beat1 = new Beat();


        // Instantiate Pattern, add beat
        Pattern pattern = new Pattern();
        pattern.addBeat(beat1);


        // Instantiate Components
        Component componentOne = new Component(R.raw.default_kick);
        Component componentTwo = new Component(R.raw.default_snare);
        Component componentThree = new Component(R.raw.default_crash);
        Component componentFour = new Component(R.raw.default_ride);
        Component componentFive = new Component(R.raw.default_highhat);
        Component componentSix = new Component(R.raw.default_tom1);
        Component componentSeven = new Component(R.raw.default_tom2);
        Component componentEight = new Component(R.raw.default_tom3);

        componentOne.setName("Default Kick");
        componentTwo.setName("Default Snare");
        componentThree.setName("Default Crash");
        componentFour.setName("Default Ride");
        componentFive.setName("Default High Hat");
        componentSix.setName("Default Tom 1");
        componentSeven.setName("Default Tom 2");
        componentEight.setName("Default Tom 3");

        // Instantiate Kit, add instruments
        Kit kit = new Kit();
        kit.addComponent(componentOne);
        kit.addComponent(componentTwo);
        kit.addComponent(componentThree);
        kit.addComponent(componentFour);
        kit.addComponent(componentFive);
        kit.addComponent(componentSix);
        kit.addComponent(componentSeven);
        kit.addComponent(componentEight);
        kit.setName("Kit name");

        // Instantiate Jam - Add Pattern, Kit, Tempo
        Jam jam = new Jam();
        jam.setKit(kit);
        jam.setPattern(pattern);
        jam.setTempo(120);
        jam.setName("Jam Name");

        return jam;
    }

    private void showKit(Kit kit){

        /***
         * showKit() is a simple method to populate the Kit Cardview
         * items with data from the jam.
         */
        TextView component1 = (TextView) findViewById(R.id.component1);
        TextView component2 = (TextView) findViewById(R.id.component2);
        TextView component3 = (TextView) findViewById(R.id.component3);
        TextView component4 = (TextView) findViewById(R.id.component4);
        TextView component5 = (TextView) findViewById(R.id.component5);
        TextView component6 = (TextView) findViewById(R.id.component6);
        TextView component7 = (TextView) findViewById(R.id.component7);
        TextView component8 = (TextView) findViewById(R.id.component8);
        TextView kitName = (TextView) findViewById(R.id.kitName);

        component1.setText(kit.getComponents().get(0).getName());
        component2.setText(kit.getComponents().get(1).getName());
        component3.setText(kit.getComponents().get(2).getName());
        component4.setText(kit.getComponents().get(3).getName());
        component5.setText(kit.getComponents().get(4).getName());
        component6.setText(kit.getComponents().get(5).getName());
        component7.setText(kit.getComponents().get(6).getName());
        component8.setText(kit.getComponents().get(7).getName());
        kitName.setText(kit.getName());
    }

    private void graphPattern(Pattern pattern){
        GraphView graph = (GraphView) findViewById(R.id.patternGraph);

        PointsGraphSeries<DataPoint> series = new PointsGraphSeries<>();
        series = pattern.getPatternDataPoints();

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0.5);
        graph.getViewport().setMaxX(pattern.getLength() + 0.5);

        // set manual Y bounds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(1);
        graph.getViewport().setMaxY(8);

        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
        staticLabelsFormatter.setVerticalLabels(new String[] {"one", "two", "three", "four", "five", "six", "seven", "eight"});
        graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);

        graph.addSeries(series);
    }

    private void createComponentsTable(){
        ContentValues contentValues;
        ArrayList<ContentValues> components = new ArrayList<>();

        contentValues = new ContentValues();
        contentValues.put(dbContract.ComponentTable.NAME, "Default Bass");
        contentValues.put(dbContract.ComponentTable.RESOURCE, R.raw.bass);
        contentValues.put(dbContract.ComponentTable.HEXID, "00");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.ComponentTable.NAME, "Default Button1");
        contentValues.put(dbContract.ComponentTable.RESOURCE, R.raw.button1);
        contentValues.put(dbContract.ComponentTable.HEXID, "01");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.ComponentTable.NAME, "Default Button3");
        contentValues.put(dbContract.ComponentTable.RESOURCE, R.raw.button3);
        contentValues.put(dbContract.ComponentTable.HEXID, "02");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.ComponentTable.NAME, "Default Crash");
        contentValues.put(dbContract.ComponentTable.RESOURCE, R.raw.default_crash);
        contentValues.put(dbContract.ComponentTable.HEXID, "03");

        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.ComponentTable.NAME, "Default HiHat");
        contentValues.put(dbContract.ComponentTable.RESOURCE, R.raw.default_highhat);
        contentValues.put(dbContract.ComponentTable.HEXID, "04");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.ComponentTable.NAME, "Default Kick");
        contentValues.put(dbContract.ComponentTable.RESOURCE, R.raw.default_kick);
        contentValues.put(dbContract.ComponentTable.HEXID, "05");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.ComponentTable.NAME, "Default Ride");
        contentValues.put(dbContract.ComponentTable.RESOURCE, R.raw.default_ride);
        contentValues.put(dbContract.ComponentTable.HEXID, "06");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.ComponentTable.NAME, "Default Snare");
        contentValues.put(dbContract.ComponentTable.RESOURCE, R.raw.default_snare);
        contentValues.put(dbContract.ComponentTable.HEXID, "07");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.ComponentTable.NAME, "Default Tom1");
        contentValues.put(dbContract.ComponentTable.RESOURCE, R.raw.default_tom1);
        contentValues.put(dbContract.ComponentTable.HEXID, "08");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.ComponentTable.NAME, "Default Tom2");
        contentValues.put(dbContract.ComponentTable.RESOURCE, R.raw.default_tom2);
        contentValues.put(dbContract.ComponentTable.HEXID, "09");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.ComponentTable.NAME, "Default Tom3");
        contentValues.put(dbContract.ComponentTable.RESOURCE, R.raw.default_tom3);
        contentValues.put(dbContract.ComponentTable.HEXID, "0A");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.ComponentTable.NAME, "Default HiHat");
        contentValues.put(dbContract.ComponentTable.RESOURCE, R.raw.hihat);
        contentValues.put(dbContract.ComponentTable.HEXID, "0B");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.ComponentTable.NAME, "Default Snare");
        contentValues.put(dbContract.ComponentTable.RESOURCE, R.raw.snare);
        contentValues.put(dbContract.ComponentTable.HEXID, "0C");
        components.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.ComponentTable.NAME, "Default Tom");
        contentValues.put(dbContract.ComponentTable.RESOURCE, R.raw.tom);
        contentValues.put(dbContract.ComponentTable.HEXID, "0D");
        components.add(contentValues);

        for(int x=0;x<components.size();x++){
            getContentResolver().insert(dbContract.buildComponentUri(), components.get(x));
        }

    }

    private void createKitTable(){
        ArrayList<ContentValues> kits = new ArrayList<>();
        ContentValues contentValues;

        contentValues = new ContentValues();
        contentValues.put(dbContract.KitTable.NAME, "Default Kit");
        contentValues.put(dbContract.KitTable.COMPONENTS, "0102030405060708");
        kits.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.KitTable.NAME, "Another Kit");
        contentValues.put(dbContract.KitTable.COMPONENTS, "0C0A0B040508090A");
        kits.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.KitTable.NAME, "A New Kit");
        contentValues.put(dbContract.KitTable.COMPONENTS, "05040609080A060C");
        kits.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.KitTable.NAME, "Unique Kit Kit");
        contentValues.put(dbContract.KitTable.COMPONENTS, "04080A0C06090703");
        kits.add(contentValues);

        for(int x=0;x<kits.size();x++){
            Uri i = getContentResolver().insert(dbContract.buildKitUri(), kits.get(x));
            Log.e("CreateKitTable", "insert() Returned URI:" + i.toString());
        }
    }

    private void createPatternTable(){
        ArrayList<ContentValues> patterns = new ArrayList<>();
        ContentValues contentValues;

        contentValues = new ContentValues();
        contentValues.put(dbContract.PatternTable.NAME, "Default Pattern");
        contentValues.put(dbContract.PatternTable.SEQUENCE, "01");
        patterns.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.PatternTable.NAME, "2 Beat");
        contentValues.put(dbContract.PatternTable.SEQUENCE, "0102");
        patterns.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.PatternTable.NAME, "4 Beat");
        contentValues.put(dbContract.PatternTable.SEQUENCE, "01010201");
        patterns.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.PatternTable.NAME, "3 Beat");
        contentValues.put(dbContract.PatternTable.SEQUENCE, "010103");
        patterns.add(contentValues);

        for(int x=0;x<patterns.size();x++){
            Uri i = getContentResolver().insert(dbContract.buildPatternUri(), patterns.get(x));
            Log.e("CreatePatternTable", "insert() Returned URI:" + i.toString());
        }
    }

    private void createJamTable(){
        ArrayList<ContentValues> jams = new ArrayList<>();
        ContentValues contentValues;

        contentValues = new ContentValues();
        contentValues.put(dbContract.JamTable.NAME, "Default Jam");
        contentValues.put(dbContract.JamTable.KIT_ID, "2");
        contentValues.put(dbContract.JamTable.PATTERN_ID, "2");
        contentValues.put(dbContract.JamTable.TEMPO, "60");
        jams.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.JamTable.NAME, "Jam Two");
        contentValues.put(dbContract.JamTable.KIT_ID, "2");
        contentValues.put(dbContract.JamTable.PATTERN_ID, "2");
        contentValues.put(dbContract.JamTable.TEMPO, "90");
        jams.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.JamTable.NAME, "Jam 3");
        contentValues.put(dbContract.JamTable.KIT_ID, "3");
        contentValues.put(dbContract.JamTable.PATTERN_ID, "2");
        contentValues.put(dbContract.JamTable.TEMPO, "120");
        jams.add(contentValues);

        contentValues = new ContentValues();
        contentValues.put(dbContract.JamTable.NAME, "Jam 4");
        contentValues.put(dbContract.JamTable.KIT_ID, "3");
        contentValues.put(dbContract.JamTable.PATTERN_ID, "3");
        contentValues.put(dbContract.JamTable.TEMPO, "180");
        jams.add(contentValues);

        for(int x=0;x<jams.size();x++){
            Uri i = getContentResolver().insert(dbContract.buildJamUri(), jams.get(x));
        }
    }

    private Jam buildJamFromDB(int id){

        /**
         * A Jam has to have the following parts:
         *
         * Name
         * Tempo
         * Kit
         * Pattern
         *
         * We'll pull the Name and Tempo from the DB directly, along with references
         * to the Kit and the Pattern info, then build the kit and the pattern from there.
         *
         */


        // For now, just grab the first Jam. I'll build a way to track the last jam
        // and pick that one specifically.

        Cursor retCursor = getContentResolver().query(dbContract.buildJamUri().buildUpon().appendPath(String.valueOf(id)).build(),
                null,
                null,
                null,
                null);
        retCursor.moveToFirst();

        String jamName = retCursor.getString(retCursor.getColumnIndex(dbContract.JamTable.NAME));
        int jamTempo = Integer.parseInt(retCursor.getString(retCursor.getColumnIndex(dbContract.JamTable.TEMPO)));
        int dbID = Integer.parseInt(retCursor.getString(retCursor.getColumnIndex(dbContract.JamTable.ID)));

        String kitID = retCursor.getString(retCursor.getColumnIndex(dbContract.JamTable.KIT_ID));
        String patternID = retCursor.getString(retCursor.getColumnIndex(dbContract.JamTable.PATTERN_ID));

        retCursor.close();

        /**
         *
         * Now we'll build the pattern. We'll use the pattern ID to get the pattern sequence
         * from the database, then use that sequence to create the beats.
         *
         * A pattern is a name and an array list of beats.
         *
         * The Pattern class has a constructor that will build a pattern directly from a
         * signature. It leverages a Beat constructor that creates a beat from an individual
         * Hex value.
         *
         */

        retCursor = getContentResolver().query(dbContract.buildPatternUri().buildUpon().appendPath(patternID).build(),
                null,
                null,
                null,
                null);
        retCursor.moveToFirst();

        String patternName = retCursor.getString(retCursor.getColumnIndex(dbContract.PatternTable.NAME));
        String patternSequence = retCursor.getString(retCursor.getColumnIndex(dbContract.PatternTable.SEQUENCE));

        Pattern pattern = new Pattern(patternName, patternSequence, this);


        /**
         *
         * Next we build the Kit. A kit is a name and an array list of components.
         *
         * We'll use the KitID to get the Kit sequence from the DB.
         *
         * The Kit class has a constructor that will build a kit directly from a signature.
         *
         */

        retCursor = getContentResolver().query(dbContract.buildKitUri().buildUpon().appendPath(kitID).build(),
                null,
                null,
                null,
                null);
        retCursor.moveToFirst();

        String kitComponents = retCursor.getString(retCursor.getColumnIndex(dbContract.KitTable.COMPONENTS));
        String kitName = retCursor.getString(retCursor.getColumnIndex(dbContract.KitTable.NAME));
        retCursor.close();

        Kit kit = new Kit(kitName, kitComponents, this);
        kit.setName(kitName);

        /**
         * Finally, we bring all of the pieces together and create the jam.
         */

        Jam jam = new Jam();

        jam.setName(jamName);
        jam.setTempo(jamTempo);
        jam.setKit(kit);
        jam.setPattern(pattern);
        jam.setDbID(dbID);

        return jam;
    }

    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putInt(String.valueOf(R.string.jamID), mJam.getDbID());
    }

}
