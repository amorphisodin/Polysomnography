package com.gomtel.polysomnography;
interface ISService {
  void setCallback( in IBinder binder );
  void removeCallback();
  void stopSampling();
  boolean isSampling();
  int getState();
  void startServiceTimer(long time);
  long getServiceTimer();
}
