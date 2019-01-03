/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see the file COPYING, or write
 * to the Free Software Foundation, Inc.
 */

package de.oltzen.example.mpu9255;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.platform.Platform;
import com.pi4j.platform.PlatformManager;

/**
 * This application can only run on 32 bit java!
 * <p>
 * If you use an odroid c2 you can start the program with: <br>
 * 
 * <pre>
 * sudo /usr/lib/jvm/java-11-openjdk-armhf/bin/java -Dpi4j.platform=odroid -Dpi4j.linking=static -jar MPU9255-0.0.1-jar-with-dependencies.jar
 * </pre>
 * 
 * For other devices you must set other parameters.
 * <p>
 * if you are not have a 32 bit java on your device:
 * 
 * <pre>
 * dpkg –add-architecture armhf
 * sudo apt-get update
 * sudo apt install default-jdk
 * 
 * </pre>
 * 
 * Search for proper java executable.
 */
public class App {
	public static void main(String[] args) throws Exception {
		System.out.println("Hello Freak!");
		boolean calib = false;
		if (args.length>0) {
			calib = "calib".equals(args[0]);
		}
		testMPU9255(calib);
	}

	public static void testMPU9255(boolean calib) throws Exception {
		MPU9255 mpu9255 = new MPU9255();

		// This program is tested on ODROID C2. If you use another device, change the platform
		PlatformManager.setPlatform(Platform.ODROID);
		// PlatformManager.setPlatform(Platform.RASPBERRYPI);

		// Check if you use the correct i2c bus.
		I2CBus i2c = I2CFactory.getInstance(I2CBus.BUS_1);

		mpu9255.init(i2c);
		if (calib) {
			mpu9255.acceleratorCalibration();
		}
		mpu9255.readLoop();

	}

}
