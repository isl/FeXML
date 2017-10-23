Copyright 2012-2017 Institute of Computer Science,
Foundation for Research and Technology - Hellas

Licensed under the EUPL, Version 1.1 or - as soon they will be approved
by the European Commission - subsequent versions of the EUPL (the "Licence");
You may not use this work except in compliance with the Licence.
You may obtain a copy of the Licence at:

http://ec.europa.eu/idabc/eupl

Unless required by applicable law or agreed to in writing, software distributed
under the Licence is distributed on an "AS IS" basis,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the Licence for the specific language governing permissions and limitations
under the Licence.

Contact:  POBox 1385, Heraklio Crete, GR-700 13 GREECE
Tel:+30-2810-391632
Fax: +30-2810-391638
E-mail: isl@ics.forth.gr
http://www.ics.forth.gr/isl

Authors : Georgios Samaritakis, Konstantina Konsolaki 

This file is part of the FeXML webapp. 

FeXML
====
FeXML is a web application used to edit/view xml files stored in an eXist database (Has been tested with eXist versions 2.1+)..

##Build - Deploy - Run##

Folder src contain all the files needed to build the web app and create a war file. This project is a Maven project, providing all the libs in pom.xml. 
You may use any application server that supports war files. (has been tested with Apache Tomcat version 7 or later). 

##Database Setup##

Once you have an eXist database instance up and running, you may use an example database setup (such as the one in Synthesis-Museum) to begin with. 
Download Synthesis-Museum and store DMSCollection into eXist's collection db.

##Configuration##

Once you have deployed the FeXML war and you have stored files from Synthesis-Museum into your eXist DB, you should
check FeXML's web.xml and make any necessary adjustments (Mostly IPs, ports and filepaths). There is a description tag for every parameter, explaining
what may be changed and what should be left untouched.





