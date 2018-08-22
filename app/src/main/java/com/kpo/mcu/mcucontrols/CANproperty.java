package com.kpo.mcu.mcucontrols;

/**
 * Created by kp on 2015/12/14.
 */
public class CANproperty {
    public static final int CAN_Baud_5K		    = 0;	        //0. 5Kbps
    public static final int CAN_Baud_10K		= 1;			//1. 10Kbps
    public static final int CAN_Baud_20K		= 2;			//2. 20Kbps
    public static final int CAN_Baud_50K		= 3;			//3. 50Kbps
    public static final int CAN_Baud_100K		= 4;			//4. 100Kbps
    public static final int CAN_Baud_125K		= 5;			//5. 125Kbps
    public static final int CAN_Baud_250K		= 6;			//6. 250Kbps
    public static final int CAN_Baud_500K		= 7;			//7. 500Kbps
    public static final int CAN_Baud_800K		= 8;			//8. 800Kbps
    public static final int CAN_Baud_1MK		= 9;

    public static final int CAN_SetReceiveID	= 0;	        //0. set the point frame ID
    public static final int CAN_SetReceiveAll	= 1;		    //1. set the all frame ID
    public static final int CAN_SetBaudRate	= 2;		        //2. set the Can baud.
    public static final int CAN_WriteData	= 3;

    public static final int CAN_STD_FRAME = 0;
    public static final int CAN_EXD_FRAME = 1;

    public static final int CAN_DATA_FRAME = 0;
    public static final int CAN_REMOTE_FRAME = 1;
    public static final int CAN_FRAME = 2;
}
