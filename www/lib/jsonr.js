var JSONR = (function(){

function _type (model, extensions) {
    if (!extensions) extensions = JSONR.extensions;
    switch (typeof model) {
    case "boolean": 
        return "boolean";
    case "string": 
        if (model=="") {
            return "string";
        } else if (extensions[model]) {
            return model;
        } else {
            return "pcre";
        }
    case "number": 
        if (model === 0) {
            return "number";
        }
        var s = model.toString();
        if (model == parseInt(s)) {
            return "integer";  
        } else {
            return "decimal";
        }
    case "object":
        if (model === null) {
            return "null";
        } else {
            var L = model.length;
            if (typeof L == 'undefined') {
                var keys = [];
                for (var k in model) {
                    if (!(typeof model[k]=="function")) {
                        keys.push(k);
                    }
                }
                if (keys.length > 1) {
                    return "namespace";
                } else if (keys.length > 0) {
                    return "dictionary";
                } else {
                    throw "{} is not a regular JSON expression";
                }
            } else if (L == 1) {
                return "collection";
            } else if (L > 1) {
                return "relation"
            } else {
                throw "[] is not a regular JSON expression";
            }
        }
    }
}

function _cast (type, value) {
    switch (type) {
    case 'null': 
        return value;
    case 'boolean':
        return value === true;
    case 'string': 
    case 'pcre': 
        if (value === null || typeof value == 'undefined') {
            return '';
        } else {
            return value.toString();
        }
    case 'number': 
    case 'integer': 
    case 'decimal':
        if (typeof value == 'number') {
            return value;
        } else if (typeof value == 'string') {
            try {
                return parseFloat(value.toString());
            } catch (e1) {
                return 0;
            }
        } else {
            return 0;
        }
    case 'collection': 
    case 'relation': 
        if (
            value !== null && 
            typeof value == 'object' &&
            typeof value.length != 'undefined'
            ) {
            return value;
        } else {
            return [];
        }
    case 'dictionary': 
    case 'namespace': 
        if (
            value !== null &&
            typeof value == 'object' &&
            typeof value.length == 'undefined'
            ) {
            return value;
        } else {
            return {};
        }
    }
    return value.toString();
}


function _get (path, value) {
    for (var i=0, L=path.length; i<L && value; i++) {
        value = value[path[i]];
    }
    return value;
}

function _set (path, value) {
    var v=JSONR.values, L=path.length - 1;
    for (var i=0; i<L; i++) {
        v = v[path[i]];
    }
    v[path[L]] = value;
    return value;
}

var JSONR = {
    models: {},
    values: {},
    errors: {},
    extensions: {},
    templates: {},
    type: _type,
    cast: _cast
};

JSONR.control = function (name, model, value, errors) {
    JSONR.models[name] = (typeof model == 'undefined')  ? null : model ;
    JSONR.values[name] = (typeof value == 'undefined')  ? null : value ;
    JSONR.errors[name] = (typeof errors == 'undefined') ? {}   : errors ;
};

JSONR.remove = function (name) {
    delete JSONR.models[name];
    delete JSONR.values[name];
    delete JSONR.errors[name];
};

JSONR.update = function (el, name) {
    JSONR.bind(HTML.update(el, JSONR.HTML([], [name], [name]).join('')));
}

JSONR.HTML = function (sb, modelPath, valuePath) {
    var model = _get(modelPath, JSONR.models);
    var value = _set(valuePath, _cast(
        _type(model), _get(valuePath, JSONR.values)
        ));
    var view = new JSONR.View(modelPath, valuePath);
    return view.buffer(sb, view.getName(), model, value);
};

JSONR.bind =  function (el) {
    $$('input, textarea, button', el)
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

JSONR.focus = function (path) {
    var value = _get(path, JSONR.values);
    if (value != null && typeof value == 'object') {
        if (typeof value.length == 'undefined') {
            if (value) {
                for (var k in value) {
                    if (typeof value[k] != 'function') {
                        path.push(k);
                        return JSONR.focus(path);
                    }
                }
            }
        } else if (value.length > 0) {
            path.push(0);
            return JSONR.focus(path);
        }
    } 
    var el = $(JSON.encode(path));
    if (el) {
        if (/(input)|(textarea)|(button)/.test (el.nodeName.toLowerCase())) {
            el.focus();
        } else {
            $$('button', el).selected[0].focus();
        }
    }
};

function _attr (el, name) {
    return el.getAttribute(name) || el[name];
}

function _bubble (event) {
    if (!event) { // IE
        event = window.event;
        window.event.cancelBubble = false;
        return true;
    } 
}

JSONR.add = function (el, event) {
    var view = new JSONR.View(
        JSON.decode(_attr(el, 'regular')),
        JSON.decode(el.id)
        );
    var name = view.getName();
    view.modelPath.push(0);
    var model = _get(view.modelPath, JSONR.models);
    view.valuePath.push(_set(view.valuePath, _cast(
        "collection", _get(view.valuePath, JSONR.values)
        )).length);
    var value = _set(view.valuePath, _cast(
        _type(model), _get(view.valuePath, JSONR.values)
        ));
    var sb = [];
    view.buffer(sb, name + 'N', model, value);
    JSONR.bind(HTML.insert(el, sb.join(''), 'beforeBegin'));
    setTimeout('JSONR.focus(' + JSON.encode(view.valuePath) + ')', 0);
    return _bubble (event);
}

JSONR.open = function (el, event) {
    var view = new JSONR.View(
        JSON.decode(_attr(el, 'regular')),
        JSON.decode(el.id)
        );
    var name = view.getName();
    var model = _get(view.modelPath, JSONR.models);
    var type = _type(model);
    var value = _set(view.valuePath, _cast(
        type, _get(view.valuePath, JSONR.values)
        ));
    var sb = [];
    view.types[type].apply(view, [sb, name, model, value]);
    JSONR.bind(HTML.replace(el, sb.join('')));
    setTimeout('JSONR.focus(' + JSON.encode(view.valuePath) + ')', 0);
    return _bubble (event);
}

JSONR.put = function (el, event) {
    var k = '~new';
    var view = new JSONR.View(
        JSON.decode(_attr(el, 'regular')),
        JSON.decode(el.id)
        );
    var model = _get(view.modelPath, JSONR.models);
    var kModel, vModel;
    for (kModel in model) {
        vModel = model[kModel];
        break;
    }
    var kModel, vModel;
    for (kModel in model) {
        vModel = model[kModel];
        break;
    }
    var value = _get(view.valuePath, JSONR.values);
    if (value === null || typeof value == 'undefined') {
        var vp = view.valuePath;
        value = _set(vp, _cast('dictionary', value));
        var fun = function () {};
        value[null] = fun;
        fun['key'] = _cast(_type(kModel), null); 
    } else if (typeof value[k] != 'undefined') {
        return;
    }
    value[k] = null;
    var name = view.getName();
    var sb = [];
    sb.push('<div class="pair"><div class="key">');
    view.valuePath.push(null);
    view.valuePath.push('key');
    view.buffer(sb, name + 'K', kModel, k)
    view.valuePath.pop();
    view.valuePath.pop();
    sb.push('</div><div class="value" key="');
    sb.push(HTML.cdata(k));
    sb.push('">');
    view.valuePath.push(k);
    view.modelPath.push(kModel);
    view.buffer(sb, name + 'V', vModel, null);
    sb.push('</div></div>');
    JSONR.bind(HTML.insert(el, sb.join(''), 'beforeBegin'));
    setTimeout('JSONR.focus(' + JSON.encode(view.valuePath) + ')', 0);
    return _bubble (event);
}

JSONR.validate = function () {
    var modelPath = JSON.decode(_attr(arguments[0], 'regular'));
    _validate[_type(_get(modelPath,  JSONR.models))].apply(
        modelPath, arguments
        );
};

JSONR.onInvalid = function (el, path, invalid) {
    CSS.add(el, ['error']); 
    var errors = JSONR.errors[path[0]];
    if (!errors) {
        errors = {};
        JSONR.errors[path[0]] = errors;
        setTimeout('JSONR.focus(' + JSON.encode(path) + ')', 0);
    }
    errors[el.id] = true;
}

JSONR.onValid = function (el, path) {
    CSS.remove(el, ['error']);
    var errors = JSONR.errors[path[0]];
    if (errors[el.id] === true) {
        delete errors[el.id];
    }
    //if (path.length > 0) {
    //    path.pop();
    //    var el = $(JSON.encode(path));
    //    if (el) {
    //        JSONR.onValid (el, path);
    //    }
    //}
};

function _test (el, value) {
    var path = JSON.decode(el.id);
    _set(path, value);
    var s = value.toString();
    if (el.value != s) {
        el.value = s; 
        return _error(el, value); 
    } else {
        return _success(el);
    }
}

function _error (el, value) {
    setTimeout('JSONR.onInvalid($(' 
        + JSON.encode(el.id) + '), ' 
        + el.id + ', ' 
        + JSON.encode(value) + ')', 0);
    return false;
}

function _success (el) {
    setTimeout('JSONR.onValid($(' 
        + JSON.encode(el.id) + '), ' 
        + el.id + ')', 0);
    return true;
}

function _range (value, low, high) {
    if (typeof low != 'undefined' && value < low) {
        return low;
    } else if (typeof high != 'undefined' && value > high) {
        return high;
    }
    return value;
}

_validate = {
    'null': function (el, event) {
        var v; 
        try {
            v = JSON.decode(el.value);
        } catch (e) {
            v = el.value;
        }
        _set(JSON.decode(el.id), v);
        return _success(el);
    },
    'boolean': function (el, event) {
        _set(JSON.decode(el.id), el.checked);
        return _success(el);
    },
    'string': function (el, event) {
        _set(JSON.decode(el.id), el.value);
        return _success(el);
    },
    'number': function (el, event, low, high) {
        return _test(el, _range(parseFloat(el.value), low, high));
    },
    'integer': function (el, event, low, high) {
        return _test(el, _range(parseInt(el.value), low, high));
    },
    'decimal': function (el, event, low, high, pow) {
        return _test(el, Math.round(
            _range(parseFloat(el.value), low, high)*pow
            )/pow);
    },
    'pcre': function (el, event, regular) {
        if (el.value.match(regular) === null) 
            return _error(el, el.value);
        else {
            var path = JSON.decode(el.id);
            _set(path, el.value);
            return _success(el);
        }
    },
    'dictionary': function () {
        var mp = this;
        var model = _get(mp, JSONR.models);
        var kModel, vModel;
        for (kModel in model) {
            vModel = model[kModel];
        }
        if (!_validate[_type(kModel)].apply(this, arguments)) {
            return false;
        };
        var el = arguments[0];
        var vp = JSON.decode(el.id);
        var newKey = _get(vp, JSONR.values);
        vp.pop();
        vp.pop();
        var value = _get(vp, JSONR.values);
        while (!/(^|.* )(key)( .*|$)/.test(el.className)) {
            el = el.parentNode;
        }
        el = HTML.next(el);
        var oldKey = _attr(el, 'key');
        if (newKey == oldKey) {
            return true;
        }
        value[newKey] = value[oldKey];
        delete value[oldKey];
        var view = new JSONR.View(mp, vp);
        var name = view.getName();
        vp.push(newKey);
        mp.push(kModel);
        var sb = [];
        sb.push('<div class="value" key="');
        sb.push(HTML.cdata(newKey));
        sb.push('">');
        view.buffer(sb, name + 'V', vModel, value[newKey]);
        sb.push('</div>');
        HTML.replace(el, sb.join(''));
        setTimeout('JSONR.focus(' + JSON.encode(vp) + ')', 0);
    }
}

JSONR.View = {
    initialize: function (modelPath, valuePath) {
        this.modelPath = modelPath || [];
        this.valuePath = valuePath || [];
    },
    getName: function (path) {
        var suffix = '';
        var key;
        var path = (path || this.modelPath.slice(0));
        while (path.length > 0) {
            key = path.pop();
            switch (_type(_get(path, JSONR.models))) {
            case 'dictionary':
                if (key === null) {
                    suffix = 'K' + suffix;
                } else {
                    suffix = 'V' + suffix;
                }
                break;
            case 'collection':
                suffix = 'N' + suffix;
                break;
            case 'relation':
                suffix = key.toString() + suffix;
                break;
            default:
                return key + suffix;
            }
        }
    },
    types: {},
    templates: {
        'namespace': ['<div class="column span-15 last">', '</div>'],
        'dictionary': ['<div class="column span-15 last">', '</div>'],
        'collection': ['<div class="column span-15 last">', '</div>'],
        'relation': ['<div class="field">', '</div>'],
        'pcre': ['<div class="column span-5">', '</div>'],
        'string': ['<div class="column span-15 last">', '</div>'],
        'number': ['<div class="column span-2">', '</div>'],
        'integer': ['<div class="column span-2">', '</div>'],
        'decimal': ['<div class="column span-2">', '</div>'],
        'boolean': ['<div class="column span-1">', '</div>'],
        'null': ['<div class="column span-15">', '</div>']
    },
    template: function (name, type) {
        var L = this.modelPath.length;
        if (L > 1) {
            switch (_type(_get(this.modelPath.slice(0, L-1), JSONR.models))) {
                case 'relation':
                case 'collection':
                case 'dictionary':
                    return this.templates[type];
                default:
                    t = this.templates[type].slice(0);
                    t[0] = '<div class="field">' + name + ' ' + t[0];
                    t[1] = t[1] + '</div>';
                    return t;
            }
        } else {
            return ['', ''];
        }
    },
    buffer: function (sb, name, model, value) {
        var type = _type(model);
        var template = JSONR.templates[name] || this.template(name, type);
        sb.push(template[0]);
        (this.types[name] || this.types[type]).apply(this, arguments);
        sb.push(template[1]);
        return sb;
    },
    bufferAttrs: function (sb, name) {
        var id = JSON.encode(this.valuePath);
        sb.push(HTML.cdata(name));
        sb.push('" id="');
        sb.push(HTML.cdata(id));
        sb.push('" regular="');
        sb.push(HTML.cdata(JSON.encode(this.modelPath)));
        return id;
    },
    bufferNumber: function (sb, name, model, value) {
        if (value == null) {
            _set(this.valuePath, 0);
            sb.push('<input type="text" class="number" value="0" name="');
        } else {
            sb.push('<input type="text" class="number" value="');
            sb.push(value.toString());
            sb.push('" name="');
        }
        this.bufferAttrs(sb, name);
    },
    order: function (model) {
        var names = {
            'simple': [],
            'relation': [],
            'string': [],
            'namespace': [],
            'dictionary': [],
            'collection': []
        }
        for (var k in model) {
            if (typeof model[k] != 'function') {
                (names[_type(model[k])]||names['simple']).push(k);
            }
        };
        var r = [];
        for (var n in names) {
            r = r.concat(names[n]);
        }
        return r;
    }
};

JSONR.View.types['namespace'] = function (sb, name, model, value) {
    if (value) {
        value = _set(this.valuePath, _cast('namespace', value));
        var names = this.order(model);
        for (var i=0, k; k=names[i]; i++) {
            this.modelPath.push(k);
            this.valuePath.push(k);
            this.buffer(sb, k, model[k], value[k]);
            this.modelPath.pop();
            this.valuePath.pop();
        }
    } else {
        // value = _set(this.valuePath, null);
        sb.push('<div class="namespace column span-2 ');
        var id = this.bufferAttrs(sb, name);
        sb.push(
            '"><button onclick="return JSONR.open(this.parentNode, event)">'
            + 'open'
            + '</button></div>'
            );
    }
};

JSONR.View.types['dictionary'] = function (sb, name, model, value) {
    var k, kModel, vModel;
    for (k in model) {
        if (typeof model[k] != 'function') {
            kModel = k;
            vModel = model[k];
            break;
        }
    }
    if (value !== null && typeof value != 'undefined') {
        var vp = this.valuePath;
        value = _set(vp, _cast('dictionary', value));
        var fun = function () {};
        value[null] = fun;
        fun['key'] = _cast(_type(kModel), null); 
        var kp = vp.slice(0);
        kp.push(null);
        kp.push('key');
        for (k in value) {
            if (typeof value[k] != 'function') {
                sb.push('<div class="pair"><div class="key">');
                this.valuePath = kp;
                // this.modelPath.push(kModel);
                this.buffer(sb, name + 'K', kModel, k)
                // this.modelPath.pop();
                this.valuePath = vp;
                sb.push('</div><div class="value" key="');
                sb.push(HTML.cdata(k));
                sb.push('">');
                this.valuePath.push(k);
                this.modelPath.push(kModel);
                this.buffer(sb, name + 'V', vModel, value[k]);
                this.modelPath.pop();
                this.valuePath.pop();
                sb.push('</div></div>');
            }
        }
    }
    sb.push('<div class="dictionary column span-2 ');
    var id = this.bufferAttrs(sb, name);
    sb.push(
        '"><button onclick="return JSONR.put(this.parentNode, event)"'
        + '>put</button></div>'
        );
};

JSONR.View.types['collection'] = function (sb, name, model, value) {
    if (value) {
        value = _set(this.valuePath, _cast('collection', value));
        var itemName = name + 'N';
        this.modelPath.push(0);
        for (var i=0, L=value.length; i<L; i++) {
            this.valuePath.push(i);
            this.buffer(sb, itemName, model[0], value[i]);
            this.valuePath.pop();
        }
        this.modelPath.pop();
    }
    sb.push('<div class="collection column span-2 ');
    var id = this.bufferAttrs(sb, name);
    sb.push(
        '"><button onclick="return JSONR.add(this.parentNode, event)"'
        + '>add</button></div>'
        );
};

JSONR.View.types['relation'] = function (sb, name, model, value) {
    value = _set(this.valuePath, _cast('relation', value));
    for (var i=0, L=model.length; i<L; i++) {
        this.modelPath.push(i);
        this.valuePath.push(i);
        this.buffer(sb, name + i.toString(), model[i], value[i]);
        this.valuePath.pop();
        this.modelPath.pop();
    }
};

JSONR.View.types['pcre'] = function (sb, name, model, value) {
    value = _set(this.valuePath, _cast('pcre', value));
    sb.push('<input type="text" name="');
    var id = this.bufferAttrs(sb, name);
    sb.push('" onblur="return JSONR.validate(this, event, new RegExp(');
    sb.push(HTML.cdata(JSON.encode(model)));
    sb.push('));" value="');
    sb.push(HTML.cdata(value));
    if (value.match(new RegExp(model)) == null) {
        sb.push('" class="pcre error" />');
        JSONR.errors[this.valuePath[0]][id] = true;
    } else {
        sb.push('" class="pcre" />');
    }
};

JSONR.View.types['number'] = function (sb, name, model, value) {
    value = _set(this.valuePath, _cast('number', value));
    this.bufferNumber (sb, name, model, value);
    sb.push('" onblur="return JSONR.validate(this, event)" />');
}
    
JSONR.View.types['integer'] = function (sb, name, model, value) {
    value = _set(this.valuePath, _cast('integer', value));
    this.bufferNumber (sb, name, model, value);
    if (model < 0) {
        sb.push('" onblur="return JSONR.validate(this, event, ');
        sb.push(model);
        sb.push(', ');
        sb.push(-model);
    } else {
        sb.push('" onblur="return JSONR.validate(this, event, 0, ');
        sb.push(model);
    }
    sb.push(')" />');
};
    
JSONR.View.types['decimal'] = function (sb, name, model, value) {
    value = _set(this.valuePath, _cast('decimal', value));
    var pow = 10, decimals = model.toString().split('.')[1].length;
    for (var i=1; i<decimals; i++) {
        pow = pow*10;
    }
    var precision = (1/pow);
    this.bufferNumber (sb, name, model, value);
    if (model < 0) {
        sb.push('" onblur="return JSONR.validate(this, event, ');
        sb.push(model+precision);
        sb.push(', ');
        sb.push(-model-precision);
    } else {
        sb.push('" onblur="return JSONR.validate(this, event, 0, ');
        sb.push(model-precision);
    }
    sb.push(', ');
    sb.push(pow);
    sb.push(')" />');
};    

JSONR.View.types['string'] = function (sb, name, model, value) {
    value = _set(this.valuePath, _cast('string', value));
    sb.push('<textarea class="string" rows="4" name="');
    this.bufferAttrs(sb, name);
    sb.push('" onblur="return JSONR.validate(this, event)" >');
    if (value == null) {
        _set(this.valuePath, '');
    } else {
        sb.push(HTML.cdata(value));
    } 
    sb.push('</textarea>');
};

JSONR.View.types['boolean'] = function (sb, name, model, value) {
    value = _set(this.valuePath, _cast('boolean', value));
    sb.push('<input type="checkbox" class="boolean" name="');
    this.bufferAttrs(sb, name);
    sb.push('" onclick="return JSONR.validate(this, event)" ');
    if (value === true) {
        sb.push('checked />');
        _set(this.valuePath, true);
    } else {
        sb.push('/>');
        _set(this.valuePath, false);
    }
};

JSONR.View.types['null'] = function (sb, name, model, value) {
    sb.push('<textarea class="null" rows="4" name="');
    this.bufferAttrs(sb, name);
    if (value == null)
        sb.push(
            '" onblur="return JSONR.validate(this, event)"'
            + ' >null</textarea>'
            );
    else {
        sb.push('" onblur="return JSONR.validate(this, event)" >');
        sb.push(HTML.cdata(JSON.pprint([], value).join('')));
        sb.push("</textarea>");
    }
};

JSONR.View = Protocols(JSONR.View);

_infer = function (name, model, types, patterns, path) {
    if (path.length > 0) {
        var fqn = path.join('') + name;
        var encoded = JSON.encode(model);
        if (typeof types[name] == 'undefined') {
            types[name] = [[fqn, encoded]];
        } else {
            types[name].push([fqn, encoded]);
        }
        if (typeof patterns[encoded] == 'undefined') {
            patterns[encoded] = [fqn];
        } else {
            patterns[encoded].push(fqn);
        }
    }
    switch (_type(model)) {
    case 'namespace':
        path.push(name + '.');
        for (var k in model) {
            _infer(k, model[k], types, patterns, path);
        }
        path.pop();
        break;
    case 'dictionary':
        for (var k in model) {
            _infer(name, model[k], types, patterns, path);
        }
        break;
    case 'collection':
        _infer(name + '[0]', model[0], types, patterns, path);
        break;
    case 'relation':
        for (var i=0, L=model.length, n; i<L; i++) {
            _infer(name + '[' + i + ']', model[i], types, patterns, path);
        }
        break;
    }
}

JSONR.infer = function (models) {
    var types = {};
    var patterns = {};
    if (typeof models == 'undefined') {
        models = JSONR.models;
    }
    for (var k in models) {
        _infer(k, models[k], types, patterns, []);
    }
    return [types, patterns];
}

return JSONR;

})();

/*
 * Synopsis
 *
 * Add a new model and value to the ones controlled by JSONR:
 * 
 *     JSONR.control('test', {".+$": [100]}, {});
 * 
 * Update the document with a regular input view:
 * 
 *     JSONR.update($('view'), 'test') // apply ...
 * 
 */
