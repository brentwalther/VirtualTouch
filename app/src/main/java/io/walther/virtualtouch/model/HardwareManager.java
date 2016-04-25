package io.walther.virtualtouch.model;

import android.content.Context;
import android.os.SystemClock;
import android.renderscript.ScriptGroup;
import android.util.Log;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanListener;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.ScratchBank;

import java.nio.charset.Charset;

import io.walther.virtualtouch.PlaybackActivity;
import io.walther.virtualtouch.util.ReactionRecorder;

/**
 * Created by brentwalther on 4/10/2016.
 */
public class HardwareManager {

    private static HardwareManager singleton;
    private InputDevice inputDevice;
    private OutputDevice outputDevice;

    private HardwareManager() {
        inputDevice = null;
        outputDevice = null;
    }

    public void connectDevice(DeviceType type, Context context, Bean bean) {
        Log.d("BRENTBRENT", "Attempting to connect to bean called: " + bean.getDevice().getName());
        Log.d("BRENTBRENT", "User claimed it was an: " + type.toString());

        if (type == DeviceType.INPUT_DEVICE) {
            inputDevice = new InputDevice(context, bean);
            inputDevice.attemptConnect();
        } else if (type == DeviceType.OUTPUT_DEVICE) {
            outputDevice = new OutputDevice(context, bean);
            outputDevice.attemptConnect();
        }
    }

    public InputDevice getInputDevice() {
        return inputDevice;
    }

    public OutputDevice getOutputDevice() {
        return outputDevice;
    }

    public Device getConnectedDevice() {
        if (getInputDevice() != null && getInputDevice().isConnected()) {
            return getInputDevice();
        }
        if (getOutputDevice() != null && getOutputDevice().isConnected()) {
            return getOutputDevice();
        }
        return null;
    }

    public Device getDevice(DeviceType type) {
        if (type == DeviceType.INPUT_DEVICE) {
            return getInputDevice();
        } else if (type == DeviceType.OUTPUT_DEVICE) {
            return getOutputDevice();
        }
        return null;
    }

    public enum DeviceType {
        INPUT_DEVICE,
        OUTPUT_DEVICE
    }

    public class InputDevice extends Device implements ReactionRecorder.ReactionDevice {

        private final int NUM_CALIBRATION_VALUES = 40;

        private boolean calibrated;
        private int[] calibrationValues;
        private int calibrationValueIndex;
        private int calibratedValue;

        private int pressure;

        public InputDevice(Context context, Bean bean) {
            super(context, bean);
            calibrated = false;
            calibrationValues = new int[NUM_CALIBRATION_VALUES];
            calibrationValueIndex = 0;
            calibratedValue = 0;
            pressure = 0;
        }

        public int getSqueezePressure() {
            return pressure;
        }

        @Override
        public void onScratchValueChanged(ScratchBank bank, byte[] value) {
            if (bank == ScratchBank.BANK_1) {
                String val = new String(value, Charset.defaultCharset());
                int uncalibratedPressure = Integer.parseInt(val);
                pressure = Math.max(0, uncalibratedPressure - calibratedValue);

//                Log.d("BRENTBRENT", "pressure changed: " + pressure);

                if (!calibrated) {
                    if (calibrationValueIndex < NUM_CALIBRATION_VALUES) {
                        calibrationValues[calibrationValueIndex++] = uncalibratedPressure;
                    } else {
                        int sum = 0;
                        for(int i : calibrationValues) { sum += i; }
                        calibratedValue = sum / NUM_CALIBRATION_VALUES;
                        calibrated = true;
                    }
                }
            }
        }

        @Override
        public DeviceType getDeviceType() {
            return DeviceType.INPUT_DEVICE;
        }

        @Override
        public boolean isReacting() {
            return getSqueezePressure() > 100;
        }
    }

    public class OutputDevice extends Device implements ReactionRecorder.ReactionDevice, PlaybackActivity.Reactor {

        private int pressure;
        private Bean bean;

        public OutputDevice(Context context, Bean bean) {
            super(context, bean);
            pressure = 0;
        }

        public int getSqueezePressure() {
            return pressure;
        }

        @Override
        public DeviceType getDeviceType() {
            return DeviceType.OUTPUT_DEVICE;
        }

        @Override
        public boolean isReacting() {
            return false;
        }

        @Override
        public void react(long time){
            bean.setScratchData(ScratchBank.BANK_1, String.valueOf("1500"));
            Runnable waiter = new OutputDeviceResetter(time, bean);
        }
    }

    private class OutputDeviceResetter implements Runnable {

        private long timeout;
        private Bean bean;

        public OutputDeviceResetter(long timeout, Bean bean) {
            this.timeout = SystemClock.uptimeMillis() + timeout;
            this.bean = bean;
        }

        public void run() {
            while(SystemClock.uptimeMillis() < this.timeout) { /* wait patiently */ }
            bean.setScratchData(ScratchBank.BANK_1, String.valueOf("0"));
        }
    }

    public class Device implements BeanListener {

        protected Bean bean;
        protected boolean deviceConnected;
        protected boolean isAttemptingConnect;
        protected Context context;

        public Device(Context context, Bean bean) {
            deviceConnected = false;
            isAttemptingConnect = false;
            this.bean = bean;
            this.context = context;
        }

        public boolean isConnected() {
            return deviceConnected;
        }

        public boolean isAttemptingConnect() {
            return isAttemptingConnect;
        }

        public void attemptConnect() {
            isAttemptingConnect = true;
            bean.connect(context, this);
        }

        @Override
        public void onConnected() {
            Log.d("BRENTBRENT", "Connected to bean!");
            deviceConnected = true;
            isAttemptingConnect = false;
        }

        @Override
        public void onConnectionFailed() {
            isAttemptingConnect = false;
            Log.d("BRENTBRENT", "Failed to connect to bean!");
        }

        @Override
        public void onDisconnected() {
            Log.d("BRENTBRENT", "Bean disconnected for no reason! :(");
            deviceConnected = false;
        }

        @Override
        public void onSerialMessageReceived(byte[] data) {}

        @Override
        public void onScratchValueChanged(ScratchBank bank, byte[] value) {
        }

        @Override
        public void onError(BeanError error) {
            isAttemptingConnect = false;
            Log.d("BRENTBRENT", "Error connecting to bean! " + error.toString());
        }

        public DeviceType getDeviceType() {
            return null;
        }
    }

    public static HardwareManager getInstance() {
        if (singleton == null) {
            singleton = new HardwareManager();
        }
        return singleton;
    }
}
