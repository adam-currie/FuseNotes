/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.adam_currie.fusenotesserver;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 *
 * @author Adam Currie
 */
public class NoteServlet extends GenericServlet{

    @Override
    public String getServletInfo(){
        return "TODO: Short description";
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException{
        response.setContentType("text/html;charset=UTF-8");
        try(PrintWriter out = response.getWriter()){
            /*
             * TODO output your page here. You may use following sample code.
             */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet NoteServlet</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet NoteServlet at " + "</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }

}
