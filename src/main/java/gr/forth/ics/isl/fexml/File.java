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
 * Authors : Georgios Samaritakis, Konstantina Konsolaki 
 * 
 * This file is part of the FeXML webapp.
 */
package gr.forth.ics.isl.fexml;

import gr.forth.ics.isl.fexml.utilities.Utils;
import isl.dbms.DBCollection;
import isl.dbms.DBFile;
import isl.dms.DMSException;
import isl.dms.file.DMSTag;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import schemareader.SchemaFile;
import timeprimitve.SISdate;

/**
 *
 * @author samarita
 */
public class File extends BasicServlet {

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

//        if (action.equals("Close")) {
//            response.setContentType("text/html;charset=UTF-8");
//        } else {
        response.setContentType("application/xml;charset=UTF-8");
//        }

        String action = request.getParameter("action");
        if (action == null) {
            action = "";
        }

        PrintWriter out = response.getWriter();

        String xml = request.getParameter("xmlString");
//        System.out.println(xml);
        //XM not pretty but efficient...
        if (xml != null) {
            xml = xml.replaceAll("parentRefIndex=\"\\d++\"", "");
            xml = xml.replaceAll("\\s*>\\s+<", ">\r\n<");
            //To remove divs added by Chrome...
            //xml = xml.replaceAll("</div>", "<br/>").replaceAll("</?div>", "");

            //<style>.*?</style>
            //<(?!/?\b(b|i|a|br|u)\b)[^<>]+>
//Test clean dirty Word
            //      xml = xml.replaceAll("(?s)<style>.*?</style>", "").replaceAll("<(?!/?\\b(b|i|a|br|u)\\b)[^<>]+>", "");
            // System.out.println("XML=" + xml);
        }
//        String queryString = URLDecoder.decode(request.getParameter("xmlString"), "ISO-8859-1");
//        System.out.println("QS="+queryString);

        String type = request.getParameter("type");
        String id = request.getParameter("id");
        String file = request.getParameter("file");
        //     String depth = request.getParameter("depth");
        String lang = request.getParameter("lang");
//        System.out.println("LANG is "+lang);
        String newTerm = request.getParameter("newTerm");
        String vocFile = request.getParameter("vocFile");
        int vocId = -1;

        //   System.out.println(xml);
        String xmlId = "";

//        System.out.println(type);
//        System.out.println(id);
        try {
            if (action.equals("Save")) {
                DBFile dbf = null;
                if (file != null) {
                    xmlId = file + ".xml";
//                    System.out.println(xmlId);
                    dbf = new DBFile(super.DBURI, applicationCollection + "/LaAndLi", xmlId, super.DBuser, super.DBpassword);
                    dbf.setXMLAsString(cleanHTML(xml));
                    dbf.store();
                } else {
                    xmlId = type + id + ".xml";
                    DBCollection dbc = new DBCollection(super.DBURI, applicationCollection + "/" + type, super.DBuser, super.DBpassword);
                    String collectionPath = getPathforFile(dbc, xmlId, id);
                    dbf = new DBFile(super.DBURI, collectionPath, xmlId, super.DBuser, super.DBpassword);

                    //Extra Logic for Games/Panoramas only
                    String zipFilename = "";

                    if (type.equals("Game") || type.equals("Video")) {

                        String[] zipFiles = dbf.queryString("//" + type + "/DigitalFile/File/string()");
                        if (zipFiles != null) {
                            if (zipFiles.length > 0) {
                                zipFilename = zipFiles[0];
                            }
                        }
                    }

                    dbf.setXMLAsString(cleanHTML(xml));
                    dbf.store();
                    dbf.xUpdate("//admin/saved", "yes");
                    if (Arrays.asList(BasicServlet.entityWithPhoto).contains(type)) {
                        String[] res = null;
                        String xpath = "/" + DMSTag.valueOf("xpath", "upload", type, this.conf)[0] + "/text()"; //exume valei sto DMSTags.xml ths exist ena neo element upload sta pedia pu xreiazetai na vazume to type sto admin part
                        res = dbf.queryString(xpath);

                        if (res.length > 0) {
                            String filename = res[0];
                            DBFile uploadsDBFile = new DBFile(this.DBURI, this.adminCollection, "Uploads.xml", this.DBuser, this.DBpassword);
                            String mime = Utils.findMime(uploadsDBFile, filename);
                            dbf.xRemove("//admin/type");
                            dbf.xInsertAfter("//admin/versions", "<type>" + mime + "</type>");
                        }
                    }

//                    if (type.equals("Archive")) {
//                        String[] res = dbf.queryString("//ΨηφιακόΑρχείο/text()");
//                        if (res.length > 0) {
//                            String filename = res[0];
//                            DBFile uploadsDBFile = new DBFile(this.DBURI, this.adminCollection, "Uploads.xml", this.DBuser, this.DBpassword);
//                            String mime = Utils.findMime(uploadsDBFile, filename);
//                            dbf.xRemove("//admin/type");
//                            dbf.xInsertAfter("//admin/versions", "<type>" + mime + "</type>");
//                        }
//                    }
                    updateReferences(dbf, id, type);
//                    System.out.println(zipFilename);
//                    System.out.println(type);
                    //Extra Logic for Games/Panoramas only
                    if (type.equals("Game") || type.equals("Video")) {
                        String[] zipFiles = dbf.queryString("//" + type + "/DigitalFile/File/string()");
                        if (zipFiles != null) {
                            if (zipFiles.length > 0) {
                                String zipFile = zipFiles[0];
//                                System.out.println(zipFile);
                                if (!zipFile.equals(zipFilename)) {
                                    String zipFolder = zipFile.substring(0, zipFile.lastIndexOf("."));
//                                    System.out.println("ZIPFOLDER=" + zipFolder);

                                    if (type.equals("Video")) {
                                        type = type + java.io.File.separator + type;
                                    }
                                    //Which reminds me: NEVER EVER USE CLASSIC NAMES FOR YOUR SERVLET (File was an epic fail...)
//                                java.io.File gamesFolder = new java.io.File(uploadsFolder + type);
//                                System.out.println(uploadsFolder + type + java.io.File.separator + zipFolder);
                                    java.io.File newFolder = new java.io.File(uploadsFolder + type + java.io.File.separator + zipFolder);
//                                  System.out.println(uploadsFolder + type + java.io.File.separator + id);
                                    FileUtils.deleteDirectory(new java.io.File(uploadsFolder + type + java.io.File.separator + id));
                                    newFolder.renameTo(new java.io.File(uploadsFolder + type + java.io.File.separator + id));

                                }
                            }
                        }
                    }
                }

                fixTime(dbf);
            } else if (action.equals("Open")) {
                DBFile dbf = null;
                if (file != null) {
                    xmlId = file + ".xml";
                    dbf = new DBFile(super.DBURI, applicationCollection + "/LaAndLi", xmlId, super.DBuser, super.DBpassword);
                } else {
                    //System.out.println("2 " + request.getParameter("versions"));

                    String versions = request.getParameter("versions");
                    if (versions == null) {
                        versions = "";
                    }

                    if (versions.equals("yes")) {
                        //tomorrow here code
                        String collectionID = request.getParameter("collectionID");
                        String vId = request.getParameter("xmlId");
                        String entityType = request.getParameter("entityType");

                        String collectionPath = this.versionCollection + entityType + "/" + vId + "/" + collectionID;
                        dbf = new DBFile(super.DBURI, collectionPath, vId + ".xml", super.DBuser, super.DBpassword);

                    } else {
                        xmlId = type + id + ".xml";
                        DBCollection dbc = new DBCollection(super.DBURI, applicationCollection + "/" + type, super.DBuser, super.DBpassword);
                        String collectionPath = getPathforFile(dbc, xmlId, id);
                        dbf = new DBFile(super.DBURI, collectionPath, xmlId, super.DBuser, super.DBpassword);
                    }
                }
                String test = cleanHTML(dbf.getXMLAsString());
                out.append(test);
//                out.append(dbf.getXMLAsString().replaceAll("<style>.*?</style>", "").replaceAll("<(?!/?\\b(b|i|a|br|u)\\b)[^<>]+>", ""));
                // System.out.println(out.toString());
//
                //System.out.println(dbf.getXMLAsString().replaceAll("(</?)div", "").replaceAll("</?div>", ""));
                //dbf.store();
            } else if (action.equals("New")) {

                SchemaFile sch = new SchemaFile(schemaFolder + type + ".xsd");
                String template = "";
                // System.out.println(sch.getElements());
                boolean legacyMode = false;
                if (sch.getElements().contains(type)) {
                    template = sch.createXMLSubtree(type, "medium");
                } else {
                    legacyMode = true;
                    template = sch.createXMLSubtree("Οντότητα", "maximum");
                }

                String[] res = this.initInsertFile(type);
                DBCollection actualCol = new DBCollection(super.DBURI, res[0], super.DBuser, super.DBpassword);
                String newId = res[1];

                xmlId = type + newId + ".xml";
                DBFile dbf = actualCol.createFile(xmlId, "XMLDBFile");
                dbf.setXMLAsString(template);
                dbf.store();
                dbf.xUpdate("//admin/id", newId + "");
                dbf.xUpdate("//admin/lang", lang);

                dbf.xUpdate("//admin/organization", getUserGroup());
                dbf.xUpdate("//admin/creator", username);
                if (editorType.equals("standalone")) {
                    dbf.xUpdate("//admin/saved", "yes");
                } else {
                    dbf.xUpdate("//admin/saved", "no");
                }
                dbf.xUpdate("//admin/read", "*");
                dbf.xUpdate("//admin/locked", username);
                dbf.xUpdate("//admin/write", username);

                String uri_name = DMSTag.valueOf("uri_name", "target", type, this.conf)[0]; //exume valei sto DMSTags.xml ths exist ena neo element uri_name
                String uriValue = this.URI_Reference_Path + uri_name + "/" + newId;
                dbf.xUpdate("//admin/uri_id", uriValue);
                if (!editorType.equals("standalone")) {
                    //Provided by konsolak...
                    dbf.xUpdate("//admin/versions/versionId", "1");
                    dbf.xUpdate("//admin/versions/versionUser", username);
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = new Date();
                    dbf.xUpdate("//admin/versions/versionDate", dateFormat.format(date));
                }

                if (!legacyMode) {
                    dbf.xUpdate("//IdentificationNumber[@sps_fixed]", uriValue);

                } else {
                    String uriPath = Utils.getPathUriField(type);
                    if (!uriPath.equals("")) {
                        dbf.xUpdate(uriPath, uriValue);
                    }
                    dbf.xRemove("//admin/refs_by");
                    dbf.xRemove("//admin/type");
                    dbf.xRemove("//admin/versions/numOfVersions");
                    dbf.xRemove("//admin/versions/webVersion");
                    // String adminPart = "<admin><id>" + newId + "</id><lang>" + lang + "</lang><creator/><saved/><locked/><status/></admin>";
                    // dbf.xUpdate("//Οντότητα/*", adminPart);
                }

                response.sendRedirect("Index?type=" + type + "&id=" + type + newId + "&lang=" + lang);
                //
                // dbf.storeInto(actualCol);
            } else if (action.equals("SaveVocTerm")) {
                DBFile dbf = null;
//                DBCollection VocDbc = new DBCollection(super.DBURI, applicationCollection + "/" + "Vocabulary", super.DBuser, super.DBpassword);
                String collectionPath = applicationCollection + "/" + "Vocabulary/" + lang + "/";
                dbf = new DBFile(super.DBURI, collectionPath, vocFile, super.DBuser, super.DBpassword);
                vocId = newId(dbf);
                addTerm(dbf, vocId, newTerm);
            } else if (action.equals("Close")) {
                xmlId = type + id + ".xml";
                DBCollection dbc = new DBCollection(super.DBURI, applicationCollection + "/" + type, super.DBuser, super.DBpassword);
                String collectionPath = getPathforFile(dbc, xmlId, id);
                DBFile dbf = new DBFile(super.DBURI, collectionPath, xmlId, super.DBuser, super.DBpassword);

                String[] savedTable = dbf.queryString("//admin/saved/string()");
                if (savedTable != null) {
                    if (savedTable.length > 0) {
                        System.out.println("SAVED=" + savedTable[0]);
                        if (savedTable[0].equals("no")) {
                            dbf.remove();
                            System.out.println("Removing File!");

                        } else {

                            dbf.xUpdate("//admin/locked/text()", "no");
                            System.out.println("Closing File!");
                        }
                    }
                }

//                System.out.println(dbf.toString());
            } else if (action.equals("references")) {
                String refs = "";
                String refs_by = "";

                if (lang.equals("gr")) {
                    refs = "Αναφέρει";
                    refs_by = "Αναφέρεται από";

                } else {
                    refs = "References";
                    refs_by = "Reference by";
                }

                String html = "<ul style='background-color:aliceblue;' id='references' class='children collapsable'>";
                xmlId = file + ".xml";
                DBCollection dbc = new DBCollection(super.DBURI, applicationCollection + "/" + type, super.DBuser, super.DBpassword);
                String collectionPath = getPathforFile(dbc, xmlId, id);
                DBFile dbf = new DBFile(super.DBURI, collectionPath, type + id + ".xml", super.DBuser, super.DBpassword);

                String query = "let $refs_by := for $b in //admin/refs_by/ref_by[@sps_type!='' and @sps_id!=\"\"]\n order by $b/@sps_type , $b/@sps_id return $b \n"
                        + "let $refs := for $b in  //admin/refs/ref[@sps_type!='' and @sps_id!=\"\"]\n order by $b/@sps_type, $b/@sps_id return $b \n"
                        + "return \n"
                        + "<res>\n"
                        + "	<refs_by>{$refs_by}</refs_by>\n"
                        + "	<refs>{$refs}</refs>\n"
                        + "</res>";
                System.out.println(query);
                String res = dbf.queryString(query)[0];
                Element e = Utils.getElement(res);
                NodeList ref_by = e.getElementsByTagName("ref_by");
                html += "<li id=" + type + "/admin/refs_by class='collapsable'>";
                html += "<div class=\"hitarea\"></div>";
                html += "<span class=\"nodeName\">" + refs_by + "</span>";

                html += "<ul>";
                for (int i = 0; i < ref_by.getLength(); i++) {

                    try {
                        NamedNodeMap attrs = ref_by.item(i).getAttributes();
                        String sps_id = attrs.getNamedItem("sps_id").getNodeValue();
                        String sps_type = attrs.getNamedItem("sps_type").getNodeValue();
                        String xpath = DMSTag.valueOf("xpath", "title", sps_type, this.conf)[0];
                        dbc = new DBCollection(super.DBURI, applicationCollection + "/" + sps_type, super.DBuser, super.DBpassword);
                        collectionPath = getPathforFile(dbc, sps_type + sps_id + ".xml", sps_id);
                        if (collectionPath != null) {
                            html += "<li>";
                            dbf = new DBFile(super.DBURI, collectionPath, sps_type + sps_id + ".xml", super.DBuser, super.DBpassword);
                            String[] title = dbf.queryString(xpath + "/text()");
                            if (title.length > 0) {
                                html += "<a style=\"cursor:pointer\" onclick=\"centeredPopup('Index?&amp;type=" + sps_type + "&amp;action=edit&amp;lang=" + lang + "&amp;id=" + sps_id + "','myWindow','900','600','yes');return false;\"\n"
                                        + " class=\"nodeFixed\">" + title[0] + " ,../" + sps_type + "/" + sps_id + "</a>";
                            } else {
                                html += "<a style=\"cursor:pointer\" onclick=\"centeredPopup('Index?&amp;type=" + sps_type + "&amp;action=edit&amp;lang=" + lang + "&amp;id=" + sps_id + "','myWindow','900','600','yes');return false;\"\n"
                                        + " class=\"nodeFixed\">" + " ,../" + sps_type + "/" + sps_id + "</a>";

                            }

                            html += "</li>";
                        }

                    } catch (DMSException ex) {
                    }

                }
                html += "</li>";
                html += "</ul>";
                NodeList ref = e.getElementsByTagName("ref");
                html += "<li id=" + type + "/admin/refs\" class='collapsable'>";
                html += "<div class=\"hitarea\"></div>";

                html += "<span class=\"nodeName\">" + refs + "</span>";

                html += "<ul>";
                for (int i = 0; i < ref.getLength(); i++) {
                    try {
                        NamedNodeMap attrs = ref.item(i).getAttributes();
                        String sps_id = attrs.getNamedItem("sps_id").getNodeValue();
                        String sps_type = attrs.getNamedItem("sps_type").getNodeValue();
                        String xpath = DMSTag.valueOf("xpath", "title", sps_type, this.conf)[0];
                        dbc = new DBCollection(super.DBURI, applicationCollection + "/" + sps_type, super.DBuser, super.DBpassword);
                        collectionPath = getPathforFile(dbc, sps_type + sps_id + ".xml", sps_id);
                        if (collectionPath != null) {
                            html += "<li>";

                            dbf = new DBFile(super.DBURI, collectionPath, sps_type + sps_id + ".xml", super.DBuser, super.DBpassword);
                            String[] title = dbf.queryString(xpath + "/text()");
                            if (title.length > 0) {
                                html += "<a style=\"cursor:pointer\" onclick=\"centeredPopup('Index?&amp;type=" + sps_type + "&amp;action=edit&amp;lang=" + lang + "&amp;id=" + sps_id + "','myWindow','900','600','yes');return false;\"\n"
                                        + " class=\"nodeFixed\">" + title[0] + " ,../" + sps_type + "/" + sps_id + "</a>";
                            } else {
                                html += "<a style=\"cursor:pointer\" onclick=\"centeredPopup('Index?&amp;type=" + sps_type + "&amp;action=edit&amp;lang=" + lang + "&amp;id=" + sps_id + "','myWindow','900','600','yes');return false;\"\n"
                                        + " class=\"nodeFixed\">" + " ,../" + sps_type + "/" + sps_id + "</a>";

                            }
                            html += "</li>";

                        }
                    } catch (DMSException ex) {
                    }

                }
                html += "</li>";
                html += "</ul>";
                html += "</ul>";
                out.append(html);
            } else if (action.equals("GetMime")) {
                DBFile uploadsDBFile = new DBFile(this.DBURI, this.adminCollection, "Uploads.xml", this.DBuser, this.DBpassword);
                String filename = request.getParameter("file");
                String mime = Utils.findMime(uploadsDBFile, filename);
                out.append(mime);
            } else {
                String schemaFilename = "AP";
                if (file != null) {
                    schemaFilename = file;
                } else {
                    schemaFilename = type;

                }
                SchemaFile sch = new SchemaFile(schemaFolder + schemaFilename + ".xsd");
                if (xml != null) {
//                    System.out.println("********");
//                    System.out.println(lang);
//                    System.out.println();
                    out.append(sch.validate(xml, lang));

                } else {
                    if (lang == null || lang.equals("gr")) {
                        out.append("Ακύρωση ελέγχου λόγω μεγέθους αρχείου!");
                    } else {
                        out.append("Validation aborted due to XML size");

                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error1: " + e.getMessage());
            e.printStackTrace();
            if (lang == null || lang.equals("gr")) {
                out.append("<Σφάλμα>" + e.getMessage() + "</Σφάλμα>");
            } else {
                out.append("<Error>" + e.getMessage() + "</Error>");

            }
        }
        if (vocId != -1) {
            try {
                /*
                 * TODO output your page here
                 */
                out.println(String.valueOf(vocId));
            } finally {
                out.close();
            }
        } else {
            out.close();
        }

    }
    //    private int newId(DBFile vocFile) {
    //        int newId;
    //        DBFile[] maxId = vocFile.query("max(//Όρος/@id)");
    //
    //        if (maxId.length == 0) {
    //            newId = 1;
    //        } else {
    //            newId = (int) Double.parseDouble(maxId[0].getXMLAsString()) + 1;
    //        }
    //
    //        return newId;
    //    }
    //    private void addTerm(DBFile vocFile, int id, String term) {
    //        String newTermQ = "<Όρος id=\"" + id + "\">" + term + "</Όρος>";
    ////        System.out.println("NEWTERM=" + newTermQ);
    //        vocFile.xAppend("//Όροι", newTermQ);
    //    }

    private int newId(DBFile vocFile) {
        int newId;
        String parentName = vocFile.query("//name(/*)")[0].toString();
        String childName = vocFile.query("//name(/" + parentName + "/*[1])")[0].toString();

        DBFile[] maxId = vocFile.query("max(//" + childName + "/@id)");

        if (maxId.length == 0) {
            newId = 1;
        } else {
            newId = (int) Double.parseDouble(maxId[0].getXMLAsString()) + 1;
        }

        return newId;
    }

    private void addTerm(DBFile vocFile, int id, String term) {
        String parentName = vocFile.query("//name(/*)")[0].toString();
        String childName = vocFile.query("//name(/" + parentName + "/*[1])")[0].toString();
        String newTermQ = "<" + childName + " id=\"" + id + "\">" + term + "</" + childName + ">";
// System.out.println("NEWTERM=" + newTermQ);
        vocFile.xAppend("//" + parentName + "", newTermQ);
    }

//    private void addTerm(DBFile vocFile, int id, String term) {
//        String parentName = vocFile.query("//name(/*)")[0].toString();
//        String childName = vocFile.query("//name(/" + parentName + "/*[1])")[0].toString();
//        String newTermQ = "<" + childName + " id=\"" + id + "\">" + term + "</" + childName + ">";
////        System.out.println("NEWTERM=" + newTermQ);
//        vocFile.xAppend("//" + childName + "", newTermQ);
//    }
    private String cleanHTML(String input) {
        //StringBuilder output = new StringBuilder();

        //Extra stage to restore CDATA sections when damaged (due to Word paste?)
        ArrayList<String> shouldBeCDATABlocks = findReg("sps_html=[^>]+>(?!<!).*?<", input, Pattern.DOTALL);
        for (String shouldBeCDATABlock : shouldBeCDATABlocks) {
            String CDATABlock = shouldBeCDATABlock.replaceAll("(?s)sps_html=\\\"\\\">(.*?)<", "sps_html=\\\"\\\"><![CDATA[$1]]><");
            CDATABlock = CDATABlock.replaceAll("&lt;", "<").replaceAll("&gt;", ">"); //.replaceAll("\\u2028", " ")
            CDATABlock = StringEscapeUtils.unescapeHtml4(CDATABlock);

            input = input.replace(shouldBeCDATABlock, CDATABlock.trim());
        }

        ArrayList<String> CDATABlocks = findReg("(?<=<!\\[CDATA\\[)\\s*.*?\\s*(?=\\]\\]>)", input, Pattern.DOTALL);
        for (String CDATABlock : CDATABlocks) {
//            System.out.println(CDATABlock);
            String cleanCDATABlock = CDATABlock.replaceAll("</\\b(div|p)\\b>", "<br/>");
            cleanCDATABlock = cleanCDATABlock.replaceAll("(?s)<style>.*?</style>", "");
            cleanCDATABlock = cleanCDATABlock.replaceAll("<(?!/?\\b(b|i|a|br|u|su[pb])\\b)[^<>]+>", "");
            cleanCDATABlock = cleanCDATABlock.replaceAll("Normal\\s+0\\s+false\\s+false\\s+false\\s+EL\\s+X-NONE\\s+X-NONE\\s+MicrosoftInternetExplorer4\\s*", "");

//            System.out.println("**********");
//            System.out.println(cleanCDATABlock);
//            input = input.replace(CDATABlock, "*************");
            input = input.replace(CDATABlock, cleanCDATABlock);
        }

        //StringBuilder output = new StringBuilder(input);
        return input;
    }

    protected static void fixTime(DBFile dbf) {
        // System.out.println(dbf.queryString("//ΗμερομηνίαΤιμή[text()!='']"));
        String[] times = dbf.queryString("//DateValue[text()!='']/text()");

        for (String time : times) {

            SISdate sisTime = sisTime = new SISdate(time);

            String from = Integer.toString(sisTime.getFrom());
            String to = Integer.toString(sisTime.getTo());
            System.out.println("from-->" + from);
            System.out.println("to---> " + to);
            dbf.xUpdate("//DateValue[text()='" + time + "']/@x", from);
            dbf.xUpdate("//DateValue[text()='" + time + "']/@y", to);

        }
//                         SISdate test = new SISdate(-98299776,-98250816);
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
