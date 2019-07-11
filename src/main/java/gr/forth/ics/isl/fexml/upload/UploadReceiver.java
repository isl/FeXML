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
package gr.forth.ics.isl.fexml.upload;

import forth.ics.isl.fedoraapi.ManageFedoraApi;
import utilities.resizer.resize_image;
import isl.dbms.DBFile;
import gr.forth.ics.isl.fexml.BasicServlet;
import gr.forth.ics.isl.fexml.utilities.Utils;
import isl.dbms.DBCollection;
import isl.dbms.DBMSException;
import java.awt.Image;
import java.awt.image.BufferedImage;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

public class UploadReceiver extends BasicServlet {

    private static File UPLOAD_DIR;
    private static File TEMP_DIR;
    private static String CONTENT_TYPE = "text/plain;charset=UTF-8";
    private static String CONTENT_LENGTH = "Content-Length";
    private static int RESPONSE_CODE = 200;
    private ManageFedoraApi fed;

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
        fed = new ManageFedoraApi();
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
        try {
            DBFile fileMappings = new DBFile(this.DBURI, this.adminCollection, "FileMappings.xml", this.DBuser, this.DBpassword);
            String fileExtension = filename.substring(filename.lastIndexOf('.') + 1);
            String temp = filename.split("___")[0];
            String originalName = temp + "." + fileExtension;
            String generatedName = filename.split(temp + "___")[1];
            String xml = "<fileMapping>"
                    + "<originalName>" + originalName + "</originalName>"
                    + "<generatedName>" + generatedName + "</generatedName>"
                    + "</fileMapping>";
            fileMappings.xAppend("//fileMappings", xml);
            filename = generatedName;
        } catch (DBMSException ex) {
        }
        if (this.fedoraStatus.equals("on")) {

            if (!fed.existsURI(this.fedoraUrl + "uploads")) {
                fed.createEmptyContainer(this.fedoraUrl, "uploads");
            }
            if (!fed.existsURI(this.fedoraUrl + "uploads/" + type)) {
                fed.createEmptyContainer(this.fedoraUrl, "uploads/" + type);
                fed.createEmptyContainer(this.fedoraUrl, "uploads/" + type + "/Photos");
                fed.createEmptyContainer(this.fedoraUrl, "uploads/" + type + "/Photos/original");
                fed.createEmptyContainer(this.fedoraUrl, "uploads/" + type + "/Photos/thumbs");
                fed.createEmptyContainer(this.fedoraUrl, "uploads/" + type + "/Photos/normal");
                fed.createEmptyContainer(this.fedoraUrl, "uploads/" + type + "/Audio");
                fed.createEmptyContainer(this.fedoraUrl, "uploads/" + type + "/Video");
                fed.createEmptyContainer(this.fedoraUrl, "uploads/" + type + "/Documents");
            }
        }
        if (fileType.equals("photo")) {
            filePath = "/Photos/original";
        } else if (fileType.equals("audio")) {
            filePath = "/Audio";
        } else if (fileType.equals("video")) {
            filePath = "/Video";
        } else if (fileType.equals("docs")) {
            filePath = "/Documents";
        } else if (fileType.equals("all")) {
            DBFile uploadsDBFile = new DBFile(this.DBURI, this.adminCollection, "Uploads.xml", this.DBuser, this.DBpassword);
            mime = Utils.findMime(uploadsDBFile, filename);
            filePath = "/" + mime;
            if (this.fedoraStatus.equals("on")) {
                if (!fed.existsURI(this.fedoraUrl + "uploads/" + type + "/" + mime)) {
                    fed.createEmptyContainer(this.fedoraUrl, "uploads/" + type + "/" + mime);
                }
            }

            if (filePath.equals("/Photos")) {
                fileType = "photo";
                filePath = filePath + "/original";
            }

        }
        resp.setContentType(CONTENT_TYPE);
        resp.setStatus(RESPONSE_CODE);
        if (msg != null) {
            resp.getWriter().print("{\"error\": \"" + msg + "\"}");
        } else {
            if (this.fedoraStatus.equals("on")) {
                upload2Fedora(req.getInputStream(), filename, type, filePath, mime, fileType, resp.getWriter());
            } else {
                UPLOAD_DIR = new File(uploadsFolder + type + filePath);
                if (!UPLOAD_DIR.exists()) {
                    UPLOAD_DIR.mkdirs();
                }

                String contentLengthHeader = req.getHeader(CONTENT_LENGTH);
                Long expectedFileSize = StringUtils.isBlank(contentLengthHeader) ? null : Long.parseLong(contentLengthHeader);

                if (!ServletFileUpload.isMultipartContent(req)) {
                    writeToTempFile(req.getInputStream(), new File(UPLOAD_DIR, filename), expectedFileSize);
                }
                writeResponse(filename, resp.getWriter(), msg, fileType, mime);
            }
        }

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
        String fs = System.getProperty("file.separator");
        if (failureReason == null) {
            String sourcePath = UPLOAD_DIR.getPath();
            if (fileType.equals("photo")) {
                resizeImage(filename, sourcePath, sourcePath.replace(fs + "original", fs + "thumbs"), thumbSize);
            }
            String json = "{\"success\": true, \"filename\": \"" + filename + "\", \"mime\": \"" + mime + "\"}";
            writer.print(json);
            if (fileType.equals("photo")) {
                resizeImage(filename, sourcePath, sourcePath.replace(fs + "original", fs + "normal"), normalSize);
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

    private void upload2Fedora(InputStream in, String filename, String type, String filePath, String mime, String fileType, PrintWriter writer) {

        try {
            File f = new File(UPLOAD_DIR, filename);
            Path path = f.toPath();
            String mimeType = Files.probeContentType(path);
            System.out.println("mimeType " + mimeType);
            System.out.println("file name" + filename);
            String uri = fed.uploadFileToContainer(this.fedoraUrl + "uploads/", type + filePath, in, mimeType, filename);
            System.out.println("uploadFileToContainer :" + uri);
            System.out.println("thumbSize" + thumbSize);
            String json = "{\"success\": true, \"filename\": \"" + filename + "\", \"mime\": \"" + mime + "\"}";
            writer.print(json);
            if (fileType.equals("photo")) {

                InputStream thumbs = resizeImage(uri, thumbSize, thumbSize, mimeType);
                String thumbsPath = filePath.replace("original", "thumbs");
                String thumbsUri = fed.uploadFileToContainer(this.fedoraUrl + "uploads/", type + thumbsPath, thumbs, mimeType, filename);
                System.out.println("thumbs uri :" + thumbsUri);
                InputStream normal = resizeImage(uri, normalSize, normalSize, mimeType);
                String normalPath = filePath.replace("original", "normal");
                String normalUri = fed.uploadFileToContainer(this.fedoraUrl + "uploads/", type + normalPath, normal, mimeType, filename);
                System.out.println("normal uri :" + normalUri);
            }
            //ekkremei gia fedora h periptwsh tu zip 

        } catch (IOException ex) {

        }
    }

    public static boolean resizeImage(String file, String sourceFolder, String destinationFolder, int pixels) {
        try {

            File original = new File(sourceFolder + System.getProperty("file.separator") + file);
            BufferedImage sourceImage = ImageIO.read(original);
            ByteArrayOutputStream imagebBuffer = new ByteArrayOutputStream();
            sourceImage = Scalr.resize(sourceImage, Method.QUALITY,
                    sourceImage.getHeight() < sourceImage.getWidth() ? Mode.FIT_TO_HEIGHT : Mode.FIT_TO_WIDTH,
                    pixels, pixels, Scalr.OP_ANTIALIAS);

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); // Needed see javadoc
            param.setCompressionQuality(1.0F); // Highest quality
            writer.setOutput(ImageIO.createImageOutputStream(imagebBuffer));
            writer.write(sourceImage);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imagebBuffer.toByteArray());
            File targetFile = new File(destinationFolder + System.getProperty("file.separator") + file);
            java.nio.file.Files.copy(byteArrayInputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            IOUtils.closeQuietly(byteArrayInputStream);

        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    //old way -- try above to check if we get better resizing results
//
//    private boolean resizeImage(String file, String sourceFolder, String destinationFolder, int pixels) {
//
//        resize_image img = new resize_image();
//        if (!new File(sourceFolder).exists()) {
//            new File(sourceFolder).mkdirs();
//        }
//        if (!new File(destinationFolder).exists()) {
//            new File(destinationFolder).mkdirs();
//        }
//
//        img.set_dir_location(sourceFolder, destinationFolder);
//        if (resizeDimension.equals("HEIGHT")) {
//            img.set_width_or_height(pixels, img.HEIGHT);
//        } else if (resizeDimension.equals("WIDTH")) {
//            img.set_width_or_height(pixels, img.WIDTH);
//        }
//
//        return img.resize_imageFile(file);
//    }
    public static InputStream resizeImage(String uri, int width, int height, String mimeType) throws IOException {
        URL url = new URL(uri);
        BufferedImage sourceImage = ImageIO.read(url);
        ByteArrayOutputStream imagebBuffer = new ByteArrayOutputStream();

        sourceImage = Scalr.resize(sourceImage, Method.ULTRA_QUALITY,
                sourceImage.getHeight() < sourceImage.getWidth() ? Mode.FIT_TO_HEIGHT : Mode.FIT_TO_WIDTH,
                Math.max(width, height), Math.max(width, height), Scalr.OP_ANTIALIAS);
        //  sourceImage = Scalr.crop(sourceImage, width, height);

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); // Needed see javadoc
        param.setCompressionQuality(1.0F); // Highest quality
        writer.setOutput(ImageIO.createImageOutputStream(imagebBuffer));
        writer.write(sourceImage);

        return new ByteArrayInputStream(imagebBuffer.toByteArray());
    }

}
