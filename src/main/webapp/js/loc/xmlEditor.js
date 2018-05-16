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

/*
 Original idea-implementation:
 
 Copyright (c) 2010 Aleksandar Kolundzija <ak@subchild.com>
 Version 1.5
 
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 
 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.
 
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 
 @TODO:
 Consider spliting app into modules: Loader, Renderer, Modifier, Writer
 
 Support comment editing and creation
 
 Attribute editing/creation:
 - removal of attributes needs work
 - typing should expand field width?
 - support blur for saves?
 
 Node editing/creation:
 - create node: add support for cancel (not remove)
 - support node renaming
 - support blur for saves?
 
 - for invalid XML, present link to XML in browser window since it displays specific error
 - use GIF for logo so IE6 likes
 - add support for session based temp directories
 - better messaging
 - add support for creating a new XML document from scratch
 - DTD/XSD generation and exporting
 - auto save
 - revert option
 - support for UNDO
 */



/**
 * Loggable adds a log method to the passed object.
 * @param 	{Object}	obj  		Object which will get a new log() method
 * @param 	{String}  	objName		Optional parameter for displaying a string before each log output
 * @param 	{boolean} 	debugMode	Optional switch for disabling logging
 */

var loggable = function(obj /* , objName, debugMode */) {
    var objName = arguments[1] || "",
            debugMode = (typeof arguments[2] !== "undefined") ? arguments[2] : true;
    prefix = objName ? objName + ": " : "";
    obj.log = (function(prefix) {
        return function() {
            if (debugMode && typeof console !== "undefined") {
                if (arguments.length) {
                    arguments[0] = prefix + arguments[0];
                }
//                console.log.apply(null, arguments);
            }
        }
    })(prefix);
    return obj;
};
/**
 * xmlEditor
 * Application for loading an XML file into memory, rendering it as an editable HTML tree,
 * and providing functionality for editing the memory copy of the file in real time.
 * Updated XML can be converted to a string and passed to a separate process for saving.
 */
var xmlEditor = (function() {

    // private members
    var _nodeRefs = [], // will hold references to XML nodes
            _initNodeState = "collapsable", //was expandable
            _depthThreshold = 5, //SAM's addition
            _maxDepth = 0,
            _lang = lang, //Unless overidden
            _showAttributes = false,
            //    _mixedContent = false,
            _$event = $({}); // beforeHtmlRendered, afterHtmlRendered, beforeToggleNode, afterToggleNode

    var htmlRendered = false;
    if (_lang == "gr") {
        _message = {
            "downloadingXML": "Κατέβασμα XML αρχείου...",
            "renderingHtml": "Σχεδιασμός XML δέντρου...",
            "readyToEdit": "Έτοιμο για επεξεργασία.",
            "removeAttrConfirm": "Are you sure want to delete this attribute and its value?",
            "invalidAttrName": "The attribute name you entered is invalid.\nPlease try again.",
            "invalidNodeName": "The node name you entered is invalid.\nPlease try again.",
            "noTextValue": "(Χωρίς τιμή. Κλικ για επεξεργασία.)",
            "noTextValueNoEdit": "(Χωρίς τιμή.)",
            "noTermMsg": "Δε συμπληρώσατε κάποιον όρο.",
            "newTerm": "Νέος Όρος:",
            "existsTerm": "Ο όρος υπάρχει ήδη.",
            "addedTerm": "Ο όρος καταχωρήθηκε με επιτυχία.",
            "removeNodeSuccess": "Ο κόμβος διεγράφη.",
            "removeNodeConfirm": "Είστε σίγουρος ότι θέλετε να διαγράψετε τον επιλεγμένο κόμβο (",
            "xmlLoadSuccess": "Το XML αρχείο άνοιξε επιτυχώς.",
            "xmlLoadProblem": "Υπήρξε πρόβλημα στο άνοιγμα του XML αρχείου.",
            "noVocabFound": "Δε βρέθηκε το αντίστοιχο λεξιλόγιο.",
            "noFacetFound": "Δεν έχει οριστεί συγκεκριμένο facet.",
            "schemaCheckFailed": "Η σύνδεση με το Schema απέτυχε",
            "validatingXml": "Έλεγχος XML ...",
            "referencesEval": "Δημιουργία Αναφορών ...",
            "saving": "Αποθήκευση ...",
            // Button labels
            "cancel": "Ακύρωση",
            "add": "Προσθήκη",
            "delete": "Διαγραφή",
            "createNew": "Δημιουργία",
            // Button tooltips
            "goUp": "Μετακίνηση μια θέση πάνω",
            "goDown": "Μετακίνηση μια θέση κάτω",
            "duplicate": "Δημιουργία αντιγράφου",
            "remove": "Διαγραφή κόμβου",
            "addRemove": "Προσθαφαίρεση κόμβων",
            "rename": "Μετονομασία",
            "show": "Εμφάνιση",
            "hide": "Εξαφάνιση",
            // Info labels
            "type": "Τύπος",
            "length": "Μήκος",
            //Type examples
            "example": "π.χ. ",
            "date": "1978 Μάρτιος 12 ή δεκαετία του 1970",
            "decimal": "10 ή 3.5",
            "externalLink": "http://... ή https://...",
            "linkFailed": "Η αναφορά απέτυχε.",
            "uploadImageMessage": "Κάνετε κλικ ή σύρετε (όχι σε Internet Explorer) αρχείο εικόνας",
            "uploadAudioMessage": "Κάνετε κλικ ή σύρετε (όχι σε Internet Explorer) αρχείο ήχου",
            "uploadVideoMessage": "Κάνετε κλικ ή σύρετε (όχι σε Internet Explorer) αρχείο βίντεο",
            "uploadZipMessage": "Κάνετε κλικ ή σύρετε (όχι σε Internet Explorer) αρχείο zip",
            "uploadDocsMessage": "Κάνετε κλικ ή σύρετε (όχι σε Internet Explorer) αρχείο κειμένου",
            "uploadAllMessage": "Κάνετε κλικ ή σύρετε (όχι σε Internet Explorer) το αρχείο",
            "underConstruction": "Η λειτουργία αυτή δεν είναι ακόμα διαθέσιμη"




        };
    } else if (_lang == "en") {
        _message = {
            "downloadingXML": "Downloading XML file...",
            "renderingHtml": "Rendering XML tree...",
            "readyToEdit": "Ready to edit.",
            "removeAttrConfirm": "Are you sure want to delete this attribute and its value?",
            "invalidAttrName": "The attribute name you entered is invalid.\nPlease try again.",
            "invalidNodeName": "The node name you entered is invalid.\nPlease try again.",
            "noTextValue": "(No value. Click to edit.)",
            "noTextValueNoEdit": "(No value.)",
            "noTermMsg": "You did not fill any term.",
            "newTerm": "New Term:",
            "existsTerm": "Term already exists.",
            "addedTerm": "Term was successfuly added.",
            "removeNodeSuccess": "Node was removed.",
            "removeNodeConfirm": "Are you sure you want to remove the selected node (",
            "xmlLoadSuccess": "XML file loaded successfully.",
            "xmlLoadProblem": "A problem came up while loading the XML file.",
            "noVocabFound": "Vocabulary not found.",
            "noFacetFound": "Facet not yet specified.",
            "schemaCheckFailed": "Schema connection failed",
            "validatingXml": "Validating XML ...",
            "referencesEval": "Create references ...",
            "saving": "Saving ...",
            // Button labels
            "cancel": "Cancel",
            "add": "Add",
            "delete": "Delete",
            "createNew": "Create new",
            // Button tooltips
            "goUp": "Move one place up",
            "goDown": "Move one place down",
            "duplicate": "Create duplicate",
            "remove": "Delete node",
            "addRemove": "Add/Remove nodes",
            "rename": "Rename",
            "show": "Show",
            "hide": "Hide",
            // Info labels
            "type": "Type",
            "length": "Length",
            //Type examples
            "example": "e.g. ",
            "date": "1978 March 12 or decade of 1970",
            "externalLink": "http://... or https://...",
            "decimal": "10 or 3.5",
            "linkFailed": "Link failed.",
            "uploadImageMessage": "Click or drag (not on Internet Explorer) image file",
            "uploadAudioMessage": "Click or drag (not on Internet Explorer) audio file",
            "uploadVideoMessage": "Click or drag (not on Internet Explorer) video file",
            "uploadZipMessage": "Click or drag (not on Internet Explorer) zip file",
            "uploadDocsMessage": "Click or drag (not on Internet Explorer) document file",
            "uploadAllMessage": "Click or drag (not on Internet Explorer) your file",
            "underConstruction": "This feature is not yet available"



        };
    } else { //Other languages
        _message = {
            "downloadingXML": "Downloading XML file...",
            "renderingHtml": "Rendering XML tree...",
            "readyToEdit": "Ready to edit.",
            "removeAttrConfirm": "Are you sure want to delete this attribute and its value?",
            "invalidAttrName": "The attribute name you entered is invalid.\nPlease try again.",
            "invalidNodeName": "The node name you entered is invalid.\nPlease try again.",
            "noTextValue": "(No value. Click to edit.)",
            "noTextValueNoEdit": "(No value.)",
            "noTermMsg": "You did not fill any term.",
            "newTerm": "New Term:",
            "existsTerm": "Term already exists.",
            "addedTerm": "Term was successfuly added.",
            "removeNodeSuccess": "Node was removed.",
            "removeNodeConfirm": "Are you sure you want to remove the selected node (",
            "xmlLoadSuccess": "XML file loaded successfully.",
            "xmlLoadProblem": "A problem came up while loading the XML file.",
            "noVocabFound": "Vocabulary not found.",
            "noFacetFound": "Facet not yet specified.",
            "schemaCheckFailed": "Schema connection failed",
            "validatingXml": "Validating XML ...",
            "referencesEval": "Create references ...",
            "saving": "Saving ...",
            // Button labels
            "cancel": "Cancel",
            "add": "Add",
            "delete": "Delete",
            "createNew": "Create new",
            // Button tooltips
            "goUp": "Move one place up",
            "goDown": "Move one place down",
            "duplicate": "Create duplicate",
            "remove": "Delete node",
            "addRemove": "Add/Remove nodes",
            "rename": "Rename",
            "show": "Show",
            "hide": "Hide",
            // Info labels
            "type": "Type",
            "length": "Length",
            //Type examples
            "example": "e.g. ",
            "date": "1978 March 12 or decade of 1970",
            "externalLink": "http://... or https://...",
            "decimal": "10 or 3.5",
            "linkFailed": "Link failed.",
            "uploadImageMessage": "Click or drag (not on Internet Explorer) image file",
            "uploadAudioMessage": "Click or drag (not on Internet Explorer) audio file",
            "uploadVideoMessage": "Click or drag (not on Internet Explorer) video file",
            "uploadZipMessage": "Click or drag (not on Internet Explorer) zip file",
            "uploadDocsMessage": "Click or drag (not on Internet Explorer) document file",
            "uploadAllMessage": "Click or drag (not on Internet Explorer) your file",
            "underConstruction": "This feature is not yet available"



        };
    }




    /**
     * Visits every node in the DOM and runs the passed function on it.
     * @param	{Object}	node	Starting node.
     * @param	{Function}	func	Function to execute on starting node and all of its descendents.
     * @TODO extend to support processing in chunks using setTimeout()s
     * @TODO move to renderer component
     */
    function _traverseDOM(node, func) {
        func(node);
        node = node.firstChild;
        while (node) {

            _traverseDOM(node, func);
            node = node.nextSibling;
        }


    }


    /**
     * Detects whether the passed node is a comment.
     * @param 	{Object}	node	An XML or DOM element.
     * @return 	{Boolean}
     */
    function _isCommentNode(node) {
        return (node.nodeType === 8);
    }

    /**
     * Retrieves XML node using nodeIndex attribute of passed $elem
     * @param 	{Object} 	$elem	jQuery DOM element
     * @return 	{Object}			A DOM element
     */
    function _getNodeFromElemAttr($elem) {
        var nodeRefIndex = $elem.closest("li.node").attr("nodeIndex"); // $elem.attr("nodeIndex");
        return _nodeRefs[nodeRefIndex];
    }

    function _get_XPath(elt) {
        var path = '';
        for (; elt && elt.nodeType === 1; elt = elt.parentNode) {
            var idx = $(elt.parentNode).children(elt.tagName).index(elt) + 1;
            idx > 1 ? (idx = '[' + idx + ']') : (idx = '');
            //idx='['+idx+']';
            path = '/' + elt.tagName + idx + path;
        }
        return path.substr(1);
    }


    /**
     * TODO
     * @param 	{Object}	node	A DOM element
     * @return 	{String}
     */
    function _getNodeDepth(node) {
        var i = 0;
        do {
            i = i + 1;
        }
        while ((node = node.parentNode) && (node.nodeName.toLowerCase() !== "#document"));
        return i;
    }


    /**
     * Replaced old _isLeaf. Works much better, since it considers the actual xsd and not a single xml instance.
     * 
     * @param 	{Int}	pathIndex   Xpath indexOf xpaths table.
     * @return 	{String}        'Leaf' if leaf. 'ParentWithActions' if parent with children that you may mess with.
     *                          'ParentWithoutActions' if parent with children that you may not mess with. 
     */

    function _getNodeType(pathIndex) {

        if (pathIndex === -1) {
            return "Leaf";
        }

        if (xpaths.length === pathIndex + 1) {
            return "Leaf";
        } else {

            /*START Code added to overcome recursion issues */
            var xpath = xpaths[pathIndex];
            var xpathWithoutRecursion = detectRecursion(xpath);
//console.log(xpath+"---"+xpathWithoutRecursion)
            if (xpath !== xpathWithoutRecursion) {//&& xpathWithoutRecursion!=='Essay'

                pathIndex = jQuery.inArray(xpathWithoutRecursion, xpaths);
            }
            /* Code added to overcome recursion issues END*/


            if (xpaths[pathIndex + 1].lastIndexOf(xpaths[pathIndex] + "/", 0) === 0) {//has children (added / after path to avoid accidental same names such as 
                // Bibliography/Title -- Bibliography/TitleOfCollectiveVolume

                if (view === "1") {//If view mode, then there is no point in looking for optional or multiple children.
                    return "Parent";
                }
                //Now we must determine if children are editable, ie if we may add or delete them.                
                if (_hasOptionalOrMultipleChildren(xpath)) {
                    return "ParentWithActions";
                } else {//If node children are neither optional nor multiple, there is no point in showing cog, so we treat node as a leaf.
                    return "ParentWithoutActions";
                }
            } else {
                return "Leaf";
            }
        }


    }
    /**
     * Checks if a node with a specific xpath has either optional or multiple children
     * @param 	{fatherXpath}	father node xpath
     * @return 	{String}
     */

    function _hasOptionalOrMultipleChildren(fatherXpath) {
        for (var i = 1; i < xpaths.length; i++) {//exclude root
            var xpath = xpaths[i];

            var pathWithoutFather = xpath.replace(fatherXpath + "/", "");
            if (pathWithoutFather.indexOf("/") === -1) {
                if (minOccurs[i] === "0" || maxOccurs[i] === "-1") {
//                    console.log("CHILD FOUND=" + pathWithoutFather + "##" + minOccurs[i] + "##" + maxOccurs[i]);
                    return true;
                }
//                console.log("CHILD FOUND="+pathWithoutFather+"##"+minOccurs[i]+"##"+maxOccurs[i]);
            }
        }
        return false;
    }

    /**
     * Binds custom event to private _$event object
     * @param	{String}	Name of custom event
     * @apram	{mixed}		Data to pass to event handler, or the handler itself (if no data is to be passed)
     * @param 	{Function}	Handler function (Optional, if already passed as second argument)
     */
    function _bind(eventName, dataOrFn, fnOrUndefined) {
        _$event.bind(eventName, dataOrFn, fnOrUndefined);
    }


    /**
     * Unbinds custom event from private _$event object
     * @param	{String}	Name of custom event
     * @param	{Function}	Handler function
     */
    function _unbind(eventName, fn) {
        _$event.unbind(eventName, fn);
    }


    /**
     * Returns an HTML string representing node attributes
     * @param  {Object} 	node 	DOM object
     * @return {String}
     */
    function _getEditableAttributesHtml(node) {
        if (!node.attributes) {
            return "";
        }
        var visibility = "";
        if (!_showAttributes) {
            visibility = "invisible";
        }
        var attrsHtml = "<span class='nodeAttrs " + visibility + "'>",
                totalAttrs = node.attributes.length;
        for (var i = 0; i < totalAttrs; i++) {
            attrsHtml += "<span class='singleAttr'>" + node.attributes[i].name +
                    "=\"<span class='attrValue' name='" + node.attributes[i].name + "'>" +
                    ((node.attributes[i].value === "") ? "&nbsp;" : node.attributes[i].value) +
                    "</span>\"</span>";
        }
        attrsHtml += "<button class='addAttr icon'/></span>";
        return attrsHtml;
    }


    /**
     * Retrieves non-empty text nodes which are children of passed XML node.
     * Ignores child nodes and comments. Strings which contain only blank spaces
     * or only newline characters are ignored as well.
     * @param  	{Object} 	node	XML DOM object
     * @return 	{jQuery}	jQuery collection of text nodes
     */
    function _getTextNodes(node) {
        return $(node).contents().filter(function() {
            return (
                    ((this.nodeName == "#text" && this.nodeType == "3") || this.nodeType == "4") && // text node, or CDATA node
                    ($.trim(this.nodeValue.replace("\n", "")) !== "") // not empty
                    );
        });
    }


    function _getNodeValue(node) {
        var $textNodes = _getTextNodes(node);
        var textValue = "";
        if (node && _isCommentNode(node)) {
            textValue = node.nodeValue;
        } else if ($textNodes[0]) {
            //Extra if added to fix IE6/7/8 bug that did not show any element values...
            if ($textNodes[0].textContent) {
                textValue = $.trim($textNodes[0].textContent);
            }
            else {
                //IE6/7/8 case
                textValue = $.trim($textNodes[0].text);
            }
        }
        return textValue;
    }


    /**
     * Detects if passed node has next sibling which is not a text node
     * @param 	{Object}	node	XML DOM object
     * @return 	{mixed} 	Returns found node or false if one doesn't exist
     */
    function _getRealNextSibling(node) {
        do {
            node = node.nextSibling;
        }
        while (node && node.nodeType !== 1);
        return node;
    }

    /**
     * Detects if passed node has next sibling which is not a text node
     * @param 	{Object}	node	XML DOM object
     * @return 	{mixed} 	Returns found node or false if one doesn't exist
     */
    function _getRealPreviousSibling(node) {
        do {
            node = node.previousSibling;
        }
        while (node && node.nodeType !== 1);
        return node;
    }




    function _getSiblingsCount(node) {
        var $node = $(node);
        return $node.parent().children(node.nodeName).length;
    }


    /**
     * Toggles display by swapping class name (collapsed/expanded) and toggling
     * visibility of child ULs.
     * @TODO make use of setTimeouts to address delay when many children
     * @TODO if only allowing single expanded node at a time, will need to collapse others
     */
    function _toggleNode() {
        _$event.trigger("beforeToggleNode");
        var $thisLi = $(this);
        // $thisLi.find(">ul").toggle("normal"); // animate({height:"toggle"});
        if ($thisLi.hasClass("collapsable")) {
            $thisLi.removeClass("collapsable").addClass("expandable");
        }
        else {
            $thisLi.removeClass("expandable").addClass("collapsable");
        }
        _$event.trigger("afterToggleNode");
    }


    /**
     * Returns number of XML nodes
     * @return	{Number}
     * @TODO Includes text nodes.  Should it?
     */
    function _getXmlNodeCount() {
        return $('*', _self.xml).length;
    }

    var _self = {
        xml: {}, // variable will hold the XML DOM object
        $container: $(document.body), // initialize as body, but should override with specific container
        log: function() {
        }, // empty function. installed via $.loggable()

        /**
         * Assigns handlers for editing nodes and attributes.
         * Happens only once, during renderAsHTML()
         */

        assignEditHandlers: function() {

            if (view == 0 || view == 2) {
                $("#xml")
                        .delegate("span.nodeName", "click", function() {
                            _toggleNode.apply($(this).parent().get(0));
                        })

                        .delegate("div.hitarea", "click", function() {
                            _toggleNode.apply($(this).parent().get(0));
                        })

                        .delegate("button.previewImage", "click", function(e) {

                            var $this = $(this);
                            //                    alert($this.siblings().attr("rel"));
                            var popUpURL;
                            if ($this.siblings().attr("rel") != "") {
                                popUpURL = $this.siblings().attr("rel");
                            }
                            else {
                                popUpURL = $this.siblings().attr("src");
                                popUpURL = popUpURL.replace(/=small/g, "=medium");
                            }
                            centeredPopup(popUpURL, 'myWindow', '700', '500', 'yes');
                            return false;
                        })
                        .delegate("button.clearImage", "click", function(e) {
                            var $this = $(this),
                                    node = _getNodeFromElemAttr($this);
                            _self.setNodeValue(node, "");
                            $this.siblings().attr("src", "FetchBinFile?file=empty_photo.jpg");
                            //                    alert($this.html());
                            $this.parent().children("button").remove();
                            return false;
                        })

                        .delegate("p.nodeValue", "click", function() {

                            var $this = $(this),
                                    node = _getNodeFromElemAttr($this);
                            //Had to change due to IE problems
                            if (node.getAttributeNode("sps_vocabulary")) {

                                $.post("Query", {
                                    vocabulary: node.getAttribute("sps_vocabulary"),
                                    xpath: _get_XPath(node),
                                    lang: _lang,
                                    prefix: "sps",
                                    id: node.getAttribute("sps_id")
                                }, function(response) {

                                    if (response.indexOf("<select") > -1) {
                                        var mode = "vocabulary";
                                        _self.editValueSelect($this, node, response, mode, "sps");
                                    } else {
                                        alert(_message["noVocabFound"]);
                                    }
                                }, "html")
                            }
                            else if (node.getAttributeNode("ics_vocabulary")) {
                                $.post("Query", {
                                    vocabulary: node.getAttribute("ics_vocabulary"),
                                    xpath: _get_XPath(node),
                                    lang: _lang,
                                    prefix: "ics",
                                    id: node.getAttribute("ics_id")
                                }, function(response) {
                                    if (response.indexOf("<select") > -1) {
                                        var mode = "vocabulary";
                                        _self.editValueSelect($this, node, response, mode, "ics");
                                    }
                                    else {
                                        alert(_message["noVocabFound"]);
                                    }
                                }, "html")
                            } else if (node.getAttributeNode("sps_type")) {
                                $.post("Query", {
                                    type: node.getAttribute("sps_type"),
                                    xpath: _get_XPath(node),
                                    lang: _lang,
                                    prefix: "sps",
                                    id: node.getAttribute("sps_id")
                                }, function(response) {
                                    if (response.indexOf("<select") > -1) {
                                        var mode = "entity";
                                        _self.editValueSelect($this, node, response, mode, "sps");
                                    }
                                    else {
                                        alert(_message["linkFailed"]);
                                    }
                                }, "html")
                            } else if (node.getAttributeNode("ics_type")) {
                                $.post("Query", {
                                    type: node.getAttribute("ics_type"),
                                    xpath: _get_XPath(node),
                                    lang: _lang,
                                    prefix: "ics",
                                    id: node.getAttribute("ics_id")
                                }, function(response) {

                                    if (response.indexOf("<select") > -1) {

                                        var mode = "entity";
                                        _self.editValueSelect($this, node, response, mode, "ics");
                                    } else {
                                        alert(_message["linkFailed"]);
                                    }
                                }, "html")
                            } else if (node.getAttributeNode("sps_facet")) {

                                $.post("Query", {
                                    facet: node.getAttribute("sps_facet"),
                                    xpath: _get_XPath(node),
                                    lang: _lang,
                                    prefix: "sps",
                                    id: node.getAttribute("sps_id")
                                }, function(response) {
                                    if (response.indexOf("<select") > -1) {
                                        var mode = "thesaurus";
                                        _self.editValueSelect($this, node, response, mode, "sps");
                                    }
                                    else {
                                        alert(_message["linkFailed"]);
                                    }
                                }, "html")

                            } else if (node.getAttributeNode("ics_html") || node.getAttributeNode("sps_html")) {
                                _self.editValue($this, node, _getNodeValue(node), true, "");
                            } else if (node.getAttributeNode("sps_browse")) {
                                var mode = "browse";
                                _self.editValueBrowse($this, node, "sps");
                            } else if (node.getAttributeNode("ics_browse")) {
                                mode = "browse";
                                _self.editValueBrowse($this, node, "ics");
                            } else {


                                var type = $("#type").val();
                                if (type.length > 0) {
                                    $.post("Query", {
                                        xpath: _get_XPath(node),
                                        value: _getNodeValue(node),
                                        type: type,
                                        lang: _lang
                                    }, function(response) {


                                        if (response.indexOf("type=") > -1) {
                                            _self.editValue($this, node, _getNodeValue(node), false, response);
                                        }
                                        else {
                                            alert(_message["schemaCheckFailed"]);
                                        }
                                    }, "html")
                                }
                                else {
                                    _self.editValue($this, node, _getNodeValue(node), false, "");
                                }
                            }
                        })
                        .delegate("a.addChild", "click", function(e) {
                            e.preventDefault();
                            var $this = $(this),
                                    node = _getNodeFromElemAttr($this);
                            _self.createChild($this, node);
                        })
                        .delegate("span.attrValue", "click", function() {
                            var $this = $(this),
                                    node = _getNodeFromElemAttr($this);
                            _self.editAttribute($this, node, $this.attr("name"), $this.text());
                        })
                        .delegate("button.addAttr", "click", function(e) {
                            e.preventDefault();
                            var $this = $(this),
                                    node = _getNodeFromElemAttr($this);
                            _self.createAttribute($this, node);
                        })
                        .delegate("button.killNode", "click", function(e) {
                            e.preventDefault();
                            var $this = $(this),
                                    node = _getNodeFromElemAttr($this);
                            _self.removeNode($this, node);
                        })

                        //Disable links - safest way
                        .delegate("p.nodeValue a", "click", function(e) {
                            e.preventDefault();
                            ev.stopPropagation();
                            return false;
                        })

                        //SAM
                        .delegate("button.remove", "click", function(e) {

                            //Code may seem weird. That is because until version 0.8.5 delete could
                            //only be performed one level up (father node)
                            var $this = $(this);
                            var node = _getNodeFromElemAttr($this.parent().parent().parent());
                            var $editButtonParent = $this.parent().parent().parent().parent();
                            var childName = $this.parent().parent().children(".nodeName").html();
                            var childPath = $this.parent().parent().attr("id");
                            $editButtonParent.find("ul.children > li[id='" + childPath + "']").css({
                                //                        "backgroundColor":"#ffffe0"
                                "border": "2px dotted #990000"
                            });
                            var posAsInt = parseInt(new Utils().getPosition(childPath)) - 1;
                            if (childPath != null) {
                                if (confirm(_message["removeNodeConfirm"] + childName + ");")) {
                                    _self.deleteNode(node, $editButtonParent, childName, childPath, posAsInt);
                                } else {
                                    $editButtonParent.find("ul.children > li[id='" + childPath + "']").css({
                                        //                                "backgroundColor":""
                                        "border": ""
                                    });
                                }
                            }



                        })
                        //SAM
                        .delegate("button.goDown", "click", function(e) {

                            var utils = new Utils();
                            var $this = $(this);
                            //XML part
                            var node = _getNodeFromElemAttr($this.parent());
                            var nextNode = _getRealNextSibling(node);
                            utils.swapNodes(node, nextNode);
                            //  alert(_self.getXmlAsString());
                            //HTML part

                            var htmlNode = $this.closest("li.node").get(0);
                            var nextHtmlNode = _getRealNextSibling(htmlNode);
                            var htmlNodePath = $(htmlNode).attr("id");
                            var nextHtmlNodePath = $(nextHtmlNode).attr("id");
                            var htmlNodeActions = $(htmlNode).children(".actionButtons").html();
                            var nextHtmlNodeActions = $(nextHtmlNode).children(".actionButtons").html();
                            //Get label rather than node name
                            var dataPath = $(htmlNode).attr("data-path");
                            var pathIndex = jQuery.inArray(dataPath, xpaths);
                            var nodeLabelName = labels[pathIndex];
                            //Swap paths
                            utils.setPath(htmlNode, nextHtmlNodePath, nodeLabelName);
                            utils.setPath(nextHtmlNode, htmlNodePath, nodeLabelName);
                            //Swap actions
                            $(htmlNode).children(".actionButtons").html(nextHtmlNodeActions);
                            $(nextHtmlNode).children(".actionButtons").html(htmlNodeActions);
                            utils.swapNodes(htmlNode, nextHtmlNode);
                        })

                        //SAM
                        .delegate("button.goUp", "click", function(e) {
                            var utils = new Utils();
                            var $this = $(this);
                            //XML part
                            var node = _getNodeFromElemAttr($this.parent());
                            var previousNode = _getRealPreviousSibling(node);
                            utils.swapNodes(node, previousNode);
                            //HTML part

                            var htmlNode = $this.closest("li.node").get(0);
                            var previousHtmlNode = _getRealPreviousSibling(htmlNode);
                            //    alert(previousHtmlNode.nodeName)

                            var htmlNodePath = $(htmlNode).attr("id");
                            var previousHtmlNodePath = $(previousHtmlNode).attr("id");
                            var htmlNodeActions = $(htmlNode).children(".actionButtons").html();
                            var previousHtmlNodeActions = $(previousHtmlNode).children(".actionButtons").html();
                            //Get label rather than node name
                            var dataPath = $(htmlNode).attr("data-path");
                            var pathIndex = jQuery.inArray(dataPath, xpaths);
                            var nodeLabelName = labels[pathIndex];
                            //Swap paths
                            utils.setPath(htmlNode, previousHtmlNodePath, nodeLabelName);
                            utils.setPath(previousHtmlNode, htmlNodePath, nodeLabelName);
                            //Swap actions
                            $(htmlNode).children(".actionButtons").html(previousHtmlNodeActions);
                            $(previousHtmlNode).children(".actionButtons").html(htmlNodeActions);
                            utils.swapNodes(htmlNode, previousHtmlNode);
                        })


                        //SAM
                        .delegate("button.add", "click", function(e) {
                            var utils = new Utils();
                            var $this = $(this);
                            //Create index for XML-HTML connection
                            var parentRefIndex = _nodeRefs.length;
                            //XML part (easy)
                            var node = _getNodeFromElemAttr($this.parent());
                            var $node = $(node);
                            var $cloneNode = $node.clone().attr("parentRefIndex", parentRefIndex);
                            $cloneNode.insertAfter($node);
                            //Have to update nodes stack
                            _nodeRefs.push($cloneNode.get(0));
                            //HTML part
                            var htmlNode = $this.closest("li.node").get(0);
                            var $htmlNode = $(htmlNode);
                            $htmlNode.children(".actionButtons").children("button.goDown").show();
                            var htmlNodePath = $(htmlNode).attr("id");
                            var htmlNodePathWithoutPosition = htmlNodePath.replace(/\[\d+\]/g, "");
                            var pathIndex = jQuery.inArray(htmlNodePathWithoutPosition, xpaths);
                            var min = minOccurs[pathIndex];
                            var max = maxOccurs[pathIndex];
                            var label = labels[pathIndex];
                            if (pathIndex === -1) {
                                var newPath = detectRecursion(htmlNodePathWithoutPosition); //check for recursion
                                pathIndex = jQuery.inArray(newPath, xpaths);
                                if (pathIndex === -1) {//still is -1, show node name instead
                                    label = node.nodeName;
                                } else {//label found when removing recursion
                                    min = minOccurs[pathIndex];
                                    max = maxOccurs[pathIndex];
                                    label = labels[pathIndex];
                                }
                            }
                            var nodeLabelName = label;
                            //Increment paths by 1 for siblings (Using data-path)
                            $htmlNode.nextAll('[data-path="' + htmlNodePathWithoutPosition + '"]').each(function(index) {
                                utils.setPath(this, utils.getNextPath($(this).attr("id")), nodeLabelName);
                            })


                            //Clone node and insert it after original (Also update nodeIndex attr)
                            $cloneNode = $htmlNode.clone();
                            utils.setPath($cloneNode.get(0), utils.getNextPath(htmlNodePath), nodeLabelName);
                            $cloneNode.attr("nodeIndex", parentRefIndex).insertAfter($htmlNode);
                            var $siblings = $htmlNode.parent().children('[data-path="' + htmlNodePathWithoutPosition + '"]');
                            var siblingsCount = $siblings.length;
                            if (min != max) {
                                $siblings.children(".actionButtons").children("button.add").show();
                                if (parseInt(siblingsCount) > parseInt(min)) {
                                    $siblings.children(".actionButtons").children("button.remove").show();
                                }
                            }

                            //Update allowed actions
                            $siblings.each(function(index) {
                                utils.updateAllowedActions(this, siblingsCount);
                            })

                        })

                        //SAM
                        .delegate("button.edit", "click", function(e) {

                            //Only one edit toolbar may exist at a time...
                            _self.$container.find("button.edit").show();
                            _self.$container.find("button.submit").remove();
                            _self.$container.find("button.cancel").remove();
                            _self.$container.find("select#add").remove();
                            _self.$container.find("select#remove").remove();
                            e.preventDefault();
                            var $this = $(this),
                                    node = _getNodeFromElemAttr($this);
                            var type = $("#type").val();
                            var children = "";
                            var childrenPaths = "";
                            $(node).children().each(function(index) {
                                if (index + 1 < $(node).children().length) {
                                    children = children + this.nodeName + "___";
                                    childrenPaths = childrenPaths + _get_XPath(this) + "___";
                                }
                                else {
                                    children = children + this.nodeName;
                                    childrenPaths = childrenPaths + _get_XPath(this);
                                }

                            })
                            $.post("Query", {
                                xpath: _get_XPath(node),
                                type: type,
                                children: children,
                                lang: _lang,
                                childrenPaths: childrenPaths
                            }, function(response) {
                                _self.editChildren($this, node, response);
                            }, "html")

                        })
                        .delegate("button.icon", "mouseover", function() {
                            $(this).css({
                                opacity: 0.5
                            });
                        })
                        .delegate("button.icon", "mouseout", function() {
                            $(this).css({
                                opacity: 1
                            });
                        })
                        .delegate("li.node", "mouseover", function() {

                        })
                        .delegate("li.node", "mouseout", function() {

                        });
            } else if (view == 1) { //View mode
                $("#xml")
                        .delegate("span.nodeName", "click", function() {
                            _toggleNode.apply($(this).parent().get(0));
                        })
                        .delegate("div.hitarea", "click", function() {
                            _toggleNode.apply($(this).parent().get(0));
                        })
                        .delegate("button.previewImage", "click", function(e) {

                            var $this = $(this);
                            var popUpURL;
                            if ($this.siblings().attr("rel") != "") {
                                popUpURL = $this.siblings().attr("rel");
                            } else {
                                popUpURL = $this.siblings().attr("src");
                                popUpURL = popUpURL.replace(/=small/g, "=medium");
                            }
                            centeredPopup(popUpURL, 'myWindow', '700', '500', 'yes');
                            return false;
                        })
                        .delegate("p.nodeValue", "click", function() {
                            var $this = $(this),
                                    node = _getNodeFromElemAttr($this);
                            //Had to change due to IE problems

                            if (node.getAttributeNode("sps_type")) {
                                var btnGo = "";
                                var id = node.getAttribute("sps_id");
                                if (id > 0) {
                                    $btnGo = $("<button class='submit go'>-></button>");
                                    $this.find("button").remove();
                                    $this.append($btnGo);
                                    $btnGo.click(function() {
                                        var type = node.getAttribute("sps_type");
                                        var popUpURL = document.URL.replace(/type=([^&]+)/g, "type=" + type);
                                        popUpURL = popUpURL.replace(/id=([^&]+)/g, "id=" + id);
                                        centeredPopup(popUpURL, 'myWindow', '700', '500', 'yes');
                                        $this.find("button").remove();
                                    });
                                }
                            }

                        }
                        )
                        ;
            }
        },
        /**
         * Returns HTML representation of passed node.
         * Used during initial render, as well as when creating new child nodes.
         * @param 	{Object}	node	XML DOM object
         * @param 	{String}  	state	Ex: "expandable"
         * @param 	{Boolean}	isLast Indicates whether there are additional node siblings
         * @returns {String}
         * @TODO replace anchor with button
         */
        getNewNodeHTML: function(node, state, isLast, pathSoFar) {

            var nodeDepth = _getNodeDepth(node);
            var oddOrEven;
            if (nodeDepth % 2) {
                oddOrEven = "odd";
            } else {
                oddOrEven = "even";
            }

            if (nodeDepth > _maxDepth) {
                _maxDepth = nodeDepth;
            }


            var nodeIndex = _nodeRefs.length - 1,
                    nodePath = pathSoFar + _get_XPath(node),
                    nodeValue = _getNodeValue(node),
                    nodeAttrs = _getEditableAttributesHtml(node),
                    nodeValueStr = (nodeValue) ? nodeValue : "<span class='noValue'>" + _message["noTextValue"] + "</span>";
            nodeHtml = "";
            if (nodeDepth < _depthThreshold || nodeValue.length) { //Nodes with value should be open!
                state = "collapsable";
            } else {
                state = "expandable";
            }

            var pathIndex = jQuery.inArray(nodePath.replace(/\[\d+\]/g, ""), xpaths);
            var label;
            if (pathIndex === -1) {
                var newPath = detectRecursion(nodePath); //check for recursion
                pathIndex = jQuery.inArray(newPath, xpaths);
                if (pathIndex === -1) {//still is -1, show node name instead
                    label = node.nodeName;
                } else {//label found when removing recursion
                    label = labels[pathIndex];
                }
            } else {
                label = labels[pathIndex];
            }


            //Decided to show nodePosition after all ...
            var pathParts = nodePath.split("/");
            var lastPathPart = pathParts[pathParts.length - 1];
            var positionWithBrackets = lastPathPart.substring(lastPathPart.indexOf("["), lastPathPart.length);
            if (positionWithBrackets !== lastPathPart) {
                label = label + positionWithBrackets;
            }

            var nodeFixed = "";
            if (node.getAttributeNode("sps_fixed") || node.getAttributeNode("ics_fixed")) {
                nodeFixed = "nodeFixed";
            }

            if (_isCommentNode(node)) { // display comment node disabled for now
                //                nodeHtml = '<li class="node comment '+ state + (isLast?' last':'') +'" nodeIndex="'+nodeIndex+'">' +
                //                '<div class="hitarea' + (isLast?' last':'') + '"/>' +
                //                '<span class="nodeName">comment</span><button class="killNode icon"/>' +
                //                '<ul class="nodeCore">' +
                //                '<li class="last"><p class="nodeValue">'+ nodeValueStr +'</p></li>' +
                //                '</ul>' +
                //                '</li>';
            } else { // display regular node

                visibility = "";
                if (pathIndex == -1) {
                    xpath = node.nodeName;
                } else {
                    xpath = xpaths[pathIndex];
                    if (displayValues[pathIndex] === "hidden") {
                        visibility = "style='display:none'";
                    }
                }

                nodeHtml = '<li ' + visibility + ' id="' + nodePath + '" title="' + nodePath + '" class="node ' + oddOrEven + ' ' + node.nodeName + ' ' + state + (isLast ? ' last' : '') + '" data-path="' + xpath + '" nodeIndex="' + nodeIndex + '" nodeDepth="' + nodeDepth + '"' + nodeFixed + '>' +
                        '<div class="hitarea' + (isLast ? ' last' : '') + '"/>';
                var spanStyle = "";
                var editButtonHtml = "";
                if (view !== "1") {
                    if (_getNodeType(pathIndex) === "ParentWithActions") {

                        spanStyle = 'style="vertical-align:top"';
                        editButtonHtml = '<button class="edit icon" title="' + _message["addRemove"] + '"><img  style="vertical-align:top" src="css/addRemove.png"/></button>&nbsp;';
                    }
                }

                nodeHtml = nodeHtml + '<span ' + spanStyle + ' class="nodeName">' + label + '</span>' + nodeAttrs + editButtonHtml;
                if (view !== "1") {
                    nodeHtml = nodeHtml + "<span class='actionButtons'>" + _self.createAllowedActions(node, pathIndex, nodePath) + "</span>";
                } else {
                    nodeValueStr = (nodeValue) ? nodeValue : "<span class='noValue'>" + _message["noTextValueNoEdit"] + "</span>";
                    if (_getNodeType(pathIndex) === "ParentWithActions") { //works?
                        return  nodeHtml + '</li>';
                    }
                }
                if (node.getAttributeNode("ics_browse")) {

                    if (nodeValueStr.substring(0, 4) == "<spa") {
                        nodeValueStr = "<img src='FetchBinFile?file=empty_photo.jpg'/>";
                    }
                    else {
                        var encodedNodeValueStr = encodeURIComponent(nodeValueStr);
                        nodeValueStr = "<img class='uploadedFile' title=" + nodeValueStr.replace(/\s/g, "%20") + " alt=" + nodeValueStr.replace(/\s/g, "%20") + " src='FetchBinFile?size=small&file=" + type.value + "/Photos/" + encodedNodeValueStr.replace(/\s/g, "%20") + "'/>" +
                                "<button style='position:relative;top:-50px;' class='clearImage'>X</button>" +
                                "<button style='position:relative;left:-24px;top:-3px;' class='previewImage'>-></button>";
                    }
                }
                if (node.getAttributeNode("sps_browse")) {

                    if (nodeValueStr.substring(0, 4) == "<spa") {
                        nodeValueStr = "<img src='FetchBinFile?file=empty_photo.jpg'/>";
                    } else {

                        encodedNodeValueStr = encodeURIComponent(nodeValueStr);
                        var browseValue = node.getAttribute("sps_browse");
                        if (browseValue != '') {
                            fileType = browseValue;
                            if (fileType == "audio") {
                                //Using player instead of any browser plugin...
                                nodeValueStr = "<img class='uploadedFile' title=" + nodeValueStr.replace(/\s/g, "%20") + " alt=" + nodeValueStr.replace(/\s/g, "%20") + " src='FetchBinFile?file=mp3.png' rel='../EKBMM/GetInfo?entity=" + type.value + "&file=" + encodedNodeValueStr.replace(/\s/g, "%20") + "'/>";
                            } else if (fileType == "video") {
                                nodeValueStr = "<img class='uploadedFile' title=" + nodeValueStr.replace(/\s/g, "%20") + " alt=" + nodeValueStr.replace(/\s/g, "%20") + " src='FetchBinFile?file=video.png' rel='FetchBinFile?file=" + type.value + "/Video/" + encodedNodeValueStr.replace(/\s/g, "%20") + "'/>";
                            } else if (fileType == "zip") {
                                nodeValueStr = "<img class='uploadedFile' title=" + nodeValueStr.replace(/\s/g, "%20") + " alt=" + nodeValueStr.replace(/\s/g, "%20") + " src='FetchBinFile?file=zip.png' rel='FetchBinFile?file=" + type.value + "/" + encodedNodeValueStr.replace(/\s/g, "%20") + "'/>";
                            } else if (fileType == "docs") {
                                nodeValueStr = "<img class='uploadedFile' title=" + nodeValueStr.replace(/\s/g, "%20") + " alt=" + nodeValueStr.replace(/\s/g, "%20") + " src='FetchBinFile?file=docs.png' rel='FetchBinFile?file=" + type.value + "/Documents/" + encodedNodeValueStr.replace(/\s/g, "%20") + "'/>";
                            } else if (fileType == "all") {
                                var xml = _self.getXmlAsString();
                                var archiveType = xml.substring(xml.indexOf("<type>") + 6, xml.indexOf("</type>"));
                                if (archiveType == 'Photos') {
                                    nodeValueStr = "<img class='uploadedFile' title=" + nodeValueStr.replace(/\s/g, "%20") + " alt=" + nodeValueStr.replace(/\s/g, "%20") + " src='FetchBinFile?size=small&file=" + type.value + "/Photos/" + encodedNodeValueStr.replace(/\s/g, "%20") + "' rel=''/>";
                                } else {
                                    nodeValueStr = "<img class='uploadedFile' title=" + nodeValueStr.replace(/\s/g, "%20") + " alt=" + nodeValueStr.replace(/\s/g, "%20") + "  rel='FetchBinFile?file=" + type.value + "/" + archiveType + "/" + encodedNodeValueStr.replace(/\s/g, "%20") + "'/>";
                                }
                            }
                        } else {
                            nodeValueStr = "<img class='uploadedFile' title=" + nodeValueStr.replace(/\s/g, "%20") + " alt=" + nodeValueStr.replace(/\s/g, "%20") + " src='FetchBinFile?size=small&file=" + type.value + "/Photos/" + encodedNodeValueStr.replace(/\s/g, "%20") + "' rel=''/>";
                        }
                        nodeValueStr = nodeValueStr + "<button style='position:relative;top:-50px;' class='clearImage'>X</button>" +
                                "<button style='position:relative;left:-24px;top:-3px;' class='previewImage'>-></button>";
                    }
                }

                var nodeValueClass = "nodeValue";
                if (nodeFixed != "") {
                    nodeValueClass = "nodeFixed";
                }


//console.log(xpaths[pathIndex])
//console.log(_getNodeType(pathIndex))
                if (_getNodeType(pathIndex) === "Leaf") {
                    nodeHtml = nodeHtml + '<ul class="nodeCore">' +
                            '<li><p class="' + nodeValueClass + '">' + nodeValueStr + '</p></li>' +
                            '</ul>';
                }

                //                }
                nodeHtml = nodeHtml + '</li>';
            }
            return nodeHtml;
        },
        createAllowedActions: function(node, pathIndex, nodePath) {
            //New code to add remove/add functionality...
            var goUpButtonHtml = '<button class="goUp icon" style="display:none;" title="' + _message["goUp"] + '"><img style="vertical-align:top" src="css/arrow_up.png"/></button>';
            var goDownButtonHtml = '<button class="goDown icon" style="display:none;" title="' + _message["goDown"] + '"><img  style="vertical-align:top" src="css/arrow_down.png"/></button>';
            var realPreviousSib = _getRealPreviousSibling(node)

            if (realPreviousSib != null) {
                if (realPreviousSib.nodeName == node.nodeName) {
                    goUpButtonHtml = '<button class="goUp icon" title="' + _message["goUp"] + '"><img style="vertical-align:top" src="css/arrow_up.png"/></button>';
                }
            }


            var realNextSib = _getRealNextSibling(node)
            if (realNextSib != null) {
                if (realNextSib.nodeName == node.nodeName) {
                    goDownButtonHtml = '<button class="goDown icon" title="' + _message["goDown"] + '"><img  style="vertical-align:top" src="css/arrow_down.png"/></button>';
                }
            }


            if (pathIndex == -1) {
                var min = 1;
                var max = -1;
            } else {
                var min = minOccurs[pathIndex];
                var max = maxOccurs[pathIndex];
            }

            var removeButtonHtml = "";
            var addButtonHtml = '<button class="add icon" style="display:none;" title="' + _message["duplicate"] + '"><img style="vertical-align:top" src="css/add.png"/></button>';
            if (min != max) {

                var siblingsCount = _getSiblingsCount(node);
                var addButtonStyle = "block";
                if (parseInt(max) != -1 && parseInt(siblingsCount) >= parseInt(max)) {
                    addButtonStyle = "none";
                }
                addButtonHtml = '<button class="add icon ' + addButtonStyle + '" title="' + _message["duplicate"] + '"><img style="vertical-align:top" src="css/add.png"/></button>';
                var removeButtonStyle = "none";
                if (parseInt(siblingsCount) > parseInt(min)) {
                    removeButtonStyle = "block";
                }
                removeButtonHtml = '<button class="remove icon ' + removeButtonStyle + '" title="' + _message["remove"] + '"><img  style="vertical-align:top" src="css/remove.png"/></button>';
            }
            return   addButtonHtml + removeButtonHtml + goUpButtonHtml + goDownButtonHtml;
        },
        /**
         * Renders XML as an HTML structure. Uses _traverseDOM() to render each node.
         * @see	_traverseDom
         * @TODO Explore use of documentFragment to optimize DOM manipulation
         */
        renderAsHTML: function() {

            var $parent = _self.$container.empty(),
                    $trueParent,
                    parentRefs = [], // hash of references to previous sibling's parents. used for appending next siblings
                    parentRefIndex = 0;
            _$event.trigger("beforeHtmlRendered");
            _nodeRefs = []; // initialize node references (clear cache)


            /**
             * local utility method for appending a single node
             * @param 	{Object}	node 	An XML DOM object.
             */
            function appendNode(node) {
                if (node.nodeType !== 1) { // exit unless regular node
                    return;
                }


                _nodeRefs.push(node); // add node to hash for future reference (cache)
                var $xmlPrevSib = $(node).prev(),
                        realNextSib = _getRealNextSibling(node),
                        nodeHtml = _self.getNewNodeHTML(node, _initNodeState, !realNextSib, ""),
                        $li = $(nodeHtml),
                        $ul;
                _self.log(node.nodeName, parentRefIndex);
                if ($xmlPrevSib.length) { // appending node to previous sibling's parent
                    _self.log("appending to prev sibling's parent");
                    $parent = parentRefs[$xmlPrevSib.attr("parentRefIndex")];
                    $xmlPrevSib.removeAttr("parentRefIndex");
                    $(node).attr("parentRefIndex", parentRefIndex);
                    parentRefs[parentRefIndex] = $parent;
                    parentRefIndex++;
                    $trueParent = $li;
                    $parent.append($li);
                    //Version 1.5 addition to show nodes with value
                    if ($li.hasClass("collapsable")) {
                        $li.parents().removeClass("expandable").addClass("collapsable");
                    }


                }
                else { // appending a new child
                    _self.log("appending new child");
                    if ($trueParent) {
                        $parent = $trueParent;
                        $trueParent = false;
                    }
                    /*
                     @TODO: move ul.children into getNewNodeHTML().
                     here's how: check if $parent.find("ul.children"), if so use it, if not make root UL
                     // $ul = ($parent.find(">ul.children").length) ? $parent.find(">ul.children:first") : $("<ul class='root'></ul>");
                     */
                    $ul = $("<ul class='children'></ul>").append($li);
                    $parent.append($ul);
                    //Version 1.5 addition to show nodes with value
                    if ($li.hasClass("collapsable")) {
                        $li.parents().removeClass("expandable").addClass("collapsable");
                    }



                    if (!_isCommentNode(node)) {
                        $parent = $li;
                        $(node).attr("parentRefIndex", parentRefIndex);
                        parentRefs[parentRefIndex] = $ul;
                        parentRefIndex++;
                    }
                }

            } // end of appendNode()

            _traverseDOM(_self.xml, appendNode);
            $("*", _self.xml).removeAttr("parentRefIndex"); // clean up remaining parentRefIndex-es
            _self.assignEditHandlers(); // bind in core app afterHtmlRendered
            $("button.icon").css({
                opacity: 1
            });
            _$event.trigger("afterHtmlRendered");
        },
        /**
         * Renders XML as an HTML structure. Uses _traverseDOM() to render each node.
         * @see	_traverseDom
         * @TODO Explore use of documentFragment to optimize DOM manipulation
         */
        renderSubTreeAsHTML: function($par, rootNode, rootNodePath) {

            var $parent = $par,
                    $trueParent,
                    parentRefs = [], // hash of references to previous sibling's parents. used for appending next siblings
                    parentRefIndex = _nodeRefs.length;
            _$event.trigger("beforeHtmlRendered");
            /**
             * local utility method for appending a single node
             * @param 	{Object}	node 	An XML DOM object.
             */
            var $subtree = $('<result/>');
            var root = true;
            function appendNode(node) {

                if (node.nodeType !== 1 || _isCommentNode(node)) { // exit unless regular node or comment
                    return;
                }
                _nodeRefs.push(node); // add node to hash for future reference (cache)
                var $xmlPrevSib = $(node).prev(),
                        realNextSib = _getRealNextSibling(node),
                        nodeHtml = _self.getNewNodeHTML(node, _initNodeState, !realNextSib, rootNodePath),
                        $li = $(nodeHtml),
                        $ul;
                _self.log(node.nodeName, parentRefIndex);
                if (parentRefs.length > 0 && $xmlPrevSib.length) { // appending node to previous sibling's parent

                    _self.log("appending to prev sibling's parent");
                    $parent = parentRefs[$xmlPrevSib.attr("parentRefIndex")];
                    $xmlPrevSib.removeAttr("parentRefIndex");
                    $(node).attr("parentRefIndex", parentRefIndex);
                    parentRefs[parentRefIndex] = $parent;
                    parentRefIndex++;
                    $trueParent = $li;
                    $parent.append($li);
                } else { // appending a new child
                    //alert("appending new child")
                    _self.log("appending new child");
                    if ($trueParent) {
                        $parent = $trueParent;
                        $trueParent = false;
                    }
                    /*
                     @TODO: move ul.children into getNewNodeHTML().
                     here's how: check if $parent.find("ul.children"), if so use it, if not make root UL
                     // $ul = ($parent.find(">ul.children").length) ? $parent.find(">ul.children:first") : $("<ul class='root'></ul>");
                     */
                    if (root) {
                        $subtree.append($li);
                    } else {
                        //$parent.append($("<ul class='children'></ul>").append($li));
                        $ul = $("<ul class='children'></ul>").append($li);
                        $subtree.find("li.node:last").append($ul);
                    }



                    if (!_isCommentNode(node)) {
                        $parent = $li;
                        $(node).attr("parentRefIndex", parentRefIndex);
                        parentRefs[parentRefIndex] = $ul;
                        parentRefIndex++;
                    }

                }
                if (root) {
                    root = false;
                }
            } // end of appendNode()

            _traverseDOM(rootNode, appendNode);
            $("*", _self.xml).removeAttr("parentRefIndex"); // clean up remaining parentRefIndex-es
            $("button.icon").css({
                opacity: 1
            });
            _$event.trigger("afterHtmlRendered");
            return($subtree.html());
        },
        /**
         * Sets value of node to the passed text. Existing value is overwritten,
         * otherwise new value is set.
         * @param 	{Object}	node	XML DOM object
         * @param 	{String}	value
         */
        setNodeValue: function(node, value) {

            var $textNodes = _getTextNodes(node);
            if ($textNodes.get(0)) {
                $textNodes.get(0).nodeValue = value;
            }
            else {
                if (value.indexOf("<", 0) > -1) {
                    node.appendChild(_self.xml.createCDATASection(value));
                }
                else {
                    node.appendChild(_self.xml.createTextNode(value));
                }


            }

        },
        /**
         * Sets value of node to the passed text. Existing value is overwritten,
         * otherwise new value is set.
         * @param 	{Object}	node	XML DOM object
         * @param 	{String}	value
         */
        setAttribute: function(node, attributeName, attributeValue) {
            $(node).attr(attributeName, attributeValue);
        },
        /**
         * Displays form for creating new child node, then processes its creation
         * @param 	{Object} 	$link	jQuery object
         * @param 	{Object}	node	XML DOM object
         * @TODO need to separate this into render vs modify components
         */
        editChildren: function($editButton, node, response) {

            $editButton.hide();
            $editButton.siblings("select").remove();
            $editButton.siblings("button").remove()


            var parentPath = _get_XPath(node);
            var $editButtonParent = $editButton.parent();
            var selects = response.split("^^^", 3);
            var $select1 = $(selects[0]);
            var $select2 = $(selects[1]);
            var $schemaChildrenString = $(selects[2]);
            var schemaChildren = $schemaChildrenString.val().split("___");
            var $submit1 = $("<button style='margin-left:3px;' class='submit'>" + _message["add"] + "</button>").click(processCreateChild);
            var $submit2 = $("<button style='margin-left:3px;' class='submit'>" + _message["delete"] + "</button>").click(processRemoveChild);
            var $cancel = $("<button style='margin-left:3px;' class='killChild cancel'>" + _message["cancel"] + "</button>").click(function() {
                $(this).remove();
                $submit1.remove();
                $select1.remove();
                $submit2.remove();
                $select2.remove();
                $editButtonParent.find("ul.children > li").css({
                    "backgroundColor": ""
                });
                $editButton.show();
            });
            $cancel.insertAfter($submit2.insertAfter($select2.insertAfter($submit1.insertAfter($select1.insertAfter($editButton)))));
            $select2.bind('change', function() { //yada yada });
                var childPath = $select2.val();
                $editButtonParent.find("ul.children > li").css({
                    "border": ""
                });
                $editButtonParent.find("ul.children > li[id='" + childPath + "']").css({
                    "border": "2px dotted #990000"
                });
            });
            $select2.bind('click', function() {
                var childPath = $select2.val();
                $editButtonParent.find("ul.children > li").css({
                    "border": ""

                });
                $editButtonParent.find("ul.children > li[id='" + childPath + "']").css({
                    "border": "2px dotted #990000"

                });
            });
            /**
             * Private method for creating a child node.
             */
            function processCreateChild() {

                var childName = $select1.find("option:selected").attr("data-schemaName");
                var childLabel = $select1.find("option:selected").text();
                var $childXML = $($.parseXML($select1.val().replace(/&apos;/g, "'"))).find(childName);
                var $node = $(node);
                var $parent = $editButtonParent.closest("li.node");
                _nodeRefs.push(node);
                var html = _self.renderSubTree($parent, $childXML.get(0), parentPath + "/");
                if ($node.children().length === 0 || ($node.children().length === 1 && $node.children("admin").length > 0)) {
                    if ($node.children().length === 0) {
                        $parent.append("<ul class='children'>" + html + "</ul>");
                        $parent.removeClass("expandable").addClass("collapsable"); //Automatically expand parent, since first child is added!


                    } else {
                        $parent.children("ul.children").prepend(html);
                    }
                    $node.prepend($childXML);
                } else {

                    if ($node.children(childName).length > 0) {//Already exists
                        var position = $node.children(childName).length + 1;
                        var myregexp = new RegExp("(\\?<!path=\")" + parentPath + "/" + childName, "g");
                        html = html.replace(myregexp, "$&[" + position + "]");
                        var $html = $(html);
                        //Next line added to show position in label
                        $html.children(".nodeName").html(childLabel + "[" + position + "]");
                        //Next 3 lines added to update paths in new subtree
                        var utils = new Utils();
                        utils.updateSubtree($html, parentPath + "/" + childName + "[" + position + "]");
                        utils.updateAllowedActions($html.get(0), position);
                        var $index = $node.children(childName + ":last").attr('parentRefIndex');
                        //XML
                        $childXML.insertAfter($node.children(childName + ":last"));
                        //HTML
                        $html.insertAfter($parent.find('[nodeindex="' + $index + '"]'));
                        //Code added to show remove button if element may be removed!
                        var remainingSibsCount = $node.children(childName).length;
                        var childId = parentPath + "/" + childName + "[" + position + "]";
                        var pathWithoutPos = childId.replace(/\[\d+\]/g, "");
                        var pathIndex = jQuery.inArray(pathWithoutPos, xpaths);
                        if (parseInt(remainingSibsCount) === parseInt(minOccurs[pathIndex])) {

                        } else { //Last node. Cannot go down, but it may be deleted!
                            //
                            var $actionButtons = $parent.find("ul.children > li[id='" + childId + "']").children(".actionButtons");
                            $actionButtons.children("button.remove").show();
                            $actionButtons.children("button.goDown").hide();
                            $parent.find('[nodeindex="' + $index + '"]').children(".actionButtons").children("button.goDown").show();
                        }


                    } else {
                        //Replace with following cause IE does not support indexOf...
                        var childIndex = jQuery.inArray(childName, schemaChildren)
                        var found = false;
                        for (var i = childIndex - 1; i >= 0; i--) {
                            if (($node.children(schemaChildren[i]).length) > 0) {

                                $index = $node.children(schemaChildren[i] + ":last").attr('parentRefIndex');
                                $childXML.insertAfter($node.children(schemaChildren[i] + ":last"));
                                //HTML
                                $(html).insertAfter($parent.find('[nodeindex="' + $index + '"]'));
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            for (i = childIndex + 1; i < schemaChildren.length; i++) {
                                if (($node.children(schemaChildren[i]).length) > 0) {
                                    $index = $node.children(schemaChildren[i] + ":first").attr('parentRefIndex');
                                    $childXML.insertBefore($node.children(schemaChildren[i] + ":first"));
                                    //HTML
                                    $(html).insertBefore($parent.find('[nodeindex="' + $index + '"]'));
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                var id = $(html).closest("li.node").attr("nodeIndex");
                var $child = $parent.find('[nodeindex="' + id + '"]');
                $child.find(">span.nodeName").css({
                    backgroundColor: "#fffc00"
                }).animate({
                    backgroundColor: "#ffffff"
                }, 1500);
                $child.find("button.icon").css({
                    opacity: 1
                });
                $submit1.remove();
                $select1.remove();
                $submit2.remove();
                $select2.remove();
                $cancel.remove();
                $editButton.show();
            }

            /**
             * Private method for creating a child node.
             */
            function processRemoveChild() {
                var childName = $select2.find("option:selected").text();
                var childPath = $select2.val();
                var posAsInt = parseInt(new Utils().getPosition(childPath)) - 1;
                if (childPath != null) {



                    if (confirm(_message["removeNodeConfirm"] + childName + ");")) {
                        _self.deleteNode(node, $editButtonParent, childName, childPath, posAsInt);
                    }
                }
                $submit1.remove();
                $select1.remove();
                $submit2.remove();
                $select2.remove();
                $cancel.remove();
                $editButton.show();
            }




        },
        /**
         * Returns string representation of private XML object
         * @return	{String}
         */
        getXmlAsString: function() {
            return (typeof XMLSerializer !== "undefined") ?
                    (new window.XMLSerializer()).serializeToString(_self.xml) :
                    _self.xml.xml;
        },
        /**
         * Converts passed XML string into a DOM element.
         * @param 		{String}			xmlStr
         * @return		{Object}			XML DOM object
         * @exception	{GeneralException}	Throws exception if no XML parser is available.
         * @TODO Should use this instead of loading XML into DOM via $.ajax()
         */
        getXmlDOMFromString: function(xmlStr) {
            if (window.ActiveXObject && window.GetObject) {

                var dom = new ActiveXObject('Microsoft.XMLDOM');
                dom.async = false;
                dom.loadXML(xmlStr);
                if (dom.parseError && dom.parseError.errorCode != 0) {
                    errorMsg = "XML Parsing Error: " + dom.parseError.reason
                            + " at line " + dom.parseError.line
                            + " at position " + dom.parseError.linepos;
                    alert(errorMsg);
                }



                return dom;
            }
            if (window.DOMParser) {

                return new DOMParser().parseFromString(xmlStr, 'text/xml');
            }
            throw new Error('No XML parser available');
        },
        /**
         * Displays form for creating a new attribute and assigns handlers for storing that value
         * @param	{Object} 	$addLink 	jQuery object
         * @param 	{Objecct}	node
         * @TODO Try using an HTML block (string) instead, and assign handlers using delegate()
         */
        createAttribute: function($addLink, node) {
            var $parent = $addLink.parent(),
                    $form = $("<form></form>"),
                    $name = $("<input type='text' class='newAttrName'  name='attrName'  value=''/>"),
                    $value = $("<input type='text' class='newAttrValue' name='attrValue' value=''/>"),
                    $submit = $("<button>Create Attribute</button>"),
                    $cancel = $("<button class='cancel'>Cancel</button>");
            /**
             * Private function for processing the values.
             */
            function processNewAttribute() {
                var aName = $name.val(),
                        aValue = $value.val();
                try {
                    $(node).attr(aName, aValue);
                }
                catch (e) {
                    GLR.messenger.inform({
                        msg: _message["invalidAttrName"],
                        mode: "error"
                    });
                    $name.val("").focus();
                    return false;
                }
                $form.remove();
                $("<span class='singleAttr'>" + aName + "=\"<span class='attrValue' name='" + aName + "'>" +
                        ((aValue === "") ? "&nbsp;" : aValue) + "</span>\"</span>").insertBefore($addLink);
                $parent
                        .find("span.attrValue:last")
                        .click(function(e) {
                            e.stopPropagation();
                            _self.editAttribute($(this), node, aName, aValue);
                        });
                $addLink.show();
            } // end of processNewAttribute()
            $form.submit(function() {
                return false;
            })
                    .append($name)
                    .append("<span class='equals'>=</span>")
                    .append($value)
                    .append($submit)
                    .append($cancel);
            $addLink.hide();
            $parent.append($form);
            $form.find("input").click(function(e) {
                e.stopPropagation();
            });
            $form.find("input.newAttrName").bind("keydown", function(e) {
                if (e.keyCode == 13 || e.keyCode == 27) {
                    return false;
                }
            });
            $form.find("input.newAttrValue").bind("keydown", function(e) {
                if (e.keyCode == 13 || e.keyCode == 27) {
                    processNewAttribute();
                }
            });
            $name.focus();
            $submit.click(function(e) {
                e.stopPropagation();
                e.preventDefault();
                processNewAttribute();
            });
            $cancel.click(function(e) {
                e.stopPropagation();
                $form.remove();
                $addLink.show();
            });
        },
        /**
         * Displays form for editing selected attribute and handles storing of that value.
         * @param {Object}	$valueWrap
         * @param {Object}	node
         * @param {String}	name
         * @param {String}	value
         */
        editAttribute: function($valueWrap, node, name, value) {
            var fieldWidth = parseInt($valueWrap.width()) + 30,
                    $field = $("<input type='text' name='' value='" + value + "' style='width:" + fieldWidth + "px;'/>"),
                    $killAttr = $("<button class='killAttr icon'/>").click(function(e) {
                e.stopPropagation();
                if (confirm(_message["removeAttrConfirm"])) {
                    $(node).removeAttr(name);
                    $(this).parent().remove();
                }
            });
            /**
             *
             */
            function updateAttribute() {
                value = $field.val();
                $(node).attr(name, value); // update value in XML
                $field.remove();
                $killAttr.remove();
                if (value === "")
                    value = "&nbsp;"
                $valueWrap.html(value).show();
            }
            $valueWrap.hide().after($field);
            $valueWrap.parent().append($killAttr);
            $field.get(0).focus();
            $field
                    .bind("keydown", function(e) {
                        if (e.keyCode == 13 || e.keyCode == 27) {
                            updateAttribute();
                        }
                    })
                    .click(function(e) {
                        e.stopPropagation();
                    });
        },
        /**
         * Displays form for editing text value of passed node, then processes new value
         * @param {Object}	$valueWrap
         * @param {Object}	node
         * @param {String}	name
         * @TODO Wrap in form.editValue
         * @TODO use delegate()
         */
        editValue: function($valueWrap, node, value, html, schemaCheckResponse) {
            //              alert("1="+value);
            var example = "";
            if (!html && schemaCheckResponse.length > 0) {
                var schemaChecks = schemaCheckResponse.split("^^^");
                for (var i = 0; i < schemaChecks.length; i++) {
                    var schemaCheckName = schemaChecks[i].split("=", 2)[0];
                    var schemaCheckValue = schemaChecks[i].split("=", 2)[1];
                    var valueType, valueLength, valueMinLength, valueMaxLength, valuePatterns, valueEnums;
                    if (schemaCheckName == "type") {
                        valueType = schemaCheckValue;
                    } else if (schemaCheckName == "length") {
                        valueLength = schemaCheckValue;
                    }
                    else if (schemaCheckName == "minLength") {
                        valueMinLength = schemaCheckValue;
                    } else if (schemaCheckName == "maxLength") {
                        valueMaxLength = schemaCheckValue;
                    } else if (schemaCheckName == "patterns") {
                        valuePatterns = schemaCheckValue;
                    } else if (schemaCheckName == "enumerations") {
                        valueEnums = schemaCheckValue;
                    }
                }

                //Create placeholders depending on type
                var trimmedValueType = valueType.replace(/^\s+|\s+$/g, '')
                example = trimmedValueType;
                if (_message[trimmedValueType]) {
                    example = _message["example"] + _message[trimmedValueType];
                }


                var valueInfoAsHTML = "<label>( " + _message["type"] + ":" + valueType;
                var lengthAsHTML = " ";
                var minLengthAsHTML = " ";
                var maxLengthAsHTML = " ";
                if (valueMinLength) {
                    minLengthAsHTML = valueMinLength;
                }
                if (valueMaxLength) {
                    maxLengthAsHTML = valueMaxLength;
                }

                if (valueLength) {
                    lengthAsHTML = valueLength;
                } else {
                    lengthAsHTML = minLengthAsHTML + " - " + maxLengthAsHTML;
                }

                if (lengthAsHTML.replace(/^\s+|\s+$/g, '').replace(/-/g, "").length > 0) {
                    valueInfoAsHTML = valueInfoAsHTML + ", " + _message["length"] + ":" + lengthAsHTML;
                }

                valueInfoAsHTML = valueInfoAsHTML + " )</label>";
            }
            var $field, nodePath;
            var componentType;
            var classOfButtons = "editTextValueButtons";
            if (valueEnums) {
                componentType = "select";
                var enumsTable = valueEnums.split("###___###");
                var inputList = "<select>";
                for (i = 0; i < enumsTable.length; i++) {
                    var en = enumsTable[i];
                    inputList = inputList + "<option value='" + en + "'>" + en + "</option>";
                }
                inputList = inputList + "<select>";
                $field = $(inputList);
            } else {

                if (jQuery.browser.msie || ((!valueMaxLength || valueMaxLength > 50) && !valuePatterns)) {
                    componentType = "textarea";
                    classOfButtons = "editTextAreaValueButtons";
                    nodePath = _get_XPath(node) + "Area";
                    $field = $("<textarea id='" + nodePath + "' placeholder='" + example + "' maxLength='" + valueMaxLength + "'>" + value + "</textarea>");
                }
                else {
                    componentType = "input";
                    var pattern = "";
                    if (valuePatterns) {
                        var pattern = valuePatterns.replace(/###___###/g, "|");
                    }

                    var size = "50";
                    if (!valueMaxLength) {
                        valueMaxLength = "";
                    }
                    else {
                        size = valueMaxLength;
                    }
                    nodePath = _get_XPath(node) + "Input";
                    $field = $("(<input onkeyup='check(this);' title='" + example + "'  type='text' name='" + nodePath + "' id='" + nodePath + "' size='" + size + "' pattern=\"" + pattern + "\" placeholder='" + example + "' maxLength='" + valueMaxLength + "' value='" + value + "'/>");
                }
            }


            var $info = $(valueInfoAsHTML);
            var $btnSubmit = $("<input name='sub' type='submit' style='float:left;' value='OK'/>");
            var $btnCancel = $("<button class='cancel' style='float:right;'>" + _message["cancel"] + "</button>");
            var $btnWrap = $("<div class='" + classOfButtons + "'></div>").append($btnCancel).append($btnSubmit);
            var $editForm = $("<form id='edit-form' action='javascript:void(0);' method='post'></form>").append($field).append($info).append($btnWrap);
            $valueWrap.hide().parent().append($editForm);
            //SAM's tricks


            if (componentType == "textarea") {
                $field.autosize();
            }
            $field.get(0).focus();
            if (html) {
                var xml = _self.getXmlAsString();
                var $test = $($.parseXML(xml)).find("Entity").children("Link");
                var selectOptions = {};
                $test.each(function() {
                    var $link = $(this);
                    if ($link.text().length > 0) {
                        if (this.getAttributeNode("ics_type")) {
                            selectOptions["Related_" + $link.attr("ics_type") + "_" + $link.attr("ics_id")] = $link.text();
                        } else {
                            selectOptions["Related_" + $link.attr("sps_type") + "_" + $link.attr("sps_id")] = $link.text();
                        }
                    }
                });
                //Change because of change at InfoText schema 
                $test = $($.parseXML(xml)).find("Components").children("Component").children("Link");
                $test.each(function() {
                    var $link = $(this);
                    if ($link.text().length > 0) {
                        if (this.getAttributeNode("ics_type")) {
                            selectOptions["Components_" + $link.attr("ics_type") + "_" + $link.attr("ics_id")] = $link.text();
                        } else {

                            selectOptions["Components_" + $link.attr("sps_type") + "_" + $link.attr("sps_id")] = $link.text();
                        }
                    }
                });
                var myNicEditor = new nicEditor({
                    buttonList: ['bold', 'italic', 'underline', 'strikeThrough', 'subscript', 'superscript', 'link', 'link2', 'unlink'],
                    maxHeight: 200,
                    selectOptions: selectOptions

                }
                //  fullPanel : true
                ).panelInstance(nodePath, {
                    hasPanel: true
                });
                $(myNicEditor).width("600px;");
            }



            $editForm.submit(function() {
                if (html) {
                    var content = myNicEditor.instanceById(nodePath).getContent();
                    myNicEditor.removeInstance(nodePath);
                    _self.setNodeValue(node, content); // update XML node value "<![CDATA["+content+"]]>"
                }
                else {

                    value = $field.val();
                    _self.setNodeValue(node, value);
                }



                if (html) {

                    if ($.trim(content.replace(/&nbsp;/g, "").replace(/<[^>]+>/g, "")).length == 0) {
                        var $emptyValue = $("<span class='noValue'>" + _message["noTextValue"] + "</span>");
                        value = $emptyValue.html();
                        $valueWrap.text(value).show().parent().find("textarea, div.editTextAreaValueButtons").remove();
                    } else {
                        $valueWrap.contents().remove();
                        $valueWrap.append(content).show().parent().find("textarea, div.editTextAreaValueButtons").remove();
                    }
                } else {
                    if ($.trim(value).length == 0) {
                        var $emptyValue = $("<span class='noValue'>" + _message["noTextValue"] + "</span>");
                        value = $emptyValue.html();
                    }
                    $valueWrap.text(value).show().parent().find("form, textarea,input,label, div.editTextValueButtons").remove();
                }
            });
            $btnCancel.click(function() {
                if (html) {
                    myNicEditor.removeInstance(nodePath);
                }
                $valueWrap.show().parent().find("form, textarea,label, input, div.editTextValueButtons").remove();
            });
        },
        editValueBrowse: function($valueWrap, node, prefix) {
            //            var imageId = "$valueWrap.parent().parent().parent().attr("id");
            $uploadForm = $("<form id='upload-form' action='' method='post' enctype='multipart/form-data'>"
                    + "<div id='jquery-wrapped-fine-uploader'></div>"
                    + "</form>");
            var fileType = "photo"; //Default value
            var allowedExtensions = ['jpeg', 'jpg', 'gif', 'png'];
            var uploadMessage = _message["uploadImageMessage"];
            var browseValue = node.getAttribute(prefix + "_browse");
            if (browseValue != '') {
                fileType = browseValue;
                if (fileType == "audio") {
                    allowedExtensions = ['mp3'];
                    uploadMessage = _message["uploadAudioMessage"];
                } else if (fileType == "video") {
                    allowedExtensions = ['zip', 'mp4'];
                    uploadMessage = _message["uploadVideoMessage"];
                } else if (fileType == "zip") {
                    allowedExtensions = ['zip'];
                    uploadMessage = _message["uploadZipMessage"];
                } else if (fileType == "docs") {
                    allowedExtensions = ['doc', 'docx', 'pdf'];
                    uploadMessage = _message["uploadDocsMessage"];
                } else if (fileType == 'all') {
                    allowedExtensions = ['jpeg', 'jpg', 'gif', 'png', 'mp3', 'zip', 'mp4', 'doc', 'docx', 'pdf', 'avi', 'mpeg', 'dwg'];
                    uploadMessage = _message["uploadAllMessage"];
                }
            }

            var $btnSubmit = $("<button class='ok' style='float:left;'>OK</button>");
            var $btnCancel = $("<button class='cancel' style='float:right;'>" + _message["cancel"] + "</button>");
            var $btnWrap = $("<div class='editTextValueButtons'></div>").append($btnCancel).append($btnSubmit);
            $valueWrap.hide().parent().append($uploadForm).append($btnWrap);
            $valueWrap.parent().find('.qq-upload-button').css('background-image', 'url(' + $valueWrap.attr("src") + ')');
            var url;
            var binaryUrl = "";
            var filename;
            $valueWrap.parent().find('#jquery-wrapped-fine-uploader').fineUploader({
                request: {
                    endpoint: 'UploadReceiver?type=' + type.value + '&fileType=' + fileType + '&id=' + id.value
                },
                multiple: false,
                validation: {
                    allowedExtensions: allowedExtensions

                },
                text: {
                    uploadButton: uploadMessage
                },
                failedUploadTextDisplay: {
                    mode: 'custom',
                    maxChars: 40,
                    responseProperty: 'error',
                    enableTooltip: true
                },
                debug: false
            }).on('complete', function(event, id, fileName, responseJSON) {
                if (responseJSON.success) {
                    filename = responseJSON.filename;
                    var mime = responseJSON.mime
                    _self.setNodeValue(node, responseJSON.filename);
                    if (fileType == "audio") {
                        url = "FetchBinFile?file=mp3.png";
                        binaryUrl = "FetchBinFile?file=" + type.value + "/Audio/" + encodeURIComponent(responseJSON.filename);
                    } else if (fileType == "video") {
                        url = "FetchBinFile?file=video.png";
                        binaryUrl = "FetchBinFile?file=" + type.value + "/Video/" + encodeURIComponent(responseJSON.filename);
                    } else if (fileType == "zip") {
                        url = "FetchBinFile?file=zip.png";
                        binaryUrl = "FetchBinFile?file=" + type.value + "/" + encodeURIComponent(responseJSON.filename);
                    } else if (fileType == "docs") {
                        url = "FetchBinFile?file=docs.png";
                        binaryUrl = "FetchBinFile?file=" + type.value + "/Documents/" + encodeURIComponent(responseJSON.filename);
                    } else if (fileType == "all") {
                        if (mime == "Photos") {
                            url = "FetchBinFile?size=small&file=" + type.value + "/" + mime + "/" + encodeURIComponent(responseJSON.filename);
                            _self.setNodeValue()
                        } else {
                            url = "FetchBinFile?file=" + type.value + "/" + mime + "/" + encodeURIComponent(responseJSON.filename);
                            binaryUrl = "FetchBinFile?file=" + type.value + "/" + mime + "/" + encodeURIComponent(responseJSON.filename);
                        }
                    } else {
                        url = "FetchBinFile?size=small&file=" + type.value + "/Photos/" + encodeURIComponent(responseJSON.filename);
                    }

                    $valueWrap.parent().find('.qq-upload-button').css('background-image', 'url("' + url + '")');
                }
            });
            $btnSubmit.click(function() {
                $valueWrap.show().parent().find("form, input, div.editTextValueButtons, button").remove();
                $valueWrap.children("img").attr("src", url).attr("title", filename).attr("alt", filename).attr("rel", binaryUrl);
                var $btnClear = $("<button style='position:relative;top:-50px;' class='clearImage'>X</button>");
                var $btnPreview = $("<button style='position:relative;left:-27px;top:-3px;' class='previewImage'>-></button>");
                $valueWrap.append($btnClear).append($btnPreview);
                $valueWrap.show();
            });
            $btnCancel.click(function() {

                $valueWrap.show().parent().find("form, input, div.editTextValueButtons").remove();
            });
        },
        /**
         * Displays form for editing text value of passed node, then processes new value
         * @param {Object}	$valueWrap
         * @param {Object}	node
         * @param {String}	name
         * @TODO Wrap in form.editValue
         * @TODO use delegate()
         */
        editValueSelect: function($valueWrap, node, response, mode, prefix) {
            var nodePath = _get_XPath(node)
            var $field = $(response),
                    $btnSubmit = $("<button class='submit' style='float:left;'>ΟΚ</button>"),
                    $btnCancel = $("<button class='cancel' style='float:right;'>" + _message["cancel"] + "</button>"),
                    $btnWrap = $("<div class='editTextValueButtons'></div>").append($btnCancel).append($btnSubmit);
            if (mode === "entity") {
                var $btnGo = $("<button class='submit go'>-></button>");
                var links = "";

                var $optgroups = $field.find("optgroup");
                var linkEntitiesCount = $optgroups.length;

                if (linkEntitiesCount > 1) {
                    $optgroups.each(function() {
                        var entityType = $(this).attr("label");
                        var popUpURL = "File?action=New&type=" + entityType + "&lang=" + lang;
                        var clickScript = "centeredPopup('" + popUpURL + "', 'myWindow', '700', '500', 'yes');return false;";

                        links = links + "<a href='#' onclick=\"" + clickScript + "\">" + entityType + "</a>";

                    })

                    $btnNew = $('<div class="dropdown">' +
                            '<button class="dropbtn">' + _message["createNew"] + '</button>' +
                            '<div class="dropdown-content">' +
                            links +
                            '</div>' +
                            '</div>');
                } else {
                    var entityType = $optgroups.attr("label");
                    var popUpURL = "File?action=New&type=" + entityType + "&lang=" + lang;
                    var clickScript = "centeredPopup('" + popUpURL + "', 'myWindow', '700', '500', 'yes');return false;";
                    var $btnNew = $("<button class='submit createNew' onclick=\"" + clickScript + "\">" + _message["createNew"] + "</button>");
                }
                $valueWrap.hide().parent().append($field).append($btnGo).append($btnNew).append($btnWrap);
            }
            else if (mode === "vocabulary") {
                var btnVoc = "";
                $btnVoc = $("<button id='voc'>+</button>");
                $valueWrap.hide().parent().append($field).append($btnVoc).append($btnWrap);
            } else if (mode === "thesaurus") {
                $btnTh = $("<button id='th'>TH</button>");
                $valueWrap.hide().parent().append($field).append($btnTh).append($btnWrap);
            } else {
                $valueWrap.hide().parent().append($field).append($btnWrap);
            }
            $field.get(0).focus();
            //Chosen plugin!

            $field.parent().find("select").chosen({
                search_contains: true
            });
            $btnSubmit.click(function() {
                var select = $(this).parent().parent()[0].getElementsByTagName("select")[0];
                value = $field.val();
                var id = select.options[select.selectedIndex].getAttribute("data-id");
                _self.setNodeValue(node, value);
                _self.setAttribute(node, prefix + "_id", id);
                if (mode == "entity") {
                    type = select.options[select.selectedIndex].getAttribute("data-type");
                    _self.setAttribute(node, prefix + "_type", type);
                } else if (mode === "thesaurus") {
                    facet = select.getAttribute("data-facet");
//                    _self.setAttribute(node, prefix + "_facet", facet); //Not needed, facet is in LaAndLi
                } else {
                    vocabulary = select.getAttribute("data-vocabulary");
                    _self.setAttribute(node, prefix + "_vocabulary", vocabulary);
                }
                $valueWrap.text(value).show().parent().find("select, button, div.editTextValueButtons,input,b, div").remove();
            });
            if (mode == "entity") {
                var select = $field.get(0);
                var data_type = select.options[select.selectedIndex].getAttribute("data-type");
                $field.change(function() {

                    var imagePath = select.options[select.selectedIndex].getAttribute("image-path")
                    $image = $('<img>', {
                        src: "FetchBinFile?size=small&file=" + data_type + "/Photos/" + imagePath,
                        height: 36,
                        title: $field.val()
                    });
                    $valueWrap.hide().parent().append($field).find("img").remove();
                    $valueWrap.hide().parent().append($field).append($image).append($btnGo).append($btnWrap);
                });
                //add photo next to combo

                if (photoTypes.indexOf(data_type) > -1) {

                    var imagePath = select.options[select.selectedIndex].getAttribute("image-path");
                    $image = $('<img>', {
                        src: "FetchBinFile?size=small&file=" + data_type + "/Photos/" + imagePath,
                        height: 36,
                        title: $field.val()

                    });
                    $valueWrap.hide().parent().append($field).append($image).append($btnGo).append($btnWrap);
                }


                $btnGo.click(function() {
                    var select = $(this).parent().parent()[0].getElementsByTagName("select")[0];
                    value = $field.val();
                    var id = select.options[select.selectedIndex].getAttribute("data-id");
                    if (id > 0) {
                        var type = select.options[select.selectedIndex].getAttribute("data-type");
                        var popUpURL = document.URL.replace(/type=([^&]+)/g, "type=" + type);
                        popUpURL = popUpURL.replace(/id=([^&]+)/g, "id=" + id);
                        popUpURL = popUpURL.replace(/type=/g, "action=view&type=");//Opens entity always in view mode!
                        centeredPopup(popUpURL, 'myWindow', '700', '500', 'yes');
                    }
                });
            } else if (mode == "vocabulary") {
                //adds terms to vocabulary
                $btnVoc.click(function() {
                    $(this).hide();
                    var $newTermDiv = $("<div class='newTermDiv' style='margin:3px;padding:3px;width:400px;border:1px dotted black;'></div>");
                    $boldText = $("<b> " + _message['newTerm'] + "</b>");
                    $textVoc = $("<input type='text' style='width:250px; height:18px'  id='newvoc' value=''>" + "</input>");
                    $btnAddTerm = $("<button class='submit' id='addTermButton'>OK</button>");
                    $btnCancelVoc = $("<button class='subbmit' id='removeTermButton'>X</button>");
                    $newTermDiv = $newTermDiv.append($boldText).append($textVoc).append($btnAddTerm).append($btnCancelVoc);
                    $valueWrap.hide().parent().append($field).append($(this)).append($newTermDiv).append($btnWrap);
                    $btnAddTerm.click(function() {
                        var thisInput = $(this).siblings("input");
                        var term = thisInput.val();
                        term = term.toString();
                        var select = $(this).parent().parent()[0].getElementsByTagName("select")[0];
                        if (term == '') {
                            alert(_message.noTermMsg);
                            return false;
                        }
                        for (var i = 0; i < select.options.length; i++) {

                            var savedTerm = select.options[i].getAttribute("value");
                            if (savedTerm == term) {
                                alert(_message["existsTerm"]);
                                return false;
                            }
                        }
                        vocabulary = select.getAttribute("data-vocabulary");
                        var id;
                        var $select = $(($valueWrap).parent().parent()[0].getElementsByTagName("select")[0]);
                        $.post("File", {
                            action: "SaveVocTerm",
                            newTerm: term,
                            lang: _lang,
                            vocFile: vocabulary
                        }, function(response) {
                            id = response.replace(/\s+/g, "");
                            $option = $("<option value='" + term + "' data-id='" + id + "'>" + term + "</option>");
                            $select.append($option);
                            $select.trigger("liszt:updated");
                        }, "html");
                        alert(_message['addedTerm']);
                        _self.setAttribute(thisInput, "value", "");
                    });
                    $btnCancelVoc.click(function() {
                        var voc = $(this).parent().siblings("button#voc");
                        voc.show();
                        $valueWrap.show().parent().find("div.newTermDiv").remove();
                        $valueWrap.hide().parent().append($field).append(voc).append($btnWrap);
                    });
                });
            } else if (mode === "thesaurus") {
                $btnTh.click(function() {
                    var select = $(this).parent().parent()[0].getElementsByTagName("select")[0];
                    value = $field.val();
                    var selectedValueId = select.options[select.selectedIndex].getAttribute("data-id");

                    var thesaurusName = select.getAttribute("data-thesaurusName");
                    var username = select.getAttribute("data-username");
                    var themasURL = select.getAttribute("data-themasURL");
                    var facet = select.getAttribute("data-facet");


                    if (selectedValueId >= 0) {
                        var popUpURL = "Service?themasURL=" + themasURL + "&username=" + username + "&thesaurusName=" + thesaurusName + "&facetId=" + facet + "&termId=" + selectedValueId;
                        centeredPopup(popUpURL, 'myWindow', '700', '500', 'yes');
                    }
                });
            }

            $btnCancel.click(function() {
                $valueWrap.show().parent().find("select,button, div.editTextValueButtons,button#voc,img, div, input, b").remove();
            });
        },
        deleteNode: function(node, $editButtonParent, childName, childPath, posAsInt) {

            //No need for position anymore...deleting it
            childName = childName.replace(/\[\d+\]/g, "")

            var $childToRemove = $editButtonParent.find("ul.children > li[id='" + childPath + "']");
            var className = $childToRemove.attr("class");
            var dataPath = $childToRemove.attr("data-path"); //NEW FEATURE
            var nameToDelete = className.split(" ")[2]; //From 1 to 2 due to odd-even class addition (DELETE BUG)

            $(node).children(nameToDelete + ":eq(" + posAsInt + ")").remove();
            var $prev = $editButtonParent.prev();
            if ($prev.length) {
                $prev.addClass("last");
                $prev.find(">div.hitarea").addClass("last");
            }

            var utils = new Utils();
            var htmlNodePathWithoutPosition = childPath.replace(/\[\d+\]/g, "");
            var pathIndex = jQuery.inArray(htmlNodePathWithoutPosition, xpaths);
            var label = labels[pathIndex];
            var nodeLabelName = label;
            //Find following siblings with class name that starts the same way as the one removed and change
            //id and title to reflect new position
            $editButtonParent.find("ul.children > li[id='" + childPath + "']").nextAll('[data-path="' + dataPath + '"]').each(function(index) {

                utils.setPath(this, utils.getPreviousPath($(this).attr("id")), nodeLabelName); //Νεα λογική

            });
            //Code added to hide remove button if element cannot be removed!
            var remainingSibsCount = $(node).children(nameToDelete).length;
            if (parseInt(remainingSibsCount) == parseInt(minOccurs[pathIndex])) {
                $editButtonParent.find("ul.children > li[data-path='" + dataPath + "']").each(function(index) {
                    $(this).children(".actionButtons").children("button.remove").hide();
                });
            }

            if (remainingSibsCount == 1) {
                $editButtonParent.find("ul.children > li[data-path='" + dataPath + "']").each(function(index) {

                    $(this).children(".actionButtons").children("button.goUp").hide();
                    $(this).children(".actionButtons").children("button.goDown").hide();
                });
            }


            $childToRemove.remove();
            GLR.messenger.inform({
                msg: _message["removeNodeSucess"],
                mode: "success"
            });
        },
        /**
         * Removes node from XML (and from displayed HTML representation).
         * @param	{Object}	$link
         * @param 	{String}	name
         * @return	{Boolean}
         */
        removeNode: function($link, node) {
            if (confirm(_message["removeNodeConfirm"])) {
                $(node).remove();
                var $prev = $link.parent().prev();
                if ($prev.length) {
                    $prev.addClass("last");
                    $prev.find(">div.hitarea").addClass("last");
                }
                $link.parent().remove();
                GLR.messenger.inform({
                    msg: _message["removeNodeSucess"],
                    mode: "success"
                });
                return true;
            }
            return false;
        },
        /**
         * Loads file path from the first argument via Ajax and makes it available as XML DOM object.
         * Sets the $container which will hold the HTML tree representation of the XML.
         * @param {String}		xmlPath          	Path to XML file
         * @param {String}		containerSelector	CSS query selector for creating jQuery reference to container
         * @param {Function}	callback
         */
        loadXmlFromFile: function(xmlPath, containerSelector, callback) {
            _self.$container = $(containerSelector);
            $.ajax({
                type: "GET",
                async: false,
                url: xmlPath,
                dataType: "xml",
                error: function() {
                    GLR.messenger.show({
                        msg: _message["xmlLoadProblem"],
                        mode: "error"
                    });
                },
                success: function(xml) {
                    GLR.messenger.show({
                        msg: _message["xmlLoadSuccess"],
                        mode: "success"
                    });
                    _self.xml = xml;
                    callback();
                }
            });
        },
        /**
         * Creates a DOM representation of passed xmlString and stores it in the .xml property
         * @param {String}		xmlPath          	Path to XML file
         * @param {String}		containerSelector	CSS query selector for creating jQuery reference to container
         * @param {Function}	callback
         */
        loadXmlFromString: function(xmlString, containerSelector, callback) {
            _self.$container = $(containerSelector);
            _self.xml = _self.getXmlDOMFromString(xmlString);
            callback();
        },
        setDepth: function(depth) {
            _depthThreshold = depth;
        },
        setLang: function(lang) {
            _lang = lang;
        },
        showAttrs: function() {
            _showAttributes = true;
        },
        /**
         * Calls methods for generating HTML representation of XML, then makes it collapsible/expandable
         */
        renderTree: function() {

            GLR.messenger.inform({
                msg: _message["renderingHtml"],
                mode: "loading"
            });
            _self.renderAsHTML();
            _self.$container.find("ul:first").attr('id', 'lalakis').addClass("treeview");
            GLR.messenger.inform({
                msg: _message["readyToEdit"],
                mode: "success"
            });
        },
        /**
         * Calls methods for generating HTML representation of XML, then makes it collapsible/expandable
         */
        renderSubTree: function($parent, rootNode, rootNodePath) {
            GLR.messenger.show({
                msg: _message["renderingHtml"],
                mode: "loading"
            });
            var html = _self.renderSubTreeAsHTML($parent, rootNode, rootNodePath);
            GLR.messenger.inform({
                msg: _message["readyToEdit"],
                mode: "success"
            });
            return html;
        }
    };
    return loggable(_self, "xmlEditor");
})();

