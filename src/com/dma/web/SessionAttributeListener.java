package com.dma.web;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Application Lifecycle Listener implementation class SessionAttributeListener
 *
 */
@WebListener
public class SessionAttributeListener implements HttpSessionAttributeListener {

    /**
     * Default constructor. 
     */
    public SessionAttributeListener() {
        // TODO Auto-generated constructor stub
    }

	/**
     * @see HttpSessionAttributeListener#attributeRemoved(HttpSessionBindingEvent)
     */
    public void attributeRemoved(HttpSessionBindingEvent arg0)  { 
         // TODO Auto-generated method stub
    }

	/**
     * @see HttpSessionAttributeListener#attributeAdded(HttpSessionBindingEvent)
     */
    public void attributeAdded(HttpSessionBindingEvent arg0)  { 
         // TODO Auto-generated method stub
    	HttpSession s = arg0.getSession();
    	InitialContext ic = (InitialContext) s.getServletContext().getAttribute("ic");
    	
    	if(arg0.getName().equalsIgnoreCase("projectPath")){
    		System.out.println("SessionId " + s.getId() + " projectPath is set to " + s.getAttribute("projectPath"));
    		Path path = Paths.get(((String) s.getAttribute("projectPath")) + "/relation.json" );  
    		if(Files.exists(path)) {
    			try {
					@SuppressWarnings("unchecked")
					Map<String, String> queries = (Map<String, String>) Tools.fromJSON(path.toFile(), new TypeReference<Map<String, String>>(){});
					if(queries != null) {
						s.setAttribute("FKQuery", queries.get("FKQuery"));
						s.setAttribute("PKQuery", queries.get("PKQuery"));
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}
    	
    	if(arg0.getName().equalsIgnoreCase("currentProject")){
    		
    		Project project = (Project) s.getAttribute("currentProject");

			String schema = (String) project.getDbSchema();
			s.setAttribute("schema", schema);
    		String dbEngine = project.getResource().getDbEngine();
    		s.setAttribute("dbEngine", dbEngine);
    		
    		String cognosCatalog = project.getResource().getCognosCatalog();
    		s.setAttribute("cognosCatalog", cognosCatalog);
    		String cognosDataSource = project.getResource().getCognosDataSource();
    		s.setAttribute("cognosDataSource", cognosDataSource);
    		String cognosSchema = project.getResource().getCognosSchema();
    		s.setAttribute("cognosSchema", cognosSchema);
    		
        	Map<String, QuerySubject> query_subjects = new HashMap<String, QuerySubject>();
        	s.setAttribute("query_subjects", query_subjects);
        	
        	String query = "";
        	try{
	    		switch(dbEngine.toUpperCase()){
	    		
	    			case "ORA":
	    				query = (String) ic.lookup("TestORAConnection");
	    				break;
	    				
	    			case "DB2":
	    				query = (String) ic.lookup("TestDB2Connection");
	    				break;
	
	    			case "DB2400":
	    				query = (String) ic.lookup("TestDB2400Connection");
	    				break;
	
	    			case "SQLSRV":
	    				query = (String) ic.lookup("TestSQLSRVConnection");
	    				break;
	
	    			case "MYSQL":
	    				query = (String) ic.lookup("TestMYSQLConnection");
	    				break;
	
	    			case "PGSQL":
	    				query = (String) ic.lookup("TestPGSQLConnection");
	    				break;

	    			case "IFX":
	    				query = (String) ic.lookup("TestIFXConnection");
	    				break;
	    				
	    			case "TD":
	    				query = (String) ic.lookup("TestTDConnection");
	    				break;	    				
	    				
	    		}
	    		s.setAttribute("query", query);
        	}
        	catch(NamingException e){
        		System.out.println("!!! WARNING: Test query for dbEngine " + dbEngine + " was not set.");
        	}
    		
    		String jndiName = project.getResource().getJndiName();
    		Connection con = null;
    		try{
				DataSource ds = (DataSource) ic.lookup(jndiName);
				
				if(dbEngine.equalsIgnoreCase("TD")){
					System.out.println("Loading TERADATA driver with DriverManager.");
					con = DriverManager.getConnection("jdbc:teradata://172.16.186.245", "dbc", "dbc");
	        		con.createStatement().execute("DATABASE " + schema);
				}
				else{
					con = ds.getConnection();
				}
				
        		
        		switch(dbEngine.toUpperCase()){
	    		
	    			case "ORA":
	    				con.createStatement().execute("ALTER SESSION SET CURRENT_SCHEMA=" + schema);
	    				con.createStatement().execute("ALTER SESSION SET NLS_SORT=BINARY_CI");
	    				con.createStatement().execute("ALTER SESSION SET NLS_COMP=LINGUISTIC");
	    				break;
	    				
	    			case "DB2":
	            		con.createStatement().execute("SET SCHEMA " + schema);
	    				break;
	
	    			case "DB2400":
	            		con.createStatement().execute("SET SCHEMA " + schema);
	    				break;
	
	    			case "SQLSRV":
	    				break;
	
	    			case "MYSQL":
	    				break;
	
	    			case "PGSQL":
	            		con.createStatement().execute("set search_path to " + schema);
	    				break;
	
	    			case "IFX":
	    				break;
	    				
        		}
        		
				s.setAttribute("con", con);
        		s.setAttribute("jndiName", jndiName);
    			System.out.println("SessionId " + s.getId() + " is now connected to " + jndiName + " using shema " + schema);
        		
    		}
    		catch(NamingException | SQLException e){
    			System.out.println("!!! ERROR: Connection to jndiName " + jndiName + " failed.");
    		}
			
			
			String aliasesQuery = "";
			
			switch(dbEngine.toUpperCase()){
			
				case "DB2":
					aliasesQuery = "SELECT base_tabname, tabname FROM syscat.tables WHERE type = 'A' AND owner = '" + schema.toUpperCase() + "'";
					break;
				
				case "ORA":
					aliasesQuery = "SELECT table_name, synonym_name FROM user_synonyms"; // or maybe all_synonyms
					break;
					
				case "SQLSRV":
					aliasesQuery = "SELECT PARSENAME(base_object_name,1), name FROM sys.synonyms";
					break;
			}
			
			Map<String, String> tableAliases = new HashMap<String, String>();
			try{
				PreparedStatement stmt = con.getMetaData().getConnection().prepareStatement(aliasesQuery);
	            ResultSet rst = stmt.executeQuery();
	            while (rst.next()) {
	                String table = rst.getString(1);
	                String alias = rst.getString(2);
	                tableAliases.put(alias, table);
	            }			
				s.setAttribute("tableAliases", tableAliases);
				if(rst != null){rst.close();}
			}
			catch(Exception e){
				//Ignore error if no alias in database
			}

    	}
    }

	/**
     * @see HttpSessionAttributeListener#attributeReplaced(HttpSessionBindingEvent)
     */
    public void attributeReplaced(HttpSessionBindingEvent arg0)  { 
         // TODO Auto-generated method stub
    }
	
}
