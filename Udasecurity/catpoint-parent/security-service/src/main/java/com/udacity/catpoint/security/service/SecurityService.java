package com.udacity.catpoint.security.service;

import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.image.ImageService;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.Sensor;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;

public final class SecurityService {

    private final SecurityRepository securityRepository;
    private final ImageService imageService;
    private final Set<StatusListener> statusListeners = ConcurrentHashMap.newKeySet();
    private volatile boolean catDetected = false;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = Objects.requireNonNull(securityRepository, "SecurityRepository cannot be null");
        this.imageService = Objects.requireNonNull(imageService, "ImageService cannot be null");

    }

    public void setArmingStatus(ArmingStatus armingStatus) {
        boolean isDisarmed = armingStatus == ArmingStatus.DISARMED;
        boolean isArmedHomeWithCat = armingStatus == ArmingStatus.ARMED_HOME && catDetected;
        if (!isDisarmed) {
            deactivateAllSensors();
            if (isArmedHomeWithCat) {
                setAlarmStatus(AlarmStatus.ALARM);
            }
        } else {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        securityRepository.setArmingStatus(armingStatus);
    }

    private void CatDetection() {
        boolean isArmedHome = getArmingStatus() == ArmingStatus.ARMED_HOME;
        boolean noSensorsActive = allSensorsInactive();
        boolean alarmIsNotTriggered = getAlarmStatus() != AlarmStatus.ALARM;
        if (catDetected && isArmedHome) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!catDetected && noSensorsActive && alarmIsNotTriggered) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        notifyCatDetection();
    }

    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    private void deactivateAllSensors() {
        for (Sensor sensor : getSensors()) {
            boolean wasActive = sensor.getActive();
            sensor.setActive(false);
            boolean shouldUpdate = wasActive || getArmingStatus() != ArmingStatus.DISARMED;
            if (shouldUpdate) {
                securityRepository.updateSensor(sensor);
            }
        }
    }

    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        if (getAlarmStatus() != AlarmStatus.ALARM) {
            boolean wasActive = sensor.getActive();
            sensor.setActive(active);
            securityRepository.updateSensor(sensor);
            handleSensorStateUpdate(wasActive, active);
        }
    }

    public void checkSensorsAndUpdateStatus() {
        if (getAlarmStatus() == AlarmStatus.PENDING_ALARM && allSensorsInactive()) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
    }
    private void handleSensorStateUpdate(boolean wasActive, boolean isActive) {
        AlarmStatus alarmStatus = getAlarmStatus();
        if (isActive) {
            handleSensorActivated(alarmStatus, wasActive);
        } else if (wasActive) {
            handleSensorDeactivated(alarmStatus);
        }
    }

    private void handleSensorDeactivated(AlarmStatus alarmStatus) {
        if (alarmStatus == AlarmStatus.PENDING_ALARM) {
            checkSensorsAndUpdateStatus();
        }
    }

    private void handleSensorActivated(AlarmStatus alarmStatus, boolean wasActive) {
        boolean isDisarmed = getArmingStatus() == ArmingStatus.DISARMED;
        boolean shouldTriggerAlarm = alarmStatus == AlarmStatus.PENDING_ALARM || wasActive;
        if (!isDisarmed) {
            AlarmStatus newStatus = shouldTriggerAlarm ? AlarmStatus.ALARM : AlarmStatus.PENDING_ALARM;
            setAlarmStatus(newStatus);
        }
    }

    public void processImage(BufferedImage image) {
        if (image != null) {
            boolean detected = imageService.imageContainsCat(image, 50.0f);
            catDetected = detected;
            CatDetection();
        }
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(listener -> listener.notify(status));
    }

    public Set<Sensor> getSensors() {
        return Collections.unmodifiableSet(securityRepository.getSensors());
    }

    private boolean allSensorsInactive() {
        return getSensors().stream().noneMatch(Sensor::getActive);
    }

    private void notifyCatDetection() {
        for (StatusListener listener : statusListeners) {
            listener.catDetected(catDetected);
        }
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
