package com.dma.web;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class GetImportedKeysServlet
 */
@WebServlet("/OpenModel")
public class OpenModelServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public OpenModelServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub

		String model = request.getParameter("model");
		
		System.out.println("**********modelName=" + model);
		
		Path path = Paths.get(request.getSession().getAttribute("projectPath") + "/models/" + model);
		
		BufferedReader br = new BufferedReader(new FileReader(path.toFile()));
		
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		String line;
		while((line = br.readLine()) != null){
			response.getWriter().write(line);
		}

		if(br != null){br.close();};
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}

