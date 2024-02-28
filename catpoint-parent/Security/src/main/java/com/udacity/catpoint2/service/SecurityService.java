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
    private boolean catDetected = false;
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
        // Check if the system is transitioning to an armed state
        if (armingStatus != ArmingStatus.DISARMED) {
            // Reset all sensors to inactive when arming the system
            deactivateAllSensors();
        }
        // Check if the system is transitioning from armed to disarmed
        if (currentStatus != ArmingStatus.DISARMED && armingStatus == ArmingStatus.DISARMED) {
            // Set the alarm status to NO_ALARM when disarmed
            setAlarmStatus(AlarmStatus.NO_ALARM);
        } else if (catDetected && armingStatus == ArmingStatus.ARMED_HOME) {
            // Set the alarm status to ALARM if a cat is detected while armed at home
            setAlarmStatus(AlarmStatus.ALARM);
        }
        // Update arming status after handling sensor states
        this.securityRepository.setArmingStatus(armingStatus);
        statusListeners.forEach(sl -> sl.sensorStatusChanged());
    }
    private void deactivateAllSensors() {
        List<Sensor> sensors = new ArrayList<>(this.securityRepository.getSensors());
        for (Sensor sensor : sensors) {
            sensor.setActive(false);
            this.securityRepository.updateSensor(sensor);
        }
    }
    private void notifyStatusListeners() {
        statusListeners.forEach(StatusListener::sensorStatusChanged);
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
        catDetected = cat;
        if (!cat && allSensorsInactive() && getAlarmStatus() != AlarmStatus.NO_ALARM) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }else if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
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
        // Check if the system is armed before resetting sensors
        if (armingStatus != ArmingStatus.DISARMED) {
            resetAllSensorsToInactive();
        }
    }
    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        // Check if the system is disarmed, in which case no action is needed.
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return;
        }
        // If the system is armed and the current alarm status is NO_ALARM, set to PENDING_ALARM.
        // If it's already in PENDING_ALARM, set to ALARM.
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
            // If the system is already in ALARM status, no change is required.
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
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     */
    public void processImage() {
        boolean catDetected = imageService.imageContainsCat();
        catDetected(catDetected);
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
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        // Fetch the current alarm and arming statuses
        AlarmStatus currentAlarmStatus = getAlarmStatus();
        ArmingStatus currentArmingStatus = getArmingStatus();
        // Update the sensor's activation status
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
        // If the alarm is active, do not change the alarm state regardless of sensor changes
        if (currentAlarmStatus == AlarmStatus.ALARM) {
            return; // Early exit to ensure alarm state is unaffected by sensor changes
        }
        // Handle sensor activation
        if (active) {
            if (currentArmingStatus != ArmingStatus.DISARMED && currentAlarmStatus == AlarmStatus.NO_ALARM) {
                setAlarmStatus(AlarmStatus.PENDING_ALARM);
            }
            else if (currentAlarmStatus == AlarmStatus.PENDING_ALARM) {
                setAlarmStatus(AlarmStatus.ALARM);
            }
            // If the system is in NO_ALARM state but disarmed, activating a sensor does not change the alarm status.
        } else {
            // If sensor is deactivated, check the state of all other sensors and current alarm status
            boolean allSensorsInactive = getSensors().stream().noneMatch(Sensor::getActive);
            // If all other sensors are inactive and system is in PENDING_ALARM, revert to NO_ALARM
            if (allSensorsInactive && currentAlarmStatus == AlarmStatus.PENDING_ALARM) {
                setAlarmStatus(AlarmStatus.NO_ALARM);
            }
            else if (!catDetected && allSensorsInactive()) {
                setAlarmStatus(AlarmStatus.NO_ALARM);
            }
        }
    }
}
