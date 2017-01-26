package com.umpquariversoftware.metronome.UI;


import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
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
import com.umpquariversoftware.metronome.elements.Beat;
import com.umpquariversoftware.metronome.elements.Component;
import com.umpquariversoftware.metronome.elements.Jam;
import com.umpquariversoftware.metronome.elements.Kit;
import com.umpquariversoftware.metronome.elements.Pattern;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    String TAG = "COUNTER";
    Timer mTimer = new Timer();
    Jam mJam = new Jam();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTimer = null;

        // Load the last one - use default if it doesn't exist
        mJam = buildTestJam();

        // Display Information about the current Kit
        showKit(mJam.getKit());

        // Display a graph of the current pattern
        graphPattern(mJam.getPattern());

        // setup Tempo Bar
        setupTempoBar();

        // Setup user controls
        setupStartStopFAB();
        setupSaveFab();
        setupFavoriteFab();

        rawResourceDbBuilder();

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

    public void setupFavoriteFab(){
        FloatingActionButton favoriteButton = new FloatingActionButton(this);
        favoriteButton = (FloatingActionButton) findViewById(R.id.favoriteJamButton);
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "favorite", Toast.LENGTH_LONG).show();
            }
        });
    }

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
        kit.setName("Default Kit");

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

    private void patternToDB (Pattern pattern){
        // Extract Information
        String name = pattern.getName();
        int length = pattern.getLength();
        String signature = pattern.getPatternHexSignature();

        // Make Content Provider call
    }

    private void kitToDB (Kit kit){
        String name = kit.getName();
        int component1 = kit.getComponents().get(0).getResource();
        int component2 = kit.getComponents().get(1).getResource();
        int component3 = kit.getComponents().get(2).getResource();
        int component4 = kit.getComponents().get(3).getResource();
        int component5 = kit.getComponents().get(4).getResource();
        int component6 = kit.getComponents().get(5).getResource();
        int component7 = kit.getComponents().get(6).getResource();
        int component8 = kit.getComponents().get(7).getResource();

        String signature = kit.getSignature();

        // Make Content Provider call
    }

    private void jamToDB (Jam jam){
        String name = jam.getName();
        String kitSignature = jam.getKit().getSignature();
        String pattern = jam.getPattern().getPatternHexSignature();
        int Tempo = jam.getTempo();
    }


    private void rawResourceDbBuilder(){
        ArrayList<Integer> components = new ArrayList<>();
        components.add(R.raw.bass);
        components.add(R.raw.button1);
        components.add(R.raw.button3);
        components.add(R.raw.default_crash);
        components.add(R.raw.default_highhat);
        components.add(R.raw.default_kick);
        components.add(R.raw.default_ride);
        components.add(R.raw.default_snare);
        components.add(R.raw.default_tom1);
        components.add(R.raw.default_tom2);
        components.add(R.raw.default_tom3);
        components.add(R.raw.hihat);
        components.add(R.raw.snare);
        components.add(R.raw.tom);

        Log.e("rawResourceDbBuilder()", "components.toString()" + components.toString());
        for(int x=0;x<components.size();++x){
            Log.e("rawResourceDbBuilder()", "x: " + String.format("%02X", x) + " : " + components.get(x));
        }
    }

}
