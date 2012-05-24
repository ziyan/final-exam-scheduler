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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import edu.rit.pj.reduction.ObjectOp;
import edu.rit.pj.reduction.Op;

/**
 * Exam Schedule class
 * @author Ziyan Zhou (zxz6862)
 * @author Kevin Cheek (kec3707)
 *
 */
public class Schedule implements Serializable, Cloneable, Comparable<Schedule> {
	/**
	 * Serilizable ID
	 */
	private static final long serialVersionUID = -4394548355369930766L;
	
	/**
	 * Total number of exam slots
	 * In this case, 4 exams per day, 5 days
	 */
	public static final int SLOTS = 20;
	
	/**
	 * Number of exam slot per day
	 */
	public static final int SLOTS_PER_DAY = 4;
	
	/**
	 * Number of exam days
	 */
	public static final int DAYS = (int)Math.ceil((double)Schedule.SLOTS/(double)Schedule.SLOTS_PER_DAY);
	
	/**
	 * Weekday names in abbreviation
	 */
	public static final String[] WEEKDAY_NAMES = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
	
	/**
	 * Slot name for each day
	 */
	public static final String[] SLOT_NAMES = {"8:00am", "10:15am", "12:30pm", "2:45pm"};
	
	/**
	 * Reduction operation on two schedules
	 * Always choose the one with lower rank (better)
	 */
	public static final Op REDUCE_OP = new ObjectOp<Schedule>() {
		@Override
		public Schedule op(final Schedule x, final Schedule y) {
			if(x.getRank() < y.getRank())
				return x;
			return y;
		}
	};
	
	/**
	 * Number of stats counters
	 */
	public static final int COUNTERS = 4;
	
	/**
	 * Stats counter location for Simultaneous exams
	 */
	public static final int SE_COUNTER = 0;
	
	/**
	 * Stats counter location for Consecutive exams
	 */
	public static final int CE_COUNTER = 1;
	
	/**
	 * Stats counter location for More than two per day exams
	 */
	public static final int ME_COUNTER = 2;
	
	/**
	 * Stats counter location for Friday/weekend exams
	 */
	public static final int FE_COUNTER = 3;
	
	
	private double rank; // rank for the schedule
	private final int[] counters; // stats counters
	private Map<String, Integer> schedule; // storage structure, Section ID -> slot (0-19)
	private int[] slotCounter; // slot counter, keeps track of # of sections in each slot
	
	/**
	 * Construct schedule with empty schedule set
	 */
	public Schedule() {
		schedule = new LinkedHashMap<String, Integer>();
		slotCounter = new int[SLOTS];
		Arrays.fill(slotCounter, 0);
		rank = Double.NaN; // rank has not been calculated yet
		counters = new int[COUNTERS];
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		final Schedule s = new Schedule();
		s.schedule = (HashMap)((LinkedHashMap)schedule).clone();
		s.slotCounter = slotCounter.clone();
		return s;
	}

	/**
	 * Get mapping of sections in the schedule
	 * @return map of section id to time slot
	 */
	public Map<String, Integer> getSchedule() {
		return schedule;
	}

	/**
	 * Get # of sections in each slot
	 * @return slot counters
	 */
	public int[] getSlotCounter() {
		return slotCounter;
	}

	/**
	 * Return the rank of the schedule
	 * @return rank of this schedule
	 */
	public double getRank() {
		if(Double.isNaN(rank))
			Ranker.rank(this);
		return rank;
	}
	
	/**
	 * Get stats counters
	 * @return stats counters
	 */
	public int[] getCounters() {
		return counters;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if(!(obj instanceof Schedule))
			return false;
		final Schedule o = (Schedule)obj;
		return schedule.equals(o.schedule);
	}

	/**
	 * Set schedule rank, used only by ranker
	 * @param rank rank to be set
	 */
	void setRank(final double rank) {
		this.rank = rank;
	}

	/**
	 * Move one section from a slot to another
	 * And keep track of slotCounter meanwhile
	 * @param section
	 * @param slot
	 */
	public void move(final String section, int slot) {
		slot = slot % Schedule.SLOTS;
		slotCounter[schedule.get(section)]--;
		schedule.put(section, slot);
		slotCounter[slot]++;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(final Schedule o) {
		final double result = this.getRank() - o.getRank();
		if(result > 0)
			return 1;
		if(result < 0)
			return -1;
		return (schedule.equals(o.schedule))?0:1;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String result = "Schedule: rank = " + rank +
			", SE = " + counters[SE_COUNTER] +
			", CE = " + counters[CE_COUNTER] +
			", ME = " + counters[ME_COUNTER] +
			", FE = " + counters[FE_COUNTER] + "\n";
		for(final String section : schedule.keySet()) {
			final int slot = schedule.get(section);
			result += section + "\t"
						+ WEEKDAY_NAMES[slot / SLOTS_PER_DAY] + "\t"
						+ SLOT_NAMES[slot % SLOTS_PER_DAY]
						+ "\n";
		}
		return result;
	}
}