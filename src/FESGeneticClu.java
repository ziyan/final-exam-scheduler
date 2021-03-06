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

import common.Resources;
import common.Schedule;

import edu.rit.mp.buf.ObjectItemBuf;
import edu.rit.pj.Comm;
import genetic.Population;

/**
 * Final Exam Scheduler using Genetic Algorithm (Cluster version)
 * Usage: java -Dpj.np=<K> FESGeneticClu <sections-file> <students-file> <relationship-file> [schedule-output-file]
 * 	<K> = number of cluster node to be used
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
 * @author Kevin Cheek (kec3707)
 *
 */
public class FESGeneticClu {
	/**
	 * Program main entry
	 * @param args program arguments
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		// Start timing
		final long t = System.currentTimeMillis();

		// check arguments
		if (args.length != 3 && args.length != 4)
			usage();

		// Initialize middleware
		Comm.init (args);
		final Comm world = Comm.world();		

		// load data
		Resources.loadFromFile(args[0], args[1], args[2]);

		// initialize algorithm
		final Population pop = new Population(Resources.getCachedSortedSectionIds().length);

		// determine number of generation to run on one node
		final int gen = (int)Math.ceil((double)Integer.parseInt(System.getProperty("fes.ga.gen", "500"))/(double)world.size());

		// repeat for number of generations
		for( int i = 0; i < gen; i++ )
			// calculate next generation
			pop.nextGeneration();

		// reduction
		world.allReduce(0,  ObjectItemBuf.buffer(pop.getBest()), Schedule.REDUCE_OP);

		// if it is node 0
		if(world.rank() == 0) {
			// save result to file
			final Schedule best = pop.getBest();
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
	}

	/**
	 * Print usage of the program and exit with -1.
	 */
	public static void usage() {
		System.err.println("Usage: java -Dpj.np=<K> FESGeneticClu <sections-file> <students-file> <relationship-file> [schedule-output-file]");
		System.err.println("  <K> = number of cluster node to be used");
		System.err.println("  <sections-file> = required input file containing all sections information");
		System.err.println("  <students-file> = required input file containing all students information");
		System.err.println("  <relationship-file> = required input file containing students sections relationships information");
		System.err.println("  [schedule-output-file] = optional file path to store the resulting exam schedule");
		System.err.println();
		System.exit(-1);
	}
}
