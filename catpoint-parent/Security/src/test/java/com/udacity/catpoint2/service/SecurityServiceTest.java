package com.udacity.catpoint2.service;
import com.udacity.catpoint.service.ImageService;
import com.udacity.catpoint2.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private ImageService imageService;
    private SecurityService securityService;
    @BeforeEach
    public void setup() {
        securityService = new SecurityService(securityRepository, imageService);
    }
    @Test
    public void whenArmedAndSensorActivated_setPendingAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        Sensor sensor = new Sensor("Door", SensorType.DOOR);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
    @Test
    public void whenSensorActivatedAndSystemPending_setAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor sensor = new Sensor("Window", SensorType.WINDOW);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    @Test
    public void whenAllSensorsInactiveAndSystemPending_setNoAlarm() {
        Set<Sensor> allSensors = new HashSet<>();
        allSensors.add(new Sensor("Door", SensorType.DOOR));
        allSensors.forEach(sensor -> sensor.setActive(true));
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        allSensors.forEach(sensor -> securityService.changeSensorActivationStatus(sensor, false));
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void whenAlarmActive_sensorChangeDoesNotAffectAlarmState(boolean status) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        Sensor sensor = new Sensor("Motion", SensorType.MOTION);
        securityService.changeSensorActivationStatus(sensor, status);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    public void whenSystemDisarmed_setNoAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test for handling sensor deactivation when already inactive and alarm status is pending
    @Test
    public void whenSensorDeactivatedWhileAlreadyInactiveAndSystemPending_makeNoChangesToAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor sensor = new Sensor("Window", SensorType.WINDOW);
        sensor.setActive(false); // Sensor is already inactive
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }
    @Test
    public void whenSensorActivatedWhileAlreadyActiveAndSystemPending_setAlarmState() {
        Sensor sensor = new Sensor("Window", SensorType.WINDOW);
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }


    @Test
    public void whenCameraDetectsCatWhileArmedHome_setAlarmStatusToAlarm() {
        // Given
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat()).thenReturn(true);

        // When
        securityService.processImage();

        // Then
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void whenNoCatDetectedAndAllSensorsInactive_setNoAlarmStatus() {
        // Given
        when(imageService.imageContainsCat()).thenReturn(false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(new HashSet<>());
        securityService.processImage();
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void whenSystemArmed_resetAllSensorsToInactive() {
        // Given
        Sensor sensor1 = new Sensor("Door", SensorType.DOOR);
        Sensor sensor2 = new Sensor("Window", SensorType.WINDOW);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor1);
        sensors.add(sensor2);
        when(securityRepository.getSensors()).thenReturn(sensors);

        // When
        securityService.armSystem(ArmingStatus.ARMED_HOME);

        // Then
        assertFalse(sensor1.getActive());
        assertFalse(sensor2.getActive());
        verify(securityRepository, times(2)).updateSensor(any(Sensor.class));
    }

    @Test
    public void whenArmedHomeAndCameraDetectsCat_setAlarmStatusToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat()).thenReturn(true);
        securityService.processImage();
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }



}