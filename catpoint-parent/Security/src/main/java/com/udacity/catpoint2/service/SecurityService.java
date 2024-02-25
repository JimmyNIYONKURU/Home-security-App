package com.udacity.catpoint2.service;
import com.udacity.catpoint.service.ImageService;
import com.udacity.catpoint2.application.StatusListener;
import com.udacity.catpoint2.data.AlarmStatus;
import com.udacity.catpoint2.data.ArmingStatus;
import com.udacity.catpoint2.data.SecurityRepository;
import com.udacity.catpoint2.data.Sensor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {
    private ImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();
    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }
    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        // Fetch the current state before any changes.
        ArmingStatus currentStatus = this.securityRepository.getArmingStatus();
        // Transition check
        if (armingStatus == ArmingStatus.ARMED_HOME) {
            List<Sensor> sensors = new ArrayList<>(this.securityRepository.getSensors());
            for (Sensor sensor : sensors) {
                if (sensor.getActive()) { //
                    sensor.setActive(false);
                    this.securityRepository.updateSensor(sensor);
                }
            }
        }
        // Update arming status after handling sensor states
        this.securityRepository.setArmingStatus(armingStatus);
        statusListeners.forEach(sl -> sl.sensorStatusChanged());
        // When disarming, always reset the alarm status to NO_ALARM.
        if (armingStatus == ArmingStatus.DISARMED) {
            this.securityRepository.setAlarmStatus(AlarmStatus.NO_ALARM);
        }
    }
    /**
     * Resets all sensors to inactive state.
     */
    private void resetAllSensorsToInactive() {
        List<Sensor> sensorsToUpdate = new ArrayList<>(securityRepository.getSensors());
        for (Sensor sensor : sensorsToUpdate) {
            sensor.setActive(false);
            securityRepository.updateSensor(sensor);
        }
    }
    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        statusListeners.forEach(sl -> sl.catDetected(cat));
    }
    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }
    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }
    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }
    public void armSystem(ArmingStatus armingStatus) {
        setArmingStatus(armingStatus);
        resetAllSensorsToInactive();
    }
    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }
    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        switch(securityRepository.getAlarmStatus()) {
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.NO_ALARM);
            case ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
        }
    }
    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        // Get the current alarm status
        AlarmStatus currentAlarmStatus = getAlarmStatus();
        ArmingStatus currentArmingStatus = getArmingStatus();
        // Check if the alarm is active and the system is armed home; if so, sensor changes should not deactivate the alarm
        if (!(currentAlarmStatus == AlarmStatus.ALARM && currentArmingStatus == ArmingStatus.ARMED_HOME)) {
            // If the alarm is not in ALARM state or the system is not in ARMED_HOME mode, handle sensor activation/deactivation normally
            if (!sensor.getActive() && active) {
                handleSensorActivated();
            } else if (sensor.getActive() && !active) {
                handleSensorDeactivated();
            }
        }
        // Always update the sensor's active status in the repository
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }
    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     */
    public void processImage() {
        boolean catDetected = imageService.imageContainsCat();
        catDetected(catDetected);
        // If a cat is not detected, check if all sensors are inactive before setting the alarm status to NO_ALARM
        if (!catDetected && allSensorsInactive()) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
    }
    /**
     * Checks if all sensors are inactive.
     * @return true if all sensors are inactive, false otherwise.
     */
    private boolean allSensorsInactive() {
        return getSensors().stream().noneMatch(Sensor::getActive);
    }
    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }
    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }
    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }
    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }
    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }
}