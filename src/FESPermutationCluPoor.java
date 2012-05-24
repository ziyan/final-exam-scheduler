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


import java.io.IOException;

import permutation.Generator;

import common.Ranker;
import common.Resources;
import common.Schedule;

import edu.rit.mp.ObjectBuf;
import edu.rit.pj.Comm;
import edu.rit.pj.CommRequest;
import edu.rit.pj.CommStatus;
import edu.rit.pj.ParallelRegion;
import edu.rit.pj.ParallelSection;
import edu.rit.pj.ParallelTeam;
import edu.rit.util.Range;

/**
 * Final Exam Scheduler using Brute-force (Cluster version)
 * Usage: java -Dpj.np=<K> FESPermutationCluPoor <sections-file> <students-file> <relationship-file> [schedule-output-file]
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
public class FESPermutationCluPoor {

	// Mark which processor is Master.
	static final int MASTER = 0;

	/**
	 * Trunk size for the scheduler, default 500
	 * Can be specified through JVM parameter -Dfes.p.trunk
	 */
	static int 		 MULTIPLE = Integer.parseInt(System.getProperty("fes.p.trunk", "500"));

	// Mark the message that worker has finished its own work.
	static final int WORKER_FINISH = 1;

	// Mark the message that all worker can exit the program.
	static final int PROGRAM_OVER  = 2;

	/**
	 * Time limit for the program, default 500
	 * Can be specified through JVM parameter -Dfes.p.time
	 */
	static long TIME_LIMIT = Integer.parseInt(System.getProperty("fes.p.time", "500"));

	// Define the state that Master and Worker is in. 
	static enum STATE {INITIATION, RUNNING, ENDING}

	// Very processor in the cluster need to remember command Comm. 
	static Comm		m_world;

	// Mark the current State.
	STATE 		 	m_state;

	// Mark the number of schedules which can be in the list.
	int 	 		m_scheduleListNumber=0;

	// Mark how many number of schedules still in the list
	int				m_curScheduleListNumber = 0;

	// Save all the schedules which will be voted.
	Schedule[] 		m_scheduleList;

	// Save the schedules the master created. 
	Schedule[]		m_masterCreatedSchedule;

	// Schedule generator, which will only be created by master.
	Generator 		m_generator = null;

	// Save the optimal schedule.
	Schedule		m_optimalSchedule = null;

	// Constructor, initialize the state.
	public FESPermutationCluPoor(){
		m_state		= STATE.INITIATION;
	}

	/**
	 * Final Exam Scheduler program main entry
	 * @param args program arguments
	 * @throws Exception
	 */
	public static void main(final String[] args){

		try{
			// Initialize the cluster.
			Comm.init (args);

			// check arguments
			if (args.length != 3 && args.length != 4)
				usage();

			// Load all the class information, student information, relation information. 
			Resources.loadFromFile(args[0], args[1], args[2]);

		}catch(final IOException e){
			System.out.println("IOException Error");
			return;
		}

		// Get the common variable.
		m_world = Comm.world();

		// Create the permutation object.
		final FESPermutationCluPoor work = new FESPermutationCluPoor(); 

		// Entry to the main work of master and worker. 
		work.work(args[0]);
	}

	//second step:
	/*
	 * Master Initiates some classes information. and then divides them into 
	 * K pieces to back processors.
	 * and then go on initiate courses.
	 * once get the finish information from one of the workers, then send
	 * another pieces of work to it.
	 * At the beginning, worker wait for the initiate classes to vote, 
	 * after getting the initiate data, then start to vote, once finish this, 
	 * worker send message to Master to get more data to vote.
	 */
	public boolean work(final String sectionFile)
	{
		if(m_world.rank() == MASTER){
			// Go into the master part
			MasterFunction(sectionFile);
		}
		else{
			// Go into the worker part
			WorkerFunction();
		}
		return true;
	}

	private boolean MasterFunction(final String sectionFile)
	{
		// Initialize all section Information.
		// and because in Processor 0, there is still a worker thread will run.
		// it also need to initialize the work part.
		if(this.m_state == STATE.INITIATION){
			if(MasterInitiation(sectionFile) && WorkerInitiation()){
				// After the initialization, go into the next step. 
				this.m_state = STATE.RUNNING;
			}
			else
				return false;
		}
		// Running Phase.
		if(this.m_state == STATE.RUNNING)
		{
			// Here I will create two threads, one is running master.
			// the other is running worker.
			try
			{
				new ParallelTeam(2).execute(new ParallelRegion(){
					public void run() throws Exception{
						execute (new ParallelSection(){
							public void run() throws Exception{
								MasterRunning();
							}
						},
						new ParallelSection() {
							public void run() throws Exception{
								WorkerRunning();
							}
						});
					}
				});
			}catch(final Exception e){
				System.out.println("Thread Creating Error");
				return false;
			}
		}
		return true;
	}
	private boolean WorkerFunction()
	{
		// Initialize all students Information.
		// and because in Processor 0, there is still a worker thread will run.
		// it also need to initialize the work part.
		if(this.m_state == STATE.INITIATION){
			if(WorkerInitiation()){
				this.m_state = STATE.RUNNING;
			}
			else
				return false;		
		}

		if(this.m_state == STATE.RUNNING){
			WorkerRunning();
		}
		return true;
	}

	private boolean MasterRunning()
	{
		// Remember the beginning time of loop.
		final long beginTime = System.currentTimeMillis();

		// Remember how many schedules have been created.
		long totalSchedule = 0;

		// Get the size of cluster.
		final int size = m_world.size();

		for(int i = 0; i < size; ++i){
			// Give the range of schedules master will sent to the worker
			final Range sliceRange = new Range((i*MULTIPLE),(i*MULTIPLE+MULTIPLE-1));

			// Get the piece of schedules.
			final ObjectBuf< Schedule > buf = ObjectBuf.sliceBuffer(m_masterCreatedSchedule, sliceRange);
			try{
				// send schedules to worker.
				m_world.send( i, buf);

				// Increment the number of schedules which have been send to workers.
				totalSchedule += MULTIPLE;
			}
			catch(final IOException e){
				System.out.println("Error In send initial data");
				return false;
			}
		}

		// After sending to the worker, master need to create new schedules to fill the schedule Array.
		int count = RefillScheduleList(0, m_masterCreatedSchedule.length-1);
		if(count < size){
			System.out.println("All Schedule have been checked");
			return true;
		}
		while(true){
			// Read the current Time, if the running time has exceed the 
			// time limit, then it will inform all worker to exit the program
			// and exit the loop itself.
			final long curTime = System.currentTimeMillis();
			if((curTime - beginTime) > TIME_LIMIT ){
				for(int i = 0; i <m_world.size(); ++i){
					try{
						// send the ending message
						m_world.send(i,PROGRAM_OVER,ObjectBuf.emptyBuffer());
					}catch(final IOException e){
						System.out.println("Send Ending Message Error");
					}
				}
				break;
			}
			try{
				// Get the message from workers, that they have finished their current work.
				final CommStatus status = m_world.receive(null,WORKER_FINISH,ObjectBuf.emptyBuffer());

				// which processor get the message from.
				final int worker = status.fromRank;

				final int beginIndex = worker*MULTIPLE;
				final int endIndex   = worker*MULTIPLE+MULTIPLE;
				// Get the new range of schedules which would be sent to the worker.
				final Range sliceRange = new Range(beginIndex,endIndex);

				// Get the schedule piece
				final ObjectBuf< Schedule > buf = ObjectBuf.sliceBuffer(m_masterCreatedSchedule, sliceRange);

				// send message
				m_world.send(worker,buf);

				// Increment the number of schedules which have been send to workers.
				totalSchedule += MULTIPLE;

				// Increment the number of schedules which have been send to workers.
				count = RefillScheduleList(beginIndex,endIndex);

				if(count < (endIndex-beginIndex+1)){
					System.out.println("All Schedule have been checked");
					return true;
				}
			}
			catch(final IOException e){
				System.out.println("Error In send initial data");
				return false;
			}
		}
		return true;
	}

	// Create new schedules
	private int RefillScheduleList(final int lowIndex, final int HighIndex){		
		int count = 0;
		for(int i = lowIndex; i <= HighIndex && m_generator.hasNext(); ++i ){
			m_masterCreatedSchedule[i] = m_generator.next();
			++count;
		}
		return count;
	}

	// Master Initialization function.
	private boolean MasterInitiation(final String sectionFile){
		if(m_world.size() == 0)
			return false;
		final int number = m_world.size() * MULTIPLE;
		// Initialize the schedule list, fill it with the new created schedules.
		m_masterCreatedSchedule = new Schedule[number];
		m_generator = Generator.create();
		for(int i = number-1; i >= 0;--i){
			m_masterCreatedSchedule[i] = m_generator.next();
		}
		return true;
	}

	// Worker Initialization function.
	private boolean WorkerInitiation()
	{
		m_scheduleListNumber = MULTIPLE;
		m_scheduleList = new Schedule[m_scheduleListNumber];
		m_curScheduleListNumber = 0;
		return true;
	}

	// Work running function.
	private boolean WorkerRunning()
	{

		try{
			// Because this receive function is a non-block method to detect
			// the ending send from master, I need to create this CommRequest Object.
			final CommRequest request = new CommRequest();
			m_world.receive(0, PROGRAM_OVER, ObjectBuf.emptyBuffer(), request);
			while(true){
				// Every time I need to detect whether has received the ending message from master.
				if(request.isFinished()){
					break;
				}

				// Get more schedules from master.
				if(GetMoreSchedules()) {
					// Find the optimal schedule from the current schedule list.
					FindOptimalSchedule();
					// Once it finish the optimal search, it send the message back to the master.
					SendFinishMessage();
				}
				else{
					break;
				}
			}
			return true;
		}catch(final IOException e){
			return false;
		}
	}

	private boolean SendFinishMessage()
	{
		try{
			m_world.send(MASTER,WORKER_FINISH, ObjectBuf.emptyBuffer());
		}catch(final IOException e){
			return false;
		}
		return true;
	}

	private boolean GetMoreSchedules()
	{
		if(m_curScheduleListNumber == 0)
		{
			final Range range = new Range(0,m_scheduleListNumber-1);
			final ObjectBuf< Schedule> buffer = ObjectBuf.sliceBuffer(m_scheduleList,range);
			try{
				m_world.receive(MASTER, buffer);
				m_curScheduleListNumber = m_scheduleList.length;
				System.out.println("Worker "+m_world.rank()+" has gotten some work");
			}catch(final IOException e){
				return false;
			}
		}
		return true;
	}
	private void FindOptimalSchedule()
	{
		final int size = m_curScheduleListNumber;
		for(int i = 0; i < size; ++i){
			final Schedule curSchedule = m_scheduleList[i];
			Ranker.rank(curSchedule);
			if(m_optimalSchedule == null)
				m_optimalSchedule = curSchedule;
			else{
				if(curSchedule.getRank() < m_optimalSchedule.getRank()){
					m_optimalSchedule = curSchedule;
				}
			}
			--m_curScheduleListNumber;
		}
	}

	/**
	 * Print usage of the program and exit with -1.
	 */
	public static void usage() {
		System.err.println("Usage: java -Dpj.np=<K> FESPermutationCluPoor <sections-file> <students-file> <relationship-file> [schedule-output-file]");
		System.err.println("  <K> = number of cluster node to be used");
		System.err.println("  <sections-file> = required input file containing all sections information");
		System.err.println("  <students-file> = required input file containing all students information");
		System.err.println("  <relationship-file> = required input file containing students sections relationships information");
		System.err.println("  [schedule-output-file] = optional file path to store the resulting exam schedule");
		System.err.println();
		System.exit(-1);
	}
}
