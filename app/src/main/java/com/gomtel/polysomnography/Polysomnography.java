package com.gomtel.polysomnography;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;


public class Polysomnography extends Activity {
    public static final int STATE_TIMER_IDLE = 0;
    public static final int STATE_TIMER_PAUSE = 1;
    public static final int STATE_TIMER_RUN = 2;
    public static final String WAKE_TIME_PERCENT = "wakeTimePercent";
    public static final String LIGHT_TIME_PERCENT = "lightTimePercent";
    public static final String DEEP_TIME_PERCENT = "deepTimePercent";
    private static final String TAG = "Polysomnography";
    private SServiceConnection sServiceConnection = null;
    private ISService sService = null;
    private boolean samplingServiceRunning = false;
    private ImageView sleepButton = null;
    private TextView sleepText = null;
    private Chronometer timer = null;
    private int state_timer = STATE_TIMER_IDLE;
    private long base_start;
    private long startTime;
    private long stopTime;
    private long[] sleepTime = new long[3];
    private long sleepDetectTime = 0;
    private float wakeTimePercent;
    private float lightTimePercent;
    private float deepTimePercent;
    private DecimalFormat df = new DecimalFormat(".##");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.main);
//        graphView = (GraphView)findViewById( R.id.graphView);
        sleepButton = (ImageView) findViewById(R.id.sleepButton);
        sleepText = (TextView)findViewById(R.id.sleepText);
        startSService();
        bindSamplingService();


    }

    private void stopTimer(){
        timer.setVisibility(View.GONE);
        sleepText.setVisibility(View.VISIBLE);
        timer.stop();
        timer.setBase(SystemClock.elapsedRealtime());
    }

    private void setTimerState(int stateTimer) {
        state_timer = stateTimer;
    }

    private void startTimer() throws RemoteException {
        timer.setVisibility(View.VISIBLE);
        sleepText.setVisibility(View.GONE);
        String[] times = timer.getText().toString().split(":");
        long time = (Integer.parseInt(times[0]) * 60 + Integer.parseInt(times[1])) * 1000;
        long base = SystemClock.elapsedRealtime();
        startTime = base;
        sService.setServiceTimer(base);
        sService.setState(STATE_TIMER_RUN);
        Log.e(TAG,"lixiang---time0= "+base);
//        base_start = base;
        //            timer.setBase(base -sService.getServiceTimer());
        timer.setBase(base -time);
        timer.start();


    }

    private void startSService() {
        if( samplingServiceRunning )
            stopSamplingService();
        Intent i = new Intent();
        i.setClassName( "com.gomtel.polysomnography","com.gomtel.polysomnography.SService" );
        startService( i );
        samplingServiceRunning = true;
    }

    private void stopSamplingService() {
        Log.d( TAG, "stopSamplingService" );
        if( samplingServiceRunning ) {
            stopSampling();
            samplingServiceRunning = false;
        }
    }

    private void stopSampling() {
        Log.d( TAG, "stopSampling" );
        if( sService == null )
            Log.e( TAG, "stopSampling: Service not available" );
        else {
            try {
                sService.stopSampling();
            } catch( DeadObjectException ex ) {
                Log.e( TAG, "DeadObjectException",ex );
            } catch( RemoteException ex ) {
                Log.e( TAG, "RemoteException",ex );
            }
        }
    }

    private void bindSamplingService() {
        sServiceConnection = new SServiceConnection();
        Intent i = new Intent();
        i.setClassName( "com.gomtel.polysomnography","com.gomtel.polysomnography.SService" );
        bindService( i, sServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
//        bindSamplingService();

        super.onResume();

    }

    @Override
    protected void onDestroy() {
        if(state_timer == STATE_TIMER_RUN) {
            stopTimer();
            try {
//            base_start = timer.getBase();

                sService.setState(STATE_TIMER_RUN);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }else if(state_timer == STATE_TIMER_IDLE){
            try {
                sService.setState(STATE_TIMER_IDLE);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
        super.onDestroy();
//        releaseSService();
    }

    private void releaseSService() {
        releaseCallbackOnService();
        unbindService( sServiceConnection );
        sServiceConnection = null;
    }

    private void releaseCallbackOnService() {
        if( sService == null )
            Log.e( TAG, "releaseCallbackOnService: Service not available" );
        else {
            try {
                sService.removeCallback();
            } catch( DeadObjectException ex ) {
                Log.e( TAG, "DeadObjectException",ex );
            } catch( RemoteException ex ) {
                Log.e( TAG, "RemoteException",ex );
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.polysomnography, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    class SServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className,IBinder boundService ) {
            Log.d( TAG, "onServiceConnected" );
            sService = ISService.Stub.asInterface((IBinder)boundService);
            timer = (Chronometer)findViewById(R.id.chronometer);
            try {
                if(sService.getState() == STATE_TIMER_RUN) {
//                timer.setBase(SystemClock.elapsedRealtime());
                    timer.setBase(sService.getServiceTimer());
                    timer.setFormat("%s");
                    timer.start();
                    timer.setVisibility(View.VISIBLE);
                    sleepText.setVisibility(View.GONE);
                    setTimerState(STATE_TIMER_RUN);
                }else if(sService.getState() == STATE_TIMER_IDLE){
                    timer.setBase(SystemClock.elapsedRealtime());
                    timer.setFormat("%s");
//                    timer.start();
//                    setTimerState(STATE_TIMER_RUN);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            sleepButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN) {
                        view.setBackgroundResource(R.drawable.background_press);
                    }
                    if(event.getAction() == MotionEvent.ACTION_UP) {
                        view.setBackgroundResource(R.drawable.background_normal);
                        if(state_timer == STATE_TIMER_IDLE) {

                            try {
                                startTimer();
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                            Log.e(TAG, "lixiang---STATE_TIMER_IDLE");

                            stopSamplingService();
                            startSService();
                            bindSamplingService();
                            setTimerState(STATE_TIMER_RUN);
                        }
                        else if(state_timer == STATE_TIMER_RUN){
                            stopTimer();
                            setTimerState(STATE_TIMER_IDLE);

                            if(sService != null){
                                try {
                                    sleepTime = sService.getSleepTime();
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                                stopTime = SystemClock.elapsedRealtime();
                                sleepDetectTime = stopTime - startTime;
                                lightTimePercent = Float.parseFloat(df.format((float)sleepTime[1]/(float)sleepDetectTime));
                                deepTimePercent = Float.parseFloat(df.format((float)sleepTime[2]/(float)sleepDetectTime));
                                wakeTimePercent = (1-lightTimePercent-deepTimePercent);
                                Log.e(TAG,"lixiang---sleepTime[0] = "+sleepTime[0]);
                                Log.e(TAG,"lixiang---sleepDetectTime = "+sleepDetectTime);
                                Log.e(TAG,"lixiang---wakeTimePercent= "+wakeTimePercent+"  lightTimePercent= "+lightTimePercent+"  deepTimePercent= "+deepTimePercent);
                            }
                            if(sleepDetectTime > 0) {
                                Intent intent = new SleepPieChart().getIntents(Polysomnography.this);
                                startActivity(intent);
                            }else{
                                Toast.makeText(Polysomnography.this,"Sleep time is too short!",Toast.LENGTH_LONG);
                            }
                            stopSampling();

                        }

                    }
                    return true;
                }
            });
            setCallbackOnService();
            updateSamplingServiceRunning();
            updateState();
//            if( state == SService.ENGINESTATES_MEASURING )
//                graphView.setVisibility( View.VISIBLE);
            Log.d( TAG,"onServiceConnected" );
        }

        public void onServiceDisconnected(ComponentName className) {
            sService = null;
            Log.d( TAG,"onServiceDisconnected" );
        }
    };

    private void updateState() {
        if( sService == null )
            Log.e( TAG, "updateState: Service not available" );
        else {
//            try {
////                state = sService.getState();
//            } catch( DeadObjectException ex ) {
//                Log.e( TAG, "DeadObjectException",ex );
//            } catch( RemoteException ex ) {
//                Log.e( TAG, "RemoteException",ex );
//            }
        }
    }

    

    private void updateSamplingServiceRunning() {
        if( sService == null )
            Log.e( TAG, "updateSamplingServiceRunning: Service not available" );
        else {
            try {
                samplingServiceRunning = sService.isSampling();
            } catch( DeadObjectException ex ) {
                Log.e( TAG, "DeadObjectException",ex );
            } catch( RemoteException ex ) {
                Log.e( TAG, "RemoteException",ex );
            }
        }
    }

    

    private void setCallbackOnService() {
        if( sService == null )
            Log.e( TAG, "setCallbackOnService: Service not available" );
        else {
            try {
                sService.setCallback( fusion.asBinder() );
            } catch( DeadObjectException ex ) {
                Log.e( TAG, "DeadObjectException",ex );
            } catch( RemoteException ex ) {
                Log.e( TAG, "RemoteException",ex );
            }
        }
    }

    public float getWakeTimePercent(){
         return wakeTimePercent;
    }

    public float getLightTimePercent(){
        return lightTimePercent;
    }

    public float getDeepTimePercent(){
        return  deepTimePercent;
    }
    private Fusion.Stub fusion = new Fusion.Stub() {
        @Override
        public void sampleCounter(int count) throws RemoteException {
            Log.d( TAG, "sample count: "+count );

        }

        public void statusMessage( int newState ) {
            Log.d(TAG, "statusMessage: "+newState );
        }

        @Override
        public void draw(int type, int sensorType, double[] values  ) throws RemoteException {
//            if( graphView != null ) {
//                Log.e(TAG,"lixiang---draw");
////                SurfaceHolder holder = graphView.getHolder();
//////                holder.addCallback(graphView);
////                Canvas c = holder.lockCanvas();
////                if( c != null ) {
////                    float[] vals = {(float) values[0], (float) values[1], (float) values[2]};
////                    graphView.drawGraph(c, vals);
////                    holder.unlockCanvasAndPost(c);
////                }
//            }


        }

        @Override
        public void displayStepDetected(){
        }
    };
    

}
