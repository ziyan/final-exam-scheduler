/*
 * Final Exam Scheduler
 * Graduate project for 4005-735-01 Parallel Computing I
 * Winter 2008 at Rochester Institute of Technology
 * Team Kyz
 * 		Kevin Cheek (kec3707)
 * 		Yandong Wang (yxw9319)
 * 		Ziyan Zhou (zxz6862)
 * For more information, visit:
 * http://www.cs.rit.edu/~zxz6862/kyz/
 * 
 */

package sa;

import common.Random;
import common.Resources;
import common.Schedule;

/**
 * Simulated Annealing Algorithm for Solving FES problem
 * Optional parameters:
 * 	fes.sa.it = Initial Temperature (0, +inf), default to 0.93
 *  fes.sa.ft = Freezing Temperature (0, +inf), less than initial temperature, default to 2^-30
 *  fes.sa.phi = Temperature decreasing rate (0, 1), default to 0.95
 *  fes.sa.p = Perturb rate (0, +inf), high perturb rate result in more perturb, default 0.1
 * @author Ziyan Zhou (zxz6862)
 *
 */
public class SimulatedAnnealing {
	/**
	 * Initial Temperature, range (0, +inf), default to 0.93
	 * Can be specified through JVM parameter -Dfes.sa.it
	 */
	public static final double INITIAL_TEMPERATURE = Double.parseDouble(System.getProperty("fes.sa.it", "0.93"));

	/**
	 * Freezing Temperature, range (0, +inf), less than initial temperature, default to 2^-30
	 * Can be specified through JVM parameter -Dfes.sa.ft
	 */
	public static final double FREEZING_TEMPERATURE = Double.parseDouble(System.getProperty("fes.sa.ft", ""+Math.pow(2, -30)));

	/**
	 * Temperature decreasing rate, range (0, 1), T1=T0*PHI, default to 0.95
	 * Can be specified through JVM parameter -Dfes.sa.phi
	 */
	public static final double PHI  = Double.parseDouble(System.getProperty("fes.sa.phi", "0.95"));

	/**
	 * Perturb rate, range (0, +inf), high perturb rate result in more perturb, default to 0.1
	 * Can be specified through JVM parameter -Dfes.sa.p
	 */
	public static final double PERTURB = Double.parseDouble(System.getProperty("fes.sa.p", "0.1"));

	private static String[] cachedSortedSectionIds;
	private Schedule best;
	private double temperature;
	public SimulatedAnnealing() {
		// initial configuration
		best = Random.generateSchedule();
		temperature = INITIAL_TEMPERATURE;

		// cache
		if(cachedSortedSectionIds == null)
			cachedSortedSectionIds = Resources.getCachedSortedSectionIds();
	}

	/**
	 * Number of perturb iteration
	 * @return number of perturb iteration
	 */
	public int getPerturbInteration() {
		return cachedSortedSectionIds.length * Schedule.SLOTS;
	}

	/**
	 * Should the algorithm terminate
	 * @return true if the current temperature is below freezing temperature
	 */
	public boolean isDone() {
		return temperature < FREEZING_TEMPERATURE;
	}

	/**
	 * Get current temperature
	 * @return current temperature
	 */
	public double getTemperature() {
		return temperature;
	}

	/**
	 * Execute iterations
	 * @param iterations number of iterations to be executed
	 */
	public void runIteration(final int iterations) {
		Schedule schedule = best;
		for(int i=0;i<iterations;i++) {
			// perturb
			final Schedule perturbed = (Schedule)schedule.clone();
			// number of changes to be made
			final int changes = (int)(Random.generateDouble() * cachedSortedSectionIds.length * PERTURB) + 1;
			for(int j=0;j<changes;j++) {
				// randomly select a section
				final String section = cachedSortedSectionIds[(int) (Random.generateDouble() * cachedSortedSectionIds.length)];
				// randomly reassign it
				perturbed.move(section, (int) (Random.generateDouble() * Schedule.SLOTS));
			}
			// delta OF1
			final double change = perturbed.getRank() - schedule.getRank();
			// if better, accept
			if(change < 0)
				schedule = perturbed;
			// otherwise accept with probability
			else if(Random.generateDouble() < Math.pow(Math.E, -change / temperature))
				schedule = perturbed;
			//save best so far
			if (schedule.getRank() < best.getRank())
				best = schedule;
		}
		// decrease temperature
		temperature *= PHI;
	}

	/**
	 * Retrieve current best schedule
	 * @return currently best schedule
	 */
	public Schedule getBest() {
		return best;
	}

	/**
	 * Set current best schedule
	 * @param schedule schedule to be set
	 */
	public void setBest(final Schedule schedule) {
		this.best = schedule;
	}
}
