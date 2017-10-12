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
var treeSelectedLeaf;
var xpaths, labels, minOccurs, maxOccurs, displayValues;


$(document).ready(function() {


    var versions = $("#versions").val();
    var collectionID = $("#collectionID").val();
    var xmlId = $("#xmlId").val();
    var entityType = $("#entityType").val();
    var depth = $("#depth").val();
    var file = $("#file").val();
    var type = $("#type").val();
    var id = $("#id").val();
    var attributes = $("#attributes");
    //            var dynamicLabels = $("#dynamicLabels").val();

    // var lang = $("#lang").val();
    //alert("lala="+lang);
    if (attributes.length > 0) {
        xmlEditor.showAttrs();
    }
    xmlEditor.setDepth(depth);

    //   xmlEditor.setLang(lang);


    GLR.messenger.inform({
        msg: _message["downloadingXML"],
        mode: "loading"
    });
    //    console.time("loadingXML");
    if (file.length === 0) {
        $('#xmlString').load("File", {
            action: "Open",
            type: type,
            lang: lang,
            id: id,
            versions: versions,
            collectionID: collectionID,
            xmlId: xmlId,
            entityType: entityType
        }, function(data) {
            if (data.error) {
                GLR.messenger.show({
                    msg: data.error,
                    mode: "error"
                });
            }
            else {

                GLR.messenger.inform({
                    msg: "ΟΚ",
                    mode: "success"
                });
                // alert($("#xmlString").html());



                init(data);


            }
        }, "xml");
    } else {
        $('#xmlString').load("File", {
            action: "Open",
            lang: lang,
            file: file
                    //xmlFilename:"<?=$xmlFilename?>"
        }, function(data) {
            if (data.error) {
                GLR.messenger.show({
                    msg: data.error,
                    mode: "error"
                });
            }
            else {
                //  alert($("#xmlString").val());
                GLR.messenger.inform({
                    msg: "ΟΚ",
                    mode: "success"
                });
                //    alert(data);

                //  alert($("#xmlString").html());

                init(data);

            }
        }, "xml");

    }


//alert('lala');
//if ($("#xmlString").val())
//setTimeout(function() {alert($("#xmlString").val());},1250);
// xmlEditor.loadXmlFromFile("/lalala", "#xml", function(){

    /*
     $("#xml").html("<span style='font:italic 11px georgia,serif; color:#f30;'>Please upload a valid XML file.</span>").show();
     
     GLR.messenger.showAndHide({
     msg:"Uploaded file is not valid XML and cannot be edited.",
     mode:"error",
     speed:3000
     });
     */

//	$("#todos, #links").height($("#about").height()+"px");
});

function init(xml) {
    //var depth = $("#depth").val();
    var file = $("#file").val();
    var type = $("#type").val();
    var id = $("#id").val();
    //    GLR.messenger.inform({
    //        msg: "test",
    //        mode: "ready"
    //    });
    //   alert($("#xmlString").val());

    xmlEditor.loadXmlFromString(xml, "#xml", function() {
        $("#actionButtons").show();

        //        console.timeEnd("loadingXML");
        $("#xml").show();
        if (file.length === 0) {
            loadVars(type);
        } else {
            xmlEditor.renderTree();

        }
        //        createMap();
        //        xmlEditor.renderTree();

        //alert("sss");
        $("button#saveFile").show().click(function() {
            GLR.messenger.show({
                //   msg:"Αποθήκευση αρχείου...",
                msg: _message["saving"],
                mode: "επεξεργασία"
            });
            //           alert(xmlEditor.getXmlAsString());
            if (file.length == 0) {
                $.post("File", {
                    xmlString: xmlEditor.getXmlAsString(),
                    action: "Save",
                    type: type,
                    id: id
                            //xmlFilename:"<?=$xmlFilename?>"
                }, function(data) {
                    if (data.error) {
                        GLR.messenger.show({
                            msg: data.error,
                            mode: "error"
                        });
                    }
                    else {
                        GLR.messenger.inform({
                            msg: "ΟΚ",
                            mode: "success"
                        });
                        if (!$("button#viewFile").length) {
                            var label;
                            if (lang == "gr") {
                                label = "Ανανεωμένο Αρχείο";
                            } else if (lang == "en") {
                                label = "View Updated File";
                            }

                            $("<button id='viewFile'>" + label + "</button>")
                                    .appendTo("#actionButtons div")
                                    .click(function() {
                                        window.location.href = window.location.href + "&output=xml";
                                        //window.open(data.filename);
                                    });
                        }
                    }
                }, "html");
            } else {
                $.post("File", {
                    xmlString: xmlEditor.getXmlAsString(),
                    action: "Save",
                    file: file
                            //xmlFilename:"<?=$xmlFilename?>"
                }, function(data) {
                    if (data.error) {
                        GLR.messenger.show({
                            msg: data.error,
                            mode: "error"
                        });
                    }
                    else {
                        GLR.messenger.inform({
                            msg: "ΟΚ",
                            mode: "success"
                        });
                        if (!$("button#viewFile").length) {
                            $("<button id='viewFile'>View Updated File</button>")
                                    .appendTo("#actionButtons div")
                                    .click(function() {
                                        window.location.href = window.location.href + "&output=xml";
                                        //window.open(data.filename);
                                    });
                        }
                    }
                }, "html");
            }

        });

        $("button#validateFile").show().click(function() {
            //  alert(lang)
            GLR.messenger.show({
                // msg:"Έλεγχος XML...",
                msg: _message["validatingXml"],
                mode: "επεξεργασία"
            });
            //alert(xmlEditor.getXmlAsString());
            if (file.length == 0) {
                $.post("File", {
                    xmlString: xmlEditor.getXmlAsString(),
                    action: "Validate",
                    type: type,
                    lang: lang,
                    id: id
                            //xmlFilename:"<?=$xmlFilename?>"
                }, function(data) {

                    if (data.error) {
                        GLR.messenger.show({
                            msg: data.error,
                            mode: "error"
                        });
                    }
                    else {
                        alert(data);
                        GLR.messenger.inform({
                            msg: "ΟΚ",
                            mode: "success"
                        });
                    }
                }, "html");
            } else {
                $.post("File", {
                    xmlString: xmlEditor.getXmlAsString(),
                    action: "Validate",
                    lang: lang,
                    file: file
                            //xmlFilename:"<?=$xmlFilename?>"
                }, function(data) {
                    if (data.error) {
                        GLR.messenger.show({
                            msg: data.error,
                            mode: "error"
                        });
                    }
                    else {
                        alert(data); //data is greek. Have to parse
                        GLR.messenger.inform({
                            msg: "ΟΚ",
                            mode: "success"
                        });
                    }
                }, "html");
            }
        });

        $("button#referencesButton").show().click(function() {

            if ($("#references").length > 0) {
                $("#references").toggle();
            } else {

                GLR.messenger.show({
                    // msg:"Έλεγχος XML...",
                    msg: _message["referencesEval"],
                    mode: "επεξεργασία"
                });
                $.post("File", {
                    action: "references",
                    type: type,
                    lang: lang,
                    id: id
                            //xmlFilename:"<?=$xmlFilename?>"
                }, function(data) {

                    if (data.error) {
                        GLR.messenger.show({
                            msg: data.error,
                            mode: "error"
                        });
                    }
                    else {
                        GLR.messenger.inform({
                            msg: "ΟΚ",
                            mode: "success"
                        });
                        references = data;
                        $d = $(references);
                        $("#lalakis").append($d);

                    }
                }, "html");
            }


        });
    });
    //sets depth
    $("button#depthButton").show().click(function() {
        $("button#depthButton").hide();
        $depth = $("<input type='text' style='margin-right:7px' size='10'  id='depth' value=''>" + "</input>");
        $getDepth = $("<button class='submit' id='getDepth'>OK</button>");
        $btnCancel = $("<button class='subbmit' id='removeButton'>X</button>");

        $('div#actionButtons').append($depth).append($getDepth).append($btnCancel);
        $getDepth.click(function() {
            var depth = document.getElementById('depth').value;
            for (var i = 0; i < $('.children').length; i++) {
                var liList = $('.children').get(i).childNodes;
                // alert(i+"-------->"+liList.length);
                for (var j = 0; j < liList.length; j++) {
                    var status = liList[j].className;
                    if (status != null) {
                        var id = liList[j].getAttribute('id');
                        //  alert(liList[j].getAttribute('class'));
                        nodeDepth = id.split("/").length;
                        if (nodeDepth < (depth)) {
                            if (status.indexOf('expandable') != -1) {
                                status = status.replace('expandable', 'collapsable');
                                // alert("first "+i+"---->"+status);
                                liList[j].className = status;
                            }
                        }
                        else {
                            //alert(status+"----->"+status.indexOf('collapsable')!=-1);
                            if (status.indexOf('collapsable') != -1) {
                                status = status.replace('collapsable', 'expandable');
                                //    alert("sec "+i+status);
                                liList[j].className = status;
                            }
                        }
                    }
                }
            }
        });

        $btnCancel.click(function() {
            $('div#actionButtons').find("input#depth, button#getDepth, button#removeButton").remove();
            $("button#depthButton").show();

        });


    });

}



var popupWindow = null;
function centeredPopup(url, winName, w, h, scroll) {
    winName = winName + url;
    LeftPosition = (screen.width) ? (screen.width - w) / 2 : 0;
    TopPosition = (screen.height) ? (screen.height - h) / 2 : 0;
    var settings = 'height=' + h + ',width=' + w + ',top=' + TopPosition + ',left=' + LeftPosition + ',location=yes,scrollbars=' + scroll + ',resizable' + ' ,toolbar=yes';

    //Classic IE bullshit (breaks when window name parameter has spaces, dashes, special characters in general)
    if (navigator.appName == "Microsoft Internet Explorer") {
        winName = "window_" + winName.length;
    }

    popupWindow = window.open(url, winName, settings);
    if (window.focus) {
        popupWindow.focus();
    }

    if (!popupWindow.closed) {
        popupWindow.focus();
    }
}
function getValueFromPopUp(value) {
    var $select = $('select');
    var newValue = $select.find('option[data-id=' + value + ']').attr("value");
    $select.val(newValue).trigger("liszt:updated");
    $select.find('option[data-id=' + value + ']').attr('selected', 'selected');
}
function changeTerm(term) {
    var $term = $(term);
    $("span").removeClass("selected");
    $term.parent().addClass("selected");
}

function trim(str) {
    return str.replace(/^\s*|\s*$/g, "");
}

function toggle_expand_all() {

    if ($('.expandable:first').is('.expandable')) {
        $('.expandable').removeClass('expandable').addClass('collapsable');
    } else {
        $('.expandable').addClass('expandable').removeClass('collapsable');
    }
}

function toggle_collapse_all() {
    if ($('.collapsable:last').is('.collapsable')) {
        $('.collapsable').removeClass('collapsable').addClass('expandable');
    } else {
        $('.collapsable').addClass('collapsable');
    }
}
function toggle_expand() {
    var curDepth = $('#xml .expandable:first').attr("nodeDepth");
    $('[nodeDepth="' + curDepth + '"]').removeClass('expandable').addClass('collapsable');
}
function toggle_collapse() {
    var curDepth = $('#xml .collapsable:last').attr("nodeDepth");
    $('[nodeDepth="' + curDepth + '"]').removeClass('collapsable').addClass('expandable');
}
/**
 * Toggles display by swapping class name (collapsed/expanded) and toggling
 * visibility of child ULs.
 * @TODO make use of setTimeouts to address delay when many children
 * @TODO if only allowing single expanded node at a time, will need to collapse others
 */
function toggleNode(node) {
    var $thisLi = $(node).parent();
    // $thisLi.find(">ul").toggle("normal"); // animate({height:"toggle"});
    if ($thisLi.hasClass("collapsable")) {
        $thisLi.removeClass("collapsable").addClass("expandable");
    }
    else {
        $thisLi.removeClass("expandable").addClass("collapsable");
    }
}




function readURL(input) {
    //  alert(input);
    if (input.files && input.files[0]) {
        var reader = new FileReader();

        reader.onload = function(e) {
            $('#blah').attr('src', e.target.result);
        }
        reader.readAsDataURL(input.files[0]);
    }
}

function closeAndUnlock(type, id) {
    //    if(window.onpagehide || window.onpagehide === null){
    //        window.addEventListener('pagehide',  centeredPopup("File?action=Close&type="+type+"&id="+id,"lala","300","150"," "), false);
    //    } else {
    //        window.addEventListener('unload',  centeredPopup("File?action=Close&type="+type+"&id="+id,"lala","300","150"," "), false);
    //    }
    //
    //    Old way
    //   centeredPopup('File?action=Close&type='+type+'&id='+id,"lala","300","150"," ");

    //Decided tha popup is no longer necessary...
    jQuery.ajax({
        url: 'File?action=Close&type=' + type + '&id=' + id,
        async: false
    });

}

function reloadMe(lang) {

    var s = window.location.href;

    if (s.indexOf("lang") > 0) {
        var myregexp = /lang=\w+/;
        result = s.replace(myregexp, "lang=" + lang);
    } else {
        result = s + "&lang=" + lang;
    }
    window.location = result;
}

function openMap() {

    $("#map").toggle();
    if ($("#map").is(":visible")) {
        $("#xml").css("position", "relative");
        $("#xml").css("left", "400px");
        $("#xml").css("width", "825px");

    } else {
        $("#xml").css("position", "");
        $("#xml").css("left", "");
        $("#xml").css("width", "97%");
    }

}

function loading() {
    $("#xml").css("position", "relative");
    $("#xml").css("left", "300px");
    $("#xml").css("width", "925px");
    $("#map").html("<img src='img/loading.gif' style='position:relative;top:100px;left:130px;'/>").show();

}


function loadVars(type) {

    var schemaName = "isl." + synthesisName + ".FeXML." + type + "." + schemaLastVersion;
    if (view !== '1') {
        action = "edit";
    } else {
        action = "view";
    }



    if (typeof (Storage) !== "undefined") {
        //localStorage and sessionStorage support!

        if (!localStorage[schemaName + ".xpaths"]) { //If first time or xpaths empty (length=2 []), ask server for info (all 4 tables)
            var request = $.ajax({
                type: "GET",
                url: "GetVars?type=" + type + "&lang=" + lang + "&action=" + action,
                async: false
            });

            request.done(function(msg) {
                localStorage[schemaName + ".xpaths"] = JSON.stringify(msg.xpaths);
                //     localStorage[schemaName + "." + lang + ".labels"] = JSON.stringify(msg.labels);

                if ($("#dynamicLabels").val() !== "on") { //Labels do not change all the time, so local storage can be used
                    //Check for specific language
                    localStorage[schemaName + "." + lang + ".labels"] = JSON.stringify(msg.labels);
                    localStorage[schemaName + ".displayValues"] = JSON.stringify(msg.displayValues);

                }
                labels = JSON.parse(JSON.stringify(msg.labels));
                displayValues = JSON.parse(JSON.stringify(msg.displayValues));


                //     if (action === "edit") {
                localStorage[schemaName + ".minOccurs"] = JSON.stringify(msg.minOccurs);
                localStorage[schemaName + ".maxOccurs"] = JSON.stringify(msg.maxOccurs);
                //  }
            });
            request.fail(function(textStatus) {
                alert("Request failed: " + textStatus);
            });

        }

        if (typeof (labels) === "undefined") {

            if ($("#dynamicLabels").val() !== "on") { //Labels do not change all the time, so local storage can be used
                //Check for specific language
                if (!localStorage[schemaName + "." + lang + ".labels"]) { //If labels are missing, then get them and displayValues!
                    var labelsAndDisplayValues = getLabelsAndDisplayValues(type);
                    localStorage[schemaName + "." + lang + ".labels"] = JSON.stringify(labelsAndDisplayValues.labels);
                    localStorage[schemaName + ".displayValues"] = JSON.stringify(labelsAndDisplayValues.displayValues);

                }
                labels = JSON.parse(localStorage[schemaName + "." + lang + ".labels"]);
                displayValues = JSON.parse(localStorage[schemaName + ".displayValues"]);
            } else {
                var labelsAndDisplayValues = getLabelsAndDisplayValues(type);

                labels = JSON.parse(JSON.stringify(labelsAndDisplayValues.labels));
                displayValues = JSON.parse(JSON.stringify(labelsAndDisplayValues.displayValues));
            }
        }

        xpaths = JSON.parse(localStorage[schemaName + ".xpaths"]);
        if (action === "edit") {
            minOccurs = JSON.parse(localStorage[schemaName + ".minOccurs"]);
            maxOccurs = JSON.parse(localStorage[schemaName + ".maxOccurs"]);
        }
    } else {
        alert("Sorry! No web storage support..");
        // Sorry! No web storage support..
        var request = $.ajax({
            type: "GET",
            url: "GetVars?type=" + type + "&lang=" + lang + "&action=" + action,
            async: false
        });

        request.done(function(msg) {
            xpaths = JSON.stringify(msg.xpaths);
            //     localStorage[schemaName + "." + lang + ".labels"] = JSON.stringify(msg.labels);
            labels = JSON.parse(JSON.stringify(msg.labels));
            displayValues = JSON.parse(JSON.stringify(msg.displayValues));

            if (action === "edit") {
                minOccurs = JSON.stringify(msg.minOccurs);
                maxOccurs = JSON.stringify(msg.maxOccurs);
            }
            //   alert(JSON.stringify(msg));
        });
        request.fail(function(textStatus) {
            alert("Request failed: " + textStatus);
        });


    }

    createMap();
    xmlEditor.renderTree();

}

function getLabelsAndDisplayValues(type) {
    var labelsAndDisplayValues;

    var request = $.ajax({
        type: "GET",
        url: "GetVars?type=" + type + "&lang=" + lang + "&action=view&labelsAndDisplayValuesOnly=",
        async: false
    });

    request.done(function(msg) {
        labelsAndDisplayValues = msg;
    });
    request.fail(function(textStatus) {
        alert("Request failed: " + textStatus);

    });

    return labelsAndDisplayValues;
}

function createMap() {
    var tree = "<ul id='tree' class='treeview'>";
    var xpath = "";
    var followingXpath = "";

    for (var i = 0; i < xpaths.length; i++) {
        xpath = "'" + xpaths[i] + "'";
        followingXpath = "'" + xpaths[i + 1] + "'";

        var depth = getDepthFromPath(xpath);
        var followingDepth = getDepthFromPath(followingXpath);

        var liClassName = "";
        var divClassName = "";
        if (i < xpaths.length - 1) {
            if (xpaths[i + 1].indexOf(xpaths[i]) !== -1) {
                divClassName = "hitarea";
                if (depth < 4) {
                    if (depth === 1) {
                        liClassName = "collapsable last";
                    } else {
                        liClassName = "collapsable";
                    }
                    //            open = 'class="collapsable"';
                } else {
                    liClassName = "expandable";

                }
            }
        } else {

        }



        var linkId = "'treeLink" + i + "'";

        //  html = '<li '+open+'><a id='+linkId+' href="#" style="text-decoration:none;" title="'+xpaths[i]+'" onclick="goTo('+linkId+','+xpath+');return false;">'+labels[i]+"</a>";
        html = '<li id=' + linkId + ' title=' + xpaths[i] + ' class="node Type ' + liClassName + '">' +
                '<div class="' + divClassName + '"  onclick="toggleNode(this);"></div>' +
                '<span class="nodeName" onclick="goTo(' + linkId + ',' + xpath + ');return false;">' + labels[i] + '</span>';
        //var dynamicLabels = $("#dynamicLabels").val();

        if ($("#dynamicLabels").val() === "on" && view === '2') {
            var displayButton;
            if (displayValues[i] === "visible") {
                displayButton = '<button id="hide' + i + '" onclick="toggleDisplay(this,' + i + ');" style="opacity: 0.5;" class="edit icon" title="' + _message["hide"] + '">' +
                        '<img style="vertical-align:top" src="css/eye-slash.png"></button>';
            } else {
                displayButton = '<button id="show' + i + '" onclick="toggleDisplay(this,' + i + ');" style="opacity: 0.5;" class="edit icon" title="' + _message["show"] + '">' +
                        '<img style="vertical-align:top" src="css/eye.png"></button>';
            }

            var labelsButton = '<button id="editLabel"' + i + ' onclick="labelChange(this,' + i + ');" style="opacity: 0.5;" class="edit icon" title="' + _message["rename"] + '">' +
                    '<img style="vertical-align:top" src="css/addRemove.png"></button>';
            html = html + labelsButton + displayButton;
        }


        if (followingDepth === depth) {
            html = html + '</li>';
        } else if (followingDepth > depth) {
            html = html + '<ul>';
        } else {
            var depthDifference = depth - followingDepth;
            var uls = "";
            for (var k = 0; k < depthDifference; k++) {
                uls = uls + "</ul></li>";
            }
            html = html + uls;
        }
        tree = tree + html;
    }
    tree = tree + "</ul>";

    $("#map").html(tree);

    $("#map").find("li").each(function() {
        if ($(this).siblings().length == 0) {
            // alert($(this).html())
            $(this).addClass("last");
        }

    });

}

function getDepthFromPath(xpath) {

    var depth = xpath.match(/\//g);
    if (depth != null) {
        depth = depth.length;
    } else {
        depth = 0;
    }
    depth = depth + 1;
    return depth;
}

function goTo(linkId, elementId) {

    var previouslySelectedPath = $("#" + treeSelectedLeaf).attr("title");
    $("#" + treeSelectedLeaf).removeAttr("style");
    var $previouslySelectedItem = $("li[data-path='" + previouslySelectedPath + "']");
    if ($previouslySelectedItem.hasClass('selectedOdd')) {
        $previouslySelectedItem.removeClass('selectedOdd').addClass('odd');
    } else {
        $previouslySelectedItem.removeClass('selectedEven').addClass('even');
    }


    $previouslySelectedItem.removeAttr("style");
    //   $("#"+treeSelectedLeaf).css("background-color", "white");
    //   $("li[id*='"+elementId+"']").css("background-color", "white");

    var $selectedItem = $("li[data-path='" + elementId + "']");

    if ($selectedItem.hasClass('odd')) {
        $selectedItem.removeClass('odd').addClass('selectedOdd');
    } else {
        $selectedItem.removeClass('even').addClass('selectedEven');
    }

    var selectedItemOffset = $selectedItem.css("border", "2px").offset();
    if (selectedItemOffset != null) {

        $("#" + linkId).css("background", "#c3effd");

        $('html, body').animate({
            scrollTop: selectedItemOffset.top - 80
        }, 1000);
    } else {
        $("#" + linkId).css("background", "pink");

    }
    treeSelectedLeaf = linkId;
}

function toggleDisplay(but, pathIndex) {

    $but = $(but);
    var action;
    var xpath = xpaths[pathIndex];
    if ($but.html().indexOf("eye-slash") !== -1) {
        action = "hide";
        $but.children("img").attr("src", "css/eye.png");
        $but.attr("title", _message["show"]);
    } else {
        action = "show";
        $but.children("img").attr("src", "css/eye-slash.png");
        $but.attr("title", _message["hide"]);
    }
    var $node = $but.parents("li");
    var $nodeParent = $node.parents("li");

    var visibleSiblingsCount = $("li[id='" + $nodeParent.attr("id") + "']").children("ul").find("img[src='css/eye-slash.png']").size();
    var hiddenSiblingsCount = $("li[id='" + $nodeParent.attr("id") + "']").children("ul").find("img[src='css/eye.png']").size();

    $("li[data-path='" + xpath + "']").toggle();

    if (action === "hide" && visibleSiblingsCount === 0) {
        console.log("should also hide father?")
    }



    $.post("LaAndLi", {
        xpath: xpath,
        type: $("#type").val(),
        action: action
    }, function(response) {
    }, "html")

}

function detectRecursion(nodePath) {
    var pathComponents = nodePath.split("/");
    var path = "";
    for (var i = 0; i < pathComponents.length; i++) {
        if (path === "") {
            path = path + "/" + pathComponents[i] + "/";

        } else {
            if (path.indexOf("/" + pathComponents[i] + "/") === -1) {
                path = path + pathComponents[i] + "/";
            } else {
                var index = path.indexOf("/" + pathComponents[i] + "/") + pathComponents[i].length + 2;
                path = path.substring(0, index);
            }
        }
    }
    return path.substring(1, path.length - 1);
}

function labelChange(but, pathIndex) {

    $but = $(but);

    $("#labels").siblings(".edit").show();
    $("#labels").remove();

    $btnSaveLabels = $("<button class='submit' id='saveLabelsButton'>OK</button>");
    $btnCancel = $("<button class='submit' id='cancel'>X</button>");


    $.post("LaAndLi", {
        xpath: xpaths[pathIndex],
        type: $("#type").val(),
        lang: lang
    }, function(response) {
        //alert(response)
        $labelsContainer = $("<div id='labels'>" + response + "</form>").append($btnSaveLabels).append($btnCancel);

        $but.hide();
        $but.after($labelsContainer);

    }, "html")

    //   $("#"+lang).focus(); does not work

    $btnSaveLabels.click(function() {

        var currentLangValue = $btnSaveLabels.parent().siblings(".nodeName").html();
        var newLangValue = $btnSaveLabels.siblings("#" + lang).val();

        if (newLangValue != currentLangValue) { //Skip code if value is unchanged!
            //Change tree values
            $("li[data-path='" + xpaths[pathIndex] + "']").children(".nodeName").each(function() {
                var curValue = $(this).html();
                var newValue = curValue.replace(currentLangValue, newLangValue);
                $(this).html(newValue);
            });


            //Change map tree values
            $btnSaveLabels.parent().siblings(".nodeName").html(newLangValue);

            //Change xpaths variable values
            labels[pathIndex] = newLangValue;

        }

        var fields = new Array();
        var values = new Array();
        fields = $btnSaveLabels.parent().children("input").serializeArray();
        $.each(fields, function(index, element) {
            values.push(element.value);
        });

        $.post("LaAndLi", {
            xpath: xpaths[pathIndex],
            type: $("#type").val(),
            labels: values
        }, function(response) {
        }, "html")



        $btnSaveLabels.parent().remove();
        $but.show();
    });

    $btnCancel.click(function() {

        $btnCancel.parent().remove();
        $but.show();
    });



}




function check(input) {
    input.setCustomValidity("");
    var $input = $(input);
    var message = $input.attr("title");

    //alert($input.attr("title"));
    if (input.validity) {
        if (input.validity.valid === true) {
            //  alert('valid');

            $input.addClass("valid");
        } else {
            //  alert('invalid');
            input.setCustomValidity(message);
            $input.addClass('invalid');
        }
    }
}
;


