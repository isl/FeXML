/*
 * Copyright 2012-2017 Institute of Computer Science,
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

import gr.forth.ics.isl.fexml.utilities.Utils;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author samarita
 */
public class Service extends BasicServlet {

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

        String themasURL = request.getParameter("themasURL");
        String username = request.getParameter("username");
        String thesaurusName = request.getParameter("thesaurusName");
        String facetId = request.getParameter("facetId");
        String termId = request.getParameter("termId");

        Utils utils = new Utils();
        String serviceURL = "";
        String xsl = "";
//        System.out.println("TERM ID=" + termId);

        //Code added to test if termId supplied returns anything. If so, then it is not a facet id, but a term or hierarchy id
        serviceURL = themasURL + "SearchResults_Terms_Hierarchical?referenceId=" + termId + "&answerType=XMLSTREAM&external_user=" + username + "&external_thesaurus=" + thesaurusName;
        String themasServiceResponse = utils.consumeService(serviceURL);

        if (themasServiceResponse.length() == 0) {
            //Hierarchy
            serviceURL = themasURL + "SearchResults_Terms_Hierarchical?referenceId=" + facetId + "&answerType=XMLSTREAM&external_user=" + username + "&external_thesaurus=" + thesaurusName;
            xsl = baseURL + "/xsl/THEMAS/Thesaurus_Global_Hierarchical_View.xsl";

        } else {

            if (termId.equals("0")) {
                //Entire Facet
                serviceURL = themasURL + "hierarchysTermsShortcuts?referenceId=" + facetId + "&action=facethierarchical&answerType=XMLSTREAM&external_user=" + username + "&external_thesaurus=" + thesaurusName;
                xsl = baseURL + "/xsl/THEMAS/Thesaurus_Global_Hierarchical_View.xsl";
            } else {
                serviceURL = themasURL + "SearchResults_Terms_Hierarchical?referenceId=" + termId + "&answerType=XMLSTREAM&external_user=" + username + "&external_thesaurus=" + thesaurusName;
                xsl = baseURL + "/xsl/THEMAS/Hierarchical_Term_Display.xsl";

            }
//            System.out.println(serviceURL);
        }
        themasServiceResponse = utils.consumeService(serviceURL);

        String test = transform(themasServiceResponse, xsl);

        out.println(test);
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
