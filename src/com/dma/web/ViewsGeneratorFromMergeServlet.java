package com.dma.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servlet implementation class AppendSelectionsServlet
 */
@WebServlet(name = "ViewsGeneratorFromMerge", urlPatterns = { "/ViewsGeneratorFromMerge" })
public class ViewsGeneratorFromMergeServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	Map<String, QuerySubject> query_subjects;
	Map<String, Integer> gRefMap;
	Map<String, QuerySubject> query_subjects_views;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ViewsGeneratorFromMergeServlet() {
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
			
			if(parms != null) {
				
				String data = (String) parms.get("data");
				List<QuerySubject> qsList = new ArrayList<QuerySubject>();
				
				try {
					ObjectMapper mapper = new ObjectMapper();
			        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			        qsList = Arrays.asList(mapper.readValue(data, QuerySubject[].class));
				}
				catch (Exception e) {
					result.put("STATUS", "KO");
					result.put("ERROR", "data input parameter is not a valid QuerySubject list.");
					throw new Exception();
				}
		        
		        query_subjects = new HashMap<String, QuerySubject>();
		        Map<String, Integer> recurseCount = new HashMap<String, Integer>();
		        
		        for(QuerySubject qs: qsList){
		        	query_subjects.put(qs.get_id(), qs);
		        	recurseCount.put(qs.getTable_alias(), 0);
		        }				
				
		        query_subjects_views = new HashMap<String, QuerySubject>();
		        gRefMap = new HashMap<String, Integer>();
		        
		        
				//scan final views
				for(Entry<String, QuerySubject> query_subject: query_subjects.entrySet()){
					
					if (query_subject.getValue().getType().equalsIgnoreCase("Final")){
						
						//Views
						if(!query_subject.getValue().getMerge().equals("")) {
							String viewsTab[] = StringUtils.split(query_subject.getValue().getMerge(), ";");
							
							for (int i=0;i<viewsTab.length;i++) {
								String viewName = viewsTab[i];
								QuerySubject qsView = new QuerySubject();
								qsView.set_id(viewName);
								qsView.setTable_name(viewName);
								qsView.setTable_alias(viewName);
								qsView.setType("Final");
								if (query_subjects_views.get(qsView.getTable_alias()) == null) {
									query_subjects_views.put(qsView.getTable_alias(), qsView);
								}		
							}
						}
						//End Views
					}
				}
				//scan final views
				for(Entry<String, QuerySubject> query_subject: query_subjects.entrySet()){
					
					if (query_subject.getValue().getType().equalsIgnoreCase("Final")){
						
						//Views
						if(!query_subject.getValue().getMerge().equals("")) {
							String viewsTab[] = StringUtils.split(query_subject.getValue().getMerge(), ";");
							
							for (int i=0;i<viewsTab.length;i++) {
								String viewName = viewsTab[i];
								QuerySubject qsView = new QuerySubject();
								qsView.set_id(viewName);
								qsView.setTable_name(viewName);
								qsView.setTable_alias(viewName);
								qsView.setType("Final");
								if (query_subjects_views.get(qsView.getTable_alias()) == null) {
									query_subjects_views.put(qsView.getTable_alias(), qsView);
								}		
							}
						}
						//End Views
					}
				}
				//scan final view
								
				for(Entry<String, QuerySubject> query_subject: query_subjects.entrySet()){
					
					if (query_subject.getValue().getType().equalsIgnoreCase("Final")){
						//ajout filter
						String filterNameSpaceSource = "[FINAL]";
						
						//lancement f1 ref
						for(QuerySubject qs: qsList){
				        	recurseCount.put(qs.getTable_alias(), 0);
				        }
						
						if (!query_subject.getValue().getMerge().equals("")) {
							f1(query_subject.getValue().getTable_alias(), query_subject.getValue().getTable_alias(), "", "[DATA].[" + query_subject.getValue().getTable_alias() + "]", query_subject.getValue().getTable_alias(), recurseCount, "Final", filterNameSpaceSource, query_subject.getValue().getMerge(), "");
						}
						//end f1		

						for(Field field: query_subject.getValue().getFields()) {
							
							//views
							if(!query_subject.getValue().getMerge().equals("") && !field.isHidden()) {
								String viewsTab[] = StringUtils.split(query_subject.getValue().getMerge(), ";");
								
								for (int i=0;i<viewsTab.length;i++) {
									String viewName = viewsTab[i];
									QuerySubject qsView = query_subjects_views.get(viewName);
									Field f = new Field();
									f = field;
									f.set_id(query_subject.getValue().getTable_alias() + "." + field.getField_name());
									f.setField_name(field.getField_name());
									String ex = "[DATA].[" + query_subject.getValue().getTable_alias() + "].[" + field.getField_name() + "]";
									f.setExpression(ex);
									f.setRole("Field");
									qsView.addField(f);
								}
							}
							//end views	
						}
					}
					
				}
						
//				// travail vues Résultats
//				for(Entry<String, QuerySubject> query_subject: query_subjects_views.entrySet()){
//					System.out.println(query_subject.getValue().get_id());
//					for(Field field: query_subject.getValue().getFields()) {
//						System.out.println(query_subject.getValue().get_id() + " *** " + field.get_id() + " * * " + field.getField_name() + " * * * * " + field.getExpression());
//					}
//				}

				/* Ta sortie */
				result.put("DATAS", query_subjects_views);
				result.put("MESSAGE", "Views generatde successfully");
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

	protected void f1(String qsAlias, String qsAliasInc, String gDirName, String qsFinal, String qsFinalName, Map<String, Integer> recurseCount, String qSleftType, String leftFilterNameSpace, String mergeView, String gDirNameView) {
		
		Map<String, Integer> copyRecurseCount = new HashMap<String, Integer>();
		copyRecurseCount.putAll(recurseCount);
		
		QuerySubject query_subject;
		if (!qSleftType.equals("Final")) {
			
			query_subject = query_subjects.get(qsAlias + qSleftType);
			
			int j = copyRecurseCount.get(qsAlias);
			if(j == query_subject.getRecurseCount()){
				return;
			}
			copyRecurseCount.put(qsAlias, j + 1);
		
		} else {
			query_subject = query_subjects.get(qsAlias + "Final");
		}

		for(Relation rel: query_subject.getRelations()){
			if(rel.isRef()){
				
				String namespaceID = "";
				String namespaceName = "";
				namespaceID = "Ref";
				namespaceName = "REF";
				
								
				String pkAlias = rel.getPktable_alias();
				Integer i = gRefMap.get(pkAlias);
				
				if(i == null){
					gRefMap.put(pkAlias, new Integer(0));
					i = gRefMap.get(pkAlias);
				}
				gRefMap.put(pkAlias, i + 1);

				//seq
				String gFieldName = "";
				String gDirNameCurrent = "";
				String gDirNameCurrentView = "";
				
				if(rel.getKey_type().equalsIgnoreCase("P") || rel.isNommageRep()){
					if (qSleftType.equals("Final")) {
						gFieldName = pkAlias;
						gDirNameCurrent = "." + pkAlias;
						gDirNameCurrentView = "." + pkAlias;
					} else {
						gFieldName = gDirName.substring(1) + "." + pkAlias;
						gDirNameCurrent = gDirName + "." + pkAlias;
						gDirNameCurrentView = gDirNameView + "." + pkAlias;
					}
					
				}
				else {
					if (qSleftType.equals("Final")) {
						gFieldName = rel.getAbove();
						gDirNameCurrent = "." + rel.getAbove();
						gDirNameCurrentView = "." + rel.getAbove();
					} else {
						gFieldName = gDirName.substring(1) + "." + rel.getAbove();
						gDirNameCurrent = gDirName + "." + rel.getAbove();
						gDirNameCurrentView = gDirNameView + "." + rel.getAbove();
					}
				}				
				
				String filterNameSpaceSource = "[" + namespaceName+ "]";
				if (namespaceID.equals("Ref")) {
					
					//Ajout du field de type RefView dans la vue de type Final
					if(!query_subjects.get(pkAlias + namespaceID).getMerge().equals("") && !query_subjects.get(pkAlias + namespaceID).getMerge().contains(mergeView)) {
						QuerySubject qsview = query_subjects_views.get(mergeView);
						Field f = new Field();
						f.set_id(qsFinalName + gDirNameCurrent);
						f.setField_name("(" + query_subjects.get(pkAlias + namespaceID).getMerge() + ") " + qsFinalName + gDirNameCurrent);
						String ex = "[DATA].[" + qsFinalName + "].[" + gDirNameCurrent.substring(1);
						f.setExpression(ex);
						f.setRole("FolderRefView");
//						System.out.println(f.get_id() + " * * * " + f.getRole());
						qsview.addField(f);
						mergeView = query_subjects.get(pkAlias + namespaceID).getMerge();
						gDirNameCurrentView = "*";
					}
					//End linked view
										
					//QsViews create Ref view
					if(!query_subjects.get(pkAlias + namespaceID).getMerge().equals("")) {
						String viewsTab[] = StringUtils.split(query_subjects.get(pkAlias + namespaceID).getMerge(), ";");
						
						for (int j=0;j<viewsTab.length;j++) {
														
							String viewName = viewsTab[j];
							QuerySubject qsView = new QuerySubject();
							qsView.set_id(viewName);
							qsView.setTable_name(viewName);
							qsView.setTable_alias(viewName);
							qsView.setType("Ref");
							if (query_subjects_views.get(qsView.getTable_alias()) == null) {
								query_subjects_views.put(qsView.getTable_alias(), qsView);
								mergeView = query_subjects.get(pkAlias + namespaceID).getMerge();
							}
						}
					}
					//end QsViews
					String viewsTab[] = StringUtils.split(query_subjects.get(pkAlias + namespaceID).getMerge(), ";");
					
					for(Field field: query_subjects.get(pkAlias + namespaceID).getFields()){
						
						if (rel.isRef()) {
		
							//views field
							if(!query_subjects.get(pkAlias + namespaceID).getMerge().equals("") && !field.isHidden()) {
								
								for (int j=0;j<viewsTab.length;j++) {
									String viewName = viewsTab[j];
									if (viewName.equals(mergeView)) {
										QuerySubject qsView = query_subjects_views.get(viewName);
										Field f = new Field();
										//Copie des éléments du field afin d'éviter la copie d'objet qui n'est pas adapté ! f = field redonne une référence existante 
										//lorsqu'on repasser par un field ou nous sommes déjà passé. 
										f.setField_type(field.getField_type());
										f.setPk(field.isPk());
										f.setIndexed(field.isIndexed());
										f.setLabel(field.getLabel());
										f.setField_size(field.getField_size());
										f.setNullable(field.getNullable());
										f.setTraduction(field.isTraduction());
										f.setHidden(field.isHidden());
										f.setTimezone(field.isTimezone());
										f.setIcon(field.getIcon());
										f.setDisplayType(field.getDisplayType());
										f.setDescription(field.getDescription());
										f.setLabels(field.getLabels());
										f.setDescriptions(field.getDescriptions());
										f.setMeasure(field.getMeasure());
										f.setCustom(field.isCustom());
										f.setRole("Field");
										
										if (qsView.getType().equals("Final")) {
											f.set_id(qsFinalName + gDirNameCurrent + "." + field.getField_name());
											f.setField_name(field.getField_name());
											String ex = "[DATA].[" + qsFinalName + "].[" + gFieldName + "." + field.getField_name() + "]";
											f.setExpression(ex);
										} else {
											f.set_id(gDirNameCurrentView + "." + field.getField_name());
											f.setField_name(field.getField_name());
											String ex = gDirNameCurrentView + "." + field.getField_name() + "]";
											f.setExpression(ex);		
										}
										Boolean addField = true;
										for(Field ff: qsView.getFields()) {
											if (ff.get_id().equals(f.get_id())) {
												addField = false;
											}
										}
										if (addField) {
											qsView.addField(f);
										}			
									}
								}
							}
							//end views field							
						}
					}
					
				}
				//end only for Ref

				if (!query_subject.getMerge().equals("")) {
					f1(pkAlias, qsFinalName + gDirNameCurrent, gDirNameCurrent, qsFinal, qsFinalName, copyRecurseCount, namespaceID, filterNameSpaceSource, mergeView, gDirNameCurrentView);
				}
			}
		}
	}	
}
