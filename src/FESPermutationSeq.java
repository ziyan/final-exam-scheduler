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

/**
 * Final Exam Scheduler using Simulated Annealing Algorithm (Sequential version)
 * Usage: java FESSASeq <sections-file> <students-file> <relationship-file> [schedule-output-file]
 * 	<sections-file> = required input file containing all sections information
 * 	<students-file> = required input file containing all students information
 * 	<relationship-file> = required input file containing students sections relationships information
 * 	[schedule-output-file] = optional file path to store the resulting exam schedule
 *
 * In the end, the program outputs the following data on standard output:
 * 	number-iterated rank-of-best SE CE ME FE
 * where,
 * 	SE = number of simultaneous exam detected for the best schedule
 * 	CE = number of consecutive exam detected for the best schedule
 *	ME = number of more-than-two-per-day exam detected for the best schedule
 *	FE = number of friday/weekend exam detected for the best schedule
 * @author Yandong
 * @annotation: instead of considering the speedup,
 * @I considering sizeup here.
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import permutation.Generator;

import common.Ranker;
import common.Resources;
import common.Schedule;

import edu.rit.pj.Comm;


/**
 * Final Exam Scheduler using Brute-force  (Sequential version)
 * Usage: java FESPermutationSeq <sections-file> <students-file> <relationship-file> [schedule-output-file]
 * 	<sections-file> = required input file containing all sections information
 * 	<students-file> = required input file containing all students information
 * 	<relationship-file> = required input file containing students sections relationships information
 * 	[schedule-output-file] = optional file path to store the resulting exam schedule
 *
 * In the end, the program outputs the following data on standard output:
 * 	msec-elasped rank-of-best SE CE ME FE
 * where,
 * 	SE = number of simultaneous exam detected for the best schedule
 * 	CE = number of consecutive exam detected for the best schedule
 *	ME = number of more-than-two-per-day exam detected for the best schedule
 *	FE = number of friday/weekend exam detected for the best schedule
 *
 * @author Yandong Wang (yxw9319)
 *
 */
public class FESPermutationSeq {

	/**
	 * Time limit for the program, default 500
	 * Can be specified through JVM parameter -Dfes.p.time
	 */
	static long TIME_LIMIT = Integer.parseInt(System.getProperty("fes.p.time", "500"));
	/**
	 * @param	args[0] = section file Name
	 * @param	args[1] = student file Name
	 * @param	args[2]	= relation file Name
	 */
	public static void main(final String[] args) throws IOException
	{
		// Initialize the cluster.
		Comm.init (args);
		
		// check arguments
		if (args.length != 3 && args.length != 4)
			usage();
		
		// Load data, section information, student information, relation file Name
		Resources.loadFromFile(args[0], args[1], args[2]);

		// Create schedule generator object.
		final Generator scheduleGenerator = Generator.create();

		// This variable is used to remember the number of schedules which have 
		// been iterated.
		long 		checkedSchedule = 0;
		
		// this variable is used to save the optimal schedule.
		Schedule	popularSchedule = null;
		
		// Remember the beginning time of the loop
		final long RunBegin_T = System.currentTimeMillis();

		// Iterate all possible schedule until there is no more possible schedule left
		while(scheduleGenerator.hasNext())
		{
			// Read the current Time, if the running time has exceed the 
			// time limit, then it will exit this loop, and mark the 
			// variable "overState" to show the reason it exit from the loop.
			final long CurTime_T = System.currentTimeMillis();
			if((CurTime_T - RunBegin_T) > TIME_LIMIT ){
				break;
			}
			
			// Increment the number of schedules which have been iterated.
			++checkedSchedule;
				
			// Get the current schedule which is created by schedule generator.
			final Schedule curSchedule = scheduleGenerator.next();
			
			// Submit this schedule to all the students, let them to vote.
			Ranker.rank(curSchedule);
			
			// If this is the first schedule, then save it to optimal schedule directly.
			if(popularSchedule == null){
				popularSchedule = curSchedule;
				continue;
			}
			// If this schedule's rank is lower than the current optimal schedule's,
			// then change the optimal schedule to this schedule.
			else if(popularSchedule.getRank() > curSchedule.getRank()){
				popularSchedule = curSchedule;
			}
		}
		
		// save result to file
		if(args.length == 4) {
			final File outputfile = new File(args[3]);
			outputfile.createNewFile();
			final PrintStream ps = new PrintStream(outputfile);
			ps.println(popularSchedule);
			ps.flush();
			ps.close();
		}
		
		// print out number of iterated schedules and stats
		final int[] counters = popularSchedule.getCounters();
		System.out.println("" + checkedSchedule + "\t" + popularSchedule.getRank() + "\t"
				+ counters[Schedule.SE_COUNTER] + "\t"
				+ counters[Schedule.CE_COUNTER] + "\t"
				+ counters[Schedule.ME_COUNTER] + "\t"
				+ counters[Schedule.FE_COUNTER]);
	}
	
	/**
	 * Print usage of the program and exit with -1.
	 */
	public static void usage() {
		System.err.println("Usage: java FESPermutationSeq <sections-file> <students-file> <relationship-file> [schedule-output-file]");
		System.err.println("  <sections-file> = required input file containing all sections information");
		System.err.println("  <students-file> = required input file containing all students information");
		System.err.println("  <relationship-file> = required input file containing students sections relationships information");
		System.err.println("  [schedule-output-file] = optional file path to store the resulting exam schedule");
		System.err.println();
		System.exit(-1);
	}
}
