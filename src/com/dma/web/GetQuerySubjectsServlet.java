package com.dma.web;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

/**
 * Servlet implementation class GetImportedKeysServlet
 */
@WebServlet("/GetQuerySubjects")
public class GetQuerySubjectsServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	Connection con = null;
	DatabaseMetaData metaData = null;
	String schema = "";
	String table = "";
	String alias = "";
	String type = "";
	String qs_id = "";
	String r_id = "";
	String linker_id = "";
	String language = "";
	boolean withRecCount = false;
	long qs_recCount = 0L;
	Map<String, Object> dbmd = null;
	Map<String, String> tableAliases = null;
	String relationsQuery = "";
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetQuerySubjectsServlet() {
        super();
        // TODO Auto-generated constructor stub
    }
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		table = request.getParameter("table");
		alias = request.getParameter("alias");
		type = request.getParameter("type");
		linker_id = request.getParameter("linker_id");
		
		System.out.println("table=" + table);
		System.out.println("alias=" + alias);
		System.out.println("type=" + type);
		List<Object> result = new ArrayList<Object>();

		try{
			
			withRecCount = (Boolean) request.getServletContext().getAttribute("withRecCount");
			System.out.println("withRecCount=" + withRecCount);
			Project project = (Project) request.getSession().getAttribute("currentProject");
			language = project.languages.get(0);
			con = (Connection) request.getSession().getAttribute("con");
			schema = (String) request.getSession().getAttribute("schema");
			dbmd = (Map<String, Object>) request.getSession().getAttribute("dbmd");
			tableAliases = (Map<String, String>) request.getSession().getAttribute("tableAliases");
			metaData = con.getMetaData();
			
			QuerySubject querySubject = getQuerySubjects();
			
			querySubject.setFields(getFields());
			
			if(schema.equalsIgnoreCase("MAXIMO")) {
				Path path = Paths.get(request.getServletContext().getRealPath("res/maximo.json"));
				relationsQuery = (String) Tools.fromJSON(path.toFile()).get("relationsQuery");

			}
			
			querySubject.addRelations(getForeignKeys());
				
			result.add(querySubject);
			
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
	
	protected QuerySubject getQuerySubjects() throws SQLException{
		
		String[] types = {"TABLE"};
		ResultSet rst = metaData.getTables(con.getCatalog(), schema, table, types);
		String label = "";
		String desc = "";
		
		while (rst.next()) {
	    	label = rst.getString("REMARKS");
	    }
		
		if(rst != null){rst.close();}
		
		if(label == null) {label = "";}
		else {
	    	if(label.length() > 50) {
		    	desc = label;
	    		label = label.substring(1, 50);
	    	}
		}
    	
		QuerySubject result = new QuerySubject();
		
		result.set_id(alias + type);
		result.setTable_alias(alias);
		result.setTable_name(table);
		result.setType(type);
		result.setLabel(label);
		result.setDescription(desc);
		if(!language.isEmpty()) {
			result.getLabels().put(language, label);
			result.getDescriptions().put(language, desc);
		}
		result.addLinker_id(linker_id);
		
		if(withRecCount){
            long recCount = 0;
    		Statement stmt = null;
    		ResultSet rs = null;
            try{
	    		String query = "SELECT COUNT(*) FROM ";
	    		if(!schema.isEmpty()){
	    			query += schema + ".";
	    		}
	    		query += table;
	    		
	    		stmt = con.createStatement();
	            rs = stmt.executeQuery(query);
	            while (rs.next()) {
	            	recCount = rs.getLong(1);
	            }
	            result.setRecCount(recCount);
	            qs_recCount = recCount;
            }
            catch(SQLException e){
            	System.out.println("CATCHING SQLEXEPTION...");
            	System.out.println(e.getSQLState());
            	System.out.println(e.getMessage());
            	
            }
            finally {
	            if (stmt != null) { stmt.close();}
	            if(rst != null){rst.close();}
				
			}
			
		}
		
		if(dbmd != null){
			@SuppressWarnings("unchecked")
			Map<String, Object> o = (Map<String, Object>) dbmd.get(table);
			if(o != null){
				label = (String) o.get("table_remarks"); 
				result.setLabel(label);
				desc = (String) o.get("table_description");
				result.setDescription(desc);
				if(!language.isEmpty()) {
					result.getLabels().put(language, label);
					result.getDescriptions().put(language, desc);
				}
			}
		}
        
        return result;
        
	}
	
	@SuppressWarnings("unchecked")
	protected List<Field> getFields() throws SQLException{
		
//		Map<String, Field> result = new HashMap<String, Field>();
		
		ResultSet rst = metaData.getPrimaryKeys(con.getCatalog(), schema, table);
	    Set<String> pks = new HashSet<String>();
	    
	    while (rst.next()) {
	    	pks.add(rst.getString("COLUMN_NAME"));
	    }

        if(rst != null){rst.close();}
        
        rst = metaData.getIndexInfo(con.getCatalog(), schema, table, false, true);
	    Set<String> indexes = new HashSet<String>();
	    
	    while (rst.next()) {
	    	indexes.add(rst.getString("COLUMN_NAME"));
	    }

        if(rst != null){rst.close();}
        
        
		Map<String, Object> recCount = new HashMap<String, Object>();
		
        rst = metaData.getColumns(con.getCatalog(), schema, table, "%");
		
	    while (rst.next()) {

	    	String table_name = (rst.getString("TABLE_NAME"));
	    	System.out.println(table_name);

		    ResultSet rst0 = metaData.getColumns(con.getCatalog(), schema, table_name, "%");
	    	StringBuffer sb = new StringBuffer("select ");
		    while(rst0.next()){
		    	String column_name =  rst0.getString("COLUMN_NAME");
		    	System.out.println("-- " + column_name);
		    	sb.append("count(" + column_name + ") as " + column_name + ", ");
		    	
		    }
		    if(rst0 != null){rst0.close();}
		    
		    String head = sb.toString();
		    head = head.substring(0, head.lastIndexOf(","));
		    
		    String sql = head + " from " + table_name;
		    PreparedStatement stmt0 = con.prepareStatement(sql);
			rst0 = stmt0.executeQuery();
			ResultSetMetaData rsmd = rst0.getMetaData();
			int colCount = rsmd.getColumnCount();

			List<String> column_names = new ArrayList<String>();
			
			for(int colid = 1; colid <= colCount; colid++){
				column_names.add(rsmd.getColumnLabel(colid));
			}
			
			while(rst0.next()){
				Map<String, Object> rec = new HashMap<String, Object>();
				for(int colid = 1; colid <= colCount; colid++){
					rec.put(column_names.get(colid -1), rst0.getObject(colid));
				}
				recCount.put(table_name, rec);
			}
			rst0.close();
			stmt0.close();		
			
		
	    }
	    if(rst != null){rst.close();}

		List<Field> result = new ArrayList<Field>();
		
        rst = metaData.getColumns(con.getCatalog(), schema, table, "%");
        
        Map<String, Object> table_labels = null;
        Map<String, Object> columns = null;
        if(dbmd != null){
			table_labels = (Map<String, Object>) dbmd.get(table);
			if(table_labels != null){
				columns = (Map<String, Object>) table_labels.get("columns");
			}
        }
		
        while (rst.next()) {
        	String field_name = rst.getString("COLUMN_NAME");
        	String field_type = rst.getString("TYPE_NAME");
//        	System.out.println(field_name + "," + field_type);
        	Field field = new Field();
        	field.setField_name(field_name);
        	field.setField_type(field_type);
        	
        	field.setLabel(rst.getString("REMARKS"));
        	
	    	String column_remarks = rst.getString("REMARKS");
	    	if(column_remarks == null) {
	    		field.setLabel("");
	    		field.setDescription("");
	    		if(!language.isEmpty()) {
        			field.getLabels().put(language, "");
        			field.getDescriptions().put(language, "");
	    		}
	    	}
	    	else {
		    	if(column_remarks.length() <= 50) {
		    		field.setLabel(column_remarks);
		    		if(!language.isEmpty()) {
	        			field.getLabels().put(language, column_remarks);
	        			field.getDescriptions().put(language, "");
		    		}
		    	}
		    	else {
		    		field.setLabel(column_remarks.substring(1, 50));
			    	field.setDescription(column_remarks);
		    		if(!language.isEmpty()) {
	        			field.getLabels().put(language, column_remarks.substring(1, 50));
	        			field.getDescriptions().put(language, column_remarks);
		    		}
		    	}
	    	}
        	
        	field.setField_size(rst.getInt("COLUMN_SIZE"));
        	field.setNullable(rst.getString("IS_NULLABLE"));
        	field.set_id(field_name + field_type);
        	if(pks.contains(rst.getString("COLUMN_NAME"))){
    			field.setPk(true);
    			field.setIcon("Identifier");
    		}
        	if(indexes.contains(rst.getString("COLUMN_NAME"))){
    			field.setIndexed(true);
    		}

        	if(columns != null){
    			Map<String, Object> column = (Map<String, Object>) columns.get(field_name);
    			if(column != null){
    				String label = (String) column.get("column_remarks");
	    			field.setLabel(label);
	    			String desc = (String) column.get("column_description");
	    			field.setDescription(desc);
	           		if(!language.isEmpty()) {
	        			field.getLabels().put(language, label);
	        			field.getDescriptions().put(language, desc);
	        		}	    			
    			}
    		}
        	
    		if(StringUtils.containsAny(field.getField_type().toUpperCase(), "DATE", "DATETIME", "TIMESTAMP")) {
    			field.setTimeDimension(true);
    		}
    		

    	    if(recCount.containsKey(table)) {
    	    	Map<String, Integer> obj = (Map<String, Integer>) recCount.get(table);
    	    	if(obj.containsKey(field_name)) {
    	    		field.setRecCount(obj.get(field_name));
    	    		if(field.getRecCount() == 0) {
    	    			field.setHidden(true);
    	    		}
    	    	}
    	    }
    		
        	result.add(field);
        }

        if(rst != null){rst.close();}
        
		return result;
		
	}
	
	protected List<Relation> getForeignKeys() throws SQLException{
		
		Map<String, Relation> map = new HashMap<String, Relation>();
		
		ResultSet rst = null;
		
		if(schema.equalsIgnoreCase("MAXIMO") && !relationsQuery.isEmpty()) {
			Statement stmt = con.createStatement();
    		stmt.execute("SET SCHEMA " + schema);
    		rst = stmt.executeQuery(relationsQuery.replace(";", "") + " WHERE TARGETOBJ = '" + table + "'");
    	}
		else {
			rst = metaData.getImportedKeys(con.getCatalog(), schema, table);
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
	        String _id = key_name + "F";
	        boolean isAlias = false;
	        
	        if(tableAliases != null){
		        if(tableAliases.containsKey(fktable_name)){
		        	fktable_name = tableAliases.get(fktable_name);
		        }
	
		        if(tableAliases.containsKey(pktable_name)){
		        	pktable_name = tableAliases.get(pktable_name);
		        	isAlias = true;
		        }
	        }

	        // Jump to other key if pktable is an alias
	        if(isAlias){continue;}

	        if(!map.containsKey(_id)){
	        	
	        	Relation relation = new Relation();
	        	
	        	relation.set_id(_id);
	        	relation.setKey_name(key_name);
	        	relation.setFk_name(fk_name);
	        	relation.setPk_name(pk_name);
	        	relation.setTable_name(fktable_name);
	        	relation.setTable_alias(alias);
	        	relation.setPktable_name(pktable_name);
	        	relation.setPktable_alias(pktable_name);
	        	relation.setRelashionship("[" + type.toUpperCase() + "].[" + alias + "].[" + fkcolumn_name + "] = [" + pktable_name + "].[" + pkcolumn_name + "]");
	        	relation.setWhere(fktable_name + "." + fkcolumn_name + " = " + pktable_name + "." + pkcolumn_name);
	        	relation.setKey_type("F");
	        	relation.setType(type.toUpperCase());
	        	relation.set_id("FK_" + relation.getPktable_alias() + "_" + alias + "_" + type.toUpperCase());
	        	relation.setAbove(fkcolumn_name);
	        	
	        	String[] types = {"TABLE"};
	    		ResultSet rst0 = metaData.getTables(con.getCatalog(), schema, pktable_name, types);
	    		while (rst0.next()) {
	    			String label = rst0.getString("REMARKS");
	    	    	relation.setLabel(label);
	    	    	
	    	    	if(label == null) {
	    	    		relation.setLabel("");
	    	    		relation.setDescription("");
			    		if(!language.isEmpty()) {
			    			relation.getLabels().put(language, "");
			    			relation.getDescriptions().put(language, "");
			    		}
	    	    	}
	    	    	else {
	    		    	if(label.length() <= 50) {
	    		    		relation.setLabel(label);
		    	    		relation.setDescription("");
				    		if(!language.isEmpty()) {
				    			relation.getLabels().put(language, label);
				    			relation.getDescriptions().put(language, "");
				    		}
	    		    	}
	    		    	else {
	    			    	relation.setDescription(label);
	    		    		relation.setLabel(label.substring(1, 50));
				    		if(!language.isEmpty()) {
				    			relation.getLabels().put(language, label.substring(1, 50));
				    			relation.getDescriptions().put(language, label);
				    		}
	    		    	}
	    	    	}
	    	    	
	    	    }
	    		if(rst0 != null){rst0.close();}
	    		
	    		if(relation.getLabel() == null) {relation.setLabel("");}
	        	
	    		if(dbmd != null){
	    			@SuppressWarnings("unchecked")
	    			Map<String, Object> o = (Map<String, Object>) dbmd.get(pktable_name);
	    			if(o != null){
	    				String label = (String) o.get("table_remarks");
		    			relation.setLabel(label);
		    			String desc = (String) o.get("table_description");
		    			relation.setDescription(desc);
		           		if(!language.isEmpty()) {
		           			relation.getLabels().put(language, label);
		           			relation.getDescriptions().put(language, desc);
		        		}	    			
	    			}
	    		}
	        	
	        	Seq seq = new Seq();
	        	seq.setTable_name(fktable_name);
	        	seq.setPktable_name(pktable_name);
	        	seq.setColumn_name(fkcolumn_name);
	        	seq.setPkcolumn_name(pkcolumn_name);
	        	seq.setKey_seq(Short.parseShort(key_seq));
	        	relation.addSeq(seq);
	        	
	        	map.put(_id, relation);

	        }
	        else{
	        	
	        	Relation relation = map.get(_id);
	        	if(!relation.getSeqs().isEmpty()){
	        		Seq seq = new Seq();
		        	seq.setTable_name(fktable_name);
		        	seq.setPktable_name(pktable_name);
		        	seq.setColumn_name(fkcolumn_name);
		        	seq.setPkcolumn_name(pkcolumn_name);
		        	seq.setKey_seq(Short.parseShort(key_seq));
		        	
		        	relation.addSeq(seq);
		        	
		        	StringBuffer sb = new StringBuffer((String) relation.getRelationship());
		        	sb.append(" AND [" + type.toUpperCase() + "].[" + alias + "].[" + fkcolumn_name + "] = [" + pktable_name + "].[" + pkcolumn_name + "]");
		        	relation.setRelashionship(sb.toString());
		        	
		        	sb = new StringBuffer((String) relation.getWhere());
		        	sb.append(" AND " + fktable_name + "." + fkcolumn_name + " = " + pktable_name + "." + pkcolumn_name);
		        	relation.setWhere(sb.toString());
		        	
	        	}
	        	
	        }
        	
	        	
	    }
	    
	    if(withRecCount){
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
	    		
	    		for(String table: tableSet){
	    			sb.append(", " + table);
	    		}
	    		String tables = sb.toString().substring(1);
	    		
	            long recCount = 0;
	    		Statement stmt = null;
	    		ResultSet rs = null;
	            try{
		    		String query = "SELECT COUNT(*) FROM " + tables + " WHERE " + rel.where;
		    		System.out.println(query);
		    		stmt = con.createStatement();
		            rs = stmt.executeQuery(query);
		            while (rs.next()) {
		            	recCount = rs.getLong(1);
		            }
		            rel.setRecCount(recCount);
		    		long result = (Math.round(((double)recCount / qs_recCount) * 100));
		            rel.setRecCountPercent((int) result);
	            }
	            catch(SQLException e){
	            	System.out.println("CATCHING SQLEXEPTION...");
	            	System.out.println(e.getSQLState());
	            	System.out.println(e.getMessage());
	            	
	            }
	            finally {
		            if (stmt != null) { stmt.close();}
		            if(rst != null){rst.close();}
					
				}
	    		
	    	}
	    }
	    
	    return new ArrayList<Relation>(map.values());
		
	}

}

