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
package gr.forth.ics.isl.fexml.utilities;

import isl.dbms.DBFile;
import isl.dms.DMSException;
import isl.dms.file.DMSTag;
import gr.forth.ics.isl.fexml.BasicServlet;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author samarita
 */
public class Utils {

    public String consumeService(String urlString) {
        try {

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/xml");

            conn.setRequestProperty("Encoding", "UTF-8");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }
            String output = "";

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            output = org.apache.commons.io.IOUtils.toString(br);

            conn.disconnect();
            return output;

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();

        }
    }

    public String getMatch(String input, String pattern) {
        String ResultString = "";
        try {
            Pattern Regex = Pattern.compile(pattern,
                    Pattern.DOTALL);
            Matcher RegexMatcher = Regex.matcher(input);
            if (RegexMatcher.find()) {
                ResultString = RegexMatcher.group();
            }
        } catch (PatternSyntaxException ex) {
            // Syntax error in the regular expression
        }
        return ResultString;
    }

    public String getDate() {

        Calendar cal = new GregorianCalendar(Locale.getDefault());
        DecimalFormat myformat = new DecimalFormat("00");

        // Get the components of the date
        int year = cal.get(Calendar.YEAR);             // 2002
        int month = cal.get(Calendar.MONTH) + 1;           // 0=Jan, 1=Feb, ...
        int day = cal.get(Calendar.DAY_OF_MONTH);      // 1...

        return myformat.format(day) + "-" + myformat.format(month) + "-" + year;
    }

    /**
     * Time method
     *
     * @return Current time as <CODE>String</CODE> in hh:mm:ss format
     */
    public String getTime() {
        Calendar cal = new GregorianCalendar(Locale.getDefault());

        // Create the DecimalFormat object only one time.
        DecimalFormat myformat = new DecimalFormat("00");

        int hour24 = cal.get(Calendar.HOUR_OF_DAY);     // 0..23
        int min = cal.get(Calendar.MINUTE);             // 0..59
        int sec = cal.get(Calendar.SECOND);             // 0..59
        return myformat.format(hour24) + myformat.format(min) + myformat.format(sec);
    }

    /**
     * Unzip it (This implementation works only when zip contains files-folders
     * with ASCII filenames Greek characters break the code!
     *
     * @param zipFile input zip file
     * @param output zip file output folder
     */
    public void unZipIt(String zipFile, String outputFolder) {

        String rootFolderName = "";
        String rootFlashFilename = "";
        byte[] buffer = new byte[1024];

        try {

            //get the zip file content
            ZipInputStream zis
                    = new ZipInputStream(new FileInputStream(outputFolder + File.separator + zipFile));
            //get the zipped file list entry

            ZipEntry ze = zis.getNextEntry();

            boolean rootDirFound = false;
            boolean flashFileFound = false;
            while (ze != null) {

                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                if (!ze.getName().contains("__MACOSX")) {
                    if (ze.isDirectory()) {
                        if (rootDirFound == false) {
                            rootFolderName = newFile.getName();
                            rootDirFound = true;
                        }
                        new File(newFile.getParent()).mkdirs();
                    } else {
                        FileOutputStream fos = null;

                        new File(newFile.getParent()).mkdirs();

                        if (flashFileFound == false && newFile.getName().endsWith(".swf") && !newFile.getName().startsWith(".")) {
                            rootFlashFilename = newFile.getName();
                            flashFileFound = true;
                        }

                        fos = new FileOutputStream(newFile);

                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }

                        fos.close();
                    }
                }
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

            //Special Postprocessing for Games 
            if (!rootFlashFilename.equals("")) {
                File rootFlashFile = new File(outputFolder + File.separator + rootFolderName + File.separator + rootFlashFilename);
                rootFlashFile.renameTo(new File(outputFolder + File.separator + rootFolderName + File.separator + "index.swf"));

                if (!rootFolderName.equals("")) {
                    File rootFolder = new File(outputFolder + File.separator + rootFolderName);
                    rootFolder.renameTo(new File(outputFolder + File.separator + zipFile.substring(0, zipFile.lastIndexOf("."))));

                }
            }

            System.out.println("Done");

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getPathUriField(String type) {
        String uri_path = "";
        try {
            uri_path = DMSTag.valueOf("uri_path", "target", type, BasicServlet.conf)[0];
        } catch (DMSException ex) {
            ex.printStackTrace();
        }

        return uri_path;
    }

    public static String findMime(DBFile uploads, String file) {

        file = file.substring(file.lastIndexOf(".") + 1);
        file = file.toLowerCase();

        String[] mimes = uploads.queryString("//mime[type='" + file + "']/../name()");
        if (mimes.length == 0) {
            return "Other";
        } else {
            return mimes[0];
        }

    }

    public static Element getElement(String xmlString) {
        Element e = null;
        try {

            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            Document doc = null;
            try {
                doc = docBuilder.parse(new InputSource(new StringReader(xmlString)));
            } catch (SAXException ex) {
            } catch (IOException ex) {
            }
            e = doc.getDocumentElement();

        } catch (ParserConfigurationException ex) {
        }
        return e;
    }

}
