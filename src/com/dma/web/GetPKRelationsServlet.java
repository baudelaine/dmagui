package com.dma.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.RoundingMode;
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class GetTablesServlet
 */
@WebServlet("/GetPKRelations")
public class GetPKRelationsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetPKRelationsServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub

		
		Map<String, Object> result = new HashMap<String, Object>();
		
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
		
		boolean importLabel = false;
		
		String table = request.getParameter("table");
		String alias = request.getParameter("alias");
		String type = request.getParameter("type");
		importLabel = Boolean.parseBoolean(request.getParameter("importLabel"));
		
		boolean withRecCount = false;
		boolean relationCount = false;

		Map<String, DBMDTable> dbmd = null;
		
		Connection con = null;
		DatabaseMetaData metaData = null;
		String schema = "";
		String language = "";
		Map<String, String> tableAliases = null;


		try {
			
			con = (Connection) request.getSession().getAttribute("con");
			schema = (String) request.getSession().getAttribute("schema");
			Project project = (Project) request.getSession().getAttribute("currentProject");
			language = project.languages.get(0);
			relationCount = project.isRelationCount();
			Map<String, QuerySubject> qsFromXML = (Map<String, QuerySubject>) request.getSession().getAttribute("QSFromXML");				

			
			dbmd = (Map<String, DBMDTable>) request.getSession().getAttribute("dbmd");
			withRecCount = (Boolean) request.getServletContext().getAttribute("withRecCount");
			tableAliases = (Map<String, String>) request.getSession().getAttribute("tableAliases");
			
		    Map<String, Relation> map = new HashMap<String, Relation>();
		    
			String PKQuery = (String) request.getSession().getAttribute("PKQuery");
		    
			Connection csvCon = null;
			PreparedStatement stmt = null;
			ResultSet rst = null;
			
			if(Files.exists(Paths.get(prj + "/relation.csv"))) {
				Properties props = new java.util.Properties();
				props.put("separator",";");
				csvCon = DriverManager.getConnection("jdbc:relique:csv:" + prj.toString(), props);
				String sql = "SELECT * FROM relation where PKTABLE_NAME = '" + table + "'";
				stmt = csvCon.prepareStatement(sql);
				rst = stmt.executeQuery();
				result.put("MODE", "CSV");
			}
			else if(PKQuery != null && !PKQuery.isEmpty()) {
				stmt = con.prepareStatement(PKQuery);
				stmt.setString(1, table);
	    		rst = stmt.executeQuery();
				result.put("MODE", "SQL");
	    	}
			else {
				metaData = con.getMetaData();
				rst = metaData.getExportedKeys(con.getCatalog(), schema, table);
				result.put("MODE", "DB");
			}
		    
		    while (rst.next()) {
		    	
		    	String key_name = rst.getString("FK_NAME");
		    	String fk_name = rst.getString("FK_NAME");
		    	String pk_name = rst.getString("PK_NAME");
		    	String key_seq = rst.getString("KEY_SEQ");
		    	String fkcolumn_name = rst.getString("FKCOLUMN_NAME");
		    	String pkcolumn_name = rst.getString("PKCOLUMN_NAME");
		        String fktable_name = rst.getString("FKTABLE_NAME");
		        String pktable_name = rst.getString("PKTABLE_NAME");
		        String _id = key_name + "P";
		        boolean isAlias = false;
		        
		        if(tableAliases != null){
			        if(tableAliases.containsKey(fktable_name)){
			        	fktable_name = tableAliases.get(fktable_name);
			        	isAlias = true;
			        }
	
			        if(tableAliases.containsKey(pktable_name)){
			        	pktable_name = tableAliases.get(pktable_name);
			        }
		        }
		        
		        // Jump to other key if fktable is an alias
		        if(isAlias){continue;}
		        
		        if(!map.containsKey(_id)){
		        	
		        	Relation relation = new Relation();
		        	
		        	relation.set_id(_id);
		        	relation.setKey_name(key_name);
		        	relation.setFk_name(fk_name);
		        	relation.setPk_name(pk_name);
		        	relation.setTable_name(pktable_name);
		        	relation.setTable_alias(alias);
		        	relation.setPktable_name(fktable_name);
		        	relation.setPktable_alias(fktable_name);
		        	relation.setRelashionship("[" + type.toUpperCase() + "].[" + alias + "].[" + pkcolumn_name + "] = [" + fktable_name + "].[" + fkcolumn_name + "]");
		        	relation.setWhere(pktable_name + "." + pkcolumn_name + " = " + fktable_name + "." + fkcolumn_name);
		        	relation.setKey_type("P");
		        	relation.setType(type.toUpperCase());
		        	relation.set_id("PK_" + relation.getPktable_alias() + "_" + alias + "_" + type.toUpperCase());
		        	relation.setAbove(pkcolumn_name);
		        	relation.setLabel("");
		        	relation.setDescription("");
		        	
		        	ResultSet rst0 = null;
		        	if(importLabel && qsFromXML == null) {
		        	
			        	String[] types = {"TABLE"};
			        	rst0 = metaData.getTables(con.getCatalog(), schema, fktable_name, types);
			    		String label = "";
			    		String desc = "";
			    		while (rst0.next()) {
			    			label = rst0.getString("REMARKS");
			    	    	relation.setLabel(label);
			    	    	
			    	    	if(label == null) {
			    	    		label = "";
			    	    		relation.setLabel(label);
			    	    		relation.setDescription(desc);
				        		if(!language.isEmpty()) {
				        			relation.getLabels().put(language, label);
				        			relation.getDescriptions().put(language, desc);
				        		}
			    	    		
			    	    	}
			    	    	else {
			    		    	if(label.length() <= 50) {
			    		    		relation.setLabel(label);
			    		    		relation.setDescription(desc);
					        		if(!language.isEmpty()) {
					        			relation.getLabels().put(language, label);
					        			relation.getDescriptions().put(language, desc);
					        		}
			    		    	}
			    		    	else {
			    			    	relation.setDescription(label);
			    		    		relation.setLabel(label.substring(0, 50));
					        		if(!language.isEmpty()) {
					        			relation.getLabels().put(language, label.substring(0, 50));
					        			relation.getDescriptions().put(language, label);
					        		}
			    		    	}
			    	    	}
			    			
			    	    }
			    		if(rst0 != null){rst0.close();}
			        	
			    		if(dbmd != null){
			    			DBMDTable dbmdTable = dbmd.get(fktable_name);
			    			if(dbmdTable != null){
			    				label = dbmdTable.getTable_remarks();
				    			relation.setLabel(label);
				    			desc = dbmdTable.getTable_description();
				    			relation.setDescription(desc);
				           		if(!language.isEmpty()) {
				           			relation.getLabels().put(language, label);
				           			relation.getDescriptions().put(language, desc);
				        		}	    			
			    			}
			    		}
		        	}
		        	
		        	Seq seq = new Seq();
		        	seq.setTable_name(pktable_name);
		        	seq.setPktable_name(fktable_name);
		        	seq.setColumn_name(pkcolumn_name);
		        	seq.setPkcolumn_name(fkcolumn_name);
		        	seq.setKey_seq(Short.parseShort(key_seq));
		        	relation.addSeq(seq);
		        	
		        	map.put(_id, relation);

		        }
		        else{
		        	
		        	Relation relation = map.get(_id);
		        	if(!relation.getSeqs().isEmpty()){
		        		Seq seq = new Seq();
			        	seq.setTable_name(pktable_name);
			        	seq.setPktable_name(fktable_name);
			        	seq.setColumn_name(pkcolumn_name);
			        	seq.setPkcolumn_name(fkcolumn_name);
			        	seq.setKey_seq(Short.parseShort(key_seq));
			        	
			        	relation.addSeq(seq);
			        	
			        	StringBuffer sb = new StringBuffer((String) relation.getRelationship());
			        	sb.append(" AND [" + type.toUpperCase() + "].[" + alias + "].[" + pkcolumn_name + "] = [" + fktable_name + "].[" + fkcolumn_name + "]");
			        	relation.setRelashionship(sb.toString());
			        	
			        	sb = new StringBuffer((String) relation.getWhere());
			        	sb.append(" AND " + fktable_name + "." + fkcolumn_name + " = " + pktable_name + "." + pkcolumn_name);
			        	relation.setWhere(sb.toString());
		        	}
		        	
		        }
	        	
		        	
		    }
		    
		    if(rst != null){rst.close();}
		    if (stmt != null) { stmt.close();}
		    if (csvCon != null) { csvCon.close();}
		    
		    if(withRecCount && qsFromXML == null){
		    	
	            long tableRecCount = 0;
	    		Statement stm = null;
	    		ResultSet rs = null;
	            try{
		    		String query = "SELECT COUNT(*) FROM ";
		    		if(!schema.isEmpty()){
		    			query += schema + ".";
		    		}
		    		query += table;
		    		
		    		stm = con.createStatement();
		            rs = stm.executeQuery(query);
		            while (rs.next()) {
		            	tableRecCount = rs.getLong(1);
		            }
			    }
	            catch(SQLException e){
	            	System.out.println("CATCHING SQLEXEPTION...");
	            	System.out.println(e.getSQLState());
	            	System.out.println(e.getMessage());
	            	
	            }
	            finally {
		            if (stm != null) { stm.close();}
		            if(rst != null){rst.close();}
					
				}
		    	
	            if(relationCount) {
			    	for(Entry<String, Relation> relation: map.entrySet()){
			    		Relation rel = relation.getValue();
			    		
			    		Set<String> tableSet = new HashSet<String>();
			    		for(Seq seq: rel.getSeqs()){
			    			if(!schema.isEmpty()){
				    			tableSet.add(schema + "." + seq.pktable_name);
				    			tableSet.add(schema + "." + seq.table_name);
			    			}
			    			else{
				    			tableSet.add(schema + seq.pktable_name);
				    			tableSet.add(schema + seq.table_name);
			    			}
			    		}
			    		
			    		System.out.println("tableSet=" + tableSet);
			    		
			    		StringBuffer sb = new StringBuffer();;
			    		
			    		for(String tbl: tableSet){
			    			sb.append(", " + tbl);
			    		}
			    		String tables = sb.toString().substring(1);
			    		
			            long recCount = 0;
			    		stm = null;
			    		rs = null;
			            try{
				    		String query = "SELECT COUNT(*) FROM " + tables + " WHERE " + rel.where;
				    		System.out.println(query);
				    		stm = con.createStatement();
				            rs = stm.executeQuery(query);
				            while (rs.next()) {
				            	recCount = rs.getLong(1);
				            }
				            rel.setRecCount(recCount);
				    		float percent = (Math.round(((float)recCount / tableRecCount) * 100));
				            rel.setRecCountPercent((int) percent);
				            
				    		double d0 = Double.parseDouble(String.valueOf(recCount));
				    		double d1 = Double.parseDouble(String.valueOf(tableRecCount));
				    		
				    		double num = (d0/d1) * 100;
				    		NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
				    		nf.setMaximumFractionDigits(3);
				    		nf.setRoundingMode(RoundingMode.UP);
				    	    num = Double.parseDouble(nf.format(num));
				            rel.setRecCountPercent(num);
				            
			            }
			            catch(SQLException e){
			            	System.out.println("CATCHING SQLEXEPTION...");
			            	System.out.println(e.getSQLState());
			            	System.out.println(e.getMessage());
			            	
			            }
			            finally {
				            if (stm != null) { stm.close();}
				            if(rst != null){rst.close();}
							
						}
			    		
			    	}
	            }
		    }		    
		    
		    result.put("DATAS", new ArrayList<Object>(map.values()));
			
			result.put("STATUS", "OK");
		    
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			result.put("STATUS", "KO");
			result.put("EXCEPTION", e.getClass().getName());
			result.put("MESSAGE", e.getMessage());
			result.put("TROUBLESHOOTING", "Check relations settings are matching connected datasource.");
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
