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
import gr.forth.ics.isl.fexml.utilities.XMLFragment;
import isl.dbms.DBCollection;
import isl.dbms.DBFile;
import isl.dbms.DBMSException;
import isl.dms.DMSException;
import isl.dms.file.DMSTag;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringEscapeUtils;
import schemareader.Element;
import schemareader.SchemaFile;

/**
 *
 * @author samarita
 */
public class Query extends BasicServlet {

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
        String result = "";
        String xpath = request.getParameter("xpath");
        String type = request.getParameter("type");
        String vocabulary = request.getParameter("vocabulary");
        String facet = request.getParameter("facet");
        String childrenParam = request.getParameter("children");
        String childrenPathsParam = request.getParameter("childrenPaths");
        String lang = request.getParameter("lang");
        String value = request.getParameter("value");

        //Prefix added so that code handles both sps_ and ics_ attribute names...
        String prefix = request.getParameter("prefix");
        if (prefix == null) {
            prefix = "ics";
        }

        if (lang == null) {
            lang = "gr";
        }

        if (childrenParam == null) {

            if (value != null) {
                //Get xpath+restrictions code

                xpath = xpath.replaceAll("\\[\\d+\\]", "");

                //Person example
                SchemaFile sch = new SchemaFile(schemaFolder + type + ".xsd");
                ArrayList<Element> elements = sch.getElements(xpath);
                Element el = elements.get(0);
                HashMap<String, String> res = el.getRestrictions();

                //Build result
                StringBuilder output = new StringBuilder("type=" + el.getType());
                if (!res.isEmpty()) {
                    Set<String> keys = res.keySet();
                    for (String key : keys) {
                        output = output.append("^^^" + key + "=" + res.get(key));
                    }
                }
                result = output.toString();

            } else {

                String id = request.getParameter("id");

                if (vocabulary == null) {
                    if (facet == null) {

                        //Links code
                        HashMap<String, String> queryResult = getValueFromAndType(xpath, prefix);
                        if (queryResult == null) {
                            result = "";
                        } else {
                            String selectedType = type;

                            for (String entityType : queryResult.keySet()) {
                                String valueFromXpath = queryResult.get(entityType);
                                String typeOfValueFromXpath = entityType;

                                if (!typeOfValueFromXpath.equals(type)) {
                                    type = typeOfValueFromXpath;
                                }

                                StringBuilder pathCondition = new StringBuilder("[");
                                StringBuilder pathOutput = new StringBuilder("");

                                //Experimental:Changed pathOutput from // to / (Zominthos)
                                if (valueFromXpath.contains(",")) {
                                    String[] paths = valueFromXpath.split(",");
                                    pathOutput = pathOutput.append("concat($i/");

                                    for (String path : paths) {

                                        pathCondition = pathCondition.append(path).append("!='' or");
                                        pathOutput = pathOutput.append(path.trim()).append("/string(),', ',$i/");

                                    }
                                    pathCondition = pathCondition.replace(pathCondition.length() - 2, pathCondition.length(), "]");
                                    pathOutput = pathOutput.replace(pathOutput.length() - 9, pathOutput.length(), ")");//was 10 with //

                                } else {
                                    pathCondition = pathCondition.append(valueFromXpath).append("!='']");
                                    pathOutput = pathOutput.append("$i/").append(valueFromXpath).append("/string()");
                                }

                                DBCollection dbc = new DBCollection(BasicServlet.DBURI, applicationCollection + "/" + type, BasicServlet.DBuser, BasicServlet.DBpassword);
                                String digitalPath = "";
                                try {
                                    digitalPath = "/" + DMSTag.valueOf("xpath", "upload", type, Query.conf)[0] + "/text()"; //exume valei sto DMSTags.xml ths exist ena neo element upload sta pedia pu xreiazetai na vazume to type sto admin part
                                } catch (DMSException ex) {
                                } catch (ArrayIndexOutOfBoundsException e) {
                                }

                                String query = "let $col := collection('" + applicationCollection + "/" + type + "')" + pathCondition + "[.//lang='" + lang + "']"
                                        + "\nreturn"
                                        + "\n<select id='" + xpath + "'>"
                                        //     + "\n<option value='----------" + type + "----------' data-id='0' data-type='" + type + "'>----------" + type + "----------</option>"

                                        + "\n<option value='--------------------' data-id='0' data-type='" + type + "'>--------------------</option>"
                                        + "\n<optgroup label='" + type + "'>"
                                        + "\n{for $i in $col"
                                        + "\nlet $name := " + pathOutput
                                        + "\nlet $imagePath := encode-for-uri($i" + digitalPath + ")"
                                        //                                    + "\nlet $imagePath := $i//Οντότητα/ΨηφιακόΑντίγραφοΟντότητα/ΨηφιακόΑρχείο/string()"
                                        + "\nlet $id := $i//admin/id/string()"
                                        + "\nlet $uri := concat('.../',substring-after($i//admin/uri_id,'" + URI_Reference_Path + "'),', ')"
                                        + "\norder by $name collation '?lang=el-gr'"
                                        + "\nreturn";

                                String returnBlock = "\n<option value='{$uri}{$name}' data-id='{$id}' image-path='{$imagePath}' data-type='" + type + "'>{$uri}{$name}</option>}";

                                if (selectedType.equals(type)) {
                                    returnBlock = "\nif ($id='" + id + "') then"
                                            + "\n<option value='{$uri}{$name}' selected='' data-id='{$id}' image-path='{$imagePath}' data-type='" + type + "'>{$uri}{$name}</option>"
                                            + "\nelse"
                                            + "\n<option value='{$uri}{$name}' data-id='{$id}' image-path='{$imagePath}' data-type='" + type + "'>{$uri}{$name}</option>}";
                                }

                                returnBlock = returnBlock + "\n</optgroup>"
                                        + "\n</select>";
                                query = query + returnBlock;
                                result = result + dbc.query(query)[0];
                            }
                            result = result.replaceAll("(?s)</select><select[^>]+>[^<]+.*?(?=<optgroup)", "");
                        }
                    } else {//Facet code
                        String facetProps = getFacetProperties(xpath, prefix);
                        Utils utils = new Utils();

                        String themasURL = utils.getMatch(facetProps, "(?<=themasUrl=\")[^\"]*(?=\")");
                        String username = utils.getMatch(facetProps, "(?<=username=\")[^\"]*(?=\")");
                        String thesaurusName = utils.getMatch(facetProps, "(?<=thesaurusName=\")[^\"]*(?=\")");
                        String facetId = utils.getMatch(facetProps, "(?<=facetId=\")[^\"]*(?=\")");
//                        System.out.println("FACET ID is:" + facetId);
                        if (!facetId.equals("")) {

                            String urlEnd = "&external_user=" + username + "&external_thesaurus=" + thesaurusName;

                            String serviceURL = themasURL + "SearchResults_Terms?updateTermCriteria=parseCriteria"
                                    + "&answerType=XMLSTREAM&pageFirstResult=SaveAll&input_term=facet&op_term=refid=&inputvalue_term=" + facetId
                                    + "&operator_term=or&output_term1=name" + urlEnd;

                            String themasServiceResponse = utils.consumeService(serviceURL);
                            if (themasServiceResponse.contains("<terms count=\"0\">")) {//Hierarchy, not facet, should call service again
                                System.out.println("IT'S A HIERARCHY!");
                                serviceURL = themasURL + "SearchResults_Terms?updateTermCriteria=parseCriteria"
                                        + "&answerType=XMLSTREAM&pageFirstResult=SaveAll&input_term=topterm&op_term=refid=&inputvalue_term=" + facetId
                                        + "&operator_term=or&output_term1=name" + urlEnd;
                                themasServiceResponse = utils.consumeService(serviceURL);
                            }

                            if (themasServiceResponse.length() > 0) {
                                XMLFragment xml = new XMLFragment(themasServiceResponse);
                                ArrayList<String> terms = xml.query("//term/descriptor/text()");
                                ArrayList<String> Ids = xml.query("//term/descriptor/@referenceId");

                                StringBuilder selectBlock = new StringBuilder(" <select id='" + xpath + "' data-thesaurusName='" + thesaurusName + "' data-username='" + username + "' data-themasURL='" + themasURL + "' data-facet='" + facetId + "'>");
                                selectBlock.append("<option value='-------------------' data-id='0'>-------------------</option>");

                                for (int i = 0; i < terms.size(); i++) {

                                    if (Ids.get(i).equals(id)) {
                                        selectBlock.append("<option selected='' value='" + terms.get(i) + "' data-id='" + Ids.get(i) + "'>" + terms.get(i) + "</option>");

                                    } else {
                                        selectBlock.append("<option value='" + terms.get(i) + "' data-id='" + Ids.get(i) + "'>" + terms.get(i) + "</option>");
                                    }
                                }
                                selectBlock.append("</select>");
                                result = selectBlock.toString();

                            } else {
                                result = "No facet found!";
                            }
                        }

                    }
                } else {
                    //Vocabulary code
                    if (vocabulary.equals("")) {
                        vocabulary = getVocabulary(xpath, prefix);
                    }

                    if (vocabulary == null || vocabulary.equals("")) {
                        result = "No vocab found!";
                    } else {
                        DBFile dbf;
                        String parentName = "";
                        String childName = "";
                        try {
                            dbf = new DBFile(BasicServlet.DBURI, applicationCollection + "/Vocabulary/" + lang, vocabulary, BasicServlet.DBuser, BasicServlet.DBpassword);
                            parentName = dbf.query("//name(/*)")[0].toString();
                            childName = dbf.query("//name(/" + parentName + "/*[1])")[0].toString();

                        } catch (DBMSException ex) {
                            DBCollection vocabCol = new DBCollection(BasicServlet.DBURI, applicationCollection + "/Vocabulary/" + lang, BasicServlet.DBuser, BasicServlet.DBpassword);
                            String randomFileName = vocabCol.listFiles()[0];
                            DBFile randomFile = vocabCol.getFile(randomFileName);
                            dbf = vocabCol.createFile(vocabulary, "XMLDBFile");
                            parentName = randomFile.query("//name(/*)")[0].toString();
                            childName = randomFile.query("//name(/" + parentName + "/*[1])")[0].toString();

                            dbf.setXMLAsString("<" + parentName + "><" + childName + " id='0'>-------------------</" + childName + "></" + parentName + ">");
                            dbf.store();
                        }

                        String query = "let $voc := doc('" + applicationCollection + "/Vocabulary/" + lang + "/" + vocabulary + "')"
                                + "\nreturn"
                                + "\n<select id='" + xpath + "' data-vocabulary='" + vocabulary + "'>"
                                + "\n{for $i in $voc//" + childName
                                + "\norder by $i collation '?lang=el-gr'"
                                + "\nreturn"
                                + "\nif ($i/@id='" + id + "') then"
                                + "\n<option value='{$i/string()}' selected='' data-id='{$i/@id}'>{$i/string()}</option>"
                                + "\nelse"
                                + "\n<option value='{$i/string()}' data-id='{$i/@id}'>{$i/string()}</option>}"
                                + "\n</select>";
                        result = dbf.queryString(query)[0];
                    }
                }
            }
        } else {
            //Schema add/remove code

            //For now remove position info from xpath
            xpath = xpath.replaceAll("\\[\\d+\\]", "");
            xpath = detectRecursion(xpath); //to solve recursion problem
            String[] children = null;
            children = childrenParam.split("___");

            String[] childrenPaths = null;
            if (childrenPathsParam != null) {
                childrenPaths = childrenPathsParam.split("___");
            }

            ArrayList childrenList = new ArrayList(Arrays.asList(children));
            ArrayList childrenPathsList = new ArrayList(Arrays.asList(childrenPaths));

            SchemaFile sch = new SchemaFile(schemaFolder + type + ".xsd");

            ArrayList<Element> elements = sch.getElements(xpath);
            ArrayList<String> schemaChildren = new ArrayList<String>();

            ArrayList<String> mayAdd = new ArrayList<String>();
            ArrayList<String> mayRemove = childrenList;
            ArrayList<String> mayRemovePaths = childrenPathsList;

            StringBuffer output = new StringBuffer("");
            String verdict = "";

            for (Element el : elements) {
                if (el != null) {

                    if (el.getFullPath().equals(xpath + "/" + el.getName())) {
                        schemaChildren.add(el.getName());

                        int occurs = Collections.frequency(childrenList, el.getName());

                        if (el.getInfo().startsWith("Choice")) {
                            int choiceMinOccurs = Integer.parseInt(el.getInfo().split("_")[1]);
                            int choiceMaxOccurs = Integer.parseInt(el.getInfo().split("_")[2]);
                            int actualMinOccurs = el.getMinOccurs();
                            if (actualMinOccurs > 0) {
                                if (choiceMinOccurs < el.getMinOccurs()) {
                                    actualMinOccurs = choiceMinOccurs;
                                }
                            }
                            int actualMaxOccurs = el.getMaxOccurs();
                            if (actualMinOccurs != -1) {
                                if (choiceMaxOccurs == -1 || choiceMaxOccurs > actualMaxOccurs) {
                                    actualMaxOccurs = choiceMaxOccurs;
                                }
                            }
                            String actualMaxOccursAsString = "" + actualMaxOccurs;
                            if (actualMaxOccurs == -1) {
                                actualMaxOccursAsString = "∞";
                            }

                            StringBuilder actions = new StringBuilder("ACTIONS:");
                            if (actualMaxOccurs == -1 || occurs < actualMaxOccurs) {
                                actions = actions.append(" + ");
                                mayAdd.add(el.getName());
                            }
                            if (occurs > actualMinOccurs) {
                                actions = actions.append(" X ");
                            } else {
                                while (mayRemove.contains(el.getName())) {
                                    mayRemovePaths.remove(mayRemove.indexOf(el.getName()));
                                    mayRemove.remove(el.getName());

                                }
                            }
                            verdict = "Element: " + el.getName() + " ## Εμφανίσεις: " + occurs + " ## Επιτρεπτές(" + actualMinOccurs + "," + actualMaxOccursAsString + ") " + actions + " ή ";

                        } else {
                            if (output.length() > 0) {
                                String lastChar = "" + output.charAt(output.length() - 2);
                                if (lastChar.equals("ή")) {
                                    output = output.replace(output.length() - 2, output.length(), "\n");
                                }
                            }
                            String maxOccurs = "" + el.getMaxOccurs();
                            if (el.getMaxOccurs() == -1) {
                                maxOccurs = "∞";
                            }

                            StringBuilder actions = new StringBuilder("ACTIONS:");
                            if (el.getMaxOccurs() == -1 || occurs < el.getMaxOccurs()) {
                                actions = actions.append(" + ");
                                mayAdd.add(el.getName());
                            }

                            if (occurs > el.getMinOccurs()) {
                                actions = actions.append(" X ");
                            } else {
                                while (mayRemove.contains(el.getName())) {
                                    mayRemovePaths.remove(mayRemove.indexOf(el.getName()));

                                    mayRemove.remove(el.getName());
                                }
                            }
                            verdict = "Element: " + el.getName() + " ## Εμφανίσεις: " + occurs + " ## Επιτρεπτές(" + el.getMinOccurs() + "," + maxOccurs + ") " + actions + "\n";

                        }
                    }

                }

            }

            output.append("<select id='add' style='margin-left:3px;' >");

            for (String addElem : mayAdd) {
                if (!addElem.equals("admin")) {
                    String fullPath = xpath + "/" + addElem;
                    fullPath = detectRecursion(fullPath); //to solve recursion problem
                    if (isVisibleFromXPath(type, fullPath, lang)) {//May only add if element is visible, otherwise no point...
                        String label = getLabelFromXPath(type, fullPath, lang);

                        if (label == null || label.equals("")) {
                            label = "(" + addElem + ")";
                        }

                        String tree = sch.createXMLSubtree(fullPath, "minimum");
                        System.out.println(tree);
                        String encodedTree = StringEscapeUtils.escapeXml(tree);
                        output.append("<option data-schemaName='").append(addElem).append("' value='").append(encodedTree).append("'>").append(label).append("</option>");
                    }
                }
            }

            output.append("</select>^^^"); //ΙΕ bad hack...
            output.append("<select id='remove' style='margin-left:3px;' >");

            for (int i = 0; i < mayRemove.size(); i++) {
                String removeElem = mayRemove.get(i);
                if (!removeElem.equals("admin") && !removeElem.equals("")) {
                    String removeElemPath = mayRemovePaths.get(i);
                    String position = "";
                    if (removeElemPath.endsWith("]")) {
                        position = removeElemPath.substring(removeElemPath.lastIndexOf("["));
                    }
                    String label = getLabelFromXPath(type, mayRemovePaths.get(i), lang);
                    if (label == null || label.equals("")) {
                        label = "(" + removeElem + ")";
                    }
                    output.append("<option value='").append(removeElemPath).append("'>").append(label).append(position).append("</option>");
                }
            }

            output.append("</select>^^^<input type='hidden' id='schemaChildren' value='");

            for (int i = 0; i < schemaChildren.size(); i++) {
                if (i == schemaChildren.size() - 1) {
                    output.append(schemaChildren.get(i));
                } else {
                    output.append(schemaChildren.get(i)).append("___");
                }
            }
            output.append("'/>");
            result = output.toString();
        }
        try {
            /*
             * TODO output your page here
             */
            out.println(result);
        } finally {
            out.close();
        }
    }

    private String detectRecursion(String xpath) {
        String[] pathComponents = xpath.split("/");
        String path = "";
        for (String pathComponent : pathComponents) {
            if (path.isEmpty()) {
                path = path + "/" + pathComponent + "/";
            } else {
                if (!path.contains("/" + pathComponent + "/")) {
                    path = path + pathComponent + "/";
                } else {
                    int index = path.indexOf("/" + pathComponent + "/") + pathComponent.length() + 2;
                    path = path.substring(0, index);
                }
            }
        }
        return path.substring(1, path.length() - 1);

    }

    private HashMap<String, String> getValueFromAndType(String xpath, String prefix) {
        HashMap<String, String> result = new HashMap<String, String>();
        DBCollection nodesCol = new DBCollection(BasicServlet.DBURI, applicationCollection + "/LaAndLi", BasicServlet.DBuser, BasicServlet.DBpassword);
        xpath = xpath.replaceAll("\\[\\d++\\]", "");
        //Should be only one
        String[] valuesFromTable = nodesCol.query("//node[xpath='" + xpath + "']/valueFrom[.!='']/string()");

        String linkToType = "";
        String linkToPath = "";

        //If no info in LaAndLi collection, then try querying legacy data
        if (valuesFromTable == null || valuesFromTable.length == 0) {
            DBCollection rootCol = new DBCollection(BasicServlet.DBURI, applicationCollection, BasicServlet.DBuser, BasicServlet.DBpassword);
            String[] typeTable = rootCol.query("//" + xpath + "/@" + prefix + "_type[.!='']/string()");
            if (typeTable != null && typeTable.length > 0) {
                String type = typeTable[0];

                if (type.equals("AP")) {
                } else if (type.equals("Archive")) {
                    linkToPath = "Οντότητα/ΑρχειακόΣτοιχείοΟντότητα/Τίτλος";
                } else if (type.equals("Bibliography")) {
                    linkToPath = "Οντότητα/ΒιβλιογραφίαΟντότητα/Τίτλος";
                } else if (type.equals("DigitalCopy")) {
                    linkToPath = "Οντότητα/ΨηφιακόΑντίγραφοΟντότητα/Τίτλος";
                } else if (type.equals("Equipment")) {
                    linkToPath = "Οντότητα/ΕξοπλισμόςΟντότητα/Ονομασία";
                } else if (type.equals("Event")) {
                    linkToPath = "Οντότητα/ΣυμβάνΟντότητα/Ονομασία";
                } else if (type.equals("KAP")) {
                } else if (type.equals("Keimena")) {
                    linkToPath = "Οντότητα/ΚείμενοΟντότητα/Τίτλος";
                } else if (type.equals("Location")) {
                    linkToPath = "Οντότητα/ΤόποςΟντότητα/Ονομασία";
                } else if (type.equals("Martyries")) {
                    linkToPath = "Οντότητα/ΜαρτυρίαΟντότητα/Θέμα";
                } else if (type.equals("Material")) {
                    linkToPath = "Οντότητα/ΥλικόΟντότητα/Ονομασία";
                } else if (type.equals("Organization")) {
                    linkToPath = "Οντότητα/ΟργανισμόςΟντότητα/Επωνυμία";
                } else if (type.equals("Part")) {
                    linkToPath = "Οντότητα/ΤμήμαΟντότητα/Ονομασία";
                } else if (type.equals("Person")) {
                    linkToPath = "Οντότητα/ΠρόσωποΟντότητα/Ονοματεπώνυμο";
                }

                if (linkToPath != null) {
                    nodesCol.xUpdate("//node[xpath='" + xpath + "']/valueFrom", linkToPath);
                }

            }

        }

        String uniqueXpath = "";
        if (valuesFromTable != null) {
            for (String valueFromTable : valuesFromTable) {
                linkToPath = valueFromTable;

                if (linkToPath.contains(",")) {
                    uniqueXpath = linkToPath.split(",")[0];
                } else {
                    uniqueXpath = linkToPath;
                }

                //Added next line in case I use [1] etc...e.g. Συγγραφέας σε Βιβλιογραφία...
                uniqueXpath = uniqueXpath.replaceAll("\\[\\d++\\]", "");
                String[] typeOfvaluesFromTable = nodesCol.query("//node[xpath='" + uniqueXpath + "']/@type/string()");
                if (typeOfvaluesFromTable != null && typeOfvaluesFromTable.length > 0) {

                    result.put(typeOfvaluesFromTable[0], linkToPath);
                }

            }
        }
        return result;

    }

    private String getVocabulary(String xpath, String prefix) {
        DBCollection nodesCol = new DBCollection(BasicServlet.DBURI, applicationCollection + "/LaAndLi", BasicServlet.DBuser, BasicServlet.DBpassword);

        xpath = xpath.replaceAll("\\[\\d++\\]", "");
        //Should be only one
        String[] vocabsTable = nodesCol.query("//node[xpath='" + xpath + "']/vocabulary[.!='']/string()");
        if (vocabsTable == null || vocabsTable.length == 0) {
            //If no info in LaAndLi collection, then try querying legacy data
            DBCollection rootCol = new DBCollection(BasicServlet.DBURI, applicationCollection, BasicServlet.DBuser, BasicServlet.DBpassword);
            vocabsTable = rootCol.query("//" + xpath + "/@" + prefix + "_vocabulary/string()");
            if (vocabsTable != null && vocabsTable.length > 0) {
                nodesCol.xUpdate("//node[xpath='" + xpath + "']/vocabulary", vocabsTable[0]);
            }
        }

        if (vocabsTable != null && vocabsTable.length > 0) {
            return vocabsTable[0];
        } else {
            return null;
        }
    }

    private String getFacetProperties(String xpath, String prefix) {
        DBCollection nodesCol = new DBCollection(BasicServlet.DBURI, applicationCollection + "/LaAndLi", BasicServlet.DBuser, BasicServlet.DBpassword);

        xpath = xpath.replaceAll("\\[\\d++\\]", "");
        //Should be only one?
        String[] facetsTable = nodesCol.query("//node[xpath='" + xpath + "']/facet");
        if (facetsTable == null || facetsTable.length == 0) {

            //If no info in LaAndLi collection, then try querying legacy data
            //TO CHECK
            DBCollection rootCol = new DBCollection(BasicServlet.DBURI, applicationCollection, BasicServlet.DBuser, BasicServlet.DBpassword);
            facetsTable = rootCol.query("//" + xpath + "/@" + prefix + "_facet");
            if (facetsTable != null && facetsTable.length > 0) {
                System.out.println("XPATH=" + xpath);
                System.out.println("FT[0]=" + facetsTable[0]);
//                nodesCol.xUpdate("//node[xpath='" + xpath + "']/facet", facetsTable[0]);
            }
        }

        if (facetsTable != null && facetsTable.length > 0) {
            return facetsTable[0];
        } else {
            return null;
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
