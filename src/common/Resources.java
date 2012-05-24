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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Globally accessible resource set and utilities
 * @author Ziyan Zhou
 * @author Kevin Cheek
 */
public class Resources {
	private static HashMap<String, Section> sections = new HashMap<String, Section>();
	private static HashMap<String, Student> students = new HashMap<String, Student>();
	private static String[] cachedSortedSectionIds;

	/**
	 * Retrieve a cached map from SectionID to Section objects
	 * @return
	 */
	public static Map<String, Section> getSections() {
		return sections;
	}

	/**
	 * Retrieve a cached map from StudentID to Student objects
	 * @return
	 */
	public static Map<String, Student> getStudents() {
		return students;
	}

	/**
	 * Retrieve an sorted array of section ids
	 * @return an sorted array of section ids
	 */
	public static String[] getCachedSortedSectionIds() {
		if(cachedSortedSectionIds == null) {
			cachedSortedSectionIds = sections.keySet().toArray(new String[0]);
			Arrays.sort(cachedSortedSectionIds);
		}
		return cachedSortedSectionIds;
	}

	/**
	 * Load all resources from MySQL database
	 * @param url URL to the MySQL database, e.g. localhost/kyz
	 * @param userId
	 * @param password
	 * @throws SQLException
	 */
	public static void loadFromMySQL(String url, final String userId, final String password) throws SQLException {
		Connection conn = null;
		url = "jdbc:mysql://" + url;
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(url, userId, password);
			loadSectionsFromMySQL(conn);
			loadStudentsFromMySQL(conn);
			loadRelationshipFromMySQL(conn);
		} catch (final InstantiationException e) {
			e.printStackTrace();
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (final Exception e) { }
			}
		}
	}

	/**
	 * Load a list of students from MySQL database
	 * @param conn connection to the MySQL database
	 */
	private static void loadStudentsFromMySQL(final Connection conn) throws SQLException{
		final Statement s = conn.createStatement();
		s.executeQuery("SELECT id, name FROM obj_student");
		final ResultSet rs = s.getResultSet();
		int count = 0;
		while (rs.next()) {
			final String studentId = rs.getString("id");
			final String name = rs.getString("name");
			final Student stu = new Student(studentId, name);
			students.put(stu.getId(), stu);
			count++;
		}
		rs.close();
		s.close();
	}

	/**
	 * Load a list of sections from MySQL database
	 * @param conn connection to the MySQL database
	 */
	private static void loadSectionsFromMySQL(final Connection conn) throws SQLException {

		final Statement s = conn.createStatement();
		s.executeQuery("SELECT * FROM obj_section");
		final ResultSet rs = s.getResultSet();
		int count = 0;
		while (rs.next()) {
			final String sectionId = rs.getString("id");
			final String title = rs.getString("title");
			final String professor = rs.getString("professor");
			final int status = rs.getInt("status");
			final int current = rs.getInt("current");
			final int max = rs.getInt("max");
			final String time = rs.getString("time");
			final Section sec = new Section(sectionId, title, professor, status,
					current, max, time);
			sections.put(sec.getId(), sec);
			count++;
		}
		rs.close();
		s.close();
	}

	/**
	 * Load relationship between students and sections from MySQL database
	 * @param conn connection to the MySQL database
	 */
	private static void loadRelationshipFromMySQL(final Connection conn) throws SQLException {
		final Statement s = conn.createStatement();
		s.executeQuery("SELECT * FROM rel_section_student");
		final ResultSet rs = s.getResultSet();
		int count = 0;
		while (rs.next()) {
			final String sectionId = rs.getString("section_id");
			final String studentId = rs.getString("student_id");
			final Section section = sections.get(sectionId);
			final Student student = students.get(studentId);
			if(section!=null && student!=null) {
				section.getStudents().add(student);
				student.getSections().add(section);
			} else
				System.err.println("relationship invalid!");
			count++;
		}
		rs.close();
		s.close();
	}

	/**
	 * Load all resources from files
	 * @param section path to file containing list of sections
	 * @param student path to file containing list of students
	 * @param relationship path to file containing list of relationships
	 * @throws IOException
	 */
	public static void loadFromFile(final String section, final String student, final String relationship) throws IOException {
		loadSectionFromFile(section);
		loadStudentFromFile(student);
		loadRelationshipFromFile(relationship);
	}

	/**
	 * Load a list of sections from file
	 * @param section path to list file
	 * @throws IOException
	 */
	private static void loadSectionFromFile(final String section) throws IOException {
		final Scanner in = new Scanner(new File(section));
		int count = 0;
		while(in.hasNext()) {
			final String line = in.nextLine();
			final String[] parts = line.split("\t");
			if (parts.length < 6) continue;

			final String sectionId = parts[0];
			final String title = parts[1];
			final String professor = parts[2];
			int status = -1;
			if (parts[3].indexOf("Open") > -1)
				status = 0;
			else if (parts[3].indexOf("Cance") > -1)
				status = -1;
			else if (parts[3].indexOf("Close") > -1)
				status = 1;
			int i = 4, max = -1;
			try {
				max = Integer.parseInt(parts[i]);
				i++;
			} catch (final NumberFormatException e) { }
			int current = max;
			try {
				current = Integer.parseInt(parts[i]);
				i++;
			} catch (final NumberFormatException e) { }
			final String time = "";
			final Section sec = new Section(sectionId, title, professor, status,
					current, max, time);
			sections.put(sec.getId(), sec);
			count++;
		}
		in.close();
	}

	/**
	 * Load a list of students from file
	 * @param student path to list file
	 * @throws IOException
	 */
	private static void loadStudentFromFile(final String student) throws IOException {
		final Scanner in = new Scanner(new File(student));
		int count = 0;
		while(in.hasNext()) {
			final String line = in.nextLine();
			final String[] parts = line.split("\t");
			if (parts.length != 2) continue;
			final String studentId = parts[0];
			final String name = parts[1];
			final Student stu = new Student(studentId, name);
			students.put(stu.getId(), stu);
			count++;
		}
		in.close();
	}

	/**
	 * Load a list of relationships from file
	 * @param relationship path to file
	 * @throws IOException
	 */
	private static void loadRelationshipFromFile(final String relationship) throws IOException {
		final Scanner in = new Scanner(new File(relationship));
		int count = 0;
		while(in.hasNext()) {
			final String line = in.nextLine();
			final String[] parts = line.split("\t");
			if (parts.length != 2) continue;
			final String sectionId = parts[0];
			final String studentId = parts[1];
			final Section section = sections.get(sectionId);
			final Student student = students.get(studentId);

			if(section!=null && student!=null) {
				section.getStudents().add(student);
				student.getSections().add(section);
			} else
				System.err.println("relationship invalid!");

			count++;
		}
		in.close();
	}
}
