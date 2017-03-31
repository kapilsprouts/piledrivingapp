package com.bridge3;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.ServerManagedPolicy;
import com.google.android.vending.licensing.AESObfuscator;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
//import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

import org.w3c.dom.Text;


//import com.bridge3.AudioMeter;


public class MainActivity extends Activity {

    private boolean timerRunning = false;
    private boolean isPaused = false;
    private float levelMax;
    //	private static float levelThreshold = (float)1600.0;
    private static int levelThreshold = 1600;
    private static final long minimumStrokeDuration = 500;
    private Long lastStrikeTime; // I am assuming this is what ours said was LastHitTime
    private Long lastStrokeDuration;
    private Long testStartTime;
    private Long testEndTime;
    private Long testTotalDuration;
    private int testStrikeCount;
    private int markStrikeCount;

    private static String I_UNITS = "ft";
    private static String M_UNITS = "m";

    private String unitsPostFix = I_UNITS;
    private boolean isOkToRestart;

    ArrayList<Long> timeList = new ArrayList<Long>();
    static LogData log = new LogData();

    ImageButton btnRecord;
    ImageButton btnMarkBlow;
    TextView tvMiniLog;
    //    TextView tvHeadingBox;
    TextView tvLastBlowDisplay;
    TextView tvLastBlowLabel;
    TextView tvLast10Display;
    TextView tvLast10Label;
    TextView tvMicVolumeValue;
    SeekBar sbVolume;
    LicenseChecker mChecker;
    MyLicenseCheckerCallback mLicenseCheckerCallback;

    //Things you will need to edit
    //don't forget you will need one of the labels above to change the unit with

    TextView tvBlowsPerMinDisplay;
    TextView tvBlowsPerUnitDisplay;
    TextView tvBlowsPerUnitLabel;
    ImageButton btnPause;
    ImageButton btnShowLog;
    ImageButton btnSendEmail;

    private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgEQiU7UXmO3q7xsl6byvuQJRYSITL+dOfUsidh7XkTqcutkU94XW7LRB45z0Z1nWOuG+JQbVdmVXzy4Qi8r8VJzfJte4WkoXM0rVdvdvnCvjEESu3YpDCSSy7/92SCKPzGK4GzwmDZs9PlBNaifTg4ylB2qyRJ5bqoKzMouXr5K4xzQijdylVx24rMDOjxz1B/LqPoampzllRPA2W4GI+sCGrp94pR11qNU3GyOkNaKkuw1q2tD5TpviEuRZR9rhjPZb6KIA01FT0SwYO/zSprBSypY1is+bwpDO20HbsgX+milDcmTzSSMqKg40P4WMI7RTD0qeo9UVfR9mTiKb1wIDAQAB";

    // Generate your own 20 random bytes, and put them here.
    private static final byte[] SALT = new byte[]{
            -45, 64, 30, -128, -103, -57, 74, -64, 51, 88, -95, -45, 77, -117, -36, -113, -11, 32, -64, 89
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isOkToRestart = true;
        testTotalDuration = null;
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Try to use more data here. ANDROID_ID is a single point of attack.
        String deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
        // Library calls this when it's done.
        mLicenseCheckerCallback = new MyLicenseCheckerCallback();
        // Construct the LicenseChecker with a policy.
        mChecker = new LicenseChecker(
                this, new ServerManagedPolicy(this, new AESObfuscator(SALT, getPackageName(), deviceId)), BASE64_PUBLIC_KEY);
        doCheck();

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(SettingsActivity.KEY_BLOW_PER_AVE) || key.equals(SettingsActivity.KEY_UNITS)) {
                    updatePref();
                }
            }
        });

        tvLastBlowDisplay = (TextView) findViewById(R.id.tvLastBlowDisplay);
        tvLast10Display = (TextView) findViewById(R.id.tvLast10Display);
//        tvHeadingBox = (TextView) findViewById(R.id.tvHeadingBox);
        tvLastBlowLabel = (TextView) findViewById(R.id.tvLastBlowLabel);
        tvLast10Label = (TextView) findViewById(R.id.tvLast10Label);
        tvMicVolumeValue = (TextView) findViewById(R.id.tvMicVolumeValue);
        tvMiniLog = (TextView) findViewById(R.id.tvMiniLog);
        tvBlowsPerUnitDisplay = (TextView) findViewById(R.id.tvBlowsPerUnitDisplay);
        tvBlowsPerMinDisplay = (TextView) findViewById(R.id.tvBlowsPerMinDisplay);
        tvBlowsPerUnitLabel = (TextView) findViewById(R.id.tvBlowsPerUnitLabel);
        sbVolume = (SeekBar) findViewById(R.id.sbVolume);

        btnRecord = (ImageButton) findViewById(R.id.btnRecord);
        btnMarkBlow = (ImageButton) findViewById(R.id.btnMarkBlow);
        btnSendEmail = (ImageButton) findViewById(R.id.btnSendEmail);
        btnShowLog = (ImageButton) findViewById(R.id.btnShowLog);
        btnPause = (ImageButton) findViewById(R.id.btnPause);
        btnPause.setVisibility(View.INVISIBLE);

        tvMicVolumeValue.setText("50%");
        updatePref();


        sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvMicVolumeValue.setText(Integer.toString(i / 200) + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        btnShowLog.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openLogList();
            }
        });

        btnRecord.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startRecording();

            }
        });

        btnMarkBlow.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                log.mark();
                refreshMiniLog();
//                if (markStrikeCount > 0) {
//                    markStrike++;
//                    tvMiniLog.setText(tvMiniLog.getText() + Integer.toString(markStrike) + "=" + Integer.toString(markStrikeCount));
//                }
//                markStrikeCount = 0;
//                scrollView1.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

        btnSendEmail.setOnClickListener((new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(testTotalDuration != null && !timerRunning) {
                    Date now = new Date();
                    long second = (testTotalDuration / 1000) % 60;
                    long minute = (testTotalDuration / (1000 * 60)) % 60;
                    long hour = (testTotalDuration / (1000 * 60 * 60)) % 24;
    //                System.out.println(testTotalDuration);
                    String time = String.format("%02d:%02d:%02d", hour, minute, second);
                    sendEmailMessage("Pile Driving Data", "\nStart Time: " + epochToHourMinutes(testStartTime) + "\nStop Time: " + epochToHourMinutes(testEndTime) + "\nCurrent Time: " + now.toString() + "\nDuration of Test: " + time + "\n(Hours: Minutes: Seconds) ", "Select Email Client");
                }
            }
        }));

        btnPause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPaused) {
                    resume();
                } else {
                    pause();
                }
            }
        });

    }
    //Might need to call this in the ASync timer so if someone switches mid way all will be updated
    public void updatePref() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String units = sharedPref.getString(SettingsActivity.KEY_UNITS, "");
        String blowPerAvg = sharedPref.getString(SettingsActivity.KEY_BLOW_PER_AVE, "");
        if(blowPerAvg.equals("mark")){
            blowPerAvg = "9999"; // magicNumber;
            tvLast10Label.setText("Average Stroke/" + unitsPostFix);

        } else {
            tvLast10Label.setText("Last " + String.valueOf(log.getBlowsPerAve()) + " Ave");
        }


        if (units.equals("M")) {
            unitsPostFix = M_UNITS;
            log.setUnitFactor(0.3048);
            tvBlowsPerUnitLabel.setText("Blows/M");
        } else {
            unitsPostFix = I_UNITS;
            log.setUnitFactor(1.0);
            tvBlowsPerUnitLabel.setText("Blows/ft");
        }

        log.setBlowsPerAve(Integer.valueOf(blowPerAvg));

    }

    private void pause() {
        if (timerRunning) {
            isPaused = true;
            btnPause.setImageResource(R.drawable.ic_pause_on);

        }
    }

    private void resume() {
        if (timerRunning) {
            btnPause.setImageResource(R.drawable.ic_pause_off);
            isPaused = false;
            TimerAsync mainTimerLoop = new TimerAsync();
            mainTimerLoop.execute();
        }
    }

    private void startRecording() {
        updatePref();
//        showAlertMessageForLog();
        if (timerRunning) {
            testEndTime = System.currentTimeMillis();
            testTotalDuration = testEndTime - testStartTime;
            btnRecord.setImageResource(R.drawable.redbtn);
//					tvMiniLog.setText(tvMiniLog.getText() + String.format("%.2f", (float)levelMax) + "   " + "Turned Off");
            timerRunning = false;
            isPaused = false;
            isOkToRestart = false;
            btnPause.setVisibility(View.INVISIBLE);
            btnMarkBlow.setVisibility(View.VISIBLE);
            tvBlowsPerMinDisplay.setText(String.format("%.0f", log.getBlowsPerMinute()));
            tvBlowsPerUnitDisplay.setText(String.format("%.0f", log.getBlowsPerUnit()));
            if (log.getLastUnitAve() == -1.0) {
                tvLast10Display.setText("N/A");
            } else {
                tvLast10Display.setText(String.format("%.1f", log.getLastUnitAve()) + " " + unitsPostFix);
            }
            tvLastBlowDisplay.setText(String.format("%.1f", log.getLastBlow()) + " " + unitsPostFix);
        } else {
            if(!isOkToRestart) {
                showAlertMessageForLog();
            }
            if(isOkToRestart) {
                // TODO: give warning before resetting.
                testStartTime = System.currentTimeMillis();
                log.reset();
                btnPause.setVisibility(View.VISIBLE);
                btnRecord.setImageResource(R.drawable.grnbtn);
                resume();
                tvMiniLog.setText("");
                tvBlowsPerMinDisplay.setText("----");
                tvBlowsPerUnitDisplay.setText("----");
                tvLast10Display.setText("----");
                tvLastBlowDisplay.setText("----");
                timeList.clear();
                levelThreshold = sbVolume.getProgress();
                timerRunning = true;
                isPaused = false;
                TimerAsync mainTimerLoop = new TimerAsync();
                mainTimerLoop.execute();
            }

        }
    }

    private void doCheck() {
//        mChecker.checkAccess(mLicenseCheckerCallback);
    }


    public Double calcStroke(Double time) {
        return ((time * time * 100) * 0.04) - 0.3;
    }

    public void refreshMiniLog() {
        int count = log.size();
        String header = "Blow # |  Last | ";
        if ((log.getBlowsPerAve()+"").length() == 2 ) {
            header += " ";
        }
        if(log.getBlowsPerAve() == 9999){
            header +=  "Mark/Ave | Depth" + System.getProperty("line.separator");
        }else{
            header += log.getBlowsPerAve() + "/Ave | Depth" + System.getProperty("line.separator");
        }
        tvMiniLog.setText(header);

        for (int i = Math.max(count - 6, 1); i < count; i++) {
            List<String> r = log.getRow(i);
            String sCnt = "       " + r.get(0);
            if(log.getBlowsPerAve() == 9999) {
                sCnt = sCnt.substring(sCnt.length() - 5);
            } else {
                sCnt = sCnt.substring(sCnt.length() - 6);
            }
            String sLst = "       " + r.get(1);
            sLst = sLst.substring(sLst.length()-5);
            String sAve = "       " + r.get(2);
            if(log.getBlowsPerAve() == 9999) {
                sAve = sAve.substring(sAve.length() - 8);
            } else {
                sAve = sAve.substring(sAve.length() - 7);
            }
            String mark = r.get(3);
            String row = sCnt + " | " + sLst + " | " + sAve + " | " + mark + "                    ";
            row = row.substring(0, 32);
            tvMiniLog.setText(tvMiniLog.getText() + row + System.getProperty("line.separator"));
        }
    }

    /*
        btnMarkBlow.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View v) {
            }
        }
    */
    private class TimerAsync extends AsyncTask<Void, Float, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {

            boolean stopped = false;

            timerRunning = true;
            testStrikeCount = 0;
            testStartTime = (long) 0;
            lastStrikeTime = (long) 0;
            lastStrokeDuration = (long) 0;
            testTotalDuration = (long) 0;
            markStrikeCount = 0;

            float curSoundLevel = (float) 0;
            long thisStrikeTime = (long) 0;
            int iCnt = 0;

            AudioRecord recorder = null;
            short[][] buffers = new short[256][160];
            int ix = 0;

            try { // ... initialise

                int N = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

                recorder = new AudioRecord(AudioSource.VOICE_RECOGNITION,
                        8000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        N * 10);

                recorder.startRecording();

                // ... loop

                while (!stopped && timerRunning && !isPaused) {
                    short[] buffer = buffers[ix++ % buffers.length];

                    N = recorder.read(buffer, 0, buffer.length);

//	                     process(buffer);

                    curSoundLevel = 0;
                    if (N < 0) {
                        curSoundLevel = 0;
                    } else {
                        int sum = 0;
                        for (int i = 0; i < N; i++) {
                            sum += Math.abs(buffer[i]);
                        }

                        if (N > 0) {
                            curSoundLevel = sum / N;
                        }
                    }

                    thisStrikeTime = (Long) System.currentTimeMillis();

                    levelThreshold = sbVolume.getProgress();

                    //					levelThreshold = 300;
                    if (curSoundLevel > levelThreshold && thisStrikeTime - lastStrikeTime > minimumStrokeDuration) {
                        if (testStrikeCount == 0) {
                            testStartTime = thisStrikeTime;
                            lastStrokeDuration = (long) 0;
                        } else {
                            lastStrokeDuration = thisStrikeTime - lastStrikeTime;
                            timeList.add(lastStrokeDuration);
                        }
                        lastStrikeTime = thisStrikeTime;
                        levelMax = curSoundLevel;
                        testStrikeCount++;
                        markStrikeCount++;
                        testTotalDuration = thisStrikeTime - testStartTime;
                        publishProgress((float) iCnt);
                        iCnt = 0;
                    }
                    iCnt++;

                }  // ... loop

                recorder.release();
//	    			tvMiniLog.setText(tvMiniLog.getText() + String.format("%.0f", (float)mLevel) + System.getProperty ("line.separator"));

            } catch (Throwable x) {
//	              log.warning(TAG,"Error reading voice audio",x);
                tvMiniLog.setText(tvMiniLog.getText() + x.getMessage() + System.getProperty("line.separator"));
            } finally {
//	              close(recorder);
//		          close();
            }

            return null;
        }

        protected void onProgressUpdate(Float... progress) {
            Float last10StrikeTime = (float) 0.00;
            Float averageStrokeDuration = (float) 0.00;
            Double lastFt = (double) 0;
            Double last10AveFt = (double) 0;
            String sCnt = "";

            if (testStrikeCount > 1) {

                Double lastStroke = ((double) lastStrokeDuration) / 1000;
                lastFt = calcStroke(lastStroke);
                log.add("" + lastFt);


                updatePref();
                tvBlowsPerMinDisplay.setText(String.format("%.0f", log.getBlowsPerMinute()));
                tvBlowsPerUnitDisplay.setText(String.format("%.0f", log.getBlowsPerUnit()));
                if (log.getLastUnitAve() == -1.0) {
                    tvLast10Display.setText("N/A");
                } else {
                    tvLast10Display.setText(String.format("%.1f", log.getLastUnitAve()) + " " + unitsPostFix);
                }
                tvLastBlowDisplay.setText(String.format("%.1f", log.getLastBlow()) + " " + unitsPostFix);

            } else {
//	            	tvMiniLog.setText(" # | Last  | Ave   | Blows/Ft" + System.getProperty ("line.separator"));

            }

//?

            sCnt = sCnt + " " + Integer.toString(testStrikeCount);
            sCnt = "    " + sCnt.substring(sCnt.length() - 2);
            if (testStrikeCount > 1) {
                sCnt = System.getProperty("line.separator") + sCnt;
            }
//	        	String sLst = String.format("%.3f", (float)((float)(Math.round(lastStrokeDuration))/1000));
            String sLst = "  " + String.format("%.3f", (float) ((float) (Math.round(1000 * lastFt)) / 1000));
            sLst = sLst.substring(sLst.length() - 6);

//	        	String sAve = String.format("%.3f", averageStrokeDuration);
            String sAve = "  " + String.format("%.3f", (float) ((float) (Math.round(1000 * last10AveFt)) / 1000));
            sAve = sAve.substring(sAve.length() - 6);

            String sMax = String.format("%.0f", (float) levelMax);


//	        	tvMiniLog.setText(tvMiniLog.getText() + sCnt + " | " + sLst + " | " + sAve + " | " + sMax + System.getProperty ("line.separator"));
//	        	tvMiniLog.setText(sdebug + System.gextProperty ("line.separator"));
//	        	tvMiniLog.setText(tvMiniLog.getText() + String.format("%.3f", levelMax) + System.getProperty ("line.separator"));
//	        	tvMiniLog.setText(tvMiniLog.getText() + sCnt + " | " + sLst + " | " + sAve + " | " + sMax + System.getProperty ("line.separator"));

            tvMiniLog.setText(tvMiniLog.getText() + sCnt + " | " + sLst + " | " + sAve + " | "); // + sMax + System.getProperty ("line.separator"));
//            scrollView1.fullScroll(ScrollView.FOCUS_DOWN);

            levelMax = 0;
            refreshMiniLog();
        }

        protected void onPostExecute(Long result) {
        }
    }
    
/*    
    static void stop() {
            if (mRecorder != null) {
                    mRecorder.stop();       
                    mRecorder.release();
                    mRecorder = null;
            }
    }
*/
    
/*    
    static float getAmplitude() {
            if (mRecorder != null)
//                    return  (float)(mRecorder.getMaxAmplitude()/2700.0);
                return  (float)(mRecorder.getMaxAmplitude());            	
            else
                    return 0;

    }
*/


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void openLogList() {
        Intent intent = new Intent(this, LogListActivity.class);
        intent.putExtra("logdata", log);
        startActivity(intent);
    }

    private class MyLicenseCheckerCallback implements LicenseCheckerCallback {
        public void allow(int reason) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                showDialog(4);
                return;
            }
            // Should allow user access.
//	        displayResult(getString(R.string.allow));
//            showDialog(2);
        }

        public void dontAllow(int reason) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
//	        displayResult(getString(R.string.dont_allow));
//	        displayResult("No Go");	        

            if (reason == Policy.RETRY) {
                // If the reason received from the policy is RETRY, it was probably
                // due to a loss of connection with the service, so we should give the
                // user a chance to retry. So show a dialog to retry.
//	            showDialog(DIALOG_RETRY);
//	            AlertDialog("Please Retry");
                showDialog(0);
            } else {
                // Otherwise, the user is not licensed to use this app.
                // Your response should always inform the user that the application
                // is not licensed, but your behavior at that point can vary. You might
                // provide the user a limited access version of your app or you can
                // take them to Google Play to purchase the app.
                showDialog(1);
            }
        }

        @Override
        public void applicationError(int errorCode) {
            // TODO Auto-generated method stub
            showDialog(3);
        }
    }

    ;

    @Override
    protected Dialog onCreateDialog(int id) {
        // We have only one dialog.
        return new AlertDialog.Builder(this)
                .setTitle(Integer.toString(id) + ". Application Not Licensed")
                .setCancelable(false)
                .setMessage(
                        "This application is not licensed. Please purchase it from Android Market")
                .setPositiveButton("Buy App",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                Intent marketIntent = new Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("http://market.android.com/details?id="
                                                + getPackageName()));
                                startActivity(marketIntent);
                                finish();
                            }
                        })
                .setNegativeButton("Exit",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                finish();
                            }
                        }).create();
    }


    private void sendEmailMessage(String subject, String body, String chooserTitle) {
        Intent mailIntent = new Intent();
        Uri U = log.createSheet(this.getApplicationContext());
        mailIntent.setAction(Intent.ACTION_SEND);
        mailIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        mailIntent.putExtra(Intent.EXTRA_STREAM, U);
        mailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{""});
        mailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        mailIntent.putExtra(Intent.EXTRA_TEXT, body);
//        mailIntent.setType("message/rfc822");

        startActivity(Intent.createChooser(mailIntent, chooserTitle));
        //attach log data

    }

    //State methods
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        System.out.println("onSaveInstanceState");

        savedInstanceState.putParcelable("test", tvMiniLog.onSaveInstanceState());

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        System.out.println("onRestoreInstanceState");
        tvMiniLog.onRestoreInstanceState(savedInstanceState.getParcelable("test"));
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void showAlertMessageForLog() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Warning");
        alertDialog.setMessage("All data will be lost on reset");
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                isOkToRestart = false;
            }
        });
        alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                isOkToRestart = true;
                startRecording();
            }
        });
        alertDialog.show();

    }

    public String epochToHourMinutes(long time) {
        Date d = new Date(time);
        String s = "";
        s += d.getHours();
        s += ":";
        s += d.getMinutes();

        return s;
    }

}
