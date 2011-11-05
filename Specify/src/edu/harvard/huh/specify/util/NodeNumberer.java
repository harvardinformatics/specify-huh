package edu.harvard.huh.specify.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

/*
 * Perform a depth-first traversal of a tree, assigning NodeNumber and HighestChildNodeNumber values
 * along the way.
 */
public class NodeNumberer {
	private static final Logger log = Logger.getLogger(NodeNumberer.class);
	
	private Statement statement;
	
	public NodeNumberer(Statement statement) {
		this.statement = statement;
	}

	public void numberNodes(String tableName, String keyFieldName) throws SQLException {

		disableKeys(tableName);

		TreeVisitor treeVisitor = new TreeVisitor(tableName, keyFieldName);
        
        int rootId = selectRootId(tableName, keyFieldName);

        resetNodeNumbers(tableName, keyFieldName);
      
        int nodes = treeVisitor.visit(rootId, 1);

        log.info("Updated " + nodes + " nodes.");

        enableKeys(tableName);
    }

	public void close() throws SQLException {
		if (statement != null) statement.close();
	}

	private class TreeVisitor {

		private String tableName;
		private String keyFieldName;
		
		TreeVisitor(String tableName, String keyFieldName) {
			this.tableName = tableName;
			this.keyFieldName = keyFieldName;
		}

		private int visit(int id, int nodeNumber) throws SQLException {

			int highestChildNodeNumber = nodeNumber;

			List<Integer> childIds = getChildIds(id);
			if (childIds.size() == 0) {
				highestChildNodeNumber = nodeNumber;

			}
			else {
				for (int childId : childIds) {
					highestChildNodeNumber = visit(childId, highestChildNodeNumber + 1);
				}
			}
			updateNode(id, nodeNumber, highestChildNodeNumber);
			return highestChildNodeNumber;
		}

		private void updateNode(int id, int nodeNumber, int highestChildNodeNumber) throws SQLException {
			String sql = "update " + tableName + " set NodeNumber=" + nodeNumber + ", HighestChildNodeNumber=" + highestChildNodeNumber +
					" where " + keyFieldName + "=" + id;
			executeUpdate(sql);
		}

		private List<Integer> getChildIds(int id) throws SQLException {
			String sql = "select " + keyFieldName + " from " + tableName + " where ParentID=" + id + " order by Name";
			ResultSet resultSet = executeQuery(sql);
			
			List<Integer> childIds = new ArrayList<Integer>();
			while (resultSet.next()) {
				childIds.add(resultSet.getInt(1));
			}
			return childIds;
		}
	}
	
    private Statement getStatement() {
        return statement;
    }
    
    private void disableKeys(String tableName) throws SQLException {

        log.info("Disabling keys");
        
        String sql = "alter table " + tableName + " disable keys";
        execute(sql);
    }
    
    private void enableKeys(String tableName) throws SQLException {        

    	log.info("Enabling keys");

    	String sql = "alter table " + tableName + " enable keys";
        execute(sql);
    }

	private int selectRootId(String tableName, String keyFieldName) throws SQLException {
        String selectRootId = "select " + keyFieldName + " from " + tableName + " where ParentID is null";
        ResultSet resultSet = executeQuery(selectRootId);
        if (!resultSet.next()) throw new RuntimeException("No root for " + tableName);
        int rootId = resultSet.getInt(1);
        if (resultSet.next()) throw new RuntimeException("Multiple roots for " + tableName);
        return rootId;
	}

	private void resetNodeNumbers(String tableName, String keyFieldName) throws SQLException {
		String updateNodeNumber = "update " + tableName + " set NodeNumber = null";
		executeUpdate(updateNodeNumber);
		
		String updateHighestChildNodeNumber = "update " + tableName + " set HighestChildNodeNumber = null";
		executeUpdate(updateHighestChildNodeNumber);
	}
	
    private void execute(String sql) throws SQLException {
    	log.trace(sql);
    	getStatement().execute(sql);
    }
    
    private int executeUpdate(String sql) throws SQLException {
    	log.trace(sql);
    	return getStatement().executeUpdate(sql);
    }

    private ResultSet executeQuery(String sql) throws SQLException {
    	log.trace(sql);
    	return getStatement().executeQuery(sql);
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();
        
        // e.g.: edu.harvard.huh.specify.util.NodeNumberer nodenumberer.properties storage geography taxonomy
        String usage = "NodeNumberer path/to/properties/file treetablename1 [treetablename2] ...\n" +
                       "  properties: host, database, user, password, driver (com.mysql.jdbc.Driver)\n" +
                       "  Requires log4j jar to be on classpath.\n" +
                       "  Database user must have privileges to disable/enable keys, and update.\n" +
                       "  Make sure you've backed up your tables first; these transactions are committed per node.\n";

        if (args.length < 2) {
        	log.fatal(usage);
        	System.exit(-1);
        }
        
        String propertiesFileName = args[0];

        Connection connection = null;

        try {
        	Properties properties = new Properties();
    		properties.load(new FileInputStream(propertiesFileName));
    		
    		String host = properties.getProperty("host");
    		String database = properties.getProperty("database");
    		String user = properties.getProperty("user");
    		String password = properties.getProperty("password");
    		String driver = properties.getProperty("driver");

    		log.debug("Driver: " + driver);
    		Class.forName(driver);
    		
    		log.debug("Connection: " + "jdbc:mysql://" + host + "/" + database + " user: " + user);
    		connection = DriverManager.getConnection("jdbc:mysql://" + host + "/" + database, user, password);

    		NodeNumberer n = new NodeNumberer(connection.createStatement());
    		
    		for (int i = 1; i < args.length; i++) {
    			String tableName = args[i];
    			tableName = tableName.toLowerCase();
    			
    			String keyFieldName = tableName.substring(0, 1).toUpperCase() + tableName.substring(1) + "ID";
    			
    			log.info("Numbering nodes in " + tableName);
        		
    			long startTime = System.currentTimeMillis();
        		
    			n.numberNodes(tableName, keyFieldName);

    			long endTime = System.currentTimeMillis();
    	        float elapsedTime = ((float) (endTime - startTime))/1000;

    	        log.info("Elapsed time:  " + elapsedTime + " seconds.");
    		}
    	}
    	catch (FileNotFoundException e) {
			log.fatal(e);
		}
    	catch (IOException e) {
    		log.fatal(e);
		}
    	catch (SQLException e) {
    		log.fatal(e);
    	}
    	catch (ClassNotFoundException e) {
			log.fatal(e);
		}
        finally {
    		try {
				if (connection != null) connection.close();
			}
    		catch (SQLException e) {
				log.error("Couldn't close connection ?!");
			}
        }
        log.info("Done.");
    }
}