package de.oltzen.example.mpu9255;

import java.io.IOException;
import java.util.Arrays;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;

public class MPU9255 {

	private final static int MAX_GRAVITY = 2;

	private I2CDevice mpu9255;
	private I2CDevice ak8963;

	public MPU9255() {
	}

	public void init(I2CBus i2c) throws Exception {
		mpu9255 = i2c.getDevice(0x68);
		ak8963 = i2c.getDevice(0x0C);
		initDevices();
	}

	public void initDevices() throws Exception {

		int fs = 0;

		mpu9255.write(MPU9250Constants.INT_PIN_CFG.getValue(), (byte) 0x22);
		mpu9255.write(MPU9250Constants.INT_ENABLE.getValue(), (byte) 1);
		Thread.sleep(10);

		// MPU_I2C_Write(MPU_I2C_ADDR, MPUREG_I2C_MST_CTRL, 1, 0x00); // Disable I2C master
		mpu9255.write(MPU9250Constants.I2C_MST_CTRL.getValue(), (byte) 0);
		// MPU_I2C_Write(MPU_I2C_ADDR, MPUREG_USER_CTRL, 1, 0x00); // Disable FIFO &
		mpu9255.write(MPU9250Constants.USER_CTRL.getValue(), (byte) 0);

		mpu9255.write(MPU9250Constants.SMPLRT_DIV.getValue(), (byte) 0x00); // Set gyro sample rate to 1 kHz
		Thread.sleep(2);
		mpu9255.write(MPU9250Constants.CONFIG.getValue(), (byte) 0x02); // Set gyro sample rate to 1 kHz and DLPF to 92 Hz
		Thread.sleep(2);
		// Set full scale range for the gyro to 250 dps
		mpu9255.write(MPU9250Constants.GYRO_CONFIG.getValue(), (byte) (1<<fs));// GyrScale.GFS_250DPS.getX());
		Thread.sleep(2);
		// Set accelerometer rate to 1 kHz and bandwidth to 92 Hz
		mpu9255.write(MPU9250Constants.ACCEL_CONFIG2.getValue(), (byte) 0x02);
		Thread.sleep(2);
		// Set full scale range for the accelerometer to 2 g
		int g = MAX_GRAVITY;
		switch (g) {
		case 2:
			mpu9255.write(MPU9250Constants.ACCEL_CONFIG.getValue(), (byte) (0));
			break;
		case 4:
			mpu9255.write(MPU9250Constants.ACCEL_CONFIG.getValue(), (byte) (1<<3));
			break;
		case 8:
			mpu9255.write(MPU9250Constants.ACCEL_CONFIG.getValue(), (byte) (2<<3));
			break;
		case 16:
			mpu9255.write(MPU9250Constants.ACCEL_CONFIG.getValue(), (byte) (3<<3));
			break;
		default:
			throw new Exception("Wrong MAX_GRAVITY value");

		}
		Thread.sleep(2);

		System.out.print("mag info="+Integer.toHexString(ak8963.read(MPU9250Constants.WHO_AM_I_AK8963.getValue())));

		ak8963.write(10, (byte) 0x12); // 16 Bit && mode 2 (100Hz)

	}

	public void readLoop() throws Exception {
		System.out.println("Start MPU9255 read loop.");

		while (true) {
			readMag();
			readAccGyro();
			Thread.sleep(500);
		}

	}

	public void readMag() throws Exception {

		short[] registers = new short[3];

		int state = ak8963.read(MPU9250Constants.AK8963_ST1.getValue());

		if (state==0) {
			System.out.print("*");
			return;
		}

		read16BitRegistersLH(ak8963, MPU9250Constants.AK8963_XOUT_L.getValue(), registers);
		System.out.println("compass: "+Arrays.toString(registers));
		System.out.println("Status2:"+ak8963.read(MPU9250Constants.AK8963_ST2.getValue()));

	}

	public void readAccGyro() throws IOException {
		short[] registers = new short[3];
		read16BitRegisters(mpu9255, MPU9250Constants.ACCEL_XOUT_H.getValue(), registers);
		System.out.println("accelerator: "+Arrays.toString(registers));

		read16BitRegisters(mpu9255, MPU9250Constants.GYRO_XOUT_H.getValue(), registers);
		System.out.println("gyro: "+Arrays.toString(registers));
	}

	public void read16BitRegisters(I2CDevice device, int address, short[] output) throws IOException {
		for (int i = 0; i<output.length; i++) {

			int datahi = device.read(address);
			address++;
			int datalow = device.read(address);
			address++;
			output[i] = (short) (((datahi<<8)&0xff00)|(datalow&0xff));
		}
	}

	public void write16BitRegisters(I2CDevice device, int address, short[] output) throws IOException {
		for (int i = 0; i<output.length; i++) {
			int datahi = (output[i]>>8)&0xFF;
			device.write(address, (byte) datahi);
			address++;
			int datalow = output[i]&0xFF;
			device.write(address, (byte) datalow);
			address++;
		}
	}

	public void read16BitRegistersLH(I2CDevice device, int address, short[] output) throws IOException {
		for (int i = 0; i<output.length; i++) {

			int datalow = device.read(address);
			address++;
			int datahi = device.read(address);
			address++;
			output[i] = (short) (((datahi<<8)&0xff00)|(datalow&0xff));
		}
	}

	public void acceleratorCalibration() throws IOException, InterruptedException {

		boolean repeat = false;
		do {
			repeat = false;
			int[] center = new int[3];
			short[] registers = new short[3];
			short[] calValue = new short[1];
			short[] currentCal = new short[3];

			read16BitRegisters(mpu9255, MPU9250Constants.XA_OFFSET_H.getValue(), calValue);
			currentCal[0] =(short) (calValue[0]>>1);
			read16BitRegisters(mpu9255, MPU9250Constants.YA_OFFSET_H.getValue(), calValue);
			currentCal[1] =(short) ( calValue[0]>>1);
			read16BitRegisters(mpu9255, MPU9250Constants.ZA_OFFSET_H.getValue(), calValue);
			currentCal[2] = (short) (calValue[0]>>1);
			System.out.println("current calibration: "+Arrays.toString(currentCal));
			final int LOOP = 256;
			for (int i = 0; i<LOOP; i++) {

				read16BitRegisters(mpu9255, MPU9250Constants.ACCEL_XOUT_H.getValue(), registers);
				for (int j = 0; j<3; j++) {
					center[j] += registers[j];
				}

				read16BitRegisters(mpu9255, MPU9250Constants.GYRO_XOUT_H.getValue(), registers);

				Thread.sleep(10);
			}
			for (int j = 0; j<3; j++) {
				center[j] /= LOOP;
			}
			System.out.println("accelerator avr: "+Arrays.toString(registers));
			center[2] -= (int) (0x8000/MAX_GRAVITY);
			for (int j = 0; j<3; j++) {
				if (Math.abs(center[j])>30) {
					repeat = true;
				}
			}
			System.out.println("center: "+Arrays.toString(center));
			if (repeat) {
				System.out.println("Set calibration values and repeat measurement.");
				// Because be measure the 2G maximal we must multiply by 2G/16G.
				for (int j = 0; j<3; j++) {
					registers[j] = (short) (currentCal[j]-(center[j]*MAX_GRAVITY/(16*2)));
					registers[j] = (short)(registers[j] <<1); 
				}

				System.out.println("accelerator calibration: "+Arrays.toString(registers));
				calValue[0] = registers[0];
				write16BitRegisters(mpu9255, MPU9250Constants.XA_OFFSET_H.getValue(), calValue);
				calValue[0] = registers[1];
				write16BitRegisters(mpu9255, MPU9250Constants.YA_OFFSET_H.getValue(), calValue);
				calValue[0] = registers[2];
				write16BitRegisters(mpu9255, MPU9250Constants.ZA_OFFSET_H.getValue(), calValue);
			}
		} while (repeat);
		System.out.println("Calibration finished.");
	}

}
