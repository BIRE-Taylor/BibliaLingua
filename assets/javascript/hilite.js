// Search Engine Keyword Highlight (http://fucoder.com/code/se-hilite/)
var hilite=window.hilite||{elementid:"content",exact:true,max_nodes:1e3,onload:true,style_name:"grayHighlight",style_name_suffix:false,debug_referrer:""};hilite.hiliteElement=function(e,t){if(!t||e.childNodes.length==0)return;var n=new Array;for(var r=0;r<t.length;r++){t[r]=t[r].toLowerCase();if(hilite.exact)n.push("\\b"+t[r]+"\\b");else n.push(t[r])}n=new RegExp(n.join("|"),"i");var i={};for(var r=0;r<t.length;r++){if(hilite.style_name_suffix)i[t[r]]=hilite.style_name+(r+1);else i[t[r]]=hilite.style_name}var s=function(e){var t=n.exec(e.data);if(t){var r=t[0];var s="";var o=e.splitText(t.index);var u=o.splitText(r.length);var a=e.ownerDocument.createElement("SPAN");e.parentNode.replaceChild(a,o);a.className=i[r.toLowerCase()];a.appendChild(o);return a}else{return e}};hilite.walkElements(e.childNodes[0],1,s)};hilite.hilite=function(e){var t=null;if(e&&(hilite.elementid&&(t=document.getElementById(hilite.elementid))||(t=document.body))){hilite.hiliteElement(t,e)}};hilite.walkElements=function(e,t,n){var r=/^(script|style|textarea)/i;var i=0;while(e&&t>0){i++;if(i>=hilite.max_nodes){var s=function(){hilite.walkElements(e,t,n)};setTimeout(s,50);return}if(e.nodeType==1){if(!r.test(e.tagName)&&e.childNodes.length>0){e=e.childNodes[0];t++;continue}}else if(e.nodeType==3){e=n(e)}if(e.nextSibling){e=e.nextSibling}else{while(t>0){e=e.parentNode;t--;if(e.nextSibling){e=e.nextSibling;break}}}}}