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

package common;

import java.util.Arrays;
import java.util.Collection;

/**
 * Ranker utilities
 * @author Ziyan Zhou (zxz6862)
 * @author Kevin Cheek (kec3707)
 * 
 */
public class Ranker {
	/**
	 * Variant weight on schedule rank, default to 1.0
	 * Variant is calculated based on the variation of slot counters
	 * Can be specified through JVM parameter -Dfes.v
	 */
	public static final double WEGIHT_VARIANT = Double.parseDouble(System.getProperty("fes.v", "1.0"));

	/**
	 * Variant threshold, default to 100.0
	 * If a schedule's slot counter varies too much, it is discarded
	 * Can be specified through JVM parameter -Dfes.vt
	 */
	public static final double WEIGHT_VARIANT_THRESHOLD = Double.parseDouble(System.getProperty("fes.vt", "100.0"));

	/**
	 * Rank the given schedule using objective function
	 * @param schedule schedule to be ranked
	 * @return the rank
	 */
	public static double rank(final Schedule schedule) {
		double rank;

		// the best distribution of schedules is that every slot has the same amount of sections
		final double mean = (double)Resources.getSections().size() / (double)Schedule.SLOTS;

		// variant
		double variant = 0.0;

		// slot counter, each int is the number of sections assigned to that slot
		// the array is always Scheulde.SLOTS long
		final int[] slotCounter = schedule.getSlotCounter();

		// initialize stats counters
		final int[] counters = schedule.getCounters();
		Arrays.fill(counters, 0);

		// calculate variant
		for(int i=0;i<slotCounter.length;i++)
			variant += ((double)slotCounter[i] - mean) * ((double)slotCounter[i] - mean);
		variant /= (double)slotCounter.length;

		if(variant > WEIGHT_VARIANT_THRESHOLD) {
			// variant is too big, discard this schedule
			rank = Double.POSITIVE_INFINITY;
		} else  {
			// add the weight
			rank = variant * WEGIHT_VARIANT;

			// get students' votes
			final Collection<Student> students = Resources.getStudents().values();
			for(final Student student : students) {
				rank += student.vote(schedule, counters);
				// a little bit optimization
				if(Double.isInfinite(rank)) break;
			}
		}
		// set the rank on the schedule
		schedule.setRank(rank);
		return rank;
	}

}
