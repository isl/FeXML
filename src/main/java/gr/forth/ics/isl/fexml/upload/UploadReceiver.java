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
package gr.forth.ics.isl.fexml.upload;

import utilities.resizer.resize_image;
import isl.dbms.DBFile;
import gr.forth.ics.isl.fexml.BasicServlet;
import gr.forth.ics.isl.fexml.utilities.Utils;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.io.File;

public class UploadReceiver extends BasicServlet {

    private static File UPLOAD_DIR;
    private static File TEMP_DIR;
    private static String CONTENT_TYPE = "text/plain;charset=UTF-8";
    private static String CONTENT_LENGTH = "Content-Length";
    private static int RESPONSE_CODE = 200;

    @Override
    public void init() throws ServletException {
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String type = req.getParameter("type");
        String fileType = req.getParameter("fileType");
        String id = req.getParameter("id");
        String filePath = "";
        RequestParser requestParser = null;
        String filename = "";
        String mime = "";
        String msg = null;
        TEMP_DIR = new File(uploadsFolder + type + "/uploadsTemp");
        if (!TEMP_DIR.exists()) {
            TEMP_DIR.mkdirs();
        }
        try {
            if (ServletFileUpload.isMultipartContent(req)) {
                requestParser = RequestParser.getInstance(req, new MultipartUploadParser(req, TEMP_DIR, getServletContext()));
                filename = doWriteTempFileForPostRequest(requestParser);
            } else {
                requestParser = RequestParser.getInstance(req, null);
                filename = requestParser.getFilename();
            }
        } catch (Exception e) {
            System.out.println("Problem handling upload request");
            e.printStackTrace();
            filename = requestParser.getFilename();
            msg = e.getMessage();
        }

        if (fileType.equals("photo")) {
            filePath = "/Photos/original";
        } else if (fileType.equals("audio")) {
            filePath = "/Audio";
        } else if (fileType.equals("video")) {
            filePath = "/Video";
        } else if (fileType.equals("all")) {
            DBFile uploadsDBFile = new DBFile(this.DBURI, this.adminCollection, "Uploads.xml", this.DBuser, this.DBpassword);
            mime = Utils.findMime(uploadsDBFile, filename);
            filePath = "/" + mime;
            if (filePath.equals("/Photos")) {
                fileType = "photo";
                filePath = filePath + "/original";
            }

        }
        UPLOAD_DIR = new File(uploadsFolder + type + filePath);

        if (!UPLOAD_DIR.exists()) {
            UPLOAD_DIR.mkdirs();
        }

        String contentLengthHeader = req.getHeader(CONTENT_LENGTH);
        Long expectedFileSize = StringUtils.isBlank(contentLengthHeader) ? null : Long.parseLong(contentLengthHeader);

        resp.setContentType(CONTENT_TYPE);
        resp.setStatus(RESPONSE_CODE);

        if (!ServletFileUpload.isMultipartContent(req)) {
            writeToTempFile(req.getInputStream(), new File(UPLOAD_DIR, filename), expectedFileSize);
        }
        writeResponse(filename, resp.getWriter(), msg, fileType, mime);



    }

    private String doWriteTempFileForPostRequest(RequestParser requestParser) throws Exception {
        String filename = requestParser.getFilename();
        writeToTempFile(requestParser.getUploadItem().getInputStream(), new File(UPLOAD_DIR, filename), null);
        return filename;
    }

    private File writeToTempFile(InputStream in, File out, Long expectedFileSize) throws IOException {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(out);

            IOUtils.copy(in, fos);

            if (expectedFileSize != null) {
                Long bytesWrittenToDisk = out.length();
                if (!expectedFileSize.equals(bytesWrittenToDisk)) {
                    throw new IOException(String.format("Unexpected file size mismatch. Actual bytes %s. Expected bytes %s.", bytesWrittenToDisk, expectedFileSize));
                }
            }

            return out;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    private void writeResponse(String filename, PrintWriter writer, String failureReason, String fileType, String mime) {
      String fs =  System.getProperty("file.separator");
        if (failureReason == null) {
            String sourcePath = UPLOAD_DIR.getPath();
            if (fileType.equals("photo")) {
                resizeImage(filename, sourcePath, sourcePath.replace(fs+"original", fs+"thumbs"), thumbSize);
            }
            String json = "{\"success\": true, \"filename\": \"" + filename + "\", \"mime\": \"" + mime + "\"}";
            writer.print(json);
            if (fileType.equals("photo")) {                
                resizeImage(filename, sourcePath, sourcePath.replace(fs+"original", fs+"normal"), normalSize);
            } else if (fileType.equals("zip")) {
                Utils utils = new Utils();
                utils.unZipIt(filename, sourcePath);
            } else if (fileType.equals("video") || fileType.equals("all")) {

                if (filename.endsWith("zip")) {
                    Utils utils = new Utils();
                    utils.unZipIt(filename, sourcePath);
                }
            }
        } else {
            writer.print("{\"error\": \"" + failureReason + "\"}");
        }
    }

    private boolean resizeImage(String file, String sourceFolder, String destinationFolder, int pixels) {

        resize_image img = new resize_image();
        if (!new File(sourceFolder).exists()) {
            new File(sourceFolder).mkdirs();
        }
        if (!new File(destinationFolder).exists()) {
            new File(destinationFolder).mkdirs();
        }

        img.set_dir_location(sourceFolder, destinationFolder);
        if (resizeDimension.equals("HEIGHT")) {
            img.set_width_or_height(pixels, img.HEIGHT);
        } else if (resizeDimension.equals("WIDTH")) {
            img.set_width_or_height(pixels, img.WIDTH);
        }
        return img.resize_imageFile(file);
    }
}
