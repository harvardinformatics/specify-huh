/**
 * 
 */
package edu.harvard.huh.specify.guids;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Creates a set of insert statements placing UUIDs into a table of available guids.
 * Workaround for MySQL UUID() function being non-deterministic and not being
 * able to be used in a query based replication scheme.
 * 
 * @author mole
 *
 */
public class GuidGenerator {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int max = 2000000;
		FileWriter file;
		try {
			file = new FileWriter("guidlist.sql");
			BufferedWriter output = new BufferedWriter(file);
				
		for (int i=0;i<max;i++) { 
			output.write("insert into availableguids (uuid) values ('" + UUID.randomUUID().toString() + "');");
			output.write("\n");
		}
		output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
