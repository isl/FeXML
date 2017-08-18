/*
 * Copyright 2012-2015 Institute of Computer Science,
 * Foundation for Research and Technology - Hellas
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations
 * under the Licence.
 *
 * Contact:  POBox 1385, Heraklio Crete, GR-700 13 GREECE
 * Tel:+30-2810-391632
 * Fax: +30-2810-391638
 * E-mail: isl@ics.forth.gr
 * http://www.ics.forth.gr/isl
 *
 * Authors : Georgios Samaritakis, Konstantina Konsolaki.
 *
 * This file is part of the FeXML webapp.
 */
package gr.forth.ics.isl.fexml;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author samarita
 */
public class LaAndLi extends BasicServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String xpath = request.getParameter("xpath");
        String type = request.getParameter("type");
        String action = request.getParameter("action");

        //Generic language handling
        String labels[] = request.getParameterValues("labels[]");

        StringBuilder output = new StringBuilder();
        if (action == null) {//get/set labels 
            if (labels == null) {
                String fullNodeBlock = getAllLabelsFromXPath(type, xpath);
                ArrayList<String> langLabels = new ArrayList<String>();

                for (String lang : languages) {
                    langLabels = findReg("(?<=<" + lang + ">)[^<]*(?=</" + lang + ">)", fullNodeBlock, 0);
                    String langLabel;
                    if (!langLabels.isEmpty()) {
                        langLabel = langLabels.get(0);

                    } else {
                        langLabel = "";
                    }
                    output = output.append("<label for='").append(lang).append("'>").append(lang).append(": </label>");
                    output = output.append("<input id='").append(lang).append("' type='text' size='30' name='").append(lang).append("' value='").append(langLabel).append("'/><br/>");
                }
            } else {
                for (int i = 0; i < languages.length; i++) {
                    setSpecificLanguageLabelFromXPath(type, xpath, languages[i], labels[i]);
                }
            }
        } else {//toggle display
            System.out.println("ACTION is:" + action);
            System.out.println("XPATH is=" + xpath);
            System.out.println("TYPE is=" + type);
            toggleSpecificNodeDisplayFromXPath(type, xpath, action);

        }

        out.println(output);
        out.close();

    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
