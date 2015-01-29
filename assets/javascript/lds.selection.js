/**
 The LDS Javascript namespace.
 */
var LDS = LDS || {};

/**
 The LDS.selection module.
 */
LDS.selection = function () {
    var self = {};

    self.getWordOffsetsFromRange = function (range) {

        var wordOffsets = new Array();

        var startContainer = range.startContainer;
        var startCharacterOffset = range.startOffset;
        var startElement = self.getNearestAncestorWithURIAttribute(startContainer);

        if (startElement == null) {
            return null;
        }

        var endContainer = range.endContainer;
        var endCharacterOffset = range.endOffset;
        var endElement = self.getNearestAncestorWithURIAttribute(endContainer);

        if (endElement == null) {
            return null;
        }

        var elements = getAllElementsInRange(startElement, endElement, range);
        for (var i = 0; i < elements.length; i++) {
            var element = elements[i];

            var uri = element.getAttribute('uri');
            var startOffset = -1;
            var endOffset = -1;

            if (element == startElement) {
                startOffset = getWordOffset(startContainer, startCharacterOffset, startElement, true);
                if (startElement.getAttribute('uri') != endElement.getAttribute('uri')) {
                    endOffset = -1;
                } else {
                    endOffset = getWordOffset(endContainer, endCharacterOffset, endElement, false);
                }
            } else if (element == endElement) {
                if (startElement.getAttribute('uri') != endElement.getAttribute('uri')) {
                    startOffset = -1;
                } else {
                    startOffset = getWordOffset(startContainer, startCharacterOffset, startElement, true);
                }
                endOffset = getWordOffset(endContainer, endCharacterOffset, endElement, false);
            }

            var annotation = {uri: uri, startOffset: startOffset, endOffset: endOffset};
            wordOffsets.push(annotation);
        }

        return wordOffsets;
    };

    self.getRectsForRange = function (range) {
        var commonAncestor = range.commonAncestorContainer;

        var nodesInRangeFilter = function (node) {
            if (rangeContainsNode(range, node)) {
                return NodeFilter.FILTER_ACCEPT;
            }
            return NodeFilter.FILTER_SKIP;
        };

        var treeWalker = document.createTreeWalker(commonAncestor, NodeFilter.SHOW_TEXT, nodesInRangeFilter, false);

        var rects = new Array();

        while (treeWalker.nextNode()) {
            var clientRects = getRectsForRangeInNode(treeWalker.currentNode, range);
            for (var i = 0; i < clientRects.length; i++) {
                rects.push(clientRects[i]);
            }
        }

        if (rects.length == 0) {
            var clientRects = getRectsForRangeInNode(commonAncestor, range);
            for (var i = 0; i < clientRects.length; i++) {
                rects.push(clientRects[i]);
            }
        }

        return rects;
    };

    self.getRangeForWordOffsets = function (wordOffsets) {
        if (wordOffsets == null || wordOffsets.length == 0) {
            return null;
        }

        var start = wordOffsets[0]; //first element in list
        if (start == null) return null;
        var startElement = self.getElementByURI(start.uri);
        if (startElement == null) return null;
        var startContainerAndOffset = getContainerAndOffset(startElement, start.startOffset, true);
        if (startContainerAndOffset == null) return null;

        var end = wordOffsets[wordOffsets.length - 1]; //last element in list
        if (end == null) return null;
        var endElement = self.getElementByURI(end.uri);
        if (endElement == null) return null;
        var endContainerAndOffset = getContainerAndOffset(endElement, end.endOffset, false);
        if (endContainerAndOffset == null) return null;

        var range = document.createRange();
        range.setStart(startContainerAndOffset.container, startContainerAndOffset.offset);
        range.setEnd(endContainerAndOffset.container, endContainerAndOffset.offset);
        return range;
    };

    self.getNearestAncestorWithURIAttribute = function (element) {
        if (element == null) {
            return null;
        }
        if (!hasUri(element)) {
            return self.getNearestAncestorWithURIAttribute(element.parentElement);
        }

//        /* START - This will go away when we switch to PIDs */
//        if (!hasPid(element)) {
//            return self.getNearestAncestorWithURIAttribute(element.parentElement);
//        }
//        /* END */

        return element;
    };

    self.getElementByURI = function (uri) {
        return document.querySelector('[uri="' + uri + '"]');
    };

    var consoleLog = function(msg) {
        ldssa.main.consoleLog(msg);
    };

    var getWordOffset = function (container, offset, element, isStart) {
        var foundWordOffset = false;
        var totalWords = 0;
        var wordOffset = 0;
        var currentOffset = 0;
        var isContainer = false;

        var allFilter = function (node) {
            return NodeFilter.FILTER_ACCEPT;
        };

        var treeWalker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, allFilter, false);
        while (treeWalker.nextNode()) {
            var node = treeWalker.currentNode;
            currentOffset = 0; //reset, new node

            var words = node.nodeValue.split(/\s+|[\u002D\u058A\u05BE\u1400\u1806\u2010-\u2015\u2053\u207B\u208B\u2212\u2E17\u2E1A\u2E3A-\u301C\u3030\u30A0\uFE31\uFE32\uFE58\uFE63\uFF0D]/g);
            for (var i = 0; i < words.length; i++) {
                var wordLength = words[i].length;
                var word = words[i].trim();

                if (word.length == 0) {
                    //empty string
                    currentOffset++;
                    continue;
                }

                totalWords++; //add to our total count of words

                if (foundWordOffset) {
                    continue; //already found the word, so just keep looping until we get the totalWords count
                }

                //we haven't found the word yet, so increment our word offset
                wordOffset++;
                
                if (node != container) {
                    //not in the container, so just continue
                    continue;
                }

                var plusSpace = (words.length > 1);
                var nextOffset = (currentOffset + wordLength + plusSpace);

                if ((isStart && offset < nextOffset) || (!isStart && offset <= nextOffset)) {
                    foundWordOffset = true;
                }
                currentOffset += (wordLength + plusSpace);
            }
        }

        if ((isStart && wordOffset == 1) || (!isStart && wordOffset == totalWords)) {
            wordOffset = -1; //start or end of paragraph
        }

        return wordOffset;
    };

    var getContainerAndOffset = function (element, wordOffset, isStart) {
        var currentWordOffset = 0;

        var allFilter = function (node) {
            return NodeFilter.FILTER_ACCEPT;
        };

        if (isStart && (wordOffset == -1 || wordOffset == 1)) {
            //find the first text node and return character 0
            var container = getFirstTextNode(element);
            return {container: container, offset: 0};
        } else if (!isStart && wordOffset == -1) {
            wordOffset = getMaxWordOffset(element);
        }

        var treeWalker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, allFilter, false);
        while (treeWalker.nextNode()) {
            var node = treeWalker.currentNode;
            var words = node.nodeValue.split(/\s+|[\u002D\u058A\u05BE\u1400\u1806\u2010-\u2015\u2053\u207B\u208B\u2212\u2E17\u2E1A\u2E3A-\u301C\u3030\u30A0\uFE31\uFE32\uFE58\uFE63\uFF0D]/g);
            var offset = 0;
            var length = words.length;

            for (var i = 0; i < length; i++) {
                var word = words[i];

                var charOffsetOfWord = word.length;

                if (length > 1 && i != (length - 1)) {
                    charOffsetOfWord++; //add +1 for the space if more than 1 word in node, and its not the last work
                }

                offset += charOffsetOfWord;

                word = word.trim();
                if (word.length == 0) {
                    continue; //no word
                }

                currentWordOffset++;

                if (wordOffset == currentWordOffset) {
                    if (isStart) {
                        //offset should be at the start of this word, not the end.
                        offset -= charOffsetOfWord;
                    } else if (length > 1 && i != (length - 1)) {
                        offset--;
                    }
                    return {container: node, offset: offset};
                }
            }
        }

        return null;
    };

    var hasUri = function (element) {
        return (element.attributes != null && element.attributes.getNamedItem('uri') != null);
    };

    var hasPid = function (element) {
        return (element.attributes != null && element.attributes.getNamedItem('pid') != null);
    };

    var getFirstTextNode = function (node) {
        var children = node.childNodes;
        for (var i = 0; i < children.length; i++) {
            var child = children[i];
            if (child.nodeType == 3) {
                return child;
            }

            var textNode = getFirstTextNode(child);
            if (textNode != null) {
                return textNode;
            }
        }
        return null;
    };

    var getMaxWordOffset = function (element) {
        var wordOffset = 0;
        var allFilter = function (node) {
            return NodeFilter.FILTER_ACCEPT;
        };
        var treeWalker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, allFilter, false);
        while (treeWalker.nextNode()) {
            var words = treeWalker.currentNode.nodeValue.split(/\s+|[\u002D\u058A\u05BE\u1400\u1806\u2010-\u2015\u2053\u207B\u208B\u2212\u2E17\u2E1A\u2E3A-\u301C\u3030\u30A0\uFE31\uFE32\uFE58\uFE63\uFF0D]/g);
            for (var i = 0; i < words.length; i++) {
                var word = words[i].trim();
                if (word.length == 0) {
                    continue; //no word
                }
                wordOffset++;
            }
        }
        return wordOffset;
    };


    var hasChildWithUri = function (element) {
        var test = element.querySelector("[uri]");
        return test != null;
    };

    /**
     Whether the `range` contains the `node`.
     */
    var rangeContainsNode = function (range, node) {
        var nodeRange = document.createRange();
        nodeRange.selectNodeContents(node);
        return (range.compareBoundaryPoints(Range.END_TO_START, nodeRange) == -1 &&
            range.compareBoundaryPoints(Range.START_TO_END, nodeRange) == 1);
    };

    var getAllElementsInRange = function (startElement, endElement, range) {
        var uriElementsRange = document.createRange();
        uriElementsRange.startContainer = startElement;
        uriElementsRange.endContainer = endElement;
        var commonAncestor = uriElementsRange.commonAncestorContainer;

        var filter = function (node) {
            if (hasChildWithUri(node, false)) {
                return NodeFilter.FILTER_SKIP;
            }

            if (hasUri(node) && rangeContainsNode(range, node)) {
                return NodeFilter.FILTER_ACCEPT;
            }

            return  NodeFilter.FILTER_SKIP;
        };

        var treeWalker = document.createTreeWalker(commonAncestor, NodeFilter.SHOW_ELEMENT, filter, false);

        var elements = [];
        while (treeWalker.nextNode()) {
            elements.push(treeWalker.currentNode);
        }

        if (elements.length == 0) {
            elements.push(startElement);
            if (startElement != endElement) {
                elements.push(endElement);
            }
        }
        return elements;
    };

    var getRectsForRangeInNode = function (node, range) {
        var tempRange = document.createRange();
        tempRange.selectNodeContents(node);

        if (range.startContainer == node) {
            tempRange.setStart(node, range.startOffset);
        }
        if (range.endContainer == node) {
            tempRange.setEnd(node, range.endOffset);
        }

        return tempRange.getClientRects();
    };

    return self;
}();