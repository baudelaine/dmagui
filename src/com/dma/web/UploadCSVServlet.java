package com.dma.web;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;


/**
 * Servlet implementation class AppendSelectionsServlet
 */
@WebServlet(name = "UploadCSV", urlPatterns = { "/UploadCSV" })
public class UploadCSVServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public UploadCSVServlet() {
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

			Path csv = null;
			
			if(ServletFileUpload.isMultipartContent(request)){
				
				List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
				for (FileItem item : items) {
					if (!item.isFormField()) {
						// item is the file (and not a field)
						csv = Paths.get(prj + "/" + item.getName());
						Files.copy(new BufferedInputStream(item.getInputStream()), csv, StandardCopyOption.REPLACE_EXISTING);
						csv.toFile().setReadable(true);

					}
				}
			}

			result.put("STATUS", "OK");
			result.put("MESSAGE", "CSV successfully saved in " + csv.toString());
			
			List<Map<String, Object>> datas = new ArrayList<Map<String,Object>>();
			
			LineNumberReader reader = new LineNumberReader(new BufferedReader(new FileReader(csv.toFile())));
			try{
				for (String line; ((line = reader.readLine()) != null) && reader.getLineNumber() <= 2; ) {
					Map<String, Object> data = new HashMap<String, Object>();
					switch(csv.getFileName().toString()) {
						case "tableLabel.csv":
							if(line.split(";").length != 2) {
								Files.deleteIfExists(csv);
								throw new ArrayIndexOutOfBoundsException();
							}
							data.put("tableName", line.split(";")[0]);
							data.put("tableLabel", line.split(";")[1]);
							break;
						case "tableDescription.csv":
							if(line.split(";").length != 2) {
								Files.deleteIfExists(csv);
								throw new ArrayIndexOutOfBoundsException();
							}
							data.put("tableName", line.split(";")[0]);
							data.put("tableDescription", line.split(";")[1]);
							break;
						case "columnLabel.csv":
							if(line.split(";").length != 3) {
								Files.deleteIfExists(csv);
								throw new ArrayIndexOutOfBoundsException();
							}
							data.put("tableName", line.split(";")[0]);
							data.put("columnName", line.split(";")[1]);
							data.put("columnLabel", line.split(";")[2]);
							break;
						case "columnDescription.csv":
							if(line.split(";").length != 3) {
								Files.deleteIfExists(csv);
								throw new ArrayIndexOutOfBoundsException();
							}
							data.put("tableName", line.split(";")[0]);
							data.put("columnName", line.split(";")[1]);
							data.put("columnDescription", line.split(";")[2]);
							break;
						default:
					}
					datas.add(data);
				}
			}
			finally{
				reader.close();
			}			
			
			result.put("DATAS", datas);
			
		}
		
		catch(java.lang.ArrayIndexOutOfBoundsException e) {
			result.put("STATUS", "KO");
			result.put("EXCEPTION", e.getClass().getName());
			result.put("MESSAGE", "Incorrect CSV format.");
			result.put("TROUBLESHOOTING", "Have a look at CSV format and e.g.");
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