package com.gomtel.polysomnography;
interface ISService {
  void setCallback( in IBinder binder );
  void removeCallback();
  void stopSampling();
  boolean isSampling();
  int getState();
  void setServiceTimer(long time);
  void setServiceStopTime(long time);
  long[] getSleepTime();
  long getServiceTimer();
  void setState(int state);
}
