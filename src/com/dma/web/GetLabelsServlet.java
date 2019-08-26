package com.dma.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servlet implementation class GetTablesServlet
 */
@WebServlet("/GetLabels")
public class GetLabelsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetLabelsServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		Connection con = null;
		Map<String, Object> results = new HashMap<String, Object>();
		Map<String, DBMDTable> dbmd = new HashMap<String, DBMDTable>();
		String schema = null;
		String dbEngine = null;
		PreparedStatement stmt = null;
		ResultSet rst = null;
		
		try {
			
			BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
			
	        ObjectMapper mapper = new ObjectMapper();
	        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	        Map<String, Object>	parms = new HashMap<String, Object>();
	        parms = mapper.readValue(br, new TypeReference<Map<String, Object>>(){});
		        
	        dbmd = (Map<String, DBMDTable>) request.getSession().getAttribute("dbmd");
			con = (Connection) request.getSession().getAttribute("con");
			schema = (String) request.getSession().getAttribute("schema");
			dbEngine = (String) request.getSession().getAttribute("dbEngine");

			if((dbEngine).equalsIgnoreCase("ORA")){
				con.createStatement().execute("ALTER SESSION SET NLS_SORT=BINARY_CI");
				con.createStatement().execute("ALTER SESSION SET NLS_COMP=LINGUISTIC");
			}

			List<String> tables = (List<String>) parms.get("tables");
			
			if(tables.size() > 0){
			
				Map<String, String> tlMap = new HashMap<String, String>();
				Map<String, String> tdMap = new HashMap<String, String>();
				Map<String, Map<String, String>> clMap = new HashMap<String, Map<String, String>>();
				Map<String, Object> cdMap = new HashMap<String, Object>();
				
				String tableInClause = "('" + StringUtils.join(tables.iterator(), "','") + "')";
				
				String tlQuery = (String) parms.get("tlQuery");
				System.out.println("tlQuery=" + tlQuery);
				if(!tlQuery.isEmpty() && StringUtils.countMatches(tlQuery, "(?)") == 1){
					tlQuery = StringUtils.replace(tlQuery, "(?)", tableInClause);
					System.out.println("tlQuery=" + tlQuery);
					stmt = con.prepareStatement(tlQuery);
					rst = stmt.executeQuery();
					while(rst.next()){
						tlMap.put(rst.getString("Table_Name").toUpperCase(), rst.getString("Table_Label"));
					}
					rst.close();
					stmt.close();					
				}
				else {
					for(String table: tables) {
						tlMap.put(table.toUpperCase(), "");
					}
				}
				
				String tdQuery = (String) parms.get("tdQuery");
				System.out.println("tdQuery=" + tdQuery);
				if(!tdQuery.isEmpty() && StringUtils.countMatches(tdQuery, "(?)") == 1){
					tdQuery = StringUtils.replace(tdQuery, "(?)", tableInClause);
					stmt = con.prepareStatement(tdQuery);
					System.out.println("tdQuery=" + tdQuery);
					rst = stmt.executeQuery();
					while(rst.next()){
						tdMap.put(rst.getString("Table_Name").toUpperCase(), rst.getString("Table_Description"));
					}
					rst.close();
					stmt.close();					
				}
				else {
					for(String table: tables) {
						tdMap.put(table.toUpperCase(), "");
					}
				}
				
				
				
				for(String table: tables){

					List<String> fields = new ArrayList<String>();
					
					DatabaseMetaData metaData = con.getMetaData();
					rst = metaData.getColumns(con.getCatalog(), schema, table, "%");
					while(rst.next()){
						fields.add(rst.getString("COLUMN_NAME"));
					}
					rst.close();

					String columnInClause = "('" + StringUtils.join(fields.iterator(), "','") + "')";
					
					String clQuery = (String) parms.get("clQuery");
					if(!clQuery.isEmpty() && StringUtils.countMatches(clQuery, "(?)") == 1 && StringUtils.countMatches(clQuery, " ? ") == 1){
						clQuery = StringUtils.replace(clQuery, "(?)", columnInClause);
						
						Map<String, String> cols = new HashMap<String, String>();
						
						stmt = con.prepareStatement(clQuery);
						stmt.setString(1, table);
						rst = stmt.executeQuery();
						
						while(rst.next()){
							cols.put(rst.getString("Column_Name").toUpperCase(), rst.getString("Column_Label"));
						}
						rst.close();
						stmt.close();
						
						clMap.put(table.toUpperCase(), cols);
					}
					else {
						Map<String, String> cols = new HashMap<String, String>();
						for(String field: fields) {
							cols.put(field, "");
						}
						clMap.put(table.toUpperCase(), cols);
					}
					
					String cdQuery = (String) parms.get("cdQuery");
					if(!cdQuery.isEmpty() && StringUtils.countMatches(cdQuery, "(?)") == 1 && StringUtils.countMatches(cdQuery, " ? ") == 1){
						cdQuery = StringUtils.replace(cdQuery, "(?)", columnInClause);
						
						Map<String, Object> cols = new HashMap<String, Object>();
						
						stmt = con.prepareStatement(cdQuery);
						stmt.setString(1, table);
						rst = stmt.executeQuery();
						while(rst.next()){
							cols.put(rst.getString("Column_Name").toUpperCase(), rst.getString("Column_Description"));
						}
						rst.close();
						stmt.close();
						
						cdMap.put(table.toUpperCase(), cols);
					}
					else {
						Map<String, String> cols = new HashMap<String, String>();
						for(String field: fields) {
							cols.put(field, "");
						}
						cdMap.put(table.toUpperCase(), cols);
					}
					
					
				}
				
				for(String table: tables){
					DBMDTable dbmdTable = new DBMDTable();
					if(dbmd != null && dbmd.containsKey(table)) {
						dbmdTable = dbmd.get(table);
					}
					dbmdTable.setTable_remarks(tlMap.get(table));
					dbmdTable.setTable_description(tdMap.get(table));

					Map<String, DBMDColumn> dbmdColumns = new HashMap<String, DBMDColumn>();
					if(dbmd != null && dbmd.containsKey(table)) {
						dbmdColumns = dbmdTable.getColumns();
					}
					
					
					Map<String, String> cls = (Map<String, String>) clMap.get(table);
					
					if(cls != null){
						for(Entry<String, String> cl: cls.entrySet()){
							String column_name = cl.getKey();
							String column_remarks = cl.getValue();	
							if(!dbmdColumns.containsKey(column_name)) {
								dbmdColumns.put(column_name, new DBMDColumn());
								dbmdColumns.get(column_name).setColumn_name(column_name);
							}
							dbmdColumns.get(column_name).setColumn_remarks(column_remarks);
						}
					}

					Map<String, String> cds = (Map<String, String>) cdMap.get(table);
					
					if(cds != null){
						for(Entry<String, String> cd: cds.entrySet()){
							String column_name = cd.getKey();
							String column_description = cd.getValue();	
							if(!dbmdColumns.containsKey(column_name)) {
								dbmdColumns.put(column_name, new DBMDColumn());
								dbmdColumns.get(column_name).setColumn_name(column_name);
							}
							dbmdColumns.get(column_name).setColumn_description(column_description);
						}
					}

					dbmdTable.setColumns(dbmdColumns);
					
					dbmd.put(table, dbmdTable);
					
				}
				
				results.put("STATUS", "OK");
				results.put("DATAS", dbmd);
				request.getSession().setAttribute("dbmd", dbmd);
				
			}
			else {
				results.put("STATUS", "OK");
				results.put("DATAS", dbmd);
				results.put("MESSAGE", "No table given in parms.");
				
			}
			
			
		}
		catch (Exception e){
			results.put("STATUS", "KO");
            results.put("EXCEPTION", e.getClass().getName());
            results.put("MESSAGE", e.getMessage());
            results.put("TROUBLESHOOTING", "Check SQL syntax.");
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            results.put("STACKTRACE", sw.toString());
            e.printStackTrace(System.err);		}
		
		finally {
		    response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(Tools.toJSON(results));
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
