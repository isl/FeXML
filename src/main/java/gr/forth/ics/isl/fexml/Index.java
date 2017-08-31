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

import isl.dbms.DBCollection;
import isl.dbms.DBFile;
import isl.dbms.DBMSException;
import isl.dms.DMSException;
import isl.dms.file.DMSTag;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import schemareader.SchemaFile;
import java.io.File;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author samarita
 */
public class Index extends BasicServlet {

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
        String output = request.getParameter("output");
        if (output == null || output.equals("") || output.equals("html")) {
            response.setContentType("text/html;charset=UTF-8");
            output = "html";
        } else {
            response.setContentType("text/xml;charset=UTF-8");

        }
        PrintWriter out = response.getWriter();

        String xslId = "/xsl/Index.xsl";
        String id = request.getParameter("id");
        String type = request.getParameter("type");
        String depth = request.getParameter("depth");
        String file = request.getParameter("file");
        String lang = request.getParameter("lang");
        String action = request.getParameter("action");
        String attributes = request.getParameter("attributes");

        if (action == null) {
            action = "";
        }

        if (lang == null && type == null && id == null && file == null) {
            response.sendRedirect("Index?type=Person&id=1&lang=gr");
        }
        if (lang == null) {
            lang = "gr";
        }
        if (id == null) {
            id = "1";
        }
        if (type == null) {
            type = "Person";
        }
        id = id.replace(type, "");

        StringBuilder xmlMiddle = new StringBuilder("<output><xml>");

        if (output.equals("xml")) {
            String xmlId = "";
            if (file != null) {
                xmlId = file + ".xml";
                DBFile dbf = new DBFile(super.DBURI, applicationCollection + "/LaAndLi", xmlId, super.DBuser, super.DBpassword);
                xmlMiddle.append(dbf.getXMLAsString());

            } else {

                xmlId = type + id + ".xml";
                DBCollection dbc = new DBCollection(super.DBURI, applicationCollection + "/" + type, super.DBuser, super.DBpassword);
                String collectionPath = getPathforFile(dbc, xmlId, id);
                xmlMiddle.append(getDBFileContent(collectionPath, xmlId));
            }

        }
        xmlMiddle.append("</xml>");
        if (output.equals("html")) {
            String xsl = super.baseURL + xslId;

            if (stateOfSite.equals("off")) {
                action = "view";
                file = "Message_" + lang;
            }

            if (depth == null || depth.equals("")) {
                depth = "1";
            }
            if (action.equals("view")) {//view mode is 1
                xmlMiddle.append("<viewMode>").append("1").append("</viewMode>");

                xmlMiddle.append("<versions>").append(request.getParameter("versions")).append("</versions>");
                xmlMiddle.append("<collectionID>").append(request.getParameter("collectionID")).append("</collectionID>");
                xmlMiddle.append("<xmlId>").append(request.getParameter("xmlId")).append("</xmlId>");
                xmlMiddle.append("<entityType>").append(request.getParameter("entityType")).append("</entityType>");
            } else if (action.equals("editAdmin")) {//introducing editAdmin mode 2 to let only admin mess with labels and tag visibility
                xmlMiddle.append("<viewMode>").append("2").append("</viewMode>");
            } else {
                xmlMiddle.append("<viewMode>").append("0").append("</viewMode>");
            }
            xmlMiddle.append("<depth>").append(depth).append("</depth>");
            if (attributes != null) {
                xmlMiddle.append("<attributes/>");
            }
            xmlMiddle.append("<lang>").append(lang).append("</lang>");
            xmlMiddle.append("<editorType>").append(editorType).append("</editorType>");
            xmlMiddle.append("<synthesisName>").append(synthesisURL.substring(1)).append("</synthesisName>");
            xmlMiddle.append("<schemaLastVersion>").append(schemaLastVersion).append("</schemaLastVersion>");

            xmlMiddle.append("<URIReferencePath>").append(URI_Reference_Path).append("</URIReferencePath>");
//            if (action.equals("editAdmin")) {
            xmlMiddle.append("<dynamicLabels>").append(dynamicLabels).append("</dynamicLabels>");
//            }
            String joinedString = StringUtils.join(entityWithPhoto, ",");

            xmlMiddle.append("<entityWithPhoto>").append(joinedString).append("</entityWithPhoto>");

            DBFile nodesFile = null;

            if (file != null) {
                xmlMiddle.append("<file>").append(file).append("</file>");
            } else {

                xmlMiddle.append("<type>").append(type).append("</type>");
                xmlMiddle.append("<id>").append(id).append("</id>");

                try {
                    nodesFile = new DBFile(BasicServlet.DBURI, applicationCollection + "/LaAndLi", type + ".xml", BasicServlet.DBuser, BasicServlet.DBpassword);

                    //Should be only one
                } catch (DBMSException ex) {

                    if (new File(schemaFolder + type + ".xsd").exists()) {
                        SchemaFile sch = new SchemaFile(schemaFolder + type + ".xsd");

                        System.out.println(type);
                        //Hard wired en...
                        String content = sch.createLabelsAndLinksFile(type, type, "en");
                        DBCollection nodesCol = new DBCollection(BasicServlet.DBURI, applicationCollection + "/LaAndLi", BasicServlet.DBuser, BasicServlet.DBpassword);
                        nodesFile = nodesCol.createFile(type + ".xml", "XMLDBFile");
                        nodesFile.setXMLAsString(content);
                        nodesFile.store();
                    } else {
                        //TODO
                        xmlMiddle.append("<fail/>");
                    }
                }

            }
            String uri_name = "";
            try {
                uri_name = DMSTag.valueOf("uri_name", "target", type, this.conf)[0];
            } catch (DMSException ex) {
                ex.printStackTrace();
            }
            xmlMiddle.append("<uri_name>").append(uri_name).append("</uri_name>");
            xmlMiddle.append("</output>");
            out.write(transform(xmlMiddle.toString(), xsl));
        } else {
            xmlMiddle.append("</output>");
            out.write(xmlMiddle.toString());
        }

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
            throws ServletException,
            IOException {
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
            throws ServletException,
            IOException {
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
