package com.udacity.catpoint2.application;

import com.udacity.catpoint2.data.AlarmStatus;

/**
 * Identifies a component that should be notified whenever the system status changes
 */
public interface StatusListener {
    void notify(AlarmStatus status);
    void catDetected(boolean catDetected);
    void sensorStatusChanged();
}
