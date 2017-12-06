package com.google.android.things.contrib.driver.si7021;

import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;


public class Si7021 implements AutoCloseable {

    private static final String TAG = Si7021.class.getSimpleName();
    /**
     * I2C address for the sensor.
     */
    public static final int I2C_ADDRESS = 0x40;

    private static final int CHIP_ID = 0x3A;

    // Measure Relative Humidity, Hold Master Mode
    private static final int MEASRH_HOLD_CMD = 0xE5;
    // Measure Relative Humidity, No Hold Master Mode
    private static final int MEASRH_NOHOLD_CMD = 0xF5;
    // Measure Temperature, Hold Master Mode
    private static final int MEASTEMP_HOLD_CMD = 0xE3;
    // Measure Temperature, No Hold Master Mode
    private static final int MEASTEMP_NOHOLD_CMD = 0xF3;
    // Read Temperature Value from Previous RH Measurement
    private static final int READPREVTEMP_CMD = 0xE0;
    // Reset
    private static final int RESET_CMD = 0xFE;
    // Write RH/T User Register 1
    private static final int WRITERHT_REG_CMD = 0xE6;
    // Read RH/T User Register 1
    private static final int READRHT_REG_CMD = 0xE7;
    // Write Heater Control Register
    private static final int WRITEHEATER_REG_CMD = 0x51;
    // Read Heater Control Register
    private static final int READHEATER_REG_CMD = 0x11;
    // Read Electronic ID 1st Byte
    private static final int ID1_CMD = 0xFA0F;
    // Read Electronic ID 2nd Byte
    private static final int ID2_CMD = 0xFCC9;
    // Read Firmware Revision
    private static final int FIRMVERS_CMD = 0x84B8;

    /**
     * Mininum temperature in Celsius the sensor can measure.
     */
    public static final float MIN_TEMP_C = -10f;
    /**
     * Maximum temperature in Celsius the sensor can measure.
     */
    public static final float MAX_TEMP_C = 85f;

    /**
     * Mininum temperature in Celsius the sensor can measure.
     */
    public static final float MIN_HUM_C = 0f;
    /**
     * Maximum temperature in Celsius the sensor can measure.
     */
    public static final float MAX_HUM_C = 80f;
    /**
     * Maximum power consumption in micro-amperes when measuring temperature.
     */
    public static final float MAX_POWER_CONSUMPTION_TEMP_UA = 120f;
    /**
     * Maximum power consumption in micro-amperes when measuring humidity.
     */
    public static final float MAX_POWER_CONSUMPTION_HUMIDITY_UA = 180f;
    /**
     * Maximum frequency of the measurements.
     */
    public static final float MAX_FREQ_HZ = 400f;
    /**
     * Minimum frequency of the measurements.
     */
    public static final float MIN_FREQ_HZ = 100f;

    private I2cDevice device;

    private final byte[] buffer = new byte[2]; // for reading sensor values

    /**
     * Create a new Si7021 sensor driver connected on the given bus.
     *
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     */
    public Si7021(String bus) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        I2cDevice device = pioService.openI2cDevice(bus, I2C_ADDRESS);
        try {
            connect(device);
        } catch (IOException | RuntimeException e) {
            try {
                close();
            } catch (IOException | RuntimeException ignored) {
            }
            throw e;
        }
    }


    /**
     * Create a new Si7021 sensor driver connected to the given I2c device.
     *
     * @param device I2C device of the sensor.
     * @throws IOException
     */
    Si7021(I2cDevice device) throws IOException {
        connect(device);
    }

    private void connect(I2cDevice device) throws IOException {
        this.device = device;

        reset();

        int mChipId = this.device.readRegByte(READRHT_REG_CMD);

        if(mChipId != CHIP_ID) {
            throw new IOException("Wrong chip!");
        }

    }

    private void reset() throws IOException {
        device.writeRegByte(I2C_ADDRESS, (byte)RESET_CMD);
    }

    /**
     * Read the current temperature.
     *
     * @return the current temperature in degrees Celsius
     */
    public float readTemperature() throws IOException, IllegalStateException {

        int rawTemp = readSample(MEASTEMP_HOLD_CMD);
        return compensateTemperature(rawTemp);
    }

    /**
     * Read the relative humidity.
     *
     * @return the current temperature in degrees Celsius
     */
    public float readHumidity() throws IOException, IllegalStateException {

        int rawTemp = readSample(MEASRH_HOLD_CMD);
        return compensateHumidity(rawTemp);
    }

    /**
     * Reads 20 bits from the given address.
     * @throws IOException
     */
    private int readSample(int address) throws IOException, IllegalStateException {
        if (device == null) {
            throw new IllegalStateException("I2C device not open");
        }
        synchronized (buffer) {
            try {
                    device.readRegBuffer(address, buffer, buffer.length);
                    // msb[7:0] lsb[7:0] xlsb[7:4]
                    int msb = buffer[0] & 0xff;
                    int lsb = buffer[1] & 0xff;
                    // Convert to 20bit integer
                    return msb << 8 | lsb;
                }

            catch (IOException e) {
                Log.e(TAG, e.toString());
                throw e;
            }
        }
    }


    // Compensation formula from the Si7021 datasheet.
    // https://cdn-learn.adafruit.com/assets/assets/000/035/931/original/Support_Documents_TechnicalDocs_Si7021-A20.pdf
    @VisibleForTesting
    static float compensateTemperature(int rawTemp) {

        return ((175.72f * (float) rawTemp) / 65536)  - 46.85f;
    }

    // Compensation formula from the Si7021 datasheet.
    // https://cdn-learn.adafruit.com/assets/assets/000/035/931/original/Support_Documents_TechnicalDocs_Si7021-A20.pdf
    @VisibleForTesting
    static float compensateHumidity(int rhCode) {

        return ((125f * (float) rhCode) / 65536)  - 6;
    }

    /**
     * Close the driver and the underlying device.
     */
    @Override
    public void close() throws IOException {
        if (device != null) {
            try {
                device.close();
            } finally {
                device = null;
            }
        }
    }

}
