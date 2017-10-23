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
import isl.dbms.DBFile;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.minidev.json.JSONObject;
import schemareader.Element;
import schemareader.SchemaFile;

/**
 *
 * @author samarita
 */
public class GetVars extends BasicServlet {

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

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String type = request.getParameter("type");
        String lang = request.getParameter("lang");
        String labelsAndDisplayValuesOnly = request.getParameter("labelsAndDisplayValuesOnly");
        DBFile nodesFile = new DBFile(BasicServlet.DBURI, applicationCollection + "/LaAndLi", type + ".xml", BasicServlet.DBuser, BasicServlet.DBpassword);
        String nodesQuery = "";
        System.out.println("");
        if (labelsAndDisplayValuesOnly == null) {//get all vars
            nodesQuery = "for $node in //node[@type='" + type + "']"
                    + " return"
                    + " <label>"
                    + "{$node/@display}"
                    + " {$node/xpath}"
                    + " <lang>{$node/" + lang + "/string()}</lang>"
                    + " </label>";
        } else {//get only labels and displayValues
            nodesQuery = "for $node in //node[@type='" + type + "']"
                    + " return"
                    + " <label>"
                    + "{$node/@display}"
                    + " <lang>{$node/" + lang + "/string()}</lang>"
                    + " </label>";
//            nodesQuery = "//node[@type='" + type + "']/" + lang + "/string()";
        }
        String[] nodes = nodesFile.queryString(nodesQuery);

        ArrayList<Element> elements = new ArrayList<Element>();
        SchemaFile sch = new SchemaFile(schemaFolder + java.io.File.separator + type + ".xsd");
        try {
            elements = sch.getElements(type);
        } catch (NullPointerException ex) { //Old type entities (Authentic)
            elements = sch.getElements("Οντότητα");
        }
        ArrayList<String> minOccurs = new ArrayList<String>();
        ArrayList<String> maxOccurs = new ArrayList<String>();
        ArrayList<String> displayValues = new ArrayList<String>();

        ArrayList<String> xpaths = new ArrayList<String>();
        ArrayList<String> labels = new ArrayList<String>();

                Utils utils = new Utils();

        
        for (int i = 0; i < nodes.length; i++) {
            labels.add(utils.getMatch(nodes[i], "(?<=<lang>)[^<]+(?=</lang>)"));

            if (labelsAndDisplayValuesOnly == null) {//get all vars
                xpaths.add(utils.getMatch(nodes[i], "(?<=<xpath>)[^<]+(?=</xpath>)"));
            }
            minOccurs.add("" + elements.get(i).getMinOccurs());
            maxOccurs.add("" + elements.get(i).getMaxOccurs());
            if (nodes[i].contains("display=\"none\"")) {
                displayValues.add("hidden");
            } else {
                displayValues.add("visible");
            }
        }

        try {
            /* TODO output your page here. You may use following sample code. */
            JSONObject jsonData = new JSONObject();
            jsonData.put("minOccurs", minOccurs);
            jsonData.put("maxOccurs", maxOccurs);
            jsonData.put("displayValues", displayValues);

            if (labelsAndDisplayValuesOnly == null) {
                jsonData.put("xpaths", xpaths);
            }
            jsonData.put("labels", labels);
            out.print(jsonData.toString());
        } finally {
            out.close();
        }
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
