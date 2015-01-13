package com.gomtel.polysomnography;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.DeadObjectException;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Chronometer;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

//import cz.muni.fi.pedometer.filters.*;
//import com.gomtel.polysomnography.Fusion;
//import com.gomtel.polysomnography.ISService;
//import cz.muni.fi.pedometer.main.MovingAverageStepDetector.MovingAverageStepDetectorState;
import com.gomtel.polysomnography.res.Matrix;



public class SService extends Service implements SensorEventListener{
    static final String LOG_TAG = "Fusion";
    public static final int IDX_X = 0;
    public static final int IDX_Y = 1;
    public static final int IDX_Z = 2;
    public static final long WAKETIME = 5*60*1000;
    public static final long DEEPSLEEPTIME = 15*60*1000;
    public static final int STATE_WAKE = 0;
    public static final int STATE_SLEEP_LIGHT = 1;
    public static final int STATE_SLEEP_DEEP = 2;
    private int sleepState = STATE_WAKE;
    private long timeOfWake = 0L;
    private long timeOfLightSleep = 0L;
    private long timeOfDeepSleep = 0L;
    private long startTime = 0L
            ;
    static final String CALIB_FILE = "calib.txt";
    static final boolean DEBUG = true;
    public static final int ENGINESTATES_MEASURING = 30;
    public static final int SENSORTYPE_ACCEL = 1;
    public static final int LINEAR_ACCELERATION_VECTOR = 2;
    private static final String TAG = "POLYSOMNOGRAPHY";
    private double gyroAccelVector[];
    private boolean samplingStarted = false;
    private PrintWriter captureFile = null;
    private SensorManager sensorManager;
    private Sensor accelSensor;
    private long accelLastTimeStamp = 0L;
    public int state;
    private long wakeTimeStamp;
//    private MovingAverageStepDetector mStepDetector
    private Kalman kalman = new Kalman(3,3);
    private double[] linAccel;
    private double gravityNoMotionLowLimit;
    private double gravityNoMotionHighLimit = 10000;
    private int buffer_count = -1;
    private float[][] accdata_buffer = new float[100][3];
    private float[] mean_accdata = new float[3];
    private double accdata_temp;
    private double[] accdata = new double[2];
    private long wakeTime;
    private boolean isMove =false;

    public int onStartCommand(Intent intent, int flags, int startId) {
           super.onStartCommand( intent, flags, startId );
           stopSampling();   
           sensorManager = (SensorManager)getSystemService( SENSOR_SERVICE );
           startSampling();
           return START_NOT_STICKY;
    }
    
    private void startSampling() {
           if( samplingStarted )
                  return;
           List<Sensor> sensors = sensorManager.getSensorList( Sensor.TYPE_ACCELEROMETER  );
           accelSensor = sensors.size() == 0 ? null : sensors.get( 0 );
              
           if( ( accelSensor != null ) ) {
                sensorManager.registerListener( this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST );

           } else {
                Toast.makeText(getApplicationContext(), "Some sensors are missing. App cannot continue.", Toast.LENGTH_LONG).show();
           }
           captureFile = null;
           if( DEBUG ) {
                Date d = new Date();
                String filename = "capture_"+
                    Integer.toString( d.getYear()+1900 )+"_"+Integer.toString( d.getMonth()+1 )+"_"+Integer.toString( d.getDate())+
                                     "_"+Integer.toString( d.getHours())+"_"+Integer.toString( d.getMinutes())+ ".csv";
                    File captureFileName = new File( Environment.getExternalStorageDirectory(), filename );
                try {
                    captureFile = new PrintWriter( new FileWriter( captureFileName, false ) );
                } catch( IOException ex ) {
                    Log.e( LOG_TAG, ex.getMessage(), ex );
                }
           }
           samplingStarted = true;
        }

    private void stopSampling() {
            if( !samplingStarted )
            return;
            if( sensorManager != null ) {
                    Log.d( LOG_TAG, "unregisterListener/SamplingService" );
                    sensorManager.unregisterListener( this );
            }
            if( captureFile != null ) {
                    captureFile.close();
                    captureFile = null;
            }
            samplingStarted = false;
        }
        
        

    
   
    @Override
    public void onSensorChanged(SensorEvent event) {
//         processSample( event );
        Log.e(TAG,"lixiang---state= "+state);
        if(state == Polysomnography.STATE_TIMER_RUN){
//            double limit = Math.sqrt(event.values[0]*event.values[0]+event.values[1]*event.values[1]+event.values[2]*event.values[2]);
//            Log.e(TAG,"lixiang---limit= "+limit);
            buffer_count = buffer_count+1;
            if(buffer_count<50) {
                accdata_buffer[buffer_count][0] = event.values[0];
                accdata_buffer[buffer_count][1] = event.values[1];
                if (buffer_count == 49) {
                    mean_accdata[0] = mean(accdata_buffer, 0);
                    mean_accdata[1] = mean(accdata_buffer, 0);
                    accdata_temp = mean_accdata[0] * mean_accdata[0] + mean_accdata[1] * mean_accdata[1];
                    accdata[0] = Math.sqrt(accdata_temp);
//                    buffer_count = -1;
//                    Log.e(TAG, "lixiang---accdata= " + accdata);
                }
            } else if(buffer_count < 100){
                accdata_buffer[buffer_count][0] = event.values[0];
                accdata_buffer[buffer_count][1] = event.values[1];
                if (buffer_count == 99) {
                    mean_accdata[0] = mean(accdata_buffer, 0);
                    mean_accdata[1] = mean(accdata_buffer, 0);
                    accdata_temp = mean_accdata[0] * mean_accdata[0] + mean_accdata[1] * mean_accdata[1];
                    accdata[1] = Math.sqrt(accdata_temp);
                    buffer_count = -1;
                    if(Math.abs(accdata[1]-accdata[0]) > 1.0){
                        Log.e(TAG,"lixiang---move");
                        isMove = true;
                    }

                        if(wakeTimeStamp > 0){
                            long intervalTime = SystemClock.elapsedRealtime()-wakeTimeStamp;
                            if(intervalTime < (WAKETIME+1) && sleepState == STATE_WAKE && isMove){
                                Log.e(TAG,"lixiang---wake");
//                                sleepState = STATE_WAKE;
                                timeOfWake += intervalTime;
                                wakeTimeStamp = SystemClock.elapsedRealtime();
                                isMove = false;
                            }else if((intervalTime > WAKETIME && intervalTime < DEEPSLEEPTIME+1 && isMove) || (sleepState == STATE_SLEEP_DEEP && isMove)){
                                Log.e(TAG,"lixiang---wake_light");
                                sleepState = STATE_SLEEP_LIGHT;
                                timeOfLightSleep += intervalTime;
                                wakeTimeStamp = SystemClock.elapsedRealtime();
                                isMove = false;

                            }else if(intervalTime >DEEPSLEEPTIME ){
                                Log.e(TAG,"lixiang---wake_deep");
                                sleepState = STATE_SLEEP_DEEP;
                                timeOfDeepSleep += intervalTime;
                                wakeTimeStamp = SystemClock.elapsedRealtime();
                            }


                    }
                    Log.e(TAG, "lixiang---accdata= " + accdata);
                }
            }

        }
         
    }

    private float mean(float[][] accdata_buffer, int n) {
        float sumNum = 0;
        for(int i = 0;i<100;i++)
            sumNum += accdata_buffer[i][n];
        return sumNum/100;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    


        private void vectorZero( double vec[] ) {
        vec[IDX_X] = 0.0;
        vec[IDX_Y] = 0.0;
        vec[IDX_Z] = 0.0;
        }


    
    public IBinder onBind(Intent intent) {
            return serviceBinder;
    }
        


    private void processSample( SensorEvent sensorEvent ) {
        float values[] = sensorEvent.values.clone();
        Log.e(LOG_TAG,"lixiang---values.length= "+values.length);
        if( values.length < 3 )
                return;
        int sensorType = SENSORTYPE_ACCEL;
        if( sensorEvent.sensor == accelSensor ) {
            sensorType = SENSORTYPE_ACCEL;
        }
        if( captureFile != null ) {
              //  captureFile.println( sensorEvent.timestamp+ ","+sensorName+ ","+values[0]+ ","+ values[1]+ ","+values[2]);
        }
                processMeasuring( sensorEvent.timestamp, sensorType, values );

    }

    

        
        

        private void processMeasuring( long timeStamp, int sensorType, float values [] ) {
    	double dValues[] = new double[3];
        dValues[IDX_X] = (double)values[IDX_X];
        dValues[IDX_Y] = (double)values[IDX_Y];
        dValues[IDX_Z] = (double)values[IDX_Z];
//        redraw( LINEAR_ACCELERATION_VECTOR, sensorType,new double[]{dValues[IDX_X], dValues[IDX_Y], dValues[IDX_Z]});
        if( sensorType == SENSORTYPE_ACCEL ) {
        	if( accelLastTimeStamp > 0L ) {
                double accelLength = vectorLength( dValues );
   Log.e(LOG_TAG,"lixiang---accelLength= "+accelLength);
            if( ( accelLength > gravityNoMotionLowLimit ) &&( accelLength < gravityNoMotionHighLimit) ) {
                    // No motion acceleration - save the acceleration vector
                if( gyroAccelVector == null )
                	gyroAccelVector = new double[3];
                    vectorCopy( gyroAccelVector, dValues );    
                    linAccel = vectorSub( dValues, gyroAccelVector );                    
                } else {
                        if( gyroAccelVector != null ) {
                        	linAccel = vectorSub( dValues, gyroAccelVector );
                        if( DEBUG )
                        //	captureFile.println( timeStamp+ ","+"linAccel"+ ","+ linAccel[IDX_X]+ ","+ linAccel[IDX_Y]+ ","+ linAccel[IDX_Z])
                        	;
                        }
                }
//!!!!!
                        //Kalman filter
            	if (linAccel != null) {
//                        kalman.Predict();
//                        Matrix accelMatrix = new Matrix(linAccel, 3);
//                        double[] out = kalman.Correct(accelMatrix).getRowPackedCopy();
//                        float[] output = {(float)out[0], (float)out[1],(float)out[2] };
////                        float stepValue = mStepDetector.processAccelerometerValues(timeStamp, output);
////                        displayStepDetect(mStepDetector.getState());
//
//
//                       //doubleintegrating to get the distance
//                        double timeDifference = (double)(timeStamp - accelLastTimeStamp)/1000000000;
//
//                        if (stepValue > prevPrevStepValue && stepValue>0.4) {
//                        velocity[0] += timeDifference * (prevOut[0] + out[0])/2;
//                        velocity[1] += timeDifference * (prevOut[1] + out[1])/2;
//                        velocity[2] += timeDifference * (prevOut[2] + out[2])/2;
//                        prevOut = out;
//                        pos[0] += timeDifference * (prevVelocity[0] + velocity[0])/2;
//                        pos[1] += timeDifference * (prevVelocity[1] + velocity[1])/2;
//                        pos[2] += timeDifference * (prevVelocity[2] + velocity[2])/2;
//                        prevVelocity = velocity;
//                        } else {
//                        	velocity[0] = 0;
//                        	velocity[1] = 0;
//                        	velocity[2] = 0;
//                        }
//
//                        if (mStepDetector.getState().states[0]) countSteps++;
//                        prevPrevStepValue = prevStepValue;
//                        prevStepValue = stepValue;
//                        integralDistance =  pos[0] + pos[1] + pos[2];
//                        double multipledDistance = countSteps * STEP_LENGTH;
//                        captureFile.println( "integral " + integralDistance );
//                        captureFile.println( "multipled " + multipledDistance );
//                        captureFile.println( "acceleration " + stepValue );

                        redraw( LINEAR_ACCELERATION_VECTOR, sensorType,new double[]{dValues[IDX_X], dValues[IDX_Y], dValues[IDX_Z]});
//!!!!!
        	}
        	}
        	accelLastTimeStamp = timeStamp;

        	}


        
       
    }  

    private double vectorLength( double vec[] ) {
            return Math.sqrt( ( vec[IDX_X]*vec[IDX_X] ) + ( vec[IDX_Y]*vec[IDX_Y] ) + ( vec[IDX_Z]*vec[IDX_Z] ) );
    }
       

    private void redraw( int type, int sensorType, double[] vals) {
                long currentTime = System.currentTimeMillis();
//                long prevTimeStamp = graphTimestamp[type];
//                long tdiff =  currentTime - prevTimeStamp;
                if( true) {
                        Log.d( LOG_TAG, "redraw; sensorType: "+sensorType+"; vals: "+vals[IDX_X]+ " : " +vals[IDX_Y]+ " : "+vals[IDX_Z]);
                        if( fusion != null ) {
                                try {
                                        fusion.draw(type, sensorType, vals );
                                } catch( DeadObjectException ex ) {
                                        Log.e( LOG_TAG,"step() callback", ex );
                                } catch( RemoteException ex ) {
                                        Log.e( LOG_TAG, "RemoteException",ex );
                                }
//                                graphTimestamp[type] = currentTime;
//                                switch( type ) {
//                                case LINEAR_ACCELERATION_VECTOR:
//                                        graphTimestamp[NO_LINEAR_ACCELERATION_VECTOR] = -1L;
//                                        break;
//
//                                case NO_LINEAR_ACCELERATION_VECTOR:
//                                        graphTimestamp[LINEAR_ACCELERATION_VECTOR] = -1L;
//                                        break;
//
//                                case EXT_MAGNETIC_FIELD_VECTOR:
//                                        graphTimestamp[NO_EXT_MAGNETIC_FIELD_VECTOR] = -1L;
//                                        break;
//
//                                case NO_EXT_MAGNETIC_FIELD_VECTOR:
//                                        graphTimestamp[EXT_MAGNETIC_FIELD_VECTOR] = -1L;
//                                        break;
//                                }
                        } else
                                        Log.e(LOG_TAG, "redraw: cannot call back main activity");
                }
        }
   

    private void vectorCopy( double target[], double source[] ) {
            target[IDX_X] = source[IDX_X];
        target[IDX_Y] = source[IDX_Y];
        target[IDX_Z] = source[IDX_Z];
    }
       
    private double[] vectorSub( double v1[],double v2[]) {
            double r[] = new double[3];
        r[IDX_X] = v1[IDX_X]-v2[IDX_X];
        r[IDX_Y] = v1[IDX_Y]-v2[IDX_Y];
        r[IDX_Z] = v1[IDX_Z]-v2[IDX_Z];
        return r;
    }

    private Fusion fusion = null;

    private long oldTime;
    private long stopTime;
    private long[] sleepTime = new long[3];
    private final ISService.Stub serviceBinder = new ISService.Stub() {


        @Override
        public void setServiceTimer(long time) {
           oldTime = time;
            wakeTimeStamp = time;
            startTime = time;
//            Log.e("lixiang","lixiang---time= "+startTime);
        }

        @Override
        public void setServiceStopTime(long time) throws RemoteException {
            stopTime = time;
        }

        @Override
        public long[] getSleepTime() throws RemoteException {
            Log.e(TAG,"lixiang---timeOfWake = "+timeOfWake);
            sleepTime[0] = timeOfWake;
            sleepTime[1] = timeOfLightSleep;
            sleepTime[2] = timeOfDeepSleep;
            return sleepTime;
        }

        public void setState(int mstate){
            state = mstate;
        }
        public long getServiceTimer(){
                return oldTime;

        }
        public void setCallback( IBinder binder ) {
                    fusion = Fusion.Stub.asInterface( binder );
                    Log.e(LOG_TAG,"lixiang---fusion= "+fusion);
        }

        public void removeCallback() {
            fusion = null;
        }

        public boolean isSampling() {
            return samplingStarted;
        }

        public void stopSampling() {
            SService.this.stopSampling();
            stopSelf();
        }

        @Override
        public int getState() throws RemoteException {
                return state;

        }

    };        
        
     
    class IIR {
            public IIR( double n_coeffs[],double dn_coeffs[] ) {
                    this.n_coeffs = n_coeffs;
                    this.dn_coeffs = dn_coeffs;
                    n_len = n_coeffs.length;
                    dn_len = dn_coeffs.length -1;
                    n_buf = new double[ n_len ];
                    dn_buf = new double[ dn_len ];
                    n_ptr = 0;
                    dn_ptr = 0;
                    for( int i = 0 ; i < n_len ; ++i )
                            n_buf[i] = 0.0;
                    for( int i = 0 ; i < dn_len ; ++i )
                            dn_buf[i] = 0.0;
                        }
                        
        public double filter( double inp ) {
                n_buf[ n_ptr ] = inp;
                int tmp_ptr = n_ptr;
                double mac = 0.0;
                for(int i = 0 ; i < n_len ; ++i ) {
                        mac = mac + n_coeffs[i]*n_buf[tmp_ptr];
                        --tmp_ptr;
                        if( tmp_ptr < 0 )
                                tmp_ptr = n_len - 1;
                }
                n_ptr = ( ++n_ptr ) % n_len;
                tmp_ptr = dn_ptr - 1;
                if( tmp_ptr < 0 )
                        tmp_ptr = dn_len -1;
                for(int i = 0 ; i < dn_len ; ++i ) {
                        mac = mac - dn_coeffs[i+1]*dn_buf[tmp_ptr];
                        --tmp_ptr;
                        if( tmp_ptr < 0 )
                                tmp_ptr = dn_len - 1;
                }
                dn_buf[dn_ptr] = mac;
                dn_ptr = ( ++dn_ptr ) % dn_len;
                return mac;
        }
        double n_coeffs[];
        double dn_coeffs[];
        double n_buf[];
        double dn_buf[];
        int n_ptr;
        int n_len;
        int dn_ptr;
        int dn_len;
                }
}
