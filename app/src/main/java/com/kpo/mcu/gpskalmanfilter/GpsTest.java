package com.kpo.mcu.gpskalmanfilter;
import static junit.framework.Assert.assertTrue;


public class GpsTest {
	public void testBearingNorth() {
		GpsKalmanFilter f = new GpsKalmanFilter(1.0);
		for (int i = 0; i < 100; ++i) {
			f.updateVelocity(i * 0.0001, 0.0, 1.0);
		}

		double bearing = f.getBearing();
		assertTrue(Math.abs(bearing - 0.0) < 0.01);
		  
		/* Velocity should be 0.0001 x units per timestep */
		double dlat = f.getVelocityLat();
		double dlon = f.getVelocityLong();		
		assertTrue(Math.abs(dlat - 0.0001) < 0.00001);
		assertTrue(Math.abs(dlon) < 0.00001);
	}


	public void testVariableTimestep() {
		GpsKalmanFilter f = new GpsKalmanFilter(1.0);
		
		/* Move at a constant speed but taking slower and slower readings */
		int east_distance = 0;
		for (int i = 0; i < 20; ++i) {
			east_distance += i;
			f.updateVelocity(0.0, east_distance * 0.0001, i);
		}
		
		double dlat = f.getVelocityLat();
		double dlon = f.getVelocityLong();
		
		assertTrue(Math.abs(dlat) < 0.000001);
		assertTrue(Math.abs(dlon - 0.0001) < 0.000001);
	}

	public void testConstantTimestep() {
		GpsKalmanFilter f = new GpsKalmanFilter(1.0);
		
		/* Move at a constant speed */
		int east_distance = 0;
		int north_distance = 0;		
		for (int i = 0; i < 20; ++i) {
			east_distance++;
			north_distance++;
			f.updateVelocity(north_distance * 0.0001, east_distance * 0.0001, 1.0);
		}
		
		double dlat = f.getVelocityLat();
		double dlon = f.getVelocityLong();
		
		assertTrue(Math.abs(dlat - 0.0001) < 0.000001);
		assertTrue(Math.abs(dlon - 0.0001) < 0.000001);
	}
	
	public void testCalculateMph() {
		double mph = GpsKalmanFilter.calculateMph(39.315842, -120.167107,
				-0.000031, 0.000003);
		assertTrue(Math.abs(mph - 7.74) < 0.01);
	}
}
