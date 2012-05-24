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

import java.io.File;
import java.io.PrintStream;

import sa.SimulatedAnnealing;

import common.Resources;
import common.Schedule;

import edu.rit.pj.Comm;

/**
 * Final Exam Scheduler using Simulated Annealing Algorithm (Sequential version)
 * Usage: java FESSASeq <sections-file> <students-file> <relationship-file> [schedule-output-file]
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
 * @author Ziyan Zhou (zxz6862)
 *
 */
public class FESSASeq {
	/**
	 * Program main entry
	 * @param args program arguments
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		// Start timing.
		final long t = System.currentTimeMillis();

		// check arguments
		if (args.length != 3 && args.length != 4)
			usage();

		// Initialize middleware
		Comm.init (args);

		// load data
		Resources.loadFromFile(args[0], args[1], args[2]);

		// initialize algorithm
		final SimulatedAnnealing sa = new SimulatedAnnealing();

		// loop until freezing temperature is reached
		while(!sa.isDone())
			// run iterations
			sa.runIteration(sa.getPerturbInteration());

		// save result to file
		final Schedule best = sa.getBest();
		if(args.length == 4) {
			final File outputfile = new File(args[3]);
			outputfile.createNewFile();
			final PrintStream ps = new PrintStream(outputfile);
			ps.println(best);
			ps.flush();
			ps.close();
		}

		// print out timing and stats
		final int[] counters = best.getCounters();
		System.out.println("" + (System.currentTimeMillis()-t) + "\t" + best.getRank() + "\t"
				+ counters[Schedule.SE_COUNTER] + "\t"
				+ counters[Schedule.CE_COUNTER] + "\t"
				+ counters[Schedule.ME_COUNTER] + "\t"
				+ counters[Schedule.FE_COUNTER]);
	}

	/**
	 * Print usage of the program and exit with -1.
	 */
	public static void usage() {
		System.err.println("Usage: java FESSASeq <sections-file> <students-file> <relationship-file> [schedule-output-file]");
		System.err.println("  <sections-file> = required input file containing all sections information");
		System.err.println("  <students-file> = required input file containing all students information");
		System.err.println("  <relationship-file> = required input file containing students sections relationships information");
		System.err.println("  [schedule-output-file] = optional file path to store the resulting exam schedule");
		System.err.println();
		System.exit(-1);
	}
}
