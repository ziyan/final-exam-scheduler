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
import java.util.Map;

/**
 * Random related utilities
 * @author Ziyan Zhou (zxz6862)
 *
 */
public class Random {
	private static java.util.Random random = new java.util.Random();

	/**
	 * Randomly generate a double
	 * @return randomly generated double
	 */
	public static double generateDouble() {
		return random.nextDouble();
	}

	/**
	 * Randomly generate an integer
	 * @return randomly generated int
	 */
	public static int generateInt() {
		return random.nextInt();
	}

	/**
	 * Randomly generate an integer given the upperbound
	 * @param n upperbound of the random integer
	 * @return randomly generated int < n
	 */
	public static int generateInt(final int n) {
		return random.nextInt(n);
	}

	/**
	 * Randomly generate a schedule
	 * @return randomly generated schedule
	 */
	public static Schedule generateSchedule() {
		final Schedule s = new Schedule();
		final Map<String, Integer> schedule = s.getSchedule();
		final int[] slotCounter = s.getSlotCounter();
		int slot;
		for(final String section : Resources.getSections().keySet()) {
			slot = (int)(Math.random() * Schedule.SLOTS);
			schedule.put(section, slot);
			slotCounter[slot]++;
		}
		return s;
	}

}
