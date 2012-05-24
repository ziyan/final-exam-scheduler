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

import common.Random;
import common.Resources;
import common.Section;
import common.Student;

/**
 * Final Exam Scheduler Data Generator
 * Usage: java FESGenerateData <sections-file> <students-file> <relationship-file> <#-of-students> <student-load>
 * 	<sections-file> = path to the file containing all courses
 *	<students-file> = path to store the students file
 *  <relationship-file> = path to store the relationships file
 *  <#-of-students> = number of students to be generated
 *  <student-load> = on average number of courses each student will be taking
 * @author Ziyan Zhou (zxz6862)
 *
 */
public class FESGenerateData {

	/**
	 * Final Exam Scheduler Data Generator program main entry
	 * @param args program arguments
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		// check for command line argument
		if (args.length != 5)
			usage();
		// load data
		Resources.loadFromFile(args[0], args[1], args[2]);
		// number of studnets to be generated
		final int numberOfStudents = Integer.parseInt(args[3]);
		// generate student
		while(Resources.getStudents().size() < numberOfStudents) {
			// generate random student id
			final int n1 = (int) (Random.generateDouble() * (100000 - 10000) + 10000);
			final int n2 = (int) (Random.generateDouble() * (10000 - 1000) + 1000);
			final Student student = new Student(""+n1+"-"+n2, "John Doe " + Resources.getStudents().size());
			Resources.getStudents().put(student.getId(), student);
		}

		// cache section array
		final Section[] sections = Resources.getSections().values().toArray(new Section[]{});
		// average stuent load
		final int load = Integer.parseInt(args[4]);
		// assign students to sections
		for(final Student student : Resources.getStudents().values()) {
			// introduce fluctuations
			final int courseload = (int)(Random.generateDouble() * 3 - 1 + load);
			while (student.getSections().size() < courseload)
				student.getSections().add(sections[(int) (Random.generateDouble()*sections.length)]);
		}

		// write out studnets file
		{
			File students = new File(args[1]);
			students.createNewFile();
			final PrintStream out = new PrintStream(students);
			for(final Student student : Resources.getStudents().values())
				out.println(student.getId() + "\t" + student.getName());
			out.flush();
			out.close();
		}

		// write out relationship file
		{
			File relationships = new File(args[2]);
			relationships.createNewFile();
			final PrintStream out = new PrintStream(relationships);
			for(final Student student : Resources.getStudents().values())
				for(final Section section : student.getSections())
					out.println(section.getId() + "\t"+student.getId());
			out.flush();
			out.close();
		}
	}

	/**
	 * Print usage of the program and exit with -1.
	 */
	public static void usage() {
		System.err.println("Usage: java FESGenerateData <sections-file> <students-file> <relationship-file> <#-of-students> <student-load>");
		System.err.println("  <sections-file> = path to the file containing all sections");
		System.err.println("  <students-file> = path to store the students file");
		System.err.println("  <relationship-file> = path to store the relationships file");
		System.err.println("  <#-of-students> = number of students to be generated");
		System.err.println("  <student-load> = on average number of courses each student will be taking");
		System.err.println();
		System.exit(-1);
	}
}
