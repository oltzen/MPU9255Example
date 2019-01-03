# MPU9255Example

This example code should help you to use the MPU9255 with a Odroid C2. With a few changes you can use a Raspberry Pi too. 

I try to keep the source code smart and simple. The MPU9255 has some tricky issues. For example, the precondition for the access to magnetometer is that you switch off the internal I2C master bus. After that, the magnetometer has the own I2C address.  
