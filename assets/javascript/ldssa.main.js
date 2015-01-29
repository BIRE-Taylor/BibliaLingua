// Namespace
var ldssa = window.ldssa || {};

window.mainInterface = window.mainInterface || undefined;
window.linkInterface = window.linkInterface || undefined;

// main module
ldssa.main = function () {
    var self = {};

    var isNightMode = false;
    var timeoutScroll = null;
    var offsetForSystemUI = 84;

    if (typeof String.prototype.startsWith != 'function') {
        String.prototype.startsWith = function (str) {
            return this.slice(0, str.length) == str;
        };
    }

    self.highlightKeywords = function(words, delim) {
        if(words != null && delim != null) {
            consoleLog("KEYWORDS: " + words);
            var kw = words.split(delim);
            for (var i = 0; i < kw.length; i++) {
                findText(kw[i]);
            }
        }
    };

    var findText = function(text) {
        if (window.find && window.getSelection) {
            document.designMode = "on";
            var sel = window.getSelection();
            sel.collapse(document.body, 0);

            // this method is great b/c it can search around tags without breaking
            //object.find ([textToFind [, matchCase[, searchUpward[, wrapAround[, wholeWord[, searchInFrames[, showDialog]]]]]]]);
            while (window.find(text, false, false, false, false, true)) {
                // highlight with grayHighlight color
                if (isNightMode) {
                    document.execCommand("HiliteColor", false, "rgba(255, 255, 255, 0.3)");
                } else {
                    document.execCommand("HiliteColor", false, "rgba(0, 0, 0, 0.2)");
                }
                sel.collapseToEnd();
            }
            document.designMode = "off";
        }
    };

    var trim = function(stringToTrim) {
        return stringToTrim.replace(/^\s+|\s+$/g, '');
    };

    self.htmlCustomizations = function(api, width, uri) {
        self.configureVideos(api, width);
        self.configureImages(true);
        self.getDocumentHeadingImage();
    };

    self.configureImages = function(addLink) {
        var imgs = document.getElementsByTagName('img');
        for (var i = 0, n = imgs.length; i < n; i++) {
            var img = imgs[i];
            if (addLink) {
                addImageLink(img);
            }
            configureImage(img);
        }
    };

    var addImageLink = function(img) {
        var pid = img.getAttribute('pid');
        if (pid != null && pid.length > 0) {
            var parent = img.parentNode;
            // get the nearest parent element
            while (parent != null && parent.nodeType != 1) {
                parent = parent.parentNode;
            }
            if (parent != null && parent.nodeType == 1 && parent.tagName.toLowese() == 'div') {
                var link = document.createElement('a');
                link.setAttribute('href', 'gl://image/?pid=' + pid);
                link.appendChild(img);
                parent.appendChild(link);
                consoleLog("Added image link for " + pid);
            }
        }
    }

    var configureImage = function(img) {
        // dynamically set the src attribute
        var attrs = img.attributes;
        if (attrs != null) {
            var targetWidth = window.innerWidth;
            var bestUrl = img.getAttribute("src");
            var bestDiff = targetWidth;
            var bestWidth = 0;
            for (var i = 0,  n = attrs.length; i < n; i++) {
                var attr = attrs[i];
                if (attr.name.startsWith("data-src")) {
                    var parts = attr.name.split("-");
                    if (parts.length == 3) {
                        parts = parts[2].split("x");
                        if (parts.length == 2) {
                            var srcWidth = parts[0];
                            var srcDiff = Math.abs(targetWidth - srcWidth);
                            if (srcDiff < bestDiff) {
                                bestUrl = attr.value;
                                bestDiff = srcDiff
                                bestWidth = srcWidth;
                            }
                        }
                    }
                }
            }
            if (bestUrl != null) {
                img.setAttribute("src", bestUrl);
                //consoleLog("Setting image src width=" + bestWidth + " (target=" + targetWidth + ")");
            }
        }
    };

    self.getDocumentHeadingImage = function() {
        var heading = document.getElementById('head1');
        if (heading == null) {
            heading = document.getElementById('heading');
        }
        if (heading != null) {
            var imgs = heading.getElementsByTagName('img');
            for (var i = 0, n = imgs.length; i < n; i++) {
                var img = imgs[i];
                var src = img.getAttribute('src');
                if (src != null && src.length > 0) {
                    window.mainInterface.jsReportDocumentHeadingImage(src);
                }
            }
        }
    };

    self.configureVideos = function(api, width) {
        var videos = document.getElementsByTagName('video');
        var videosArray = new Array();
        for (var i = 0, n = videos.length; i < n; i++) {
            videosArray.push(self.configureVideo(i, api, width));
        }
        var jsonVideos = { 'videos' : videosArray };
        window.mainInterface.jsReportInlineVideoInfo(JSON.stringify(jsonVideos, null));
    };

    self.configureVideo = function(videoIndex, api, width) {
        var videos = document.getElementsByTagName('video');
        var video = videos[videoIndex];
        video.videoIndex = videoIndex;
        //video.poster = "data:image/png;base64,AAAA";
        video.addEventListener('play', videoEvent, false);
        video.addEventListener('click', videoEvent, false);
        video.addEventListener('seeking', preventDefault, false);
        video.addEventListener('seeked', preventDefault, false);
        return getVideoInfo(video);
    };

    var preventDefault = function(event) {
        event.preventDefault();
    }

    var videoEvent = function(event) {
        event.preventDefault();
        var jsonVideo = getVideoInfo(event.target);
        window.mainInterface.jsReportVideoTapped(JSON.stringify(jsonVideo, null));
    };

    var getVideoInfo = function(video) {
        var sources = video.getElementsByTagName('source-no-preload');
        if (sources.length == 0) {
            sources = video.getElementsByTagName('source');
        }
        var sourcesArray = new Array();
        for (var i = 0, n = sources.length; i < n; i++) {
            var source = sources[i];
            var sourceJson = {
                'src' : source.getAttribute('src'),
                'type' : source.getAttribute('type'),
                'data-container' : source.getAttribute('data-container'),
                'data-encodingbitspersec' : source.getAttribute('data-encodingbitspersec'),
                'data-width' : source.getAttribute('data-width'),
                'data-height' : source.getAttribute('data-height'),
                'data-sizeinbytes' : source.getAttribute('data-sizeinbytes'),
                'data-durationms' : source.getAttribute('data-durationms'),
                'data-alloweduses' : source.getAttribute('data-alloweduses')
            };
            sourcesArray.push(sourceJson);
        }

        // get the index, title and id, if present
        var videoId = null;
        var videoTitle = null;
        var videoIndex = -1;
        if ("videoIndex" in video) {
            videoIndex = video.videoIndex;
        }
        if ("title" in video && video.title.length > 0) {
            videoTitle = video.title;
        }
        if ("id" in video && video.id.length > 0) {
            videoId = video.id;
        }
        var parent = video.parentNode;
        if (parent != null && parent.nodeType == 1 && parent.className == 'video') {
            if (videoTitle == null) {
                videoTitle = parent.getAttribute('title');
            }
            if (videoId == null) {
                videoId = parent.getAttribute('id');
            }
        }
        // sometimes have to go two levels up to get these items
        parent = parent.parentNode;
        if (parent != null && parent.nodeType == 1 && parent.className == 'video') {
            if (videoTitle == null) {
                videoTitle = parent.getAttribute('title');
            }
            if (videoId == null) {
                videoId = parent.getAttribute('id');
            }
        }

        var jsonVideo = {
            'id' : videoId,
            'title' : videoTitle,
            'index' : videoIndex,
            'sources' : sourcesArray
        }

        return jsonVideo;
    };

    self.getImageInfoForPid = function(pid) {
        var element = document.querySelector('[pid="' + pid + '"]');
        if (element != null) {
            var imageInfo = {}
            for (var i = 0; i < element.attributes.length; i++) {
                var a = element.attributes[i];
                if (a.specified) {
                    imageInfo[a.name] = a.value;
                }
            }
            window.mainInterface.jsReportImageInfo(JSON.stringify(imageInfo, null));
        }
    };

    var currentYPosition = function() {
        // Firefox, Chrome, Opera, Safari
        if (pageYOffset) return pageYOffset;
        // Internet Explorer 6 - standards mode
        if (document.documentElement && document.documentElement.scrollTop)
            return document.documentElement.scrollTop;
        // Internet Explorer 6, 7 and 8
        if (document.body.scrollTop) return document.body.scrollTop;
        return 0;
    };

    var smoothToY = function(stopY) {
        var startY = currentYPosition();
        var distance = stopY > startY ? stopY - startY : startY - stopY;
        if (distance < 50) {
            window.scrollTo(0, stopY);
            return;
        }
        var speed = Math.round(distance / 50);
        if (speed > 15) speed = 15;
        var step = Math.round(distance / 25);
        var leapY = stopY > startY ? startY + step : startY - step;
        var timer = 0, i = 0;
        if (stopY > startY) {
            for ( i=startY; i<stopY; i+=step ) {
                timeoutScroll = setTimeout("window.scrollTo(0, "+leapY+")", timer * speed);
                leapY += step; if (leapY > stopY) leapY = stopY; timer++;
            }
        }
        else {
            for ( i=startY; i>stopY; i-=step ) {
                timeoutScroll = setTimeout("window.scrollTo(0, "+leapY+")", timer * speed);
                leapY -= step; if (leapY < stopY) leapY = stopY; timer++;
            }
        }
    };

    /**
     * Finds the first element identified by uri="<uri>"
     * @param uri
     * @returns Node element
     */
    var findElementByUri = function(uri) {
        if (uri == null) return null;
        return document.querySelector("[uri=\"" + uri + "\"]");
    };

    var clearGrayHighlights = function() {
        var list = document.querySelectorAll(".grayHighlight");
        for(var i = 0; i < list.length; i++) {
            var el = list[i];
            clearGrayHighlight(el);
        }
    };

    var clearGrayHighlight = function(el) {
        el.className = el.className.replace(/grayHighlight/g, "");
    };

    var consoleLog = function(msg) {
        self.consoleLog(msg);
    };

    // Used for Linking
    // document.elementFromPoint may not work on the android 2.1 need to test that.
    self.getUriFromPosition = function(x, y) {
        var node = document.elementFromPoint(x, y);
        var uri = getUriForNode(node);
        if (uri) {
            consoleLog("Element uri: " + uri);
        }
        else {
            consoleLog("Element doesn't have uri.");
        }
        //noinspection JSUnresolvedFunction
        window.linkInterface.jsReportUri(uri);
    };

    var getUriForNode = function(node) {
        if (node != null) {
            //consoleLog(node.nodeName + ": " + node.textContent);
            var attrs = node.attributes;
            if (attrs != null) {
                var uri = attrs.getNamedItem("uri");
                if (uri != null) {
                    return uri.value;
                }
                else {
                    var siblings = node.parentNode.querySelectorAll("[uri]");
                    if (siblings == null || siblings.length == 0) {
                        return getUriForNode(node.parentNode);
                    }
                    else {
                        return null;
                    }
                }
            }
            else {
                return getUriForNode(node.parentNode);
            }
        }
        return null;
    };

    /**
     * scroll to a given element identified by uri="<uri>"
     * @param uri
     * @param verses
     */
    self.scrollToElementUri = function(uri, verses) {

        // check for verses that are present for highlighting
        if(verses != null) {

            clearGrayHighlights();

            // MSA-295 set up the colors for highlighting in nightmode or regular mode
            var color = " grayHighlight";
            // for regular mode

            // Adding for MSA-295
            // Handle the highlighting of all the verses that we have from the footnote
            var initUri = uri.split(".");
            var versesArr = verses.split(",");
            var verseElement = null;
            consoleLog("versesArr len: " + versesArr.length);
            if (versesArr.length > 1) {
                // multiple verses
                for (var i = 0; i < versesArr.length; i++) {
                    var newuri = initUri[0] + "." + versesArr[i];
                    consoleLog("newuri: " + newuri);
                    verseElement = findElementByUri(newuri);
                    if (verseElement != null) {
                        verseElement.className += color;
                    }
                }
            }
            else {
                // one verse only
                verseElement = findElementByUri(uri);
                if(verseElement != null) {
                    verseElement.className += color;
                }
            }
        }

        var element = findElementByUri(uri);
        if (element != null) {
            scrollToElement(element);
        }
    };

    var scrollToElement = function(element) {
        var y = 0;
        while (element != null) {
            y += element.offsetTop;
            element = element.offsetParent;
        }
        //window.scrollTo(0, y - 5);
        smoothScrollToY(y - offsetForSystemUI);
    }

    self.highlightRelatedContentItemWithId = function(id, scroll) {

        self.removeTemporaryHighlightEffects();

        try {
            var list = document.querySelectorAll("div[annotationId=\"" + id + "\"]");
            var rect = null;
            var i, el = null;
            var yOffset = window.pageYOffset;
            for(i = 0; i < list.length; i++) {
                el = list[i];
                if (el.className.indexOf("sticky") != -1) {
                    el = null
                }

                if (el != null) {
                    //el.className += " temp_highlight temp_highlight_effects";
                    rect = processRectForTemporaryHighlight(rect, el.getBoundingClientRect(), yOffset);
                }
            }

            // check for f_id
            if (rect == null) {
                list = document.querySelectorAll("[href=\"" + "f_" + id + "\"]");
                for(i = 0; i < list.length; i++) {
                    el = list[i];
                    //el.className += " temp_highlight temp_highlight_effects";
                    rect = processRectForTemporaryHighlight(rect, el.getBoundingClientRect(), yOffset);
                }
            }

            // check for fn_id
            if (rect == null) {
                list = document.querySelectorAll("[href=\"" + "fn_" + id + "\"]");
                for(i = 0; i < list.length; i++) {
                    el = list[i];
                    //el.className += " temp_highlight temp_highlight_effects";
                    rect = processRectForTemporaryHighlight(rect, el.getBoundingClientRect(), yOffset);
                }
            }
            
            // check for id
            if (rect == null) {
                list = document.querySelectorAll("[href=\"" + id + "\"]");
                for(i = 0; i < list.length; i++) {
                    el = list[i];
                    //el.className += " temp_highlight temp_highlight_effects";
                    rect = processRectForTemporaryHighlight(rect, el.getBoundingClientRect(), yOffset);
                }
            }

            if (rect != null) {
                var div = document.createElement("div");
                var screenWidth = window.screen.width;
                while (rect.left > screenWidth) {
                    rect.left -= screenWidth;
                }
                while (rect.left < 0) {
                    rect.left += screenWidth;
                }
                var style = "position:absolute; top:" + rect.top + "px; left:" + rect.left + "px; width:" + rect.width + "px; height:" + rect.height + "px; z-index:-1";
                //consoleLog("STYLE: " + style);

                div.setAttribute("style", style);
                div.setAttribute("class", "temp_highlight");
                document.body.appendChild(div);

                if (scroll != null && scroll == "true") {
                    var scrolled = false;
                    try {
                        // scroll only if not visible
                        //noinspection UnnecessaryLocalVariableJS
                        var docViewTop = yOffset;
                        var docViewBottom = docViewTop + window.innerHeight;

                        var elemTop = rect.top;
                        var elemBottom = elemTop + rect.height;

                        if ((elemBottom >= docViewTop) && (elemTop <= docViewBottom)
                            && (elemBottom <= docViewBottom) &&  (elemTop >= docViewTop) ) {
                            // no scroll needed
                            scrolled = true;
                        }
                    }
                    catch (err) { consoleLog(err); }

                    if (!scrolled) {
                        try {
                            smoothScrollToY(rect.top - offsetForSystemUI);
                        } catch (err) { consoleLog(err); }
                    }
                }
            }
        } catch (err) { consoleLog(err); }

//    //consoleLog("SCHEDULE REMOVE TEMP HIGHLIGHTS");
//    clearTimeout(timeoutHighlight);
//    timeoutHighlight = setTimeout('removeTemporaryHighlightEffects()', 2000);

    };

    var processRectForTemporaryHighlight = function(rect, bounds, yOffset) {
        var boundingRect = document.documentElement.getBoundingClientRect();
        var xOffset = boundingRect.left; // if we're swiping between chapters

        if (rect == null) {
            rect = {};
            rect.top = bounds.top + yOffset;
            rect.bottom = bounds.bottom + yOffset;
            rect.left = bounds.left - xOffset;
            rect.right = bounds.right - xOffset;
        }
        else {
            rect.top = Math.min(bounds.top + yOffset, rect.top);
            rect.bottom = Math.max(bounds.bottom + yOffset, rect.bottom);
            rect.left = Math.min(bounds.left - xOffset, rect.left);
            rect.right = Math.max(bounds.right - xOffset, rect.right);
        }
        rect.width = rect.right - rect.left;
        rect.height = rect.bottom - rect.top;
        return rect;
    };

    self.removeTemporaryHighlightEffects = function() {
        //consoleLog("REMOVE TEMP HIGHLIGHTS");
        //clearTimeout(timeoutHighlight);
        var list = document.querySelectorAll(".temp_highlight");
        for (var i=0; i < list.length; i++) {
            var item = list[i];
            item.parentNode.removeChild(item);
        }
    };

    self.getOffsetsForUris = function() {
        var offsets = [];
        var elements = document.querySelectorAll("[uri]");
        for (var i=0; i < elements.length; i++) {
            var element = elements[i];
            var uri = element.getAttribute("uri");
            if (uri != null) {
                var offset = 0;
                var tempElem = element;
                while (tempElem) {
                    offset += tempElem.offsetTop;
                    tempElem = tempElem.offsetParent;
                }
                //consoleLog("verseTop:" + offset + " uri:" + uri);
                offsets.push({id: uri, top: offset});
            }
        }
        //window.mainInterface.reportHtml("OffsetString:" + offsets.join(" "));
        //noinspection JSUnresolvedFunction
        window.mainInterface.jsReportUriOffsets(JSON.stringify(offsets, null));
    };

    self.getOffsetsForRcaItems = function() {
        var offsets = [];

        // footnotes
        var elements = document.querySelectorAll("[href]");
        var element, href, tempElem, offsetTop, offsetLeft, i;
        for (i=0; i < elements.length; i++) {
            element = elements[i];
            href = element.getAttribute("href");
            if (href != null) {
                href = href.replace("fn_","");
                href = href.replace("f_","");
                offsetTop = 0;
                offsetLeft = 0;
                tempElem = element;
                while (tempElem) {
                    offsetTop += tempElem.offsetTop;
                    offsetLeft += tempElem.offsetLeft;
                    tempElem = tempElem.offsetParent;
                }

                offsets.push({id: href, left: offsetLeft, top: offsetTop});
            }
        }

        // annotations
        elements = document.querySelectorAll("[firstRect]");
        for (i=0; i < elements.length; i++) {
            element = elements[i];
            href = element.getAttribute("annotationId");
            if (href != null) {
                offsetTop = 0;
                offsetLeft = 0;
                tempElem = element;
                while (tempElem) {
                    offsetTop += tempElem.offsetTop;
                    offsetLeft += tempElem.offsetLeft;
                    tempElem = tempElem.offsetParent;
                }

                offsets.push({id: href, left: offsetLeft, top: offsetTop});
            }
        }
        //window.mainInterface.reportHtml("OffsetString:" + offsets.join(" "));
        //noinspection JSUnresolvedFunction
        window.mainInterface.jsReportRcaOffsets(JSON.stringify(offsets, null));
    };

    self.createHighlightColorClass = function (hexColor) {
        var color = hexColor.toLowerCase();
        var style = "background-color: " + hexToRgba(color, 0.5);
        var selector = ".hl-" + hexColor;
        createCssClass(selector, style, false);
    };

    self.createHighlightUnderlineClass = function (hexColor) {
        var color = hexColor.toLowerCase();
        var style = "border-bottom-color: " + hexToRgba(color, 0.8);
        var selector = ".hl-" + hexColor + "-underline";
        createCssClass(selector, style, false);
    };

    self.removeCssClassFromAll = function(className) {
        var list = document.querySelectorAll("." + className);
        if (list != null) {
            var re = new RegExp(className, "g");
            for (var i = 0; i < list.length; ++i) {
                var el = list[i];
                el.className = el.className.replace(re, "");
            }
        }
    };

    var hexToRgba = function(hex, alpha) {
        var bigint = parseInt(hex, 16);
        var r = (bigint >> 16) & 255;
        var g = (bigint >> 8) & 255;
        var b = bigint & 255;

        return "rgba(" + r + "," + g + "," + b + "," + alpha + ");";
    };

    var createCssClass = function(selector, style, replace) {
        // using information found at: http://www.quirksmode.org/dom/w3c_css.html
        // doesn't work in older versions of Opera (< 9) due to lack of styleSheets support
        if (!document.styleSheets)
            return;
        if (document.getElementsByTagName("head").length == 0)
            return;
        var styleSheet, media, mediaType, i;
        if (document.styleSheets.length > 0) {
            for (i = 0; i < document.styleSheets.length; i++) {
                if (document.styleSheets[i].disabled)
                    continue;
                media = document.styleSheets[i].media;
                mediaType = typeof media;
                if (mediaType == "object") {
                    //noinspection JSUnresolvedVariable
                    if (media.mediaText == "" || media.mediaText.indexOf("screen") != -1) {
                        styleSheet = document.styleSheets[i];
                    }
                }
                // stylesheet found, so break out of loop
                if( typeof styleSheet != "undefined")
                    break;
            }
        }
        // if no style sheet is found
        if (typeof styleSheet == "undefined") {
            // create a new style sheet
            var styleSheetElement = document.createElement("style");
            styleSheetElement.type = "text/css";
            // add to <head>
            document.getElementsByTagName("head")[0].appendChild(styleSheetElement);
            // select it
            for(i = 0; i < document.styleSheets.length; i++) {
                if (document.styleSheets[i].disabled)
                    continue;
                styleSheet = document.styleSheets[i];
            }
            // get media type
            media = styleSheet.media;
            mediaType = typeof media;
        }
        if (mediaType == "object") {
            index = 0;
            if (styleSheet.cssRules != null) {
                for (i = 0; i < styleSheet.cssRules.length; i++) {
                    // if there is an existing rule set up, replace it
                    if (styleSheet.cssRules[i].selectorText.toLowerCase() == selector.toLowerCase()) {
                        //consoleLog("FOUND CSS CLASS: " + selector + " " + styleSheet.cssRules[i].style.cssText);
                        if (replace) {
                            styleSheet.cssRules[i].style.cssText = style;
                        }
                        return;
                    }
                }
                index = styleSheet.cssRules.length;
            }
            // or insert new rule
            //consoleLog("INSERTING CSS CLASS: " + selector + " " + style);
            styleSheet.insertRule(selector + "{" + style + "}", index);
        }
    };

    self.getOffsetsForPageNumbers = function() {
        var reportArray = [];
        var breaks = document.querySelectorAll("span.pageBreak");
        for (i = 0; i < breaks.length; i++) {
            el = breaks[i];
            //el.setAttribute("style", "display: inline;");
            var top = el.offsetTop;
            var page = el.getAttribute("page-number");
            var entry = {
                "top" : top,
                "page" : page
            };
            reportArray.push(entry);
        }
        //noinspection JSUnresolvedFunction
        window.mainInterface.jsReportPageOffsets(JSON.stringify(reportArray, null));
    };

    self.consoleLog = function(msg) {
        if (typeof window.mainInterface != "undefined") {
            //noinspection JSUnresolvedFunction
            window.mainInterface.jsConsoleLog(msg);
        }
        else {
            console.log(msg);
        }
    };

    self.setNightMode = function(mode) {
        //consoleLog("SETTING NIGHT MODE: " + mode);
        isNightMode = mode;
        // convert colors here?
    };

    self.getElementByPid = function (pid) {
        return document.querySelector('[pid="' + pid + '"]');
    };

    self.requestTtsText = function() {
        var ttsArray = new Array();
        var elements = document.querySelectorAll('[pid]');
        if (elements != null) {
            for (i = 0; i < elements.length; i++) {
                var element = elements[i];
                var pid = element.getAttribute('pid');
                //consoleLog("tts pid: " + pid);
                ttsArray.push(self.requestTtsTextForPid(element, pid, false));
            }
        }

        if (typeof window.mainInterface != "undefined") {
            //noinspection JSUnresolvedFunction
            var ttsArrayString = JSON.stringify(ttsArray, null)
            window.mainInterface.jsReportTtsText(ttsArrayString);
        }
    };

    self.requestTtsTextForPid = function(element, pid, reportImmediately) {
        var text = "";
        var top = 0;
        var left = 0;
        if (element == null) {
            element = self.getElementByPid(pid);
        }
        if (element != null) {
            var allFilter = function (node) {
                var parent = node.parentElement;
                if (parent != null) {
                    // skip ruby and sup elements
                    var tagName = parent.tagName.toLowerCase();
                    if (tagName == "sup") {
                        return NodeFilter.FILTER_REJECT;
                    } else if (tagName == "rt") {
                        return NodeFilter.FILTER_REJECT;
                    } else if (tagName == "rp") {
                        return NodeFilter.FILTER_REJECT;
                    }
                }
                return NodeFilter.FILTER_ACCEPT;
            };
            top = element.offsetTop;
            left = element.offsetLeft;
            var treeWalker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, allFilter, false);
            while (treeWalker.nextNode()) {
                var currentNode = treeWalker.currentNode;
                text += currentNode.nodeValue;
            }

            text = text.trim();

            // add pauses after the first number in a paragraph
            text = text.replace(/^([0-9]+(?:\.[0-9]+|){0,3})(\s)/, "$1.$2");
            // don't read square brackets [ ] -- just read what's inside
            text = text.replace(/(\[)(.*)(\])/g, "$2");
            // add pauses at the open and close of parentheses ( )
            text = text.replace(/(\()(.*)(\))/g, ". $2. ");
        }

        var textEntry = {
            "top" : top,
            "left" : left,
            "pid" : pid,
            "text" : text,
        };

        if (reportImmediately && typeof window.mainInterface != "undefined") {
            //noinspection JSUnresolvedFunction
            var ttsString = JSON.stringify(textEntry, null)
            window.mainInterface.jsReportTtsTextForPid(ttsString);
        }

        return textEntry;
    };

    self.highlightTtsElementWithPid = function(pid) {
        self.removeCssClassFromAll("ttsHighlight");
        var element = self.getElementByPid(pid);
        if (element != null) {
            element.className = element.className + " ttsHighlight";
            scrollToElement(element);
        }
    };

    // window.mainInterface.reportHtml("ldssa.js reporting in");

    self.highlightUri = function(uri) {
        // MSA-295 set up the colors for highlighting in nightmode or regular mode
        var color = " grayHighlight";
        // for regular mode
        var element = findElementByUri(uri);
        if(element != null) {
            element.className += color;
        }

    };

    self.removeHighlightUri = function(uri) {
        var element = findElementByUri(uri);
        if(element != null) {
            clearGrayHighlight(element);
        }
    };

    return self;

}();
