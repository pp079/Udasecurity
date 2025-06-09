package com.udacity.catpoint.security.service;

import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import com.udacity.catpoint.image.ImageService;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;
import java.awt.image.BufferedImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    private SecurityService securityService;

    @Mock
    private ImageService mockimageService;
    @Mock
    private SecurityRepository mocksecurityRepository;
    @Mock
    private StatusListener listener;
    @Mock
    private StatusListener listener1;
    @Mock
    private StatusListener listener2;

    @BeforeEach
    void setup() {
        securityService = new SecurityService(mocksecurityRepository, mockimageService);
    }

    @Test // Test1
    void changeSensorActivationStatus_shouldSetNoAlarm_whenPendingAndSensorsInactive() {
        doReturn(AlarmStatus.PENDING_ALARM).when(mocksecurityRepository).getAlarmStatus();
        doReturn(new HashSet<>()).when(mocksecurityRepository).getSensors();

        securityService.checkSensorsAndUpdateStatus();

        ArgumentCaptor<AlarmStatus> alarmStatusCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(mocksecurityRepository).setAlarmStatus(alarmStatusCaptor.capture());

        assertEquals(AlarmStatus.NO_ALARM, alarmStatusCaptor.getValue());
    }

    @Test // Test2
    void changeSensorActivationStatus_shouldNotChangeAlarm_whenSensorAlreadyInactive() {

        Sensor sensor = new Sensor("BackDoor", SensorType.DOOR);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        mocksecurityRepository.setAlarmStatus(captor.capture());

        assertTrue(captor.getAllValues().isEmpty(), "The alarm status should not be changed.");
    }

    @Test // Test3
    void changeSensorActivationStatus_shouldSetAlarm_whenSensorAlreadyActiveAndReactivatedDuringPending() {
        Sensor sensor = new Sensor("MotionSensor", SensorType.MOTION);
        sensor.setActive(true);
        AlarmStatus alarmStatus = AlarmStatus.PENDING_ALARM;
        ArmingStatus armingStatus = ArmingStatus.ARMED_HOME;
        doReturn(alarmStatus).when(mocksecurityRepository).getAlarmStatus();
        doReturn(armingStatus).when(mocksecurityRepository).getArmingStatus();
        securityService.changeSensorActivationStatus(sensor, true);

        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(mocksecurityRepository).setAlarmStatus(captor.capture());
        assertEquals(AlarmStatus.ALARM, captor.getValue());
    }

    @Test // Test4
    void processImage_shouldSetNoAlarm_whenNoCatAndNoActiveSensors() {
        mockImageServiceForNoCatDetection();
        mockSecurityRepositoryForNoActiveSensors();
        BufferedImage mockImage = mock(BufferedImage.class);
        securityService.processImage(mockImage);
        assertAlarmStatusSetToNoAlarm();
    }

    private void mockImageServiceForNoCatDetection() {
        doReturn(false).when(mockimageService).imageContainsCat(any(), anyFloat());
    }

    private void mockSecurityRepositoryForNoActiveSensors() {
        doReturn(new HashSet<>()).when(mocksecurityRepository).getSensors();
    }

    private void assertAlarmStatusSetToNoAlarm() {
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(mocksecurityRepository).setAlarmStatus(captor.capture());
        assertEquals(AlarmStatus.NO_ALARM, captor.getValue());
    }

    @Test // Test5
    void processImage_shouldTriggerAlarm_whenCatDetectedAndSystemArmedHome() {
        BufferedImage image = mock(BufferedImage.class);
        mockImageServiceForCatDetection(true);
        mockSecurityRepositoryForArmedHome();
        securityService.processImage(image);
        assertAlarmStatusSetToAlarm();
    }

    private void mockImageServiceForCatDetection(boolean catDetected) {
        doReturn(catDetected).when(mockimageService).imageContainsCat(any(), anyFloat());
    }

    private void mockSecurityRepositoryForArmedHome() {
        doReturn(ArmingStatus.ARMED_HOME).when(mocksecurityRepository).getArmingStatus();
    }

    private void assertAlarmStatusSetToAlarm() {
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(mocksecurityRepository).setAlarmStatus(captor.capture());
        assertEquals(AlarmStatus.ALARM, captor.getValue());
    }

    @Test // Test6
    void setArmingStatus_shouldDisarmSystemAndSetNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(mocksecurityRepository).setAlarmStatus(captor.capture());
        assertEquals(AlarmStatus.NO_ALARM, captor.getValue());
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = { "ARMED_HOME", "ARMED_AWAY" })
    void changeSensorActivationStatus_shouldSetPending_whenSystemArmedAndSensorActivated(ArmingStatus armingStatus) {
        Sensor sensor = new Sensor("TestSensor", SensorType.MOTION);
        mockArmingStatus(armingStatus);
        mockAlarmStatus(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(mocksecurityRepository).setAlarmStatus(captor.capture());
        assertEquals(AlarmStatus.PENDING_ALARM, captor.getValue());
    }

    private void mockArmingStatus(ArmingStatus status) {
        doReturn(status).when(mocksecurityRepository).getArmingStatus();
    }

    private void mockAlarmStatus(AlarmStatus status) {
        doReturn(status).when(mocksecurityRepository).getAlarmStatus();
    }

    @Test // Test 8
    void setArmingStatus_shouldDeactivateAllSensors() {
        Sensor sensorOne = createActiveSensor("s1", SensorType.DOOR);
        Sensor sensorTwo = createActiveSensor("s2", SensorType.MOTION);
        Set<Sensor> sensors = new HashSet<>(Arrays.asList(sensorOne, sensorTwo));
        doReturn(sensors).when(mocksecurityRepository).getSensors();
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        assertSensorsDeactivatedAndUpdated(sensorOne, sensorTwo);
    }

    private Sensor createActiveSensor(String name, SensorType type) {
        Sensor sensor = new Sensor(name, type);
        sensor.setActive(true);
        return sensor;
    }

    private void assertSensorsDeactivatedAndUpdated(Sensor... sensors) {
        for (Sensor sensor : sensors) {
            assertFalse(sensor.getActive(), "Sensor should be deactivated: " + sensor.getName());
            verify(mocksecurityRepository).updateSensor(sensor);
        }
    }

    @Test // Test 9
    void processImage_shouldHandleNullImage() {
        securityService.processImage(null);
        verifyNoInteractions(mocksecurityRepository);
    }

    @Test // Test10
    void changeSensorActivationStatus_shouldSetNoAlarm_whenPendingAndSensorDeactivated() {
        Sensor sensor = new Sensor("MainDoor", SensorType.DOOR);
        sensor.setActive(true);
        doReturn(AlarmStatus.PENDING_ALARM).when(mocksecurityRepository).getAlarmStatus();
        doReturn(new HashSet<>()).when(mocksecurityRepository).getSensors();
        securityService.changeSensorActivationStatus(sensor, false);
        assertAlarmStatusSetTo(AlarmStatus.NO_ALARM);
    }

    private void assertAlarmStatusSetTo(AlarmStatus expectedStatus) {
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(mocksecurityRepository).setAlarmStatus(captor.capture());
        assertEquals(expectedStatus, captor.getValue());
    }

    @Test // Test11
    void changeSensorActivationStatus_shouldIgnoreChange_whenAlarmAlreadyActive() {
        Sensor sensor = new Sensor("GlassBreak", SensorType.WINDOW);
        doReturn(AlarmStatus.ALARM).when(mocksecurityRepository).getAlarmStatus();
        securityService.changeSensorActivationStatus(sensor, true);
        verify(mocksecurityRepository, never()).setAlarmStatus(any());

    }

    @Test // Test12
    void removeSensor_shouldCallRepositoryRemove() {
        Sensor sensor = new Sensor("Removable", SensorType.MOTION);
        securityService.removeSensor(sensor);

        verify(mocksecurityRepository).removeSensor(sensor);
    }

    @Test // Test 13
    void processImage_shouldSetNoAlarm_whenNoCatAndAllSensorsInactive() {
        Sensor frontSensor = createInactiveSensor("Front", SensorType.DOOR);
        Sensor backSensor = createInactiveSensor("Back", SensorType.WINDOW);
        Set<Sensor> sensors = Set.of(frontSensor, backSensor);

        doReturn(sensors).when(mocksecurityRepository).getSensors();
        doReturn(false).when(mockimageService).imageContainsCat(any(), anyFloat());
        doReturn(AlarmStatus.PENDING_ALARM).when(mocksecurityRepository).getAlarmStatus();

        BufferedImage dummyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        securityService.processImage(dummyImage);

        verify(mocksecurityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    private Sensor createInactiveSensor(String name, SensorType type) {
        Sensor sensor = new Sensor(name, type);
        sensor.setActive(false);
        return sensor;
    }

    @Test // Test14
    void removeStatusListener_shouldNotFail_whenRemovingUnregisteredListener() {
        securityService.addStatusListener(listener1);
        securityService.removeStatusListener(listener2);
        doReturn(true).when(mockimageService).imageContainsCat(any(), anyFloat());
        BufferedImage dummyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        securityService.processImage(dummyImage);
        verify(listener1).catDetected(true);
    }

    @Test // Test15
    void processImage_shouldNotTriggerAlarm_whenCatDetectedAndSystemArmedAway() {
        doReturn(ArmingStatus.ARMED_AWAY).when(mocksecurityRepository).getArmingStatus();
        doReturn(true).when(mockimageService).imageContainsCat(any(), anyFloat());
        securityService.processImage(mock(BufferedImage.class));
        verify(mocksecurityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = { "ARMED_HOME", "ARMED_AWAY" })
    void changeSensorActivationStatus_shouldSetAlarm_whenSensorActivatedWhilePending(ArmingStatus armingStatus) {
        Sensor sensor = new Sensor("WindowSensor", SensorType.WINDOW);
        doReturn(armingStatus).when(mocksecurityRepository).getArmingStatus();
        doReturn(AlarmStatus.PENDING_ALARM).when(mocksecurityRepository).getAlarmStatus();

        securityService.changeSensorActivationStatus(sensor, true);

        ArgumentCaptor<AlarmStatus> alarmStatusCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(mocksecurityRepository).setAlarmStatus(alarmStatusCaptor.capture());
        assertEquals(AlarmStatus.ALARM, alarmStatusCaptor.getValue());
    }

    @Test // Test17
    void processImage_shouldNotifyStatusListener_whenCatDetected() {
        securityService.addStatusListener(listener);
        doReturn(true).when(mockimageService).imageContainsCat(any(), anyFloat());
        BufferedImage dummyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        securityService.processImage(dummyImage);

        ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);
        verify(listener).catDetected(captor.capture());
        assertTrue(captor.getValue(), "Listener should be notified with catDetected(true)");
    }

    @Test // Test18
    void setArmingStatus_shouldDeactivateAllSensorTypes() {
        Sensor doorSensor = new Sensor("DoorSensor", SensorType.DOOR);
        Sensor windowSensor = new Sensor("WindowSensor", SensorType.WINDOW);
        Sensor motionSensor = new Sensor("MotionSensor", SensorType.MOTION);

        doorSensor.setActive(true);
        windowSensor.setActive(true);
        motionSensor.setActive(false);

        Set<Sensor> allSensors = new HashSet<>(Arrays.asList(doorSensor, windowSensor, motionSensor));
        when(mocksecurityRepository.getSensors()).thenReturn(allSensors);

        doAnswer(invocation -> {
            Sensor incomingSensor = invocation.getArgument(0);
            incomingSensor.setActive(false);
            return null;
        }).when(mocksecurityRepository).updateSensor(any());

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        assertAll(
                () -> assertFalse(doorSensor.getActive()),
                () -> assertFalse(windowSensor.getActive()),
                () -> assertFalse(motionSensor.getActive()));

        verify(mocksecurityRepository).updateSensor(doorSensor);
        verify(mocksecurityRepository).updateSensor(windowSensor);
        verify(mocksecurityRepository).updateSensor(motionSensor);
    }

    @Test // Test19
    void addSensor_shouldCallRepositoryAdd() {
        Sensor sensor = new Sensor("ExtraSensor", SensorType.WINDOW);
        securityService.addSensor(sensor);

        verify(mocksecurityRepository).addSensor(sensor);
    }

    @Test // Test20
    void setArmingStatus_shouldImmediatelyRaiseAlarm_ifCatAlreadyDetected() {
        ImageService mockImageService = mock(ImageService.class);
        SecurityRepository mockSecurityRepository = mock(SecurityRepository.class);
        SecurityService securityService = new SecurityService(mockSecurityRepository, mockImageService);
        when(mockImageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        ArgumentCaptor<AlarmStatus> alarmStatusCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(mockSecurityRepository).setAlarmStatus(alarmStatusCaptor.capture());
        assertEquals(AlarmStatus.ALARM, alarmStatusCaptor.getValue());
    }

    @Test // Test21
    void removeStatusListener_shouldPreventFutureNotifications() {
        ImageService mockImageService = mock(ImageService.class);
        SecurityRepository mockSecurityRepository = mock(SecurityRepository.class);
        StatusListener mockListener = mock(StatusListener.class);
        SecurityService securityService = new SecurityService(mockSecurityRepository, mockImageService);

        securityService.addStatusListener(mockListener);
        securityService.removeStatusListener(mockListener);
        when(mockImageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));

        ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);
        verify(mockListener, never()).catDetected(captor.capture());

        verifyNoInteractions(mockListener);
    }

    @Test // Test22
    void removeStatusListener_shouldStopNotifyingRemovedListeners() {
        ImageService mockImageService = mock(ImageService.class);
        SecurityRepository mockSecurityRepository = mock(SecurityRepository.class);
        StatusListener listener1 = mock(StatusListener.class);
        StatusListener listener2 = mock(StatusListener.class);
        SecurityService securityService = new SecurityService(mockSecurityRepository, mockImageService);

        securityService.addStatusListener(listener1);
        securityService.addStatusListener(listener2);
        securityService.removeStatusListener(listener1);

        when(mockImageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));

        ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);
        verify(listener2).catDetected(captor.capture());
        assertTrue(captor.getValue());

        verify(listener1, never()).catDetected(anyBoolean());
    }

}
