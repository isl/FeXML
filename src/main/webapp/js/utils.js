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
function Utils() {

    this.swapNodes = function (a, b) {
        var aparent= a.parentNode;
        var asibling= a.nextSibling===b? a : a.nextSibling;
        b.parentNode.insertBefore(a, b);
        aparent.insertBefore(b, asibling);
    }

    this.updateSubtree = function($tree, newPath) {
        $tree.attr("id",newPath).attr("title",newPath);
        var newPathParts = newPath.split("/");

        $tree.find("li.node").each(function(index){

            var nodePath = $(this).attr("id");
            var nodePathParts = nodePath.split("/");

            var newNodePath="";
            for (var i=0;i<nodePathParts.length;i++) {
                if (i<newPathParts.length) {
                    newNodePath = newNodePath + newPathParts[i]+"/";
                } else {
                    newNodePath = newNodePath + nodePathParts[i]+"/";
                }
            }
            
            if (newNodePath.slice(-1)=="/") {
            newNodePath = newNodePath.substring(0,newNodePath.length-1);
            }
            //            var newNodePath = newPath+"/"+nodePath.substring(newPath.length+1,nodePath.length);
            $(this).attr("id",newNodePath).attr("title",newNodePath);

        })
    }

    this.setPath = function(node, newPath, XMLnodeName) {
             

        var $node = $(node);

        var newPosition = this.getPosition(newPath);
        if (newPosition==1) {
            $node.children(".nodeName").html(XMLnodeName);
        } else {
            $node.children(".nodeName").html(XMLnodeName+"["+newPosition+"]");
        }
        
        this.updateSubtree($node, newPath);
        
    }

    this.getNextPath = function(path) {
       
        var position = this.getPosition(path);
        var newPosition="";

        var newPath;
        if (position==1) {
            newPosition = "2";
            newPath = path+"["+newPosition+"]";
        } else {
            newPosition = parseInt(position)+1;
            var start = path.lastIndexOf("[")+1;
            var end =  path.lastIndexOf("]")+1;
            newPath  = path.substring(0, start-1)+"["+newPosition+"]"+path.substring(end);
        }
      
        return newPath;
    }

    this.getPreviousPath = function(path) {

        var position = this.getPosition(path);
        var newPosition="";

        var newPath;
        if (position==2) {
            newPath = path.substring(0, path.lastIndexOf("["));
        } else {
            newPosition = parseInt(position)-1;
            var start = path.lastIndexOf("[")+1;
            var end =  path.lastIndexOf("]")+1;
            newPath  = path.substring(0, start-1)+"["+newPosition+"]"+path.substring(end);
        }

        return newPath;
    }

  
    this.getPosition = function (path) {

        var start = path.lastIndexOf("[")+1;
        var end = path.lastIndexOf("]");

      
        var position="1";
        if (start>0 && end>0 && end==path.length-1) {
            position = path.substring(start, end);
        }
        var posAsInt = parseInt(position);
        return posAsInt;

    }

    this.updateAllowedActions = function (node, siblingsCount) {
        var $node = $(node);
        var path = $node.attr("id");
    
        var  position = this.getPosition(path);


        if (position!=1) {
            $node.children(".actionButtons").children("button.goUp").show();
        }
        if (position==siblingsCount) {
            $node.children(".actionButtons").children("button.goDown").hide();
        }


    }

}





