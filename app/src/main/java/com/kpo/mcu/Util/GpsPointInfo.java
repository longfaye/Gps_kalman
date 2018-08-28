package com.kpo.mcu.Util;

/**
 * Created by LoongfayeWoo on 2018/8/28.
 */

public class GpsPointInfo {
    public  long		status;			//状态位定义
    public  int			latitude;		//纬度	1/10000分
    public  int			longitude;		//经度	1/10000分
    public  int			speed;			//速度	1/10KM/H
    public  int 		altitude;		//高度	海拔高度，单位米
    public  int			direction;		//方向	0~359°，正北为0,顺时针
    public  String		time;			//时间	yyyy-MM-dd HH:mm:ss

    public GpsPointInfo()
    {
        status=0;
        latitude=0;
        longitude=0;
        altitude=0;
        speed=0;
        direction=0;
        time="";
    }
}
