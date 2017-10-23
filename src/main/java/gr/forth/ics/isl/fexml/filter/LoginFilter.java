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
package gr.forth.ics.isl.fexml.filter;

import isl.dms.DMSException;
import isl.dms.file.DMSUser;
import gr.forth.ics.isl.fexml.BasicServlet;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginFilter extends BasicServlet implements Filter {

    private FilterConfig filterConfig = null;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    public void destroy() {
        this.filterConfig = null;
    }


    public String getCookieValue(Cookie[] cookies) {
        try {
            String[] users = DMSUser.getUsers(BasicServlet.conf);
            List<String> usersL = Arrays.asList(users);
            if (cookies != null) {
                for (int i = 0; i < cookies.length; i++) {
                    Cookie cookie = cookies[i];
                    if (usersL.contains(URLDecoder.decode(cookie.getName()))) {
                        BasicServlet bs = new BasicServlet();
                        bs.setUsername(URLDecoder.decode(cookie.getName()));
                        return (cookie.getName() + "-" + cookie.getValue());
                    }
                }
            }
        } catch (DMSException ex) {
            Logger.getLogger(LoginFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (editorType.equals("standalone")) {
            //TEMP CODE
            BasicServlet bs = new BasicServlet();
            bs.setUsername("user");
            chain.doFilter(request, response);
        } else {

            if (request instanceof HttpServletRequest) {
                HttpServletRequest hrequest = (HttpServletRequest) request;
                HttpServletResponse hresponse = (HttpServletResponse) response;

                String res = getCookieValue(hrequest.getCookies());
                if (res == null) {
                    hresponse.sendRedirect(synthesisURL);
                } else {
                    String cookieName = res.split("-")[0];
                    String cookieValue = res.split("-")[1];
                    Cookie cookie = new Cookie(cookieName, cookieValue);
                    String editorWebapp = "FeXML";
                    cookie.setPath("/" + editorWebapp);
                    cookie.setMaxAge(10800);
                    hresponse.addCookie(cookie);
                    chain.doFilter(request, response);
                }

            }
        }
    }
}
