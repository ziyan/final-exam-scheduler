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

package permutation;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import common.Resources;
import common.Schedule;


/**
 * Permutation generator
 * This generator generates all schedule between given start and end schedules
 * @author Ziyan Zhou (zxz6862)
 * @author yandong wang
 *
 */
public class Generator implements Iterator<Schedule> {
	private static String[] cachedSortedSectionIds; // cached section ids
	
	/**
	 * Creates a generator that generates schedule between begin and end indexes
	 * @param beginIndex
	 * @param endIndex
	 * @return Generator
	 */
	public static Generator create(final int beginIndex, final int endIndex) {
		if (cachedSortedSectionIds == null)
			cachedSortedSectionIds = Resources.getCachedSortedSectionIds();
		
		final Schedule start = new Schedule();
		final Schedule end	 = new Schedule();
		final Map<String, Integer> schedule_1 = start.getSchedule();
		final Map<String, Integer> schedule_2 = end.getSchedule();
		for(int i=0; i<cachedSortedSectionIds.length; i++)
		{
			schedule_1.put(cachedSortedSectionIds[i], beginIndex);
			schedule_2.put(cachedSortedSectionIds[i], endIndex);
		}
		start.getSlotCounter()[beginIndex] = cachedSortedSectionIds.length;
		end.getSlotCounter()[endIndex]	   = cachedSortedSectionIds.length;
		return new Generator(start, end);
	}

	/**
	 * Create a generator that generates every possible schedule
	 * @return Generator
	 */
	public static Generator create() {
		if (cachedSortedSectionIds == null) {
			cachedSortedSectionIds = Resources.getSections().keySet().toArray(new String[]{});
			Arrays.sort(cachedSortedSectionIds);
		}
		final Schedule start = new Schedule();
		final Map<String, Integer> schedule = start.getSchedule();
		for(int i=0; i<cachedSortedSectionIds.length; i++)
			schedule.put(cachedSortedSectionIds[i], 0);
		start.getSlotCounter()[0] = cachedSortedSectionIds.length;
		final Schedule end = start;
		return new Generator(start, end);
	}

	private final Schedule start; // start schedule
	private Schedule next; // next schedule
	private final Schedule end; // end schedule

	/**
	 * Private constructor of generator
	 * This iterator generates all possible schedule between start and end schedules
	 * @param start start schedule
	 * @param end end schedule
	 */
	private Generator(final Schedule start, final Schedule end) {
		this.start = start;
		this.end = end;
		this.next = this.start;
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext() {
		return next != null;
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
	public Schedule next() {
		// return next schedule and generate the next one
		// if there is none, return null
		if (next == null) return null;
		// current is holding the return value
		final Schedule current = next;
		// clone the old next
		next = (Schedule)next.clone();
		// hold the map
		final Map<String, Integer> schedule = next.getSchedule();
		// starting from the lowest in the order
		for(int i=cachedSortedSectionIds.length - 1; i >= 0; i--) {
			// get current slot #
			int slot = schedule.get(cachedSortedSectionIds[i]);
			// increment the slot #
			slot = (slot+1) % Schedule.SLOTS;
			// move the schedule
			next.move(cachedSortedSectionIds[i], slot);
			// if it gets over, we continue to increment the previous section in order
			if (slot > 0) break;
		}
		// if the end if reached, we quit
		if(next.equals(end))
			next = null;
		return current;
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
