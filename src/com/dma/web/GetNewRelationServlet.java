package com.dma.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class GetTablesServlet
 */
@WebServlet("/GetNewRelation")
public class GetNewRelationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetNewRelationServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		Relation result = null;
		Connection con = null;
		DatabaseMetaData metaData = null;
		String schema = "";
		

		try {
			String table = request.getParameter("table");
			@SuppressWarnings("unchecked")
			Map<String, Object> dbmd = (Map<String, Object>) request.getSession().getAttribute("dbmd");
			
			con = (Connection) request.getSession().getAttribute("con");
			schema = (String) request.getSession().getAttribute("schema");
			
			
			Project project = (Project) request.getSession().getAttribute("currentProject");
			String language = project.languages.get(0);			
	        result = new Relation();
	        
        	String[] types = {"TABLE"};
        	metaData = con.getMetaData();
    		ResultSet rst0 = metaData.getTables(con.getCatalog(), schema, table, types);
    		String label = "";
    		String desc = "";
    		while (rst0.next()) {
    			label = rst0.getString("REMARKS");
    	    	result.setLabel(label);
    	    	
    	    	if(label == null) {
    	    		label = "";
    	    		result.setLabel(label);
    	    		result.setDescription(desc);
    				if(!language.isEmpty()) {
    					result.getLabels().put(language, label);
    					result.getDescriptions().put(language, desc);
    				}
    	    		
    	    	}
    	    	else {
    		    	if(label.length() <= 50) {
    		    		result.setLabel(label);
    		    		result.setDescription(desc);
    					if(!language.isEmpty()) {
    						result.getLabels().put(language, label);
    						result.getDescriptions().put(language, desc);
    					}
    		    		
    		    	}
    		    	else {
    		    		result.setDescription(label);
    		    		result.setLabel(label.substring(1, 50));
    					if(!language.isEmpty()) {
    						result.getLabels().put(language, label.substring(1, 50));
    						result.getDescriptions().put(language, label);
    					}
    		    	}
    	    	}
    			
    	    }
    		if(rst0 != null){rst0.close();}
	        
			if(dbmd != null){
				@SuppressWarnings("unchecked")
				Map<String, Object> o = (Map<String, Object>) dbmd.get(table);
				if(o != null){
					label = (String) o.get("table_remarks"); 
					desc = (String) o.get("table_description");
					result.setLabel(label);
					result.setDescription(desc);
					
					if(!language.isEmpty()) {
						result.getLabels().put(language, label);
						result.getDescriptions().put(language, desc);
					}
				}
			}
	        
		    response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(Tools.toJSON(result));
			
		}
		catch (Exception e){
			e.printStackTrace(System.err);
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
