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

import gr.forth.ics.isl.fexml.connections.ConnectionPool;
import isl.binaryFile.BinaryFile;
import isl.dbms.DBCollection;

import isl.dms.file.EntryNotFoundException;
import isl.dbms.DBFile;
import isl.dbms.DBMSException;
import isl.dms.DMSConfig;
import isl.dms.DMSException;
import isl.dms.file.DMSUser;
import isl.dms.file.DMSXQuery;
import isl.dms.xml.XMLTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServlet;

public class BasicServlet extends HttpServlet {

    protected static String contextPath, baseURL;
    protected static int serverPort, maxCollsize;
    protected static String configCollection, configXMLCollection, configQueriesCollection;
    protected static String stateOfSite, editorType, dynamicLabels;
    protected static String[] languages;
    protected static String[] entityWithPhoto;

    protected static String servletName, dmsCollection, adminCollection, applicationCollection, versionCollection;
    protected static String DBURI, DBuser, DBpassword;
    protected static int userId = 1;
    protected static String schemaFolder, uploadsFile, uploadsFolder, schemaLastVersion;
    protected static String synthesisURL;
    protected static ConnectionPool connectionPool;
    protected static String cookieSep = "---";
    private static boolean configured = false;
    protected static Logger logger;
    public static DMSConfig conf;
    protected static String username;
    public static String URI_Reference_Path, resizeDimension;
    public static int thumbSize, normalSize;
    protected static String fedoraStatus = "";
    protected static String fedoraUrl = "";

    // The strings used to build the query that asks for query source text, from file
    // DMSQueries.xml.
    // See method doQueryForSource for usage
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        if (!configured) {
            ServletContext sc = getServletContext();
            servletName = config.getServletName();
            DBURI = sc.getInitParameter("DBURI");

            baseURL = sc.getInitParameter("baseURL");

            adminCollection = sc.getInitParameter("adminCollection");
            applicationCollection = sc.getInitParameter("applicationCollection");
            versionCollection = sc.getInitParameter("versionCollection");

            DBuser = sc.getInitParameter("DBuser");
            DBpassword = sc.getInitParameter("DBpassword");

            configCollection = sc.getInitParameter("configCollection");
            configXMLCollection = sc.getInitParameter("configXMLCollection");
            configQueriesCollection = sc.getInitParameter("configQueriesCollection");
            schemaFolder = sc.getInitParameter("schemaFolder");
            uploadsFolder = sc.getInitParameter("uploadsFolder");
            synthesisURL = sc.getInitParameter("synthesisURL");
            stateOfSite = sc.getInitParameter("stateOfSite");
            editorType = sc.getInitParameter("editorType");
            URI_Reference_Path = sc.getInitParameter("URI_Reference_Path");
            maxCollsize = Integer.parseInt(sc.getInitParameter("maxCollsize"));
            dynamicLabels = sc.getInitParameter("dynamicLabels");
            languages = sc.getInitParameter("languages").split(",");
            schemaLastVersion = sc.getInitParameter("schemaLastVersion");
            entityWithPhoto = sc.getInitParameter("entityWithPhoto").split(",");

            resizeDimension = sc.getInitParameter("resizeDimension");
            thumbSize = Integer.parseInt(sc.getInitParameter("thumbSize"));
            normalSize = Integer.parseInt(sc.getInitParameter("normalSize"));

            boolean verbose = sc.getInitParameter("verboseConnections").equals("true") ? true : false;

            ConnectionPool.configure(DBuser, DBpassword, 504, verbose);
            connectionPool = ConnectionPool.getInstance();

            configured = true;
            this.fedoraStatus = getServletContext().getInitParameter("fedoraStatus");
            this.fedoraUrl = getServletContext().getInitParameter("fedoraUrl");

        }

        // initialize the subsystems
        //Changed with new DMS
        conf = new DMSConfig(this.DBURI, this.adminCollection, this.DBuser, this.DBpassword);
    }

    public static Logger getLogger() {
        if (!configured) {
            throw new Error("FATAL ERROR: getLogger() called before init() !");
        }
        return logger;
    }

    public String xmlPart(String part) {
        String partContent = "";
        try {
            partContent = getDBFileContent(configXMLCollection, part + ".xml");
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(BasicServlet.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        return partContent;
    }

    public Hashtable<String, Object> getParams(HttpServletRequest request) {
        String qId = request.getParameter("qid");
        if (qId == null) {
            qId = "";
        }
        String category = request.getParameter("category");
        if (category == null) {
            category = "";
        }
        String mnemonicName = request.getParameter("mnemonicName");
        if (mnemonicName == null) {
            mnemonicName = "";
        }
        String[] targets = request.getParameterValues("target");
        String operator = request.getParameter("operator");
        String[] inputs = request.getParameterValues("input");
        if (inputs == null) {
            inputs = new String[0];
        }
        String[] inputsIds = request.getParameterValues("inputid");
        if (inputsIds == null) {
            inputsIds = new String[0];
        }
        String[] inputsParameters = request.getParameterValues("inputparameter");
        if (inputsParameters == null) {
            inputsParameters = new String[0];
        }
        String[] inputsValues = request.getParameterValues("inputvalue");
        if (inputsValues == null) {
            inputsValues = new String[0];
        }

        String[] outputs = request.getParameterValues("output");
        if (outputs == null) {
            outputs = new String[0];
        }

        // Is an array of checkboxes on the html page that controls which categories are selected
        String[] checkboxes = request.getParameterValues("checkboxId");
        if (checkboxes == null) {
            checkboxes = new String[0];
        }

        // each input xpath can have its own matching operator
        String[] operators = request.getParameterValues("oper");
        if (operators == null) {
            operators = new String[inputs.length];
            for (int i = 0; i < operators.length; i++) {
                operators[i] = "=";
            }
        }

        Hashtable<String, Object> params = new Hashtable<String, Object>();
        params.put("qId", qId);
        params.put("category", category);
        params.put("mnemonicName", mnemonicName);
        params.put("targets", targets);
        params.put("operator", operator);
        params.put("inputs", inputs);
        params.put("inputsIds", inputsIds);
        params.put("inputsParameters", inputsParameters);
        params.put("checkboxes", checkboxes);
        params.put("operators", operators);
        params.put("inputsValues", inputsValues);
        params.put("outputs", outputs);

        // the length of checkboxes used for category search
        String chkL = request.getParameter("chkL");
        params.put("chkL", chkL != null ? chkL : "");

        // this controls sevlet behavior
        String type = request.getParameter("type");
        params.put("type", type != null ? type : "");

        return params;
    }

    protected String[] queryCollection(String q, String coll)
            throws IOException {

        DBCollection collection = connectionPool.connect(DBURI, coll);
        return collection.query(q);
    }

    public String getDBFileContent(String coll, String id) throws IOException {
        String fileContent = null;
        try {
            ArrayList<String> params = new ArrayList<String>();
            params.add(coll);
            params.add(id);
            String[] results = doQueryForSource("Get_DBFile", params, coll);
            if (results != null && results.length > 0) {
                fileContent = results[0].toString();
            }

        } catch (DMSException ex) {

            java.util.logging.Logger.getLogger(BasicServlet.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        return fileContent;
    }

    public String[] doQueryForSource(String queryName, ArrayList<String> queryParams, String colPath) throws DMSException {
        try {
            String queryFilename = new DMSXQuery(queryName, 0, conf).getInfo("external_source");

            BinaryFile query = new BinaryFile(configQueriesCollection, queryFilename, conf);
            String q = query.toString(queryParams);

            return queryCollection(q, colPath);
        } catch (EntryNotFoundException ex) {
            java.util.logging.Logger.getLogger(BasicServlet.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            return null;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(BasicServlet.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            return null;
        } catch (DMSException ex) {
            java.util.logging.Logger.getLogger(BasicServlet.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            return null;
        }
    }

    public String[] doQueryForSource(String queryName, String colPath) throws DMSException {
        try {
            String queryFilename = new DMSXQuery(queryName, 0, conf).getInfo("external_source");

            BinaryFile query = new BinaryFile(configQueriesCollection, queryFilename, conf);
            String q = query.toString();
            return queryCollection(q, colPath);
        } catch (EntryNotFoundException ex) {
            java.util.logging.Logger.getLogger(BasicServlet.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            return null;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(BasicServlet.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            return null;
        } catch (DMSException ex) {
            java.util.logging.Logger.getLogger(BasicServlet.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            return null;
        }
    }

    public static String getminColl(String systemDBCollection, String type, DBCollection col) {
        String results = "";
        String queryString = "let $results := for $i in xmldb:get-child-collections('" + systemDBCollection + "/" + type + "') " + "let  $b := collection(concat('" + systemDBCollection + "/" + type + "/',$i)) " + "return count($b), " + "$minimum:=min($results), " + "$coll:=for $c in xmldb:get-child-collections('" + systemDBCollection + "/" + type + "') " + "let  $b := collection(concat('" + systemDBCollection + "/" + type + "/',$c)) " + "where count($b)=$minimum return <coll>{$c}</coll> " + "return " + "<results> " + "<min>{$minimum}</min><res>{$coll}</res></results> ";
        try {
            String[] dbf = col.query(queryString);
            results = dbf[0].toString();
        } catch (DBMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return results;
    }

    public static String getmaxCollName(String systemDBCollection, String type, DBCollection col) {
        String results = "";
        String queryString = "let $results :=" + "for $i in xmldb:get-child-collections('" + systemDBCollection + "/" + type + "')" + "let  $b := collection(concat('" + systemDBCollection + "/" + type + "/',$i))" + "return number($i)," + "$maximum:=max($results)" + "return $maximum";
        try {
            String[] dbf = col.query(queryString);
            results = dbf[0].toString();
        } catch (DBMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return results;
    }

    public String getPathforFile(DBCollection col, String filename, String id) {
        String collectionPath = null;

        try {
            ArrayList<String> params = new ArrayList<String>();
            params.add(id);
            String[] results = doQueryForSource("Get_FullPath_For_DBFile", params, col.getPath());
            if (results != null && results.length > 0) {
                collectionPath = results[0].substring(0, results[0].lastIndexOf("/"));

            }

        } catch (DMSException ex) {
            ex.printStackTrace();
        }

        return collectionPath;

    }

    // a very simple transform method. more sophisticated ones were used in IMKE
    public String transform(String xml, String xsl) {
        try {
            XMLTransform xmlTrans = new XMLTransform(xml);

            return xmlTrans.transform(xsl);
        } catch (DMSException ex) {
            ex.printStackTrace();
        }

        return null;

    }

    public ArrayList<String> findReg(String regexp, String text, int flags) {

        ArrayList<String> results = new ArrayList();
        Pattern pattern;
        try {
            pattern = Pattern.compile(regexp, flags);
            Matcher matcher = pattern.matcher(text);
            boolean found = false;

            while ((found = matcher.find())) {
                results.add(matcher.group());
            }
        } catch (PatternSyntaxException ex) {
            System.out.println(ex.getDescription());
            System.out.println(ex.getMessage());
        }

        return results;

    }

    public void setUsername(String username) {
        this.username = username;
    }

    public static String getRights() {
        String UserRights = null;
        try {
            DMSUser Actions = new DMSUser(username, conf);
            String[] ActionArray = Actions.getActions();
            UserRights = ActionArray[0];
        } catch (DMSException e) {
            e.printStackTrace();
        }
        return UserRights;
    }

    public void setAdminProperty(String property, String value, DBFile dbFile) {
        //should not check, but demand to exist...temporary allow
        if (dbFile.exist("//admin/" + property) == false) {
            dbFile.xAppend("//admin", "<" + property + "/>");
        }

        if (value.length() == 0) {
            dbFile.xRemove("//admin/" + property);
            dbFile.xAppend("//admin", "<" + property + ">" + value + "</" + property + ">");
        } else {
            dbFile.xUpdate("//admin/" + property, value);
        }
    }

    public String getUserGroup() {
        //from the database
        //implies that user belongs to ONLY one group...
        DMSUser user;
        String[] ret;
        try {
            user = new DMSUser(this.username, this.conf);
            ret = user.getGroups();
            if (ret.length == 0) {
                return null;
            } else {
                return ret[0];
            }
        } catch (DMSException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String[] getUserEditors() {
        //from the database
        String[] ret;
        try {
            ret = DMSUser.getUsersByAction("editor", conf);
            if (ret.length == 0) {
                return null;
            } else {
                return ret;
            }
        } catch (DMSException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String updateReferences(DBFile dbf, String id, String type) {
        String ret = "";
        //First of all get existing ref
        String query = "for $i in //ref\n"
                + "let $j := concat($i/@sps_type,'_',$i/@sps_id)\n"
                + "return\n"
                + "$j";

        String[] currentRefs = dbf.queryString(query);
        ArrayList<String> obsoleteRefsList = new ArrayList<String>();
        if (currentRefs != null) {
            obsoleteRefsList = new ArrayList(Arrays.asList(currentRefs));
        }

        ArrayList<String> firstTimeRefsList = new ArrayList<String>();

        query = "for $i in //*[name(.)!= 'ref' and name(.)!='ref_by' and @sps_type!='' and @sps_id!='' and @sps_id!='0']\n" //eXist 2.1 compatible
                + "let $j := concat($i/@sps_type,'_',$i/@sps_id)\n"
                + "return\n"
                + "$j";
        String[] newRefs = dbf.queryString(query);

        query = "//admin/status/string()";
        String[] statuses = dbf.queryString(query);
        String status = "";
        if (statuses != null) {
            if (statuses.length > 0) {
                status = statuses[0];
            }
        }
        String isUnpublishedAttr = "";
        if (status.length() > 0) {
            if (status.equals("published") || status.equals("pending")) {
                isUnpublishedAttr = " isUnpublished='false'";
            } else {
                isUnpublishedAttr = " isUnpublished='true'";
            }
        }
        String ref_byBlock = "<ref_by sps_type='" + type + "' sps_id='" + id + "'" + isUnpublishedAttr + "/>";

        ArrayList<String> finalRefs = new ArrayList<String>();
        StringBuilder refsBlock = new StringBuilder();
        String sps_id = "";
        String sps_type = "";
        for (String newRef : newRefs) {
            String[] sps = newRef.split("_");
            sps_id = sps[1];
            sps_type = sps[0];
            //Create refs block first
            if (finalRefs.contains(newRef)) { //Duplicate
            } else {
                finalRefs.add(newRef);
                if (!sps_id.equals("0")) {
                    refsBlock = refsBlock.append("\n<ref sps_id='").append(sps_id).append("' sps_type='").append(sps_type).append("'/>");
                }
            }

            //If value existed in currentRefs, then no need to search referenced entity and add ref there
            if (!obsoleteRefsList.remove(newRef)) {
                firstTimeRefsList.add(newRef);
            }
        }

        //First of all update admin/refs/ref of file
        if (currentRefs.length > 0) {
            if (refsBlock.length() > 0) {
                dbf.xUpdate("//admin/refs", refsBlock.toString());
            } else {
                dbf.xRemove("//admin/refs");
            }
        } else {
            if (refsBlock.length() > 0) {
                dbf.xAppend("//admin", "<refs>" + refsBlock + "</refs>");
            }
        }

        //Then add new dependencies
        if (!firstTimeRefsList.isEmpty()) {
            for (String firstTimeRef : firstTimeRefsList) {
                String[] sps = firstTimeRef.split("_");
                sps_id = sps[1];
                sps_type = sps[0];
                query = "//admin[id='" + sps_id + "']/..";

                String filename = sps_type + sps_id + ".xml";
                DBCollection dbc = new DBCollection(DBURI, this.applicationCollection + "/" + sps_type, DBuser, DBpassword);
                DBFile refFile = new DBFile(DBURI, getPathforFile(dbc, filename, sps_id), filename, DBuser, DBpassword);
                String[] refsBy = refFile.queryString("//admin/refs_by/ref_by[@sps_type='" + type + "' and @sps_id='" + id + "']");
                String refFileXML = refFile.getXMLAsString();

                if (refsBy != null) {

                    if (refsBy.length > 0) {
                    } else {
                        if (refFileXML.contains("<refs_by>")) {
                            refFile.xAppend("//admin/refs_by", ref_byBlock);
                        } else {
                            refFile.xAppend("//admin", "<refs_by>" + ref_byBlock + "</refs_by>");
                        }
                    }
                }

            }
        }

        //Last but not least remove old dependencies
        if (!obsoleteRefsList.isEmpty()) {
            for (String obsoleteRef : obsoleteRefsList) {
                String[] sps = obsoleteRef.split("_");
                sps_id = sps[1];
                sps_type = sps[0];
                query = "//admin[id='" + sps_id + "']/..";
                String filename = sps_type + sps_id + ".xml";

                DBCollection dbc = new DBCollection(DBURI, this.applicationCollection + "/" + sps_type, DBuser, DBpassword);
                DBFile obsoleteRefFile = new DBFile(DBURI, getPathforFile(dbc, filename, sps_id), filename, DBuser, DBpassword);
                obsoleteRefFile.xRemove("//admin/refs_by/ref_by[@sps_type='" + type + "' and @sps_id='" + id + "']");

            }
        }

        return ret;
    }

    public String[] initInsertFile(String type) {
        DBCollection col = new DBCollection(DBURI, applicationCollection + "/" + type, DBuser, DBpassword);

        String collName = "";
        String id = "";
        int idInt = getLastId(col);
        if (idInt != 0) {
            idInt = idInt + 1;
            id = String.valueOf(idInt);
        } else {
            id = newId(col, type);
        }

        String fileId = type + id;

        String results = getminColl(applicationCollection, type, col);
        String minColl = results.substring(results.indexOf("<coll>") + 6, results.indexOf("</coll>"));
        String minfiles = results.substring(results.indexOf("<min>") + 5, results.indexOf("</min>"));

        int filesSize = Integer.parseInt(minfiles);
        if (filesSize > maxCollsize) {

            String maxCollName = getmaxCollName(applicationCollection, type, col);
            int maxColl = Integer.parseInt(maxCollName);
            maxColl = maxColl + 1;
            maxCollName = String.valueOf(maxColl);

            try {
                col.createCollection(maxCollName);
            } catch (DBMSException e) {
                e.printStackTrace();
            }

            collName = this.applicationCollection + "/" + type + "/" + maxCollName;
        } else {
            collName = this.applicationCollection + "/" + type + "/" + minColl;
        }

        String[] result = new String[2];
        result[0] = collName;
        result[1] = id;

        return result;
    }

    public static String newId(DBCollection col, String type) {
        String fileId = "";
        String[] files = col.listFiles();

        if (files.length != 1) {
            //if there are files in the collection

            int[] ids = new int[files.length - 1];
            //get the 'numbers' id only (full id is: typeXX.xml)
            //we want 'XX' only
            int ind = 0;
            for (int i = 0; i < files.length; i++) {
                if (files[i].equals(type + ".xml")) {
                    continue;
                }
                ids[ind++] = Integer.parseInt(files[i].split(type)[1].split("\\.")[0]);
            }
            //sort the ids
            java.util.Arrays.sort(ids);
            //and get the max + 1
            fileId = String.valueOf(ids[ids.length - 1] + 1);
        } else //if there not any files in the collection, it is the first
        {
            fileId = "1";
        }

        return fileId;
    }

    public static int getLastId(DBCollection col) {
        int id = 0;
        try {
            String queryString = "let $results := for $i in //id/text() return $i,$maximum:=max($results)return $maximum";
            String[] dbf = col.query(queryString);
            if (dbf.length != 0) {
                String idString = dbf[0].toString();
                id = Integer.parseInt(idString);
            }
        } catch (DBMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return id;
    }

    public static String getLabelFromXPath(String type, String xpath, String lang) {
        xpath = xpath.replaceAll("\\[\\d++\\]", "");
        DBFile nodesFile = new DBFile(BasicServlet.DBURI, applicationCollection + "/LaAndLi", type + ".xml", BasicServlet.DBuser, BasicServlet.DBpassword);
        String[] labels = nodesFile.queryString("//node[xpath='" + xpath + "']/" + lang + "/string()");
        if (labels != null && labels.length > 0) {
            return labels[0];
        } else {
            return null;
        }
    }

    public static boolean isVisibleFromXPath(String type, String xpath, String lang) {
        xpath = xpath.replaceAll("\\[\\d++\\]", "");
        DBFile nodesFile = new DBFile(BasicServlet.DBURI, applicationCollection + "/LaAndLi", type + ".xml", BasicServlet.DBuser, BasicServlet.DBpassword);
        String[] displayValues = nodesFile.queryString("//node[xpath='" + xpath + "']/@display/string()");

        if (displayValues != null && displayValues.length > 0) {
            return false;
        } else {
            return true;
        }
    }

    public static String getAllLabelsFromXPath(String type, String xpath) {
        xpath = xpath.replaceAll("\\[\\d++\\]", "");
        DBFile nodesFile = new DBFile(BasicServlet.DBURI, applicationCollection + "/LaAndLi", type + ".xml", BasicServlet.DBuser, BasicServlet.DBpassword);
        String[] labels = nodesFile.queryString("//node[xpath='" + xpath + "']");
        if (labels != null && labels.length > 0) {
            return labels[0];
        } else {
            return null;
        }
    }

    public static void setSpecificLanguageLabelFromXPath(String type, String xpath, String lang, String value) {
        xpath = xpath.replaceAll("\\[\\d++\\]", "");
        DBFile nodesFile = new DBFile(BasicServlet.DBURI, applicationCollection + "/LaAndLi", type + ".xml", BasicServlet.DBuser, BasicServlet.DBpassword);
        nodesFile.xUpdate("//node[xpath='" + xpath + "']/" + lang, value);
    }

    public static void toggleSpecificNodeDisplayFromXPath(String type, String xpath, String action) {
        DBFile nodesFile = new DBFile(BasicServlet.DBURI, applicationCollection + "/LaAndLi", type + ".xml", BasicServlet.DBuser, BasicServlet.DBpassword);
        if (action.equals("show")) {
            nodesFile.xRemove("//node[xpath='" + xpath + "']/@display");
        } else {
            nodesFile.xAddAttribute("//node[xpath='" + xpath + "']", "display", "none");
        }
    }

}
