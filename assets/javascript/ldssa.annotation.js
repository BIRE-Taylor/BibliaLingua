// Namespace
var ldssa = window.ldssa || {};

window.annotationInterface = window.annotationInterface || undefined;
document.caretRangeFromPoint = document.caretRangeFromPoint || undefined;

// annotation module
ldssa.annotation = function () {
    var self = {};

    var adjustHandleY = 40;
    var adjustHandleX = 0;

    // boolean flags
    var isBasicSelectionOnly = (typeof(document.caretRangeFromPoint) !== "function");
    var isInSelectMode = false;
    var isSelecting = false;
    var didTouchLeftHandle = false;
    var didTouchRightHandle = false;
    var didRemoveMarkerTransforms = false;
    var touchDownDidFire = false;

    var touchDownPoint = null;
    var touchMovedCount = 0;

    var selectionRangeStart = null;
    var selectionRangeEnd = null;
    var selectedAnnotation = null;

    self.useBasicTextSelection = function(use) {
        isBasicSelectionOnly = use;
    };

    //noinspection JSUnusedLocalSymbols
    self.onTouchDown = function(x, y, ver) {
        //consoleLog("onTouchDown");
        touchMovedCount = 0;
        touchDownDidFire = true;
        if (isSelecting) return true;

        saveTouchDownPoint(x, y);

        //consoleLog("onTouchDown");
        return true;
    };

    //noinspection JSUnusedLocalSymbols
    self.onTouchMove = function(x, y, ver) {
        if (!isSelecting) return true;

        if (!didRemoveMarkerTransforms) {
            self.removeAllMarkerTransforms(true);
            selectSticky(selectedAnnotation);
            didRemoveMarkerTransforms = true;
        }

        var r1 = createRangeAtPoint(touchDownPoint);
        if (r1 == null) {
            if (didTouchRightHandle) {
                r1 = selectionRangeStart.cloneRange();
            } else {
                r1 = selectionRangeEnd.cloneRange();
            }
        } else if (!didTouchRightHandle && !didTouchLeftHandle && !isBasicSelectionOnly) {
            r1.expand("word");
        }

        if (r1 == null) {
            return true;
        }

        //noinspection JSUnresolvedFunction
        var rect1 = r1.getClientRects()[0];

        x += adjustHandleX;
        y -= adjustHandleY;

        var point = {'x': x, 'y': y};
        var r2 = createRangeAtPoint(point);
        r2.expand("word");

        if (y < rect1.top || (y < rect1.bottom && x < rect1.left)) {
            selectionRangeStart = r2.cloneRange();
            selectionRangeEnd = r1.cloneRange();
            r1.setStart(r2.startContainer, r2.startOffset);
        } else {
            selectionRangeStart = r1.cloneRange();
            selectionRangeEnd = r2.cloneRange();
            r1.setEnd(r2.endContainer, r2.endOffset);
        }

        var sel = window.getSelection();
        //r1.expand("word");
        sel.removeAllRanges();
        sel.addRange(r1);
        r2.detach();

        //consoleLog("WebKit onMove: (" + x + ", " + y + ")");
        return true;
    };

    //noinspection JSUnusedLocalSymbols
    self.onTouchUp = function(ver) {
        //consoleLog("onTouchUp: " + document.getSelection());
        touchDownDidFire = false;
        self.showMarkers();
        if (!isSelecting) return true;

        touchDownPoint = null;
        isSelecting = false;
        didTouchLeftHandle = false;
        didTouchRightHandle = false;
        didRemoveMarkerTransforms = false;
        adjustHandleY = 0;
        adjustHandleX = 0;
        
        var sel = window.getSelection();
        if (sel != null && !sel.isCollapsed) {
            //snapSelectionToWord(sel, ver);
            //removeTrailingSpace(sel);
            var range = sel.getRangeAt(0);
            if (range != null) {
                deselectWhiteSpace(range);
                reportHighlightDataForRange(range, selectedAnnotation);
                selectedAnnotation = null;
                sel.removeAllRanges();
                try { range.detach(); } catch (err) { }
            }
        }

        return true;
    };

    self.removeAllMarkerTransforms = function(hide) {
        var list = document.querySelectorAll(".sticky, .ribbon");
        for(var i = 0; i < list.length; i++) {
            removeMarkerTransforms(list[i], hide);
        }
    };

    var removeMarkerTransforms = function(el, hide) {
        el.className = el.className.replace(/stickySelected/g, "");
        el.className = el.className.replace(/stickyDisambiguate/g, "");
        if (hide) {
            var clsName = el.className;
            if (clsName.indexOf("hiddenElement") < 0) {
                el.className += " hiddenElement";
            }
        }
        el.firstChild.className = el.firstChild.className.replace(/stickySelectedDisplay/g, "");
        el.firstChild.className = el.firstChild.className.replace(/stickyDisambiguateDisplay/g, "");
        var top = el.style["top"];
        el.style.cssText = "top: " + top;
    };

    var getStyle = function(el, style) {
        if (!document.getElementById) return null;

        var value = el.style[style];

        if (!value) {
            if (document.defaultView) {
                value = document.defaultView.getComputedStyle(el, "").getPropertyValue(style);
            }
        } else if (el.currentStyle) {
            value = el.currentStyle[style];
        }

        return value;
    };

    var selectSticky = function(annotObj) {
        if (annotObj == null) return;
        var list = document.querySelectorAll("[sticky]");
        for (var i = 0; i < list.length; i++) {
            var el = list[i];
            el.className = el.className.replace(/stickySelected/g, "");
            el.firstChild.className = el.firstChild.className.replace(/stickySelectedDisplay/g, "");

            if (el.getAttribute("annotationId") == annotObj.id) {
                el.className += " stickySelected";
                el.firstChild.className += " stickySelectedDisplay";
                if (annotObj['hasContent']) {
                    el.firstChild.className += " stickyNote";
                } else {
                    el.firstChild.className.replace(/stickyNote/g, "");
                }
            }
        }
    };

    var disambiguateSticky = function(sticky, top, pos) {
        sticky.className += " stickyDisambiguate";
        sticky.firstChild.className += " stickyDisambiguateDisplay";
        //sticky.style["z-index"] = 5;
        var yAdjust = parseInt(getStyle(sticky, "width")) * 1.25;
        var relTop = top - sticky.offsetTop + (yAdjust * pos);
        sticky.style["WebkitTransform"] = "translate(-" + yAdjust + "px, " + relTop + "px)";
    };

    var sortStickies = function(a, b) {
        var val = a.getAttribute("hlTop") - b.getAttribute("hlTop");
        if (val <= 2 && val >= -2) {
            //consoleLog("VAL: " + val + " " + (a.offsetLeft - b.offsetLeft));
            val = a.getAttribute("hlLeft") - b.getAttribute("hlLeft");
        }
        return val;
    };

    var isDisambiguate = function(el) {
        //return (el.style["z-index"] > 0);
        return (el.className.indexOf("stickyDisambiguate") != -1);
    };

    var haltEvent = function(event) {
        if (event != null) {
            event.preventDefault();
            event.stopPropagation();
        }
    };

    self.onRibbonTouchStart = function(event, element) {
        haltEvent(event);
        touchMovedCount = 0;
    };

    self.onRibbonTouchMove = function(event, element) {
        haltEvent(event);
        touchMovedCount++;
    };

    self.onRibbonTouchCancel = function(event, element) {
        consoleLog("Ribbon Touch Cancel");
    };

    self.onRibbonTouchEnd = function(event, element) {

        if (touchMovedCount > 25) {
            consoleLog("skipping ribbon tap because of movement");
            touchMovedCount = 0;
            return true;
        }

        touchMovedCount = 0;
        haltEvent(event);
        var id = element.getAttribute("annotationId");
        var uri = element.getAttribute("blockUri");
        var top = element.getBoundingClientRect().top;
        var el = LDS.selection.getElementByURI(uri);
        if (el != null) {
            top = el.getBoundingClientRect().top;
        }

        if (typeof window.annotationInterface != "undefined") {
            //noinspection JSUnresolvedFunction
            window.annotationInterface.jsReportAnnotationRibbonTapped(id, top);
        }

        return true;
    };

    self.programmaticRibbonTouch = function(annotationId) {
        var item = document.querySelector("[annotationId=\"" + annotationId + "\"]");
        if (item != null) {
            self.onRibbonTouchEnd(null, item);
        }
    };

    self.onStickyTouchStart = function(event, element) {
        haltEvent(event);
        touchMovedCount = 0;
    };

    self.onStickyTouchMove = function(event, element) {
        haltEvent(event);
        touchMovedCount++;
        //consoleLog("Sticky Touch Move " + touchMovedCount);
    };

    self.onStickyTouchCancel = function(event, element) {
        consoleLog("Sticky Touch Cancel");
    };

    self.onStickyTouchEnd = function(event, element) {

        if (touchMovedCount > 25) {
            consoleLog("skipping sticky tap because of significant movement");
            touchMovedCount = 0;
            return true;
        }

        touchMovedCount = 0;
        haltEvent(event);
        var id = element.getAttribute("annotationId");

        if (typeof window.annotationInterface != "undefined") {
            var sticky, el = null;
            var list = document.querySelectorAll("[annotationId=\"" + id + "\"]");
            for (var i = 0; i < list.length; i++) {
                el = list[i];
                if (el.getAttribute("sticky") != null) {
                    sticky = el;
                }
            }
            var disambiguate = false;
            if (sticky != null) {
                if (!isDisambiguate(sticky)) {
                    self.removeAllMarkerTransforms(false);

                    var multiple = getOverlappingStickies(sticky);
                    if(multiple != null && multiple.length > 0) {
                        disambiguate = true;
                        var top = sticky.offsetTop;
                        if (top > 50) {
                            top -= 50;
                        }
                        multiple.push(sticky);
                        multiple.sort(sortStickies);
                        var timeout = 1000;
                        for(var j = 0; j < multiple.length; j++) {
                            el = multiple[j];
                            disambiguateSticky(el, top, j);
                            timeout += 100;
                        }
                        //alert(ids);
                    }

                } else {
                    disambiguate = true;
                }
            }

            //noinspection JSUnresolvedFunction
            window.annotationInterface.jsReportAnnotationStickyTapped(id, disambiguate);
        }

        return true;
    };

    self.setSelectedAnnotation = function(annotJsonText) {
        var annotObj = JSON.parse(annotJsonText, null);
        selectSticky(annotObj);
    };

    self.clearAnnotations = function() {
        var elements = document.querySelectorAll("[annotationId]");
        for(var i = 0; i < elements.length; i++) {
            var el = elements[i];
            el.parentNode.removeChild(el);
        }
    };

    var createRangeAtPoint = function(point) {
        if (point != null) {
            if (typeof(document.caretRangeFromPoint) == "function") {
                //consoleLog("X:" + x + " Y:" + y);
                var range = document.caretRangeFromPoint(point.x, point.y);
                if (range == null) {
                    return null;
                }
                //range.expand("word");
                return range;
            }
        }

        return null;
    };

    var saveTouchDownPoint = function(x, y) {
        touchDownPoint = {'x': x, 'y': y};
    };

    var consoleLog = function(msg) {
        ldssa.main.consoleLog(msg);
    };

    //noinspection JSUnusedLocalSymbols
    var logTime = function(msg, start) {
        var now = new Date();
        consoleLog(msg + ": " + (now.getMilliseconds() - start.getMilliseconds()) + "ms");
    };

    self.enterSelectModeFromHandle = function(annotJsonText, x, y, rightHandle, xAdjust, yAdjust) {
        self.removeAllMarkerTransforms(true);
        var annotObj = JSON.parse(annotJsonText, null);
        consoleLog("enterSelectModeFromHandle: (" + x + ", " + y + ") " + annotObj.id);

        selectSticky(annotObj);
        didRemoveMarkerTransforms = true;

        saveTouchDownPoint(x, y - window.pageYOffset);

        adjustHandleX = xAdjust;
        adjustHandleY = yAdjust;
        isInSelectMode = true;
        isSelecting = true;
        selectedAnnotation = annotObj;
        didTouchLeftHandle = !rightHandle;
        didTouchRightHandle = rightHandle;
    };

    self.enterSelectModeFromLongPress = function(annotJsonText, x, y, adjusted) {
        self.removeAllMarkerTransforms(true);
        var annotObj = JSON.parse(annotJsonText, null);
        consoleLog("enterSelectModeFromLongPress: (" + x + ", " + y + ") " + annotObj.id);
        saveTouchDownPoint(x, y);

        if (!touchDownDidFire) {
            //isBasicSelectionOnly = true;
            if (typeof window.annotationInterface != "undefined") {
                //noinspection JSUnresolvedFunction
                window.annotationInterface.jsReportSelectionProblem();
            }
            return;
        }
        var r1 = createRangeAtPoint(touchDownPoint);

        if (r1 == null) {
            consoleLog("Unable to create range for selection!");
            return;
        }

        if (!isBasicSelectionOnly) {

            // expand to the word boundaries
            r1.expand("word");

            // validate the range placement contains the touch
            if (!rangeContainsPoint(r1, touchDownPoint)) {
                r1.detach();
                var adjustment = 12;
                if (!adjusted) { // first move the touch point down and try again
                    consoleLog("Adjusting invalid range down (point.y+" + adjustment + ")...");
                    self.enterSelectModeFromLongPress(annotJsonText, x, y + adjustment, "down");
                } else if (adjusted === "down") { // next move the touch point up and try again
                    consoleLog("Adjusting invalid range up (point.y-" + adjustment + ")...");
                    self.enterSelectModeFromLongPress(annotJsonText, x, y - adjustment * 2, "up");
                } else if (adjusted === "up") { // next move the touch point left and try again
                    consoleLog("Adjusting invalid range left (point.x-" + adjustment + ")...");
                    self.enterSelectModeFromLongPress(annotJsonText, x - adjustment, y + adjustment, "left");
                } else { // give up
                    consoleLog("Invalid touch point to create a selection -- aborting!");
                }
                return;
            }

            // validate the range contents contain text
            var rangeText = r1.toString().trim();
            if (rangeText === "") {
                try {
                    consoleLog("Adjusting empty range left on character...");
                    r1.setStart(r1.startContainer, r1.startOffset - 1);
                    r1.expand("word");
                } catch(err) {
                    consoleLog("Unable to adjust empty range -- aborting!");
                    r1.detach();
                    return;
                }
                rangeText = r1.toString().trim();
                if (rangeText === "") {
                    consoleLog("Range is still empty after adjustment -- aborting!");
                    r1.detach();
                    return;
                }
            }

            deselectWhiteSpace(r1);
        }

        isInSelectMode = true;
        isSelecting = true;
        selectedAnnotation = annotObj;

        var sel = window.getSelection();
        sel.removeAllRanges();
        //sel.addRange(r1);
        selectionRangeStart = r1.cloneRange();
        selectionRangeEnd = r1.cloneRange();
        reportHighlightDataForRange(r1, annotObj);

    };

    var deselectWhiteSpace = function(range) {
        if (range != null) {
            var rangeText = range.toString();
            if (rangeText.length > 1) {
                var whitespace = /\s+|[\u002D\u058A\u05BE\u1400\u1806\u2010-\u2015\u2053\u207B\u208B\u2212\u2E17\u2E1A\u2E3A-\u301C\u3030\u30A0\uFE31\uFE32\uFE58\uFE63\uFF0D]/g;
                var firstChar = rangeText.substr(0, 1);
                if (firstChar.match(whitespace)) {
                    try {
                        consoleLog("Removing whitespace from the beginning of the range...");
                        range.setStart(range.startContainer, range.startOffset + 1);
                    } catch(err) {
                        consoleLog("Unable to adjust range start boundary: " + err);
                    }
                }
                var lastChar = rangeText.substr(rangeText.length - 1, 1);
                if (lastChar.match(whitespace)) {
                    try {
                        consoleLog("Removing whitespace from the end of the range...");
                        range.setEnd(range.endContainer, range.endOffset - 1);
                    } catch(err) {
                        consoleLog("Unable to adjust range end boundary: " + err);
                    }
                }
            }
        }
    }

    var rangeContainsPoint = function(range, point) {
        if (range != null && point != null) {
            var rects = range.getClientRects();
            for (var i=0; i<rects.length; i++) {
                var rect = rects[i];
                var xIsInside = rect.left < point.x && point.x < rect.right;
                var yIsInside = rect.top < point.y && point.y < rect.bottom;
                if (xIsInside && yIsInside) {
                    return true;
                }
            }
        }
        return false;
    }

    var getOverlappingStickies = function(sticky) {
        var elements = document.querySelectorAll(".sticky");
        var overlapping = [];
        for(var i = 0; i < elements.length; i++) {
            var el = elements[i];
            if (sticky == el) continue;
            if (isOverlapping(sticky, el)) {
                overlapping.push(el);
            }
        }
        return overlapping;
    };

    self.hideMarkers = function() {
        var elements = document.querySelectorAll(".sticky");
        for(var i = 0; i < elements.length; i++) {
            var el = elements[i];
            var clsName = el.className;
            if (clsName.indexOf("hiddenElement") < 0) {
                el.className += " hiddenElement";
            }
        }
    };

    self.showMarkers = function() {
        //consoleLog("showMarkers");
        var elements = document.querySelectorAll(".sticky, .ribbon");
        for(var i = 0; i < elements.length; i++) {
            var el = elements[i];
            var clsName = el.className;
            if (clsName.indexOf("hiddenElement") != -1) {
                el.className = clsName.replace(/hiddenElement/g, "");
            }
        }
    };

    var HighlightRect = function(left, top, width, height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.right = left + width;
        this.bottom = top + height;
    };

    HighlightRect.prototype.isOnSameLineAsRect = function(other) {
        if (this.top != other.top) {
            if (this.bottom > other.top && other.bottom > this.top) {
                if (this.top > other.top) { // other extends up past this
                    return this.top - other.top < other.bottom - this.top;
                } else {
                    return other.top - this.top < this.bottom - other.top;
                }
            }
        } else if (this.bottom != other.bottom) {
            if (this.bottom > other.top && other.bottom > this.top) {
                if (this.bottom < other.bottom) { // other extends down past this
                    return other.bottom - this.bottom < this.bottom - other.top;
                } else {
                    return this.bottom - other.bottom < other.bottom - this.top;
                }
            }
        }
        return false;
    };

    HighlightRect.prototype.containsOrIsContainedByRect = function(other) {
        return (other.bottom <= this.bottom && other.top >= this.top) ||
            (other.bottom >= this.bottom && other.top <= this.top);
    };

    var setSupStyleHidden = function() {
        var theRules = [];
        if (document.styleSheets[0].cssRules) {
            theRules = document.styleSheets[0].cssRules;
        } else if (document.styleSheets[0].rules) {
            theRules = document.styleSheets[0].rules;
        }
        for (var n in theRules) {
            //noinspection JSUnfilteredForInLoop
            if ('sup' == theRules[n].selectorText) {
                //noinspection JSUnfilteredForInLoop
                theRules[n].style.display = "none"
            }
        }
    };

    var setSupStyleVisible = function() {
        //return; // no-op
        var theRules = [];
        if (document.styleSheets[0].cssRules) {
            theRules = document.styleSheets[0].cssRules;
        } else if (document.styleSheets[0].rules) {
            theRules = document.styleSheets[0].rules;
        }
        for (var n in theRules) {
            //noinspection JSUnfilteredForInLoop
            if ('sup' == theRules[n].selectorText) {
                //noinspection JSUnfilteredForInLoop
                theRules[n].style.display = "inline"
            }
        }
    };

    var isOverlapping = function(el1, el2) {
        var pos_el1 = absolutePosition(el1);
        var pos_el2 = absolutePosition(el2);
        var top1 = pos_el1.y;
        var left1 = pos_el1.x;
        var right1 = left1 + el1.offsetWidth;
        var bottom1 = top1 + el1.offsetHeight;
        var top2 = pos_el2.y;
        var left2 = pos_el2.x;
        var right2 = left2 + el2.offsetWidth;
        var bottom2 = top2 + el2.offsetHeight;
        var _getSign = function(v) {
            if(v > 0)
                return "+";
            else if(v < 0)
                return "-";
            else
                return 0;
        };
        return (_getSign(top1 - bottom2) != _getSign(bottom1 - top2)) &&
            (_getSign(left1 - right2) != _getSign(right1 - left2));

    };

    var absolutePosition = function(el) {
        var posObj = {
            'x' : el.offsetLeft,
            'y' : el.offsetTop
        };
        if(el.offsetParent) {
            var temp_pos = absolutePosition(el.offsetParent);
            posObj.x += temp_pos.x;
            posObj.y += temp_pos.y;
        }
        return posObj;
    };

    var getParentUriNodeForNode = function(node) {
        if (node != null) {
            var isText = node.nodeType == Node.TEXT_NODE;
            var docId = !isText ? node.getAttribute("doc-id") : null;
            if (docId != null) {
                return null;
            }
            var uri = !isText ? node.getAttribute("uri") : null;
            //consoleLog("GET PARENT URI NODE: " + node.nodeName + ": " + uri);
            if (uri != null) {
                return node;
            } else {
                node = node.parentNode;
                return getParentUriNodeForNode(node);
            }
        }
        return null;
    };

    var getParentUriForNode = function(node) {
        if (node != null) {
            var isText = node.nodeType == Node.TEXT_NODE;
            var docId = !isText ? node.getAttribute("doc-id") : null;
            if (docId != null) {
                return null;
            }
            var uri = !isText ? node.getAttribute("uri") : null;
            //consoleLog("GET PARENT URI: " + node.nodeName + ": " + uri);
            if (uri != null) {
                return uri;
            } else {
                node = node.parentNode;
                return getParentUriForNode(node);
            }
        }
        return null;
    };

    var uri_identifiers = ["head", "s", "t", "p", "fn", "f", "closing"];

    var isValidBlockLevelUri = function (uri) {
        if (uri == null || uri == "null" || uri.length == 0) return false;
        if (uri.indexOf("/") != 0) return false;

        var isValid = false;
        var lastDotIndex = uri.lastIndexOf('.');
        if (lastDotIndex > -1 && uri.length > lastDotIndex + 1) {
            var blockLevelIdentifier = uri.substring(lastDotIndex + 1);
            if (blockLevelIdentifier.length > 0) {
                isValid = isValidBlockLevelIdentifier(blockLevelIdentifier);
            }
        }

        return isValid;
    };

    var isValidBlockLevelIdentifier = function (str) {
        if (str == null || str.length == 0) return false;
        if (str.indexOf(" ") >= 0) return false;

        var isValid = false;

        var len = str.length;
        var digit = -1;
        for (var i = 0; i < len; i++) {
            var char = str.charAt(i);
            if (isDigits(char)) {
                digit = i;
                break;
            }
        }

        if (digit >= 0) {
            if (digit == 0) {
                isValid = (str.indexOf("0") != 0 && isDigits(str));
            } else {
                var identifierWithoutIndex = str.substr(0, digit);
                for (var i = 0; i < uri_identifiers.length; i++) {
                    var identifier = uri_identifiers[i];
                    if (identifierWithoutIndex == identifier) {
                        isValid = true;
                        break;
                    }
                }
            }
        }

        return isValid;
    };

    var isDigits = function (str) {
        return !isNaN(parseInt(str)) && isFinite(str);
    }

    var getColorFromHlInfo = function(hlInfo) {
        var hlColorValue = hlInfo['colorValue'];
        if (hlColorValue == null ||
            hlColorValue.length == 0 ||
            hlColorValue == '000000' ||
            hlColorValue == 0 ) {
            hlColorValue = "clear";
        }
        return hlColorValue;
    };

    var getStyleFromHlInfo = function(hlInfo) {
        var hlStyle = hlInfo['style'];
        if (hlStyle == null ||
            hlStyle.length == 0) {
            hlStyle = "";
        }
        return hlStyle;
    };

    var getUriFromHlInfo = function(annotObj) {
        var hlUri = annotObj['uri'];
        if (hlUri == null) {
            var highlights = annotObj.highlights;
            if (highlights != null && highlights.length > 0) {
                // each highlight is in a container "highlight" : { <highlight> }
                var highlightObj = highlights[0]['highlight'];
                hlUri = highlightObj['uri'];
            }
        }
        if (hlUri == null ||
            hlUri.length == 0 ||
            hlUri == 0 ) {
            hlUri = null;
        }
        return hlUri;
    };

    self.requestTextForAnnotation = function(annotJsonText, sendTo) {
        consoleLog("Annotation to copy:\n" + annotJsonText);
        var annotObj = JSON.parse(annotJsonText, null);
        var text = "";
        var highlights = annotObj['highlights'];
        var uri = "";

        if (highlights !== undefined) {
            try {
                var allFilter = function (node) {
                    return NodeFilter.FILTER_ACCEPT;
                };
                for (var i = 0; i < highlights.length; i++) {
                    var hlInfo = highlights[i]['highlight'];
                    var range = LDS.selection.getRangeForWordOffsets([hlInfo]);
                    var fragment = range.cloneContents();
                    var treeWalker = document.createTreeWalker(fragment, NodeFilter.SHOW_TEXT, allFilter, false);

                    if (text.length > 0) {
                        text += "\n";
                    }

                    while (treeWalker.nextNode()) {
                        var node = treeWalker.currentNode;
                        var parent = node.parentElement;
                        if (parent == null || parent.tagName.toLowerCase() != "sup") {
                            text += node.nodeValue;
                        }
                    }

                    uri += hlInfo['uri'] + "|";
                }
            } catch (err) {
                consoleLog("Error in requestTextForAnnotation: " + err.toString());
            }
        }

        if (typeof window.annotationInterface != "undefined") {
            if (sendTo == 101) {
                if (uri.indexOf("null") != -1) {
                    uri = annotObj['uri'];
                }
                //noinspection JSUnresolvedFunction
                window.annotationInterface.jsReportAnnotationText(uri, text, sendTo);
            } else {
                //noinspection JSUnresolvedFunction
                window.annotationInterface.jsReportAnnotationText(annotObj['id'], text, sendTo);
            }
        }
    };

    self.bookmarkRibbonUpdated = function(top) {
        ldssa.main.removeCssClassFromAll("bookmarkHighlight");
        var el = null;
        var increment = 16;
        var x = 64;
        top = parseInt(top, 10) + increment;
        while ((el == null || el.nodeName == "IMG") && x < increment * 20) {
            x += increment;
            el = document.elementFromPoint(x, top);
        }
        el = getParentUriNodeForNode(el);
        if (el != null) {
            var uri = getParentUriForNode(el);
            if (uri != null) {
                el.className = el.className + " bookmarkHighlight";
                if (typeof window.annotationInterface != "undefined") {
                    //noinspection JSUnresolvedFunction
                    window.annotationInterface.jsReportBookMarkRibbonUri(uri, el.getBoundingClientRect().top);
                }
            }
        }
    };

    self.createBookmarkIndicator = function(annotJsonText) {
        var annotObj = JSON.parse(annotJsonText, null);
        //consoleLog("ANNOTATION: " + annotJsonText);
        var bookmark = annotObj.bookmark;
        if (bookmark != null) {
            self.removeDivsForAnnotationId(annotObj.id, false);
            var uri = bookmark['blockUri'];
            var el = LDS.selection.getElementByURI(uri);
            var top = 96;
            if (el != null) {
                top = el.getBoundingClientRect().top + window.pageYOffset;
            }
            top -= 4; // adjust for top padding of tappable area
            var div = document.createElement("div");
            div.setAttribute("style", "top:" + top + "px;");
            div.setAttribute("class", "ribbon");
            div.setAttribute("annotationId", annotObj.id);
            div.setAttribute("blockUri", uri);
            div.setAttribute("onTouchStart", "ldssa.annotation.onRibbonTouchStart(event, this)");
            div.setAttribute("onTouchMove", "ldssa.annotation.onRibbonTouchMove(event, this)");
            div.setAttribute("onTouchEnd", "ldssa.annotation.onRibbonTouchEnd(event, this)");
            div.setAttribute("onTouchCancel", "ldssa.annotation.onRibbonTouchCancel(event, this)");
            var inner = document.createElement("div");
            inner.setAttribute("class", "ribbonDisplay");
            div.appendChild(inner);
            document.body.appendChild(div);
        }
    };

    self.applyRedRibbonEffects = function(id) {
        var item = document.querySelector("[annotationId=\"" + id + "\"] > div.ribbonDisplay");
        if (item != null) {
            item.className += " ribbonDisplayRed";
            setTimeout(self.removeRedRibbonEffects, 2000);
        }
    };

    self.removeRedRibbonEffects = function() {
        var list = document.querySelectorAll(".ribbonDisplayRed");
        for (var i=0; i < list.length; i++) {
            var item = list[i];
            item.className = item.className.replace(/ribbonDisplayRed/g, "");
        }
    };

    self.requestInfoForHighlights = function(annotJsonText) {
        var annotObj = JSON.parse(annotJsonText, null);

        //consoleLog("ANNOTATION: " + annotJsonText);
        var sel = window.getSelection();
        sel.removeAllRanges();
        var highlights = annotObj.highlights;
        if (highlights != null && highlights.length > 0) {
            // each highlight is in a container { "highlight" : { <highlight> }
            var wordOffsets = [];
            for (var i = 0; i < highlights.length; i++) {
                var highlightObj = highlights[i]['highlight'];
                //consoleLog("  HIGHLIGHT: " + JSON.stringify(highlightObj, null));
                wordOffsets.push(highlightObj);
            }
            var range = LDS.selection.getRangeForWordOffsets(wordOffsets);
            //consoleLog("RANGE: '" + range.toString() + "'");
            reportHighlightDataForRange(range, annotObj);
        }

        selectSticky(annotObj);
    };

    self.setActiveAnnotationIdOnHighlights = function(annotId) {
        var list = document.querySelectorAll("[annotationId=\"null\"]");
        if (list != null) {
            for (var i = 0; i < list.length; ++i) {
                var item = list[i];
                //consoleLog("Setting active ID \"" + annotId + "\" on " + item.className);
                item.setAttribute("annotationId", annotId);
            }
        }
    };

    self.removeDivsForAnnotationId = function(annotId, saveSticky) {
        //consoleLog("REMOVE HIGHLIGHT RECTS: " + annotId);
        var list = document.querySelectorAll("[annotationId=\"" + annotId + "\"]");
        var didFindSticky = false;
        if (list != null) {
            for (var i = 0; i < list.length; ++i) {
                var item = list[i];
                if (saveSticky && isDisambiguate(item)) {
                    didFindSticky = true;
                    //consoleLog("DID FIND STICKY: " + annotId);
                } else {
                    //consoleLog("REMOVING HIGHLIGHT DIV: " + item.toString());
                    item.parentNode.removeChild(item);
                }
            }
        }
        return didFindSticky;
    };

    var pushRectIntoKeepers = function(lineRect, keepers) {
        if (lineRect != null) {
            var spliced = false;
            var k = keepers.length;
            // Compare the top of lineRect with our last few keepers.
            // If the tops are the same, then take whichever of them is wider
            // and splice it into the keepers array
            while (k--) {
                var keeper = keepers[k];
                //consoleLog("COMPARE TOPS: " + lineRect.top + " == " + keeper.top );
                if (lineRect.top == keeper.top) {
                    //consoleLog("TOPS ARE EQUAL: " + lineRect.left + ", " + lineRect.top + ", " + lineRect.width + ", " + lineRect.height);
                    if (lineRect.width < keeper.width) {
                        lineRect = keeper;
                        //consoleLog("KEEPER IS WIDER: " + lineRect.left + ", " + lineRect.top + ", " + lineRect.width + ", " + lineRect.height);
                    }
                    keepers.splice(k, 1, lineRect);
                    spliced = true;
                    break;
                }

                // shouldn't have to go back more than 2 lines
                if (keepers.length - k > 2) {
                    break;
                }
            }

            // if we didn't need to splice the rect in to the keepers array, just push it here
            if (!spliced) {
                keepers.push(lineRect);
            }
        }
    };

    var getHighlightClassName = function(color, style) {
        if (style == null || style.length == 0) return color;
        return style;
    };

    var createHighlightDivsForRects = function(keepers, addedRects, didPlaceSticky, annotObj) {

        //hasContent = true;
        var processed = {};

        var hlUri = getUriFromHlInfo(annotObj);
        var uriValid = isValidBlockLevelUri(hlUri);
        consoleLog("Annotation URI: " + hlUri + " valid:" + uriValid);

        if (!uriValid) {
            try {
                //noinspection JSUnresolvedFunction
                window.annotationInterface.jsReportInvalidUri(hlUri);
            } catch(err)  {
                consoleLog("reportInvalidUri Error:" + err);
            }
        }

        var hasContent = annotObj['hasContent'];
        var hlColor = getColorFromHlInfo(annotObj);
        var hlStyle = getStyleFromHlInfo(annotObj);
        var cssColorClass = getHighlightClassName(hlColor, hlStyle);
        ldssa.main.createHighlightColorClass(cssColorClass);
        var cssStyleClass = "";
        if (hlStyle != null && hlStyle.length > 0) {
            cssStyleClass = " hl-" + hlColor + "-underline";
            ldssa.main.createHighlightUnderlineClass(hlColor);
        }
        for (var l = 0; l < keepers.length; l++) {
            var rect = keepers[l];
            var key = rect.top + "_" + rect.left;
            if (isBasicSelectionOnly || processed[key] != 1) { //addedRects.indexOf(rect) == -1) {
                addedRects.push(rect);
                processed[key] = 1;
                //consoleLog("PROCESSED: " + key);
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

                var classes = "hl-" + cssColorClass + cssStyleClass + " hlextra-" + cssColorClass + " hl-box";
                div.setAttribute("style", style);
                if (isBasicSelectionOnly) {
                    div.setAttribute("class", classes);
                } else {
                    var inner = document.createElement("div");
                    var topAdjust = rect.height * 0.05;
                    var btmAdjust = rect.height * 0.1;
                    var innerStyle = "position:absolute; top:" + topAdjust + "px; width:" + rect.width + "px; height:" + (rect.height - topAdjust - btmAdjust) + "px; z-index:-1";
                    inner.setAttribute("style", innerStyle);
                    inner.setAttribute("class", classes);
                    div.appendChild(inner);
                }
                div.setAttribute("annotationId", annotObj.id);
                div.setAttribute("hasContent", hasContent);
                if (l == 0) {
                    div.setAttribute("firstRect", "true");
                }
                document.body.appendChild(div);

                if (l == 0 && !didPlaceSticky) {
                    didPlaceSticky = true;
                    div.setAttribute("firstRect", "true");
                    //<div class=\"stickyDisplay hl2\"></div></div>"
                    var stickyTap = document.createElement("div");
                    stickyTap.setAttribute("style", "top: " + (rect.top - 7) + "px;");
                    stickyTap.setAttribute("annotationId", annotObj.id);
                    stickyTap.setAttribute("hasContent", hasContent);
                    stickyTap.setAttribute("sticky", "true");
                    stickyTap.setAttribute("class", "sticky stickyRotate1");
                    stickyTap.setAttribute("hlTop", rect.top);
                    stickyTap.setAttribute("hlLeft", rect.left);
                    stickyTap.setAttribute("onTouchStart", "ldssa.annotation.onStickyTouchStart(event, this)");
                    stickyTap.setAttribute("onTouchMove", "ldssa.annotation.onStickyTouchMove(event, this)");
                    stickyTap.setAttribute("onTouchEnd", "ldssa.annotation.onStickyTouchEnd(event, this)");
                    stickyTap.setAttribute("onTouchCancel", "ldssa.annotation.onStickyTouchCancel(event, this)");
                    var stickyShow = document.createElement("div");
                    stickyShow.setAttribute("class", "stickyDisplay");
                    stickyShow.setAttribute("style", "margin:0px; padding: 0px;");
                    if(hasContent) {
                        stickyShow.className += " stickyNote";
                    }
                    stickyTap.appendChild(stickyShow);
                    document.body.appendChild(stickyTap);
                }
            }
        }

        return didPlaceSticky;
    };

    var reportHighlightDataForRange = function(range, annotObj) {
        try {
            if(range != null) {
                if(!isBasicSelectionOnly) {
                    var startRange = range.cloneRange();
                    startRange.collapse(true);
                    startRange.expand("word");
                    if (startRange.collapsed) {
                        //consoleLog("START RANGE COLLAPSED");
                        return;
                    }
                    selectionRangeStart = startRange.cloneRange();
                    startRange.detach();

                    var endRange = range.cloneRange();
                    endRange.collapse(false);
                    endRange.expand("word");
                    selectionRangeEnd = endRange.cloneRange();
                    endRange.detach();
                }

                try {
                    //noinspection JSUnresolvedFunction
                    window.annotationInterface.jsReportCurrentAnnotationText(range.toString());
                } catch(err)  {
                    consoleLog("reportCurrentAnnotationText Error:" + err);
                }

                var didPlaceSticky = self.removeDivsForAnnotationId(annotObj.id, true);

                var highlightDataArray = [];
                var offsets = LDS.selection.getWordOffsetsFromRange(range);

                annotObj.uri = null;
                if (offsets != null && offsets.length > 0) {
                    for(var i = 0; i < offsets.length; i++) {
                        var hlInfo = offsets[i];
                        var hlUri = hlInfo['uri'];
                        if (annotObj.uri == null || !isValidBlockLevelUri(hlUri)) {
                            annotObj.uri = hlUri;
                            consoleLog("URI: " + hlUri);
                        }
                        hlInfo.colorName = annotObj['colorName'];
                        hlInfo.colorValue = getColorFromHlInfo(annotObj);
                        hlInfo.style = getStyleFromHlInfo(annotObj);
                        //consoleLog("HLINFO: " + JSON.stringify(hlInfo, null));

                        var info = {
                            "offsets" : hlInfo
                        };

                        highlightDataArray.push(info);
                    }
                }

                var keepers = []; // this is the array of coalesced rects (1 per line) of text
                var yOffset = window.pageYOffset;

                if (isBasicSelectionOnly) {
                    var uriNode = getParentUriNodeForNode(range.startContainer);
                    var fullRect = new HighlightRect(uriNode.offsetLeft, uriNode.offsetTop, uriNode.clientWidth, uriNode.clientHeight);
                    keepers.push(fullRect);
                } else {
                    //noinspection JSUnresolvedFunction
                    var clientRects = LDS.selection.getRectsForRange(range);
                    var boundingRect = document.documentElement.getBoundingClientRect();
                    var xOffset = boundingRect.left; // if we're swiping between chapters
                    var entireLineRect = null;

                    for (var j = 0; j < clientRects.length; j++) {
                        var r = clientRects[j];
                        var left = r.left - xOffset;
                        var top = r.top + yOffset;
                        var width = r.width;
                        var height = r.height;

                        // adjust the rect height a little bit to make
                        // for nicer-size highlight boxes
                        var tAdjust = height * 0.15;
                        var hAdjust = height * 0.20;
                        top += tAdjust;
                        height -= hAdjust;

                        var currentRect = new HighlightRect(left, top, width, height);

                        // check for line overlap. if there is overlap, adjust the
                        // existing rect to swallow the new one.
                        var isNewLine = true;
                        if (entireLineRect != null && currentRect != null) {
                            var rectIsOffset = entireLineRect.isOnSameLineAsRect(currentRect);
                            var rectIsContained = !rectIsOffset && entireLineRect.containsOrIsContainedByRect(currentRect);

                            if (rectIsOffset || rectIsContained) {
                                isNewLine = false;

                                // set the left value of entireLineRect the leftmost "left" value of the two rects
                                entireLineRect.left = Math.min(currentRect.left, entireLineRect.left);
                                // set the right value of entireLineRect the rightmost "right" value of the two rects
                                entireLineRect.right = Math.max(currentRect.right, entireLineRect.right);

                                if (rectIsOffset) {
                                    // set the top value of entireLineRect the lowest (visually on the page) "top" value of the two rects
                                    entireLineRect.top = Math.max(currentRect.top, entireLineRect.top);
                                    // set the bottom value of entireLineRect the lowest (visually on the page) "bottom" value of the two rects
                                    entireLineRect.bottom = Math.max(currentRect.bottom, entireLineRect.bottom);
                                } else {
                                    // set the top value of entireLineRect the highest (visually on the page) "top" value of the two rects
                                    entireLineRect.top = Math.min(currentRect.top, entireLineRect.top);
                                    // set the bottom value of entireLineRect the lowest (visually on the page) "bottom" value of the two rects
                                    entireLineRect.bottom = Math.max(currentRect.bottom, entireLineRect.bottom);
                                }

                                // re-calculate width and height with our new values
                                entireLineRect.width = entireLineRect.right - entireLineRect.left;
                                entireLineRect.height = entireLineRect.bottom - entireLineRect.top;

                                //consoleLog("UPDATED: " + entireLineRect.left + ", " + entireLineRect.top + ", " + entireLineRect.width + ", " + entireLineRect.height);
                            }
                        }

                        if (isNewLine) {
                            // the new rect does not overlap with our entireLineRect, so save entireLineRect into the keepers array
                            pushRectIntoKeepers(entireLineRect, keepers);

                            // then create a new entireLineRect with our rect values. this should be the first rect in a new line
                            entireLineRect = currentRect;

                        }

                        // if this is our last rect, we need to push it into our keepers array
                        if (j == clientRects.length - 1) {
                            pushRectIntoKeepers(entireLineRect, keepers);
                        }

                    }
                }

                var addedRects = [];
                createHighlightDivsForRects(keepers, addedRects, didPlaceSticky, annotObj);

                if (typeof window.annotationInterface != "undefined") {
                    var selInfo = {
                        "highlights" : highlightDataArray,
                        "rects" : addedRects,
                        "yScrollOffset" : yOffset,
                        "annotationId" : annotObj.id
                    };
                    //noinspection JSUnresolvedFunction
                    var selInfoString = JSON.stringify(selInfo, null)
                    window.annotationInterface.jsReportHighlightData(selInfoString);
                }

                range.detach();
            }
            window.getSelection().removeAllRanges();
        } catch (err) {
            consoleLog("reportHighlightData Error:" + err);
        }
    };

    return self;

}();