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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

/**
 * Servlet implementation class AppendSelectionsServlet
 */
@WebServlet(name = "GetCsvLabels", urlPatterns = { "/GetCsvLabels" })
public class GetCsvLabelsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetCsvLabelsServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
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
			
			Map<String, Object> parms = Tools.fromJSON(request.getInputStream());
			
			@SuppressWarnings("unchecked")
			Map<String, DBMDTable> dbmd = (Map<String, DBMDTable>) request.getSession().getAttribute("dbmd");
			
			@SuppressWarnings("unchecked")
			Map<String, QuerySubject> qsFromXML = (Map<String, QuerySubject>) request.getSession().getAttribute("QSFromXML");
			
			if(parms != null && parms.get("tables") != null) {
				
				String tlCsvFileName = "tableLabel.csv";
				String tdCsvFileName = "tableDescription.csv";
				String clCsvFileName = "columnLabel.csv";
				String cdCsvFileName = "columnDescription.csv";
				
				if(parms.get("lang") != null && parms.get("lang").toString().length() ==2) {
					String lang = parms.get("lang").toString();
					tlCsvFileName = "tableLabel-" + lang + ".csv";
					tdCsvFileName = "tableDescription-" + lang + ".csv";
					clCsvFileName = "columnLabel-" + lang + ".csv";
					cdCsvFileName = "columnDescription-" + lang + ".csv";
				}
				
				
				@SuppressWarnings("unchecked")
				List<String> tables = (List<String>) parms.get("tables");
				
				if(tables.size() > 0){
				
					Map<String, String> tlMap = new HashMap<String, String>();
					Map<String, String> tdMap = new HashMap<String, String>();
					Map<String, Map<String, String>> clMap = new HashMap<String, Map<String, String>>();
					Map<String, Map<String, String>> cdMap = new HashMap<String, Map<String, String>>();
					
					String tableInClause = "('" + StringUtils.join(tables.iterator(), "','") + "')";
				
					Properties props = new java.util.Properties();
					props.put("separator",";");
					Connection csvCon = DriverManager.getConnection("jdbc:relique:csv:" + prj.toString(), props);
					Statement csvStmt = csvCon.createStatement();
					ResultSet csvRst = null;
			
					if(Files.exists(Paths.get(prj + "/" + tlCsvFileName))){
						csvRst = csvStmt.executeQuery("SELECT * FROM tableLabel where TABLE_NAME in " + tableInClause);
						while(csvRst.next()){
							tlMap.put(csvRst.getString("Table_Name").toUpperCase(), csvRst.getString("Table_Label"));
						}
						csvRst.close();
					}
					
					if(Files.exists(Paths.get(prj + "/" + tdCsvFileName))){
						csvRst = csvStmt.executeQuery("SELECT * FROM tableDescription where TABLE_NAME in " + tableInClause);
						while(csvRst.next()){
							tdMap.put(csvRst.getString("Table_Name").toUpperCase(), csvRst.getString("Table_Description"));
						}
						csvRst.close();
					}
					
					for(String table: tables) {
						
						List<String> fields = new ArrayList<String>();
						
						if(qsFromXML != null) {
							List<Field> fieldsFromXML = qsFromXML.get(table).getFields();
							for(Field fieldFromXML: fieldsFromXML) {
								fields.add(fieldFromXML.getField_name());
							}
						}						
						else {
							Connection con = (Connection) request.getSession().getAttribute("con");
							String schema = (String) request.getSession().getAttribute("schema");
							DatabaseMetaData metaData = con.getMetaData();
							ResultSet rst = metaData.getColumns(con.getCatalog(), schema, table, "%");
							while(rst.next()){
								fields.add(rst.getString("COLUMN_NAME"));
							}
							if(rst != null) {rst.close();}
						}
			
						String columnInClause = "('" + StringUtils.join(fields.iterator(), "','") + "')";
			
						if(Files.exists(Paths.get(prj + "/" + clCsvFileName))){
							Map<String, String> cols = new HashMap<String, String>();
							String sql = "SELECT * FROM columnLabel where TABLE_NAME = '" + table + "' and COLUMN_NAME in " + columnInClause;
							System.out.println(sql);
							csvRst = csvStmt.executeQuery(sql);
							while(csvRst.next()){
								cols.put(csvRst.getString("Column_Name").toUpperCase(), csvRst.getString("Column_Label"));
							}
							csvRst.close();
							clMap.put(table.toUpperCase(), cols);
						}
						
						if(Files.exists(Paths.get(prj + "/" + cdCsvFileName))){
							Map<String, String> cols = new HashMap<String, String>();
							csvRst = csvStmt.executeQuery("SELECT * FROM columnDescription where TABLE_NAME = '" + table + "' and COLUMN_NAME in " + columnInClause);
							while(csvRst.next()){
								cols.put(csvRst.getString("Column_Name").toUpperCase(), csvRst.getString("Column_Description"));
							}
							csvRst.close();
							cdMap.put(table.toUpperCase(), cols);
						}
						
					}
					
					csvStmt.close();
					if(csvCon != null){csvCon.close();}
					
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
						if(dbmd == null) {
							dbmd = new HashMap<String, DBMDTable>();
						}
						dbmd.put(table, dbmdTable);
						
					}
					
					result.put("STATUS", "OK");
					result.put("DATAS", dbmd);
					request.getSession().setAttribute("dbmd", dbmd);
					
				}
				else {
					result.put("STATUS", "OK");
					result.put("MESSAGE", "No table given in parms.");
					
				}
				
			}
			else {
				result.put("STATUS", "KO");
				result.put("ERROR", "Input parameters are not valid.");
				throw new Exception();
			}
			
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