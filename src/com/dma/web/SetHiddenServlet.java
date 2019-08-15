package com.dma.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Servlet implementation class AppendSelectionsServlet
 */
@WebServlet(name = "SetHidden", urlPatterns = { "/SetHidden" })
public class SetHiddenServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public SetHiddenServlet() {
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
			
			if(parms != null && parms.get("qs") != null && parms.get("query") != null) {
				
				String json = (String) parms.get("qs");
				QuerySubject qs = (QuerySubject) Tools.fromJSON(json, new TypeReference<QuerySubject>(){});
				List<Field> fields = qs.getFields();
				Map<String, Field> fieldsMap = new HashMap<String, Field>();
				for(Field field: fields) {
					fieldsMap.put(field.getField_name().toUpperCase(), field);
				}
				
				Connection con = (Connection) request.getSession().getAttribute("con");
				DatabaseMetaData metaData = con.getMetaData();
				String schema = (String) request.getSession().getAttribute("schema");
				String table = qs.getTable_name();
				String query0 = (String) parms.get("query");
				query0 = query0.replaceAll("\\$TABLE", table);
				
				ResultSet rst0 = metaData.getColumns(con.getCatalog(), schema, table, "%");
				Statement stmt = con.createStatement();
			    
			    while (rst0.next()) {
			    	String colName = rst0.getString("COLUMN_NAME").toUpperCase();
			    	String query1 = query0.replaceAll("\\$FIELD", colName);
		    		ResultSet rst1 = null;
		            try{
			            rst1 = stmt.executeQuery(query1);
		            	System.out.println(query1); 
			            if (!rst1.next()) {    
			                System.out.println("No data"); 
			                fieldsMap.get(colName).setHidden(true);
			            } 		            
		            }
		            catch(SQLException e){
		            	System.out.println("CATCHING SQLEXEPTION...");
		            	System.out.println(e.getSQLState());
		            	System.out.println(e.getMessage());
		            	
		            }
		            finally {
			            if(rst1 != null){rst1.close();}
					}
			    }

			    if(stmt != null) {stmt.close();}
		        if(rst0 != null){rst0.close();}
		        
		        
		        
		        qs.setFields(new ArrayList<Field>(fieldsMap.values()));
				result.put("DATAS", qs);
				result.put("STATUS", "OK");
				
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