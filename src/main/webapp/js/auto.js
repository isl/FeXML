/*
 * Copyright 2012-2018 Institute of Computer Science,
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
 * auto.js contains a library of methods used to calculate and modify specific values based on other fields' values.
 * @type type
 */

/*
 * Declare all XOR xpaths organized by entity type
 * @type type
 */
var xorPaths = {
    "Contest": [
        ["Contest/Winners/DionysiacArtist", "Contest/Winners/Choregos"]
    ]
};
/*
 * Run auto checks for entire file (mostly used on load)
 * XOR check only (FOR NOW)
 */
function auto(type) {
    detectXOR(type);
}

/*
 * Run auto checks for specific xpath (mostly used on submit field value)
 * XOR check only (FOR NOW)
 */
function autoForXpath(type, nodePath, value) {
    detectXORForXpath(type, nodePath, value);
}

/* Private methods */

function detectXOR(type) {
    var xorGroups = getAllXORGroups(type);
    if (typeof xorGroups !== "undefined") {

        for (var i = 0; i < xorGroups.length; i++) {

            var xorGroup = xorGroups[i];
            for (var j = 0; j < xorGroup.length; j++) {
                var xpath = xorGroup[j];
                var $field = $("li[id='" + xpath + "']");
                if ($field.find(".nodeValue").children("span.noValue").length === 0) {
                    var value = $field.find(".nodeValue").html();
                    if (value !== "--------------------") {//xor field has value
                        autoForXpath(type, xpath, value);
                        break;
                    }
                }

            }
        }
    }
}

function detectXORForXpath(type, nodePath, value) {
    var xorGroup = getXORGroupFromXpath(type, nodePath);
    if (typeof xorGroup !== "undefined") {//specific path belongs to XOR group
        setRestXORFields(xorGroup, nodePath, value);
    }
}

function makeFieldReadOnly(xpath, label) {
    var $field = $("li[id='" + xpath + "']");
    $field.find(".nodeValue").addClass("nodeFixed").removeClass("nodeValue");
    $field.attr("title", _message["lockedByField"] + " '" + label + "'");
}
function makeFieldEditable(xpath) {
    var $field = $("li[id='" + xpath + "']");
    $field.find(".nodeFixed").addClass("nodeValue").removeClass("nodeFixed");
    $field.attr("title", "");
}

function setRestXORFields(xorGroup, nodePath, value) {

    if (value !== null && value.length > 0 && value !== "--------------------") {//have to make rest paths read-only
        for (var i = 0; i < xorGroup.length; i++) {
            var xpath = xorGroup[i];
            if (xpath !== nodePath) {
                var pathIndex = jQuery.inArray(nodePath, xpaths);
                var label = labels[pathIndex];
                makeFieldReadOnly(xpath, label);
            }
        }
    } else {//have to make rest paths editable again
        for (var i = 0; i < xorGroup.length; i++) {
            var xpath = xorGroup[i];
            if (xpath !== nodePath) {
                makeFieldEditable(xpath);
            }
        }
    }
}
function getAllXORGroups(type) {
    var xorGroups = xorPaths[type];
    return xorGroups;
}

function getXORGroupFromXpath(type, nodePath) {
    var xorGroups = xorPaths[type];
    if (typeof xorGroups !== "undefined") {
        for (var i = 0; i < xorGroups.length; i++) {
            var group = xorGroups[i];
            if (group.indexOf(nodePath) !== -1) {
                return group;
            }
        }
    }
}





