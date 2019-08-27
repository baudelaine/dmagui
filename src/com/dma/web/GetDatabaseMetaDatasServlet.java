package com.dma.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Servlet implementation class GetTablesServlet
 */
@WebServlet("/GetDBMD")
public class GetDatabaseMetaDatasServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetDatabaseMetaDatasServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	
	@SuppressWarnings({ "resource", "unchecked" })
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub

		Map<String, Object> result = new HashMap<String, Object>();
		
		try {

			result.put("CLIENT", request.getRemoteAddr() + ":" + request.getRemotePort());
			result.put("SERVER", request.getLocalAddr() + ":" + request.getLocalPort());
			result.put("FROM", this.getServletName());
			String user = request.getUserPrincipal().getName();
			result.put("USER", user);
			result.put("JSESSIONID", request.getSession().getId());
			Path wks = Paths.get(getServletContext().getRealPath("/datas") + "/" + user);			
			result.put("WKS", wks.toString());
			Path prj = Paths.get((String) request.getSession().getAttribute("projectPath"));
			result.put("PRJ", prj.toString());
//			Map<String, Object> parms = Tools.fromJSON(request.getInputStream());
			
			Path path = Paths.get((String) request.getSession().getAttribute("projectPath") + "/dbmd.json");
			
			Map<String, DBMDTable> dbmd = new HashMap<String, DBMDTable>();
			
			if(Files.exists(path)){
				
				dbmd = (Map<String, DBMDTable>) Tools.fromJSON(path.toFile(), new TypeReference<Map<String, DBMDTable>>(){});				
				result.put("DATAS", dbmd);
				
			}
			else{			
			
				Connection con = (Connection) request.getSession().getAttribute("con");
				String schema = (String) request.getSession().getAttribute("schema");
				
			    DatabaseMetaData metaData = con.getMetaData();
			    //String[] types = {"TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"};
			    String[] types = {"TABLE"};
			    ResultSet rst0 = metaData.getTables(con.getCatalog(), schema, "%", types);	
	
			    
			    while (rst0.next()) {

			    	String table_name = rst0.getString("TABLE_NAME");
			    	
			    	ResultSet rst = null;
			    	PreparedStatement stmt = null;
			    	int FKSeqCount = 0;
			    	Set<String> FKSet = new HashSet<String>();
			    	
//			    	Path rels = Paths.get(prj + "/relation.json");
//			    	
//			    	if(Files.exists(rels)) {
//						String FKQuery = (String) Tools.fromJSON(rels.toFile()).get("FKQuery");
//						if(FKQuery != null) {
//							FKQuery = FKQuery.replace(";", "");
//				    		stmt = con.prepareStatement(FKQuery);
//				    		stmt.setString(1, table_name);
//				    		rst = stmt.executeQuery();
//				    		result.put("FKS", "FKQuery");
//						}
//			    	}
//			    	if( rst == null) {
//				    	rst = metaData.getImportedKeys(con.getCatalog(), schema, table_name);
//			    		result.put("FKS", "DB");
//			    	}

			    	Connection csvCon = null;
			    	String FKQuery = (String) request.getSession().getAttribute("FKQuery");
					if(Files.exists(Paths.get(prj + "/relation.csv"))) {
						Properties props = new java.util.Properties();
						props.put("separator",";");
						csvCon = DriverManager.getConnection("jdbc:relique:csv:" + prj.toString(), props);
						String sql = "SELECT * FROM relation where FKTABLE_NAME = '" + table_name + "'";
						stmt = csvCon.prepareStatement(sql);
						rst = stmt.executeQuery();
						result.put("FKS", "CSV");
					}
					else if(FKQuery != null && !FKQuery.isEmpty()) {
						stmt = con.prepareStatement(FKQuery);
						stmt.setString(1, table_name);
			    		rst = stmt.executeQuery();
						result.put("FKS", "SQL");
			    	}
					else {
						rst = metaData.getImportedKeys(con.getCatalog(), schema, table_name);
						result.put("FKS", "DB");
					}
			    	
			    	while(rst.next()){
			    		String FKName = rst.getString("FK_NAME");
			    		FKSet.add(FKName);
			    		FKSeqCount++;
			    	}
		            if(rst != null) {
		            	rst.close();
		            	rst = null;
		            }
		    		if(stmt != null) {
		    			stmt.close();
		    			stmt = null;
		    		}
		    		if(csvCon != null) {
		    			csvCon.close();
		    			csvCon = null;
		    		}

			    	int PKSeqCount = 0;
			    	Set<String> PKSet = new HashSet<String>();

			    	String PKQuery = (String) request.getSession().getAttribute("PKQuery");
					if(Files.exists(Paths.get(prj + "/relation.csv"))) {
						Properties props = new java.util.Properties();
						props.put("separator",";");
						csvCon = DriverManager.getConnection("jdbc:relique:csv:" + prj.toString(), props);
						String sql = "SELECT * FROM relation where PKTABLE_NAME = '" + table_name + "'";
						stmt = csvCon.prepareStatement(sql);
						rst = stmt.executeQuery();
						result.put("PKS", "CSV");
					}
					else if(PKQuery != null && !PKQuery.isEmpty()) {
						stmt = con.prepareStatement(PKQuery);
						stmt.setString(1, table_name);
			    		rst = stmt.executeQuery();
						result.put("PKS", "SQL");
			    	}
			    	
			    	if( rst == null) {
			    		rst = metaData.getExportedKeys(con.getCatalog(), schema, table_name);
			    		result.put("PKS", "DB");
			    	}
			    	while(rst.next()){
			    		String PKName = rst.getString("FK_NAME");
			    		PKSet.add(PKName);
			    		PKSeqCount++;
			    	}
		            if(rst != null) {
		            	rst.close();
		            	rst = null;
		            }
		    		if(stmt != null) {
		    			stmt.close();
		    			stmt = null;
		    		}
		    		if(csvCon != null) {
		    			csvCon.close();
		    			csvCon = null;
		    		}

				    rst = metaData.getPrimaryKeys(con.getCatalog(), schema, table_name);
				    Set<String> pks = new HashSet<String>();
				    
				    while (rst.next()) {
				    	pks.add(rst.getString("COLUMN_NAME"));
				    }
			        if(rst != null){rst.close();}

				    rst = metaData.getIndexInfo(con.getCatalog(), schema, table_name, false, true);
				    Set<String> indexes = new HashSet<String>();
				    
				    while (rst.next()) {
				    	indexes.add(rst.getString("COLUMN_NAME"));
				    }
			        if(rst != null){rst.close();}
			    	
			    	long recCount = 0;
		    		Statement stm = null;
		    		ResultSet rs = null;
		            try{
			    		String query = "SELECT COUNT(*) FROM ";
			    		if(!schema.isEmpty()){
			    			query += schema + ".";
			    		}
			    		query += table_name;
			    		stm = con.createStatement();
			            rs = stm.executeQuery(query);
			            while (rs.next()) {
			            	recCount = rs.getLong(1);
			            }
		            }
		            catch(SQLException e){
		            	System.out.println("CATCHING SQLEXEPTION...");
		            	System.out.println(e.getSQLState());
		            	System.out.println(e.getMessage());
		            	
		            }
		            finally {
			            if (stm != null) { stm.close();}
			            if(rs != null){rs.close();}
						
					}
			    	
		            
		            DBMDTable table = new DBMDTable();
		            
			    	String table_type = rst0.getString("TABLE_TYPE");
			    	String table_remarks = rst0.getString("REMARKS");
			    	String table_schema = rst0.getString("TABLE_SCHEM");
			    	table.setTable_name(table_name);
			    	table.setTable_schema(table_schema);
			    	table.setTable_type(table_type);
			    	table.setTable_remarks(table_remarks);
			    	
			    	if(table_remarks == null) {
			    		table.setTable_remarks("");
			    		table.setTable_description("");
			    	}
			    	else {
			    		if(table_remarks.length() <= 50) {
				    		table.setTable_remarks(table_remarks);
				    		table.setTable_description("");
			    		}
			    		else {
				    		table.setTable_remarks(table_remarks.substring(1, 50));
				    		table.setTable_description(table_remarks);
			    		}
			    	}
			    	
			    	table.setTable_recCount(recCount);
		    		table.setTable_primaryKeyFieldsCount(pks.size());
		    		table.setTable_importedKeysCount(FKSet.size());
		    		table.setTable_importedKeysSeqCount(FKSeqCount);
		    		table.setTable_exportedKeysCount(PKSet.size());
		    		table.setTable_exportedKeysSeqCount(PKSeqCount);
		    		table.setTable_indexesCount(indexes.size());

		    		String stats = "(" + pks.size() + ") (" + FKSet.size() + ") (" + FKSeqCount + ") (" + PKSet.size() +
		    				") (" + PKSeqCount + ") (" +  indexes.size() + ") (" + recCount + ")";  
			    	table.setTable_stats(stats);
				    
				    ResultSet rst1 = metaData.getColumns(con.getCatalog(), schema, table_name, "%");
				    
				    Map<String, DBMDColumn> fields = new HashMap<String, DBMDColumn>();
				    
				    while(rst1.next()){
					    DBMDColumn field = new DBMDColumn();
				    	field.setColumn_name(rst1.getString("COLUMN_NAME"));
				    	field.setColumn_type(rst1.getString("TYPE_NAME"));
				    	String column_remarks = rst1.getString("REMARKS");
				    	if(column_remarks == null) {
				    		field.setColumn_remarks("");
					    	field.setColumn_description("");
				    	}
				    	else {
					    	if(column_remarks.length() <= 50) {
					    		field.setColumn_remarks(column_remarks);
						    	field.setColumn_description("");
					    	}
					    	else {
					    		field.setColumn_remarks(column_remarks.substring(1, 50));
						    	field.setColumn_description(column_remarks);
					    	}
				    	}
			        	field.setColumn_size(rst1.getInt("COLUMN_SIZE"));
			        	if(pks.contains(rst1.getString("COLUMN_NAME"))){
			    			field.setColumn_isPrimaryKey(true);
			    		}
			        	else{
			    			field.setColumn_isPrimaryKey(false);
			        	}
			        	if(indexes.contains(rst1.getString("COLUMN_NAME"))){
			        		field.setColumn_isIndexed(true);
			        	}
			        	else{
			        		field.setColumn_isIndexed(false);
			        	}
				    	
			        	field.setColumn_isNullable(rst1.getString("IS_NULLABLE"));
				    	field.setColumn_isFiltered(false);
				    	field.setFiltered(false);
					    fields.put(rst1.getString("COLUMN_NAME"), field);
				    }
				    if(rst1 != null){rst1.close();}
				    table.setColumns(fields);
				    dbmd.put(table_name, table);
				    
			    }		    
			    
			    if(rst0 != null){rst0.close();}
			    
				request.getSession().setAttribute("dbmd", dbmd);
				result.put("DATAS", dbmd);
			    
			}
			result.put("STATUS", "OK");
			
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			result.put("STATUS", "KO");
			result.put("EXCEPTION", e.getClass().getName());
			result.put("MESSAGE", e.getMessage());
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			result.put("STACKTRACE", sw.toString());
			e.printStackTrace(System.err);
		}

		finally{
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(Tools.toJSON(result));
		}
		
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
