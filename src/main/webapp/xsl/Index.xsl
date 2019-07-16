<?xml version="1.0" encoding="UTF-8"?>
<!--
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

Authors : Georgios Samaritakis, Konstantina Konsolaki.

This file is part of the FeXML webapp.

-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="html"  />

    <!-- TODO customize transformation rules
         syntax recommendation http://www.w3.org/TR/xslt
    -->
    <xsl:template match="/">
        <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html></xsl:text>
        <html>
            <xsl:variable name="lang" select="//output/lang"/>
            <xsl:variable name="type" select="//output/type"/>
            <xsl:variable name="schemaLastVersion" select="//output/schemaLastVersion"/>
            <xsl:variable name="synthesisName" select="//output/synthesisName"/>
            <xsl:variable name="apos">'</xsl:variable>
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
                <!--Favicon line follows-->
                <link rel="shortcut icon" href="img/xml.png"/>
                <title>FeXML</title>
                

                <script>
                    var lang =<xsl:value-of select="concat($apos,$lang,$apos)"/>;
                    var synthesisName =<xsl:value-of select="concat($apos,$synthesisName,$apos)"/>;
                    var schemaLastVersion =<xsl:value-of select="concat($apos,$schemaLastVersion,$apos)"/>;
                    var view =<xsl:value-of select="concat($apos,//output/viewMode,$apos)"/>;
                    var photoTypes =<xsl:value-of select="concat($apos,//output/entityWithPhoto,$apos)"/>;                                        
                </script>
        
                <link href="js/chosen.css" rel="stylesheet"/>
                <link href="css/fineuploader.css" rel="stylesheet"/>
                <link href="css/main.css"  rel="stylesheet"/>

            </head>

            <body onunload="closeAndUnlock('{$type}','{//output/id}');">
   
                <div id="header">
                   
                    <div id="info">F<span onclick="window.open('about.html');">e</span>XML
                        <!--a href="changes.html" id="home" style="display:inline;">v1.8.5.1 (05/10/2015)</a-->
                        <xsl:variable name="siteType">
                            <xsl:choose>
                                <xsl:when test="$type='Study'">
                                    <xsl:text>meleti</xsl:text>
                                </xsl:when>
                                <xsl:when test="$type='Route'">
                                    <xsl:text>diadromi</xsl:text>
                                </xsl:when>
                                <xsl:when test="$type='Game'">
                                    <xsl:text>game</xsl:text>
                                </xsl:when>
                                
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:if test="$type='Study' or $type='Route' or $type='Game'">
                            <img src="img/ekbmm.jpg" height="20px" style='position:relative;cursor: hand; cursor: pointer;' onclick="centeredPopup('/EKBMM/Page?name={$siteType}&amp;lang={$lang}&amp;id={//output/id}','myWindow','700','500','yes');return false;"/>
                        </xsl:if>
                        <xsl:if test="$type='Podcast'">
                            <img src="img/ekbmm.jpg" height="20px" style='position:relative;cursor: hand; cursor: pointer;' onclick="centeredPopup('/EKBMM/Page?name=podcasts&amp;lang={$lang}','myWindow','700','500','yes');return false;"/>
                        </xsl:if>
                    </div>
                   
                    <div id="toolbar">
                        
                        <xsl:variable name="saveText">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Αποθήκευση XML</xsl:when>
                                <xsl:when test="$lang='en'">Save XML</xsl:when>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:variable name="validateText">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Έλεγχος XML</xsl:when>
                                <xsl:when test="$lang='en'">Validate XML</xsl:when>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:variable name="expandText">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Άνοιγμα</xsl:when>
                                <xsl:when test="$lang='en'">Expand</xsl:when>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:variable name="collapseText">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Κλείσιμο</xsl:when>
                                <xsl:when test="$lang='en'">Collapse</xsl:when>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:variable name="expandAllText">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Άνοιγμα Όλων</xsl:when>
                                <xsl:when test="$lang='en'">Expand All</xsl:when>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:variable name="collapseAllText">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Κλείσιμο Όλων</xsl:when>
                                <xsl:when test="$lang='en'">Collapse All</xsl:when>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:variable name="editText">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Επεξεργασία</xsl:when>
                                <xsl:when test="$lang='en'">Edit</xsl:when>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:variable name="viewText">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Προβολή</xsl:when>
                                <xsl:when test="$lang='en'">View</xsl:when>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:variable name="referenceText">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Αναφορές</xsl:when>
                                <xsl:when test="$lang='en'">References</xsl:when>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:variable name="createText">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Δημιουργία Νέου</xsl:when>
                                <xsl:when test="$lang='en'">Create New</xsl:when>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:variable name="settingsText">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Ρυθμίσεις</xsl:when>
                                <xsl:when test="$lang='en'">Settings</xsl:when>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:variable name="documentMapText">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Χάρτης XML</xsl:when>
                                <xsl:when test="$lang='en'">XML Map</xsl:when>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:variable name="timePrimitiveExpressionsText">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Παραδείγματα επιτρεπτών εκφράσεων χρόνου</xsl:when>
                                <xsl:when test="$lang='en'">Examples of accepted time expressions</xsl:when>
                            </xsl:choose>
                        </xsl:variable>

                        <xsl:choose>
                            <xsl:when test="//output/viewMode='0' or //output/viewMode='2'">
                                <xsl:if test="//output/editorType='standalone'">
                                    <a href="File?action=New&amp;type={$type}&amp;lang={$lang}&amp;depth={//output/depth}" id="home">
                                        <button id="createFile" title="{$createText}">
                                            <img src="img/create.png" alt="{$createText}" title="{$createText}" border="0" />
                                        </button>
                                    </a>
                                </xsl:if>
                                <button id="saveFile" title="{$saveText}">
                                    <img src="img/save.png" alt="{$saveText}" title="{$saveText}" />
                                </button>
                                <button id="view" title="{$viewText}" onclick="centeredPopup('Index?&amp;type={$type}&amp;action=view&amp;lang={$lang}&amp;id={//output/id}&amp;depth={//output/depth}','myWindow','900','600','yes');return false;">
                                    <img src="img/view.png" alt="{$viewText}" title="{$viewText}" />
                                </button>
                                <button id="referencesButton" title="{$referenceText}">
                                    <img src="img/references.png" alt="{$referenceText}" title="{$referenceText}" />
                                </button>
                                <button id="validateFile" title="{$validateText}">
                                    <img src="img/validate.png" alt="{$validateText}" title="{$validateText}" />
                                </button>
                   
 
                            </xsl:when>
                            <xsl:otherwise>
                                <button id="edit" title="{$editText}" onclick="window.open('Index?&amp;type={$type}&amp;lang={$lang}&amp;id={//output/id}','_self')">
                                    <img src="img/edit.png" alt="{$editText}" title="{$editText}" />
                                </button>    
                                                               
                            </xsl:otherwise>
                            
                        </xsl:choose>
                     
                        
                        
                        <button id="documentMap" title="{$documentMapText}" onclick="openMap();">
                            <img src="img/documentMap.png" alt="{$documentMapText}" title="{$documentMapText}" />
                        </button>
                        <button id="expand" title="{$expandText}" onclick="toggle_expand()">
                            <img src="img/expand.png" alt="{$expandText}" title="{$expandText}" />
                        </button>
                        <button id="collapse" title="{$collapseText}" onclick="toggle_collapse()">
                            <img src="img/collapse.png" alt="{$collapseText}" title="{$collapseText}" />
                        </button>
                        <button id="expandAll" title="{$expandAllText}" onclick="toggle_expand_all()">
                            <img src="img/expandAll.png" alt="{$expandAllText}" title="{$expandAllText}" />
                        </button>
                        <button id="collapseAll" title="{$collapseAllText}" onclick="toggle_collapse_all()">
                            <img src="img/collapseAll.png" alt="{$collapseAllText}" title="{$collapseAllText}" />
                        </button>
                        <button id="timePrimitiveExpressions" title="{$timePrimitiveExpressionsText}" onclick="centeredPopup('HelpPage_{$lang}.html','myWindow','900','600','yes');return false;">
                            <img src="img/clock.png" alt="{$timePrimitiveExpressionsText}" title="{$timePrimitiveExpressionsText}" />
                        </button>
                        

                        <xsl:if test="//output/viewMode='0' or //output/viewMode='2'">
                            <xsl:if test="//output/editorType='standalone'">
                                <a href="" id="home" onclick="centeredPopup('Index?file={$type}&amp;depth=5','myWindow','700','500','yes');return false;">
                                    <button id="settings" title="{$settingsText}">
                                        <img src="img/settings.png" alt="{$settingsText}" title="{$settingsText}" border="0" />
                                    </button>
                                </a>
                            </xsl:if>
                        </xsl:if>

                       
                        <div id="nodePath">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Εγγραφή: </xsl:when>
                                <xsl:when test="$lang='en'">Record: </xsl:when>
                            </xsl:choose>
                            <xsl:value-of select="//output/uri_name"/>
                            <xsl:text>/</xsl:text>
                            <xsl:value-of select="//output/id"/>
                            <br/>
                            <span style="float:right;">
                                <xsl:if test="//output/editorType='standalone'">
                                    <a  href="javascript:reloadMe('gr');">gr</a> |
                                    <a  href="javascript:reloadMe('en');">en</a>
                                </xsl:if>
                            </span>
                        </div>

                    </div>
                    
                </div>
                <div   id="xml" style="display:block;">
                   
                    <img src='img/spiffygif_102x102.gif' style="margin:auto;display:block;" />
                </div>
                
                <div id="map" style="display:none;">
                    

                </div>
              
                <textarea id="xmlString" style="display:none;">
                   
                </textarea>
                <div class="footer">                       
                    <div style="background-color:#5B5B5B;height:63px;margin-top:5px;margin-bottom:5px;">
                        <a href="http://www.ics.forth.gr/" target="_blank">
                            <img id="forthImg" src="{concat('img/forth_',$lang,'.png')}"/>
                        </a>
                        <a href="http://www.ics.forth.gr/isl" target="_blank">
                            <img src="{concat('img/isl_',$lang,'.png')}"/>
                        </a>
                        <a href="https://www.ics.forth.gr/isl/index_main.php?c=252" target="_blank">
                            <img src="{concat('img/cci_',$lang,'.png')}"/>
                        </a>
                    </div>                              
                </div>
                <script src="js/ext/jquery-1.7.2.min.js"></script>
                <div class="footer2">                       
                    <div  style="padding-top:13px;font-size:12px;color:white;">
                        <a style="color:white!important;text-decoration:none;" target="_blank" href="/{$synthesisName}/Privacy?action=conditions&amp;lang={$lang}">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Όροι Χρήσης</xsl:when>
                                <xsl:otherwise>Terms of Use</xsl:otherwise>
                            </xsl:choose>
                        </a>
                        |
                        <a target="_blank" style="padding-left:0px!important;color:white!important;text-decoration:none;" href="/{$synthesisName}/Privacy?action=privacy&amp;lang={$lang}">
                            <xsl:choose>
                                <xsl:when test="$lang='gr'">Πολιτική Προστασίας</xsl:when>
                                <xsl:otherwise>Privacy Policy</xsl:otherwise>
                            </xsl:choose>
                        </a>
                        | © 
             
                        2012-<span id="year">2019</span> 
                        <script>$("#year").html((new Date()).getFullYear());</script>
                        <a target="_blank" style="font-size:12px;padding-left:0px!important;color:white!important;text-decoration:none;" href="http://www.ics.forth.gr/isl"> FORTH-ICS</a>
                        |
                        <xsl:choose>
                            <xsl:when test="$lang='gr'">Αδειοδότηση υπό τους όρους της EUPL</xsl:when>
                            <xsl:otherwise>Licensed under the EUPL</xsl:otherwise>
                        </xsl:choose>
                    </div>
                </div>
                <xsl:if test="//output/attributes">
                    <input type="hidden" id="attributes" value="{//output/attributes}" name="attributes"></input>
                </xsl:if>
                <input type="hidden" id="depth" value="{//output/depth}" name="depth"></input>
                <input type="hidden" id="type" value="{$type}" name="type"></input>
                <input type="hidden" id="id" value="{//output/id}" name="id"></input>
                <input type="hidden" id="file" value="{//output/file}" name="file"></input>
                <input type="hidden" id="lang" value="{$lang}" name="lang"></input>
                <input type="hidden" id="editorType" value="{//output/editorType}" name="editorType"></input>
                <input type="hidden" id="dynamicLabels" value="{//output/dynamicLabels}" name="dynamicLabels"></input>

                <!--version inputs-->
                <input type="hidden" id="versions" value="{//output/versions}" name="versions"></input>
                <input type="hidden" id="collectionID" value="{//output/collectionID}" name="collectionID"></input>
                <input type="hidden" id="xmlId" value="{//output/xmlId}" name="xmlId"></input>
                <input type="hidden" id="entityType" value="{//output/entityType}" name="entityType"></input>
               
                <script  src="js/ext/jquery-color.js"></script>
                <script  src="js/ext/GLR/GLR.js"></script>
                <script src="js/ext/GLR/GLR.messenger.js"></script>
                <script  src="js/utils.js"></script>
                           
                <script   src="js/loc/xmlEditor.js"></script>
                <script src="js/scripts.js"/>
              
                
                <script src="js/chosen.jquery.js"></script>
                <script src='js/jquery.autosize-min.js'></script>
                <script src='js/nicEdit.js'></script>
             
                <script src='js/jquery.form.js'></script>
                <script src="js/jquery.fineuploader-3.0.js"/>
          
                <script src="js/auto.js"/>               
            </body>
        </html>
    </xsl:template>

  

</xsl:stylesheet>
