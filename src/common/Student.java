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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Student class
 * @author Ziyan Zhou (zxz6862)
 *
 */
public class Student {
	/**
	 * Student's weight on each consecutive exam, default to 2.0
	 * Can be specified through JVM parameter -Dfes.ce
	 */
	private static final double WEIGHT_CONSECUTIVE_EXAMS = Double.parseDouble(System.getProperty("fes.ce", "2.0"));
	
	/**
	 * Student's weight on each day that more than two exams have been schedule for, default to 5.0
	 * Can be specified through JVM parameter -Dfes.me
	 */
	private static final double WEIGHT_MORE_THAN_TWO_EXAM_PER_DAY = Double.parseDouble(System.getProperty("fes.me", "5.0"));
	
	/**
	 * Student's weight on each Friday/weekend exam, default to 0.5
	 * Can be specified through JVM parameter -Dfes.fe
	 */
	private static final double WEIGHT_FRIDAY_EXAMS = Double.parseDouble(System.getProperty("fes.fe", "0.5"));

	/**
	 * Student's weight on each Simultaneous exam, default to positive infinity
	 * Can be specified through JVM parameter -Dfes.se
	 */
	private static final double WEIGHT_SIMULTANEOUS_EXAMS = Double.parseDouble(System.getProperty("fes.se", "Infinity"));
	
	private final String id; // student id
	private final String name; // student name
	private final HashSet<Section> sections; // sections the student is taking

	/**
	 * Student constructor
	 * @param id
	 * @param name
	 */
	public Student(final String id, final String name) {
		this.id = id;
		this.name = name;
		this.sections = new LinkedHashSet<Section>();
	}

	/**
	 * Get student Id
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * Get student name
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get a set of sections the student is enrolled in
	 * @return
	 */
	public Set<Section> getSections() {
		return sections;
	}

	/**
	 * Ask this student to vote on the given schedule
	 * @param schedule schedule to be voted
	 * @param counters stats counter to be updated
	 * @return the rank
	 */
	double vote(final Schedule schedule, final int[] counters) {
		double vote = 0.0;
		final int[] mine = new int[sections.size()];
		final int[] week = new int[Schedule.DAYS];
		Arrays.fill(week, 0);
		int i = 0;
		for(final Section section : sections) {
			mine[i] = schedule.getSchedule().get(section.getId());
			week[mine[i]/Schedule.SLOTS_PER_DAY]++;
			i++;
		}

		for(int a=0;a<mine.length-1;a++) {
			for(int b=i;b<mine.length;b++) {
				// detect conflict
				if(mine[a]==mine[b]) {
					counters[Schedule.SE_COUNTER]++;
					vote += WEIGHT_SIMULTANEOUS_EXAMS;
				}

				// consecutive exams
				if(Math.abs(mine[a]-mine[b]) == 1) {
					counters[Schedule.CE_COUNTER]++;
					vote += WEIGHT_CONSECUTIVE_EXAMS;
				}
			}
		}

		// detect more than two exam per day
		for(int d=0;d<week.length;d++) {
			if(week[d] > 2) {
				counters[Schedule.ME_COUNTER]++;
				vote += WEIGHT_MORE_THAN_TWO_EXAM_PER_DAY;
			}
			// detect friday exams
			if(d > 3) {
				counters[Schedule.FE_COUNTER] += week[d];
				vote += week[d] * WEIGHT_FRIDAY_EXAMS;
			}
		}
		return vote;
	}

	/**
	 * Print out the schedule for this student
	 * @param schedule given schedule
	 */
	public void printSchedule(final Schedule schedule) {
		final Section[] slots = new Section[Schedule.SLOTS];
		for(final Section section : sections)
			slots[schedule.getSchedule().get(section.getId())] = section;
		System.out.println("Schedule for Student "+ id + " " + name);
		System.out.println("==========================================");
		System.out.print(" \t\t");
		for(int d = 0; d < Schedule.DAYS; d++)
			System.out.print(Schedule.WEEKDAY_NAMES[d]+"\t\t");
		System.out.println();
		for(int t = 0; t < Schedule.SLOTS_PER_DAY; t++) {
			System.out.print(Schedule.SLOT_NAMES[t]+"\t\t");
			for(int d = 0; d< Schedule.DAYS; d++) {
				final int i = d * Schedule.SLOTS_PER_DAY + t;
				if(slots[i]!=null)
					System.out.print(slots[i].getId() + "\t");
				else
					System.out.print("N/A\t\t");
			}
			System.out.println();
		}
		System.out.println();
	}
}
