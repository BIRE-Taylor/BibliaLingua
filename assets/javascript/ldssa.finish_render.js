// Namespace
var ldssa = window.ldssa || {};
ldssa.finish_render = {};

//noinspection JSUnusedLocalSymbols
ldssa.finish_render.transitionListener = function(e) {
    //noinspection JSUnresolvedVariable
    if (typeof window.mainInterface != "undefined") {
        //noinspection JSUnresolvedVariable,JSUnresolvedFunction // give a slight delay here
        setTimeout(function() { window.mainInterface.jsFinishedRendering(window.devicePixelRatio); }, 300);
    }
    ldssa.finish_render.resetTransitionElement();
};

ldssa.finish_render.loadTransitionElement = function() {
    //console.log("LOAD TRANSITION ELEMENT");
    document.getElementById('transitionElement').className = 'finish';
    document.getElementById('transitionElement').addEventListener('webkitTransitionEnd', ldssa.finish_render.transitionListener);
};

ldssa.finish_render.resetTransitionElement = function() {
    document.getElementById('transitionElement').removeEventListener('webkitTransitionEnd', ldssa.finish_render.transitionListener);
    document.getElementById('transitionElement').className = '';
};

window.onload = ldssa.finish_render.loadTransitionElement;