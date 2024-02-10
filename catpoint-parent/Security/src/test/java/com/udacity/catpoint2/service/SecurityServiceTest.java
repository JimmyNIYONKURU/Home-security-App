
package com.udacity.catpoint2.service;

import com.udacity.catpoint.service.ImageService;
import com.udacity.catpoint2.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

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
        allSensors.forEach(sensor -> sensor.setActive(false));
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(allSensors);
        allSensors.forEach(sensor -> securityService.changeSensorActivationStatus(sensor, false));
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    @Test
    public void whenAlarmActive_sensorChangeDoesNotAffectAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        Sensor sensor = new Sensor("Motion", SensorType.MOTION);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }
}