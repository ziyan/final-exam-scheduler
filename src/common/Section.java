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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Section class
 * Example: 0101-301-01	FINANCIAL ACCOUNTING	DEY,R	*Open	40	39	MW	1000AM	1150AM	12	1135
 * @author Ziyan Zhou (zxz6862)
 * 
 */
public class Section {
	/**
	 * Course Section Status
	 * could be one of Open, Closed, Cancelled
	 * @author Ziyan Zhou (zxz6862)
	 *
	 */
	public static enum Status { Open, Closed, Cancelled }

	/**
	 * Convert integer to section status
	 * @param status
	 * @return
	 */
	public static Status intToStatus(final int status){
		switch(status) {
		case -1:
			return Status.Cancelled;
		case 0:
			return Status.Open;
		case 1:
			return Status.Closed;
		}
		return Status.Cancelled;
	}

	private final String id;
	private final String title;
	private final String professor;
	private final Status status;
	private final int current;
	private final int max;
	private final HashSet<Student> students;

	/**
	 * Course Section constructor
	 * Example: 0101-301-01	FINANCIAL ACCOUNTING	DEY,R	*Open	40	39	MW	1000AM	1150AM	12	1135
	 *
	 * @param id
	 * @param title
	 * @param professor
	 * @param status
	 * @param current
	 * @param max
	 * @param schedule
	 */
	public Section(final String id,
			final String title,
			final String professor,
			final int status,
			final int current,
			final int max,
			final String times) {
		this.id = id;
		this.title = title;
		this.professor = professor;
		this.status = intToStatus( status );
		this.current = current;
		this.max = max;
		//this.schedule = Schedule.createFromBitmap( times );
		this.students = new LinkedHashSet<Student>();
	}

	/**
	 * Get section id
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * Get section title
	 * @return
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Get section professor
	 * @return
	 */
	public String getProfessor() {
		return professor;
	}

	/**
	 * Get section status
	 * @return
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Get section current enrollment
	 * @return
	 */
	public int getCurrent() {
		return current;
	}

	/**
	 * Get section maximun enrollment
	 * @return
	 */
	public int getMax() {
		return max;
	}


	/**
	 * Get enrolled students in a set
	 * @return
	 */
	public Set<Student> getStudents() {
		return students;
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof Section && ((Section)o).id.equals(id);
	}

}
