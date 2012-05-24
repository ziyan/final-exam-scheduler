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
 * @author Yandong
 * @annotation: instead of considering the speedup,
 * @I considering sizeup here.
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
 *
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

import edu.rit.mp.ObjectBuf;
import edu.rit.mp.buf.ObjectItemBuf;
import edu.rit.pj.Comm;
import edu.rit.pj.CommStatus;
import edu.rit.pj.ParallelRegion;
import edu.rit.pj.ParallelSection;
import edu.rit.pj.ParallelTeam;
import edu.rit.util.Range;


/**
 * Final Exam Scheduler using Brute-force (Cluster version)
 * Usage: java -Dpj.np=<K> FESPermutationClu <sections-file> <students-file> <relationship-file> [schedule-output-file]
 *  <K> = number of cluster node to be used
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
 *
 * @author Yandong Wang (yxw9319)
 * 
 */
public class FESPermutationClu{

	/**
	 * Time limit for the program, default 500
	 * Can be specified through JVM parameter -Dfes.p.time
	 */
	static long TIME_LIMIT 		= Integer.parseInt(System.getProperty("fes.p.time", "500")); 

	// Very processor in the cluster need to remember command Comm.
	static Comm world	   		= null;

	// Mark the message that worker has finished its own work.
	static int	WORK_DONE		= 1;

	// Mark the message that the time worker has used excess the time limit
	static long FINISH_PIECE	= (long)3;

	/**
	 * Trunk size for scheduler, default 1
	 * Can be specified through JVM parameter -Dfes.p.trunk
	 */
	static final int  INCREMENT_STEP	= Integer.parseInt(System.getProperty("fes.p.trunk", "1"));
	

	// Mark how many schedule have been iterated
	static long totalSchedules = 0;

	// Local optimal schedule.
	static 	Schedule	popularSchedule = null;

	public static void main(final String[] args) throws IOException
	{
		// Initialize the cluster.
		Comm.init (args);

		// check arguments
		if (args.length != 3 && args.length != 4)
			usage();

		// Get the common variable.
		world = Comm.world();

		/**
		 * @param	args[0] = section file Name
		 * @param	args[1] = student file Name
		 * @param	args[2]	= relation file Name
		 */
		Resources.loadFromFile(args[0], args[1], args[2]);

		if(world.rank() == 0){
			try{
				// Here I will create two threads, one is running master.
				// the other is running worker.
				new ParallelTeam(2).execute(new ParallelRegion(){
					public void run() throws Exception{
						execute (new ParallelSection(){
							public void run() throws Exception{
								master();
							}
						},
						new ParallelSection() {
							public void run() throws Exception{
								Worker();
							}
						});
					}
				});
			}catch(final Exception e){}
		}
		else{
			Worker();
		}

		final ObjectItemBuf<Schedule> buf = ObjectItemBuf.buffer();
		// reduce
		buf.item = popularSchedule;
		world.allReduce(0, buf, Schedule.REDUCE_OP);

		if(world.rank() == 0)
		{
			// save file
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
			System.out.println("" + totalSchedules + "\t" + popularSchedule.getRank() + "\t"
					+ counters[Schedule.SE_COUNTER] + "\t"
					+ counters[Schedule.CE_COUNTER] + "\t"
					+ counters[Schedule.ME_COUNTER] + "\t"
					+ counters[Schedule.FE_COUNTER]);
		}
	}


	public static void master()
	{
		try{
			final int size = world.size();

			// Send the range to all the workers.
			int UpperBound = 0;
			for(int i = 0; i < size; ++i)
			{
				final int lower 	= i * INCREMENT_STEP;
				final int upper	= lower + INCREMENT_STEP;
				final Range range = new Range(lower,upper);
				world.send(i,ObjectBuf.buffer(range));
				UpperBound = upper;
			}



			int  quitSize = 0;
			while(true)
			{
				// Once it get the message from worker that it has finished 
				// its current work, then it send a new range to the worker
				final ObjectItemBuf<Long> buf = ObjectBuf.buffer();
				final CommStatus status = world.receive(null,WORK_DONE,buf);
				// if the message get from worker show that it is finish work message,
				// then send a new message, if the message show that is because exceeding the 
				// time limit, then master record how many workers have exit the program.
				if(buf.item == FINISH_PIECE){
					final int worker = status.fromRank;
					final Range range = new Range(UpperBound,UpperBound+INCREMENT_STEP);
					world.send(worker,ObjectBuf.buffer(range));
					UpperBound += INCREMENT_STEP ;
				}
				else{
					totalSchedules += buf.item;
					// Number of workers who have exceeded the time limit.
					++quitSize;
					if(quitSize == world.size()){
						break;
					}
				}
			}
		}
		catch(final IOException e){
		}

	}
	public static void Worker()
	{	
		try{ 
			long 		checkedSchedule = 0;
			// The loop beginning time.
			final long BeginTime = System.currentTimeMillis();
			while(true){
				//wait for getting schedule data to vote
				final ObjectItemBuf<Range> rangeBuf = ObjectBuf.buffer();
				world.receive(0, rangeBuf);
				final Range range = rangeBuf.item;
				if(range == null) break;

				// Local schedule Generator 
				final Generator scheduleGenerator = Generator.create(range.lb(),range.ub());

				while(scheduleGenerator.hasNext())
				{
					final long CurrentTime = System.currentTimeMillis();
					if((CurrentTime - BeginTime) > TIME_LIMIT )
					{
						final Long lastData = checkedSchedule;
						world.send(0, WORK_DONE, ObjectBuf.buffer(lastData));
						return;
					}
					++checkedSchedule;
					final Schedule curSchedule = scheduleGenerator.next();
					Ranker.rank(curSchedule);
					if(popularSchedule == null){
						popularSchedule = curSchedule;
						continue;
					}
					if(curSchedule.getRank() < popularSchedule.getRank()){
						popularSchedule = curSchedule;
					}
				}	
				final Long tag = FINISH_PIECE;
				world.send(0, WORK_DONE, ObjectBuf.buffer(tag));
			}
		}
		catch(final IOException e)
		{

		}
	}
	/**
	 * Print usage of the program and exit with -1.
	 */
	public static void usage() {
		System.err.println("Usage: java -Dpj.np=<K> FESPermutationClu <sections-file> <students-file> <relationship-file> [schedule-output-file]");
		System.err.println("  <K> = number of cluster node to be used");
		System.err.println("  <sections-file> = required input file containing all sections information");
		System.err.println("  <students-file> = required input file containing all students information");
		System.err.println("  <relationship-file> = required input file containing students sections relationships information");
		System.err.println("  [schedule-output-file] = optional file path to store the resulting exam schedule");
		System.err.println();
		System.exit(-1);
	}
}
