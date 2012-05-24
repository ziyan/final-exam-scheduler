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

package genetic;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import common.Random;
import common.Ranker;
import common.Resources;
import common.Schedule;

/**
 * This class performs the actual work for the genetic algorithm.
 *
 * @author kevin cheek
 */
public class Population {

	/**
	 * The crossover rate is the percentage of the population that will breed each generation.
	 * Can be specified through JVM parameter -Dfes.ga.cr
	 */
	private static final double		   CROSSOVERRATE   = Double.parseDouble(System.getProperty("fes.ga.cr", "0.75"));

	/**
	 * The mutation rate is the percentage of the children from the crossover that will have a mutation introduced in their genes.
	 * Can be specified through JVM parameter -Dfes.ga.mr
	 */
	private static final double		   MUTATIONRATE	= Double.parseDouble(System.getProperty("fes.ga.mr", "0.25"));

	/**
	 * The topbreed rate is the upper percent of the the individuals that are automatically entered into the breeding wheel
	 * Can be specified through JVM parameter -Dfes.ga.tbr
	 */
	private static final double		   TOPBREEDINGRATE = Double.parseDouble(System.getProperty("fes.ga.tbr", "0.2"));

	/**
	 * The freshbloodrate is the percentage of individuals that are removed from the population then reintroduced. This is a
	 * substitute for actual migration.
	 * Can be specified through JVM parameter -Dfes.ga.fb
	 */
	private static final double		   FRESHBLOODRATE  = Double.parseDouble(System.getProperty("fes.ga.fb", "0.1"));

	private static String[]		 cachedSortedSectionIds; //Cached Section id's
	private static Schedule		 best;			  //Reference to
	private static int			  populationSize; //Population size

	private final TreeSet<Schedule> pool; //The population pool

	/**
	 * Create a random population with the given size
	 *
	 * @param size	The size of the population
	 */
	public Population(final int size) {
		populationSize = size;
		if( cachedSortedSectionIds == null )
			cachedSortedSectionIds = Resources.getCachedSortedSectionIds();
		pool = new TreeSet<Schedule>();

		for( int i = 0; i < size; i++ )
			pool.add(Random.generateSchedule());
	}

	/**
	 * Returns the current best schedule in the population
	 *
	 * @return	The best schedule in the population.
	 */
	public Schedule getBest() {
		return best;
	}


	/**
	 * Run the genetic algorithm on the population for a single generation.
	 */
	public void nextGeneration() {

		best = (Schedule) pool.first();


		final Schedule[] wheel = new Schedule[(int) (pool.size() * CROSSOVERRATE)];
		final double topRank = pool.first().getRank();
		final Iterator<Schedule> iter = pool.iterator();
		for( int i = 0; iter.hasNext() && i < wheel.length; i++ ){
			if( (i < pool.size() * TOPBREEDINGRATE) ){
				wheel[ i ] = (Schedule) iter.next();
			}else{
				final Schedule sch = (Schedule) iter.next();
				final double probabilty = topRank / sch.getRank();
				if( Random.generateDouble() < probabilty )
					wheel[ i ] = sch;
			}
		}

		for( int i = 0; i < wheel.length / 2; i++ ){
			final Schedule p1 = wheel[ Random.generateInt(wheel.length - 1) ];
			final Schedule p2 = wheel[ Random.generateInt(wheel.length - 1) ];
			if( p1 != null && p2 != null ){
				final Schedule[] children = crossover(p1, p2);
				for( int t = 0; t < children.length; t++ ){
					if( pool.last().getRank() > children[ t ].getRank() ){
						pool.remove(pool.last());
						pool.add(children[ t ]);
					}
				}
			}
		}

		for( int i = 0; i < (int) (pool.size() * FRESHBLOODRATE); i++ ){
			pool.remove(pool.last());
		}
		while( pool.size() < populationSize ){
			pool.add(Random.generateSchedule());
		}

	}


	/**
	 * The crossover function takes two parent schedules and performs a cross over and mutation on them
	 * this means that each parent is duplicated then those two new schedules become the children.
	 * Genes from the children are randomly swapped then based on the mutation rate the children have a gene mutated.
	 *
	 * @param p1 	First parent to crossover and mutate
	 * @param p2	Second parent to crossover and mutate
	 * @return 		The children created by the parents
	 */
	private Schedule[] crossover(final Schedule p1, final Schedule p2) {
		String section;
		final Schedule[] children = new Schedule[2];
		Map<String, Integer> ISChild1;// 1st childs internal schedule
		Map<String, Integer> ISChild2; //2nd chilss Internal Schedule
		final Schedule child1 = (Schedule) p1.clone();
		final Schedule child2 = (Schedule) p2.clone();

		ISChild1 = child1.getSchedule();
		ISChild2 = child2.getSchedule();

		int newSlotLoc = 0;
		final int k = Random.generateInt(cachedSortedSectionIds.length - 1) + 1;
		int j = Random.generateInt(k);
		for( ; j < k; j++ ){
			section = cachedSortedSectionIds[ j ];
			newSlotLoc = ISChild1.get(section);
			child1.move(section, ISChild2.get(section));
			child2.move(section, newSlotLoc);
		}

		if( Random.generateDouble() < MUTATIONRATE ){
			mutate(child1);
		}
		if( Random.generateDouble() < MUTATIONRATE ){
			mutate(child2);
		}

		Ranker.rank(child1);
		Ranker.rank(child2);
		children[ 0 ] = child1;
		children[ 1 ] = child2;
		return children;
	}

	/**
	 * Introduce a single mutation within the schedule
	 *
	 * @param a The schedule to perform the mutation on.
	 */
	private void mutate(final Schedule a) {
		final String section = cachedSortedSectionIds[ Random.generateInt(cachedSortedSectionIds.length - 1) ];
		a.move(section,
				(Integer) Random.generateInt(cachedSortedSectionIds.length));
	}

}
