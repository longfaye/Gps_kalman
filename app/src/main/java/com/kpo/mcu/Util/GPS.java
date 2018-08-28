package com.kpo.mcu.Util;

import java.util.Calendar;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus.NmeaListener;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;

/**
 * GPS定位信息管理
 *
 * @author wuzongbo
 *
 */
public class GPS {
	private final String mTag = GPS.class.getSimpleName();

	private LocationManager GpsManager;
	private Location Info = null;
	/*GPSConstantPermission*/
	private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
	private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;
	/*Position*/
	private static final int MINIMUM_TIME = 1000;//1s
	private static final int MINIMUM_DISTANCE = 10;//10m
	private boolean mConnected = false;

	public boolean getConnected() {
		return mConnected;
	}

	public void Init(Context context) {
		GpsManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		//API23:we have to check if ACCESS_FINE_LOCATION and/or ACCESS_COARSE_LOCATION permission are granted
		if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
				||ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
			GpsManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINIMUM_TIME, MINIMUM_DISTANCE, new GpsLocationListener());
			GpsManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000, 0, new GpsLocationListener());
			GpsManager.addNmeaListener(nmeaListener);
		}
	}

	public float getSpeed() {
		float i = 0.00f;

		if (Info == null) {
			i = 0.00f;
		} else {
			i = (Info.getSpeed() * 36 / 10);
		}
		return i;
	}

	public float getBearing() {
		if (Info == null)
			return 0.00f;
		else
			return Info.getBearing();
	}

	@SuppressWarnings("deprecation")
	public boolean isGPSEnable(Context context) {
		String str = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
		// Writelog.v(mTag, str);
		if (str != null) {
			return str.contains("gps");
		} else {
			return false;
		}
	}

	public GpsPointInfo GetPoint() {
		double latitude = 0;
		double longitude = 0;
		int direction = 0;
		Calendar cal = Calendar.getInstance();
		GpsPointInfo point = new GpsPointInfo();
		point.time = "2015-01-01 00:00:00";
		if (Info != null) {
			// double d;
			latitude = Info.getLatitude();
			longitude = Info.getLongitude();
			direction  = (int) Info.getBearing();
			point.latitude = (int) (latitude * 600000.00f);
			point.longitude = (int) (longitude * 600000.00f);

			// 经度 1/10000分
			if (mConnected) {
				point.speed = (int) (Info.getSpeed() * 36.00f);
			} else {
				point.speed = 0;
			}
			// 高度 UINT16 海拔高度，单位米
			point.altitude = (int) (Info.getAltitude());
			// 方向0—178,刻度为2度，正北为0，顺时针
			point.direction = direction/2;
		}
		
		// 时间BCD[6] yyy-MM-dd HH:mm:ss
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		point.time = String.format("%d-%02d-%02d %02d:%02d:%02d", year,month, day, hour, min, sec);
		Writelog.i(mTag, "经度:%f,纬度:%f,方向:%d,时间:%s", longitude, latitude, direction, point.time);
		return point;
	}

	public class GpsLocationListener implements LocationListener {

		public void onLocationChanged(Location location) {
			if (location==null) {
				mConnected = false;
                Writelog.i(mTag, "GPSStatus:" + false);
			}else {
				mConnected = true;
			}
			Info = location;
		}

		/**
		 * 定位关闭
		 */
		public void onProviderDisabled(String provider) {
			Writelog.i(mTag, "GPSOnProviderDisabled:" + provider);
		}

		/**
		 * 定位开启
		 */
		public void onProviderEnabled(String provider) {
			Writelog.i(mTag, "GPSOnProviderEnabled:" + provider);
		}

		/**
		 * 状态变更
		 */
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Writelog.i(mTag, "onStatusChanged:" + provider + ",status:" + status);
		}
	}

	public String gpsRMCdata = "";
	private final NmeaListener nmeaListener = new NmeaListener() {

		public void onNmeaReceived(long timestamp, String nmea) {
			if (((nmea.indexOf("$GPRMC") >= 0) || (nmea.indexOf("$GNRMC") >= 0)) && !(gpsRMCdata.equals(nmea))) {
				gpsRMCdata=nmea;
			}
		}
	};
}
