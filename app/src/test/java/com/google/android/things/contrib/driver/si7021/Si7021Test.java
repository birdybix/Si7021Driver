package com.google.android.things.contrib.driver.si7021;

import com.google.android.things.pio.I2cDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;



/**
 * Created by Cirrus on 21/11/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class Si7021Test {
    public static final int I2C_ADDRESS = 0x40;
    private static final int CHIP_ID = 0x3A;
    private static final int READRHT_REG_CMD = 0xE7;
    private static final int MEASRH_NOHOLD_CMD = 0xF5;

    private static final int RESET_CMD = 0xFE;

    byte[] mBuffer = {1, 2, 3};

    @Mock
    private I2cDevice device;

    private Si7021 driver;

    @Before
    public void setUp() throws IOException {
        when(device.readRegByte(READRHT_REG_CMD)).thenReturn((byte)CHIP_ID);
        driver = new Si7021(device);
        verify(device).writeRegByte(I2C_ADDRESS, (byte)RESET_CMD);
    }

    @Test
    public void readRegBuffer() throws Exception {
        driver.readTemperature();
        verify(device).readRegBuffer(anyInt(), any(byte[].class), anyInt());
    }
}
