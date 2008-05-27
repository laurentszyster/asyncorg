$$.extend('hide', function (el) { 
    CSS.add(el, ['hide']); 
});
$$.extend('show', function (el) { 
    CSS.remove(el, ['hide']); 
});
$$.extend('enable', function (el) { 
    el.disabled = false;
    CSS.remove(el.parentNode, ['disabled']); 
});
$$.extend('disable', function (el) { 
    el.disabled = true;
    CSS.add(el.parentNode, ['disabled']); 
});
$$.extend('clearValue', function (el) { 
    el.value=''; 
});
$$.extend('fitAreaToText', function (el) {
    var lines = el.value.split('\n').length;
    el.parentNode.style.height = (1.5*lines) + "em";
    el.rows = lines;
});

var UI = {
    bound2: {},
    onclick: {}
};

UI.bindHoverActive = function (selector, el) {
    $$(selector, el)
        .bind('mouseover', function (event) {
            CSS.add (this, ['hover']);
        })
        .bind('mouseout', function (event) {
            CSS.remove (this, ['hover']);
        })
        .bind('focus', function (event) {
            CSS.add (this, ['hover']);
        })
        .bind('blur', function (event) {
            CSS.remove (this, ['hover']);
        })
        .bind('mousedown', function (event) {
            CSS.add (this, ['active']);
        })
        .bind('mouseup', function (event) {
            CSS.remove (this, ['active']);
        })
        ;
};

UI.ok = function (response) {
    
}

UI.error = function () {
    
}

UI.bind2jsonr = function (method, url, id, model) {
    JSONR.models[url] = model;
    JSONR.update($(id), url);
    var onclick = function () {
        method ('url', JSONR.values[url], UI.onclick[url] || UI.ok);
    }
}

/**
 * UI.bound2jsonr (JSON.GET, '/login', 'id');
 */
UI.bound2jsonr = function (method, url, id) {
    UI.bound2jsonr[url] = id;
    JSON.GET(method, url, null, function ok (response) {
        UI.bind2jsonr(method, url, id, response);
        });
}
