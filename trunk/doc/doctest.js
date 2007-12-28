var javadoc = {
    title: 'asyncorg',
    packages: {},
    classes: {},
    index: {},
    current: null,
    history: []
    };
javadoc.packageName = function (fqn) {
    var match = /^(.+?)[.][A-Z].+$/.exec(fqn);
    if (match == null)
        return fqn;

    return match[1];
};
javadoc.localName = function (fqn) {
    var match = /[A-Z].+$/.exec(fqn);
    if (match == null)
        return fqn;

    return match[0];
};
javadoc.linkHTML = function (sb, fqn, text) {
    sb.push('<a href="#');
    sb.push(HTML.cdata(fqn));
    sb.push('" onclick="javadoc.get(');
    sb.push(HTML.cdata(JSON.encode(fqn)));
    sb.push(');">');
    sb.push(HTML.cdata(text));
    sb.push('</a>');
    return sb;
};
javadoc.indexHTML = function (sb) {
    var path, interfaces, implementations, exceptions;
    sb.push('<dl>');
    for (var p in javadoc.packages) {
        path = p + '.';
        sb.push('<di><dt>');
        javadoc.linkHTML(sb, p, p);
        sb.push('</dt><dd>');
        types = javadoc.packages[p];
        if (types.length > 0) {
            for (var i=0, k; k=types[i]; i++) {
                javadoc.linkHTML(sb, path + k, k);
                sb.push(', ');
            }
            sb.pop();
        }
        sb.push('</dd></di>');
    }
    sb.push('</dl>');
    return sb;
};
javadoc.classHTML = function (fqn, sb, properties) {
    sb.push('<dl>');
    var path = fqn + '.';
    var contained = properties["contains"];
    if (contained) {
        sb.push('<di class="types"><dt>Contains</dt><dd>');
        for (var i=0, L=contained.length; i<L; i++) {
            javadoc.linkHTML(
                sb, contained[i], /[^.]+$/.exec(contained[i])[0]
                );
            sb.push(', ');
        }
        sb.pop();
        sb.push('</dd></di>');
    }
    var extended = properties["extends"];
    if (extended) {
        sb.push('<di class="extends"><dt>Extends</dt><dd>');
        if (javadoc.classes[extended]) {
            javadoc.linkHTML(sb, extended, javadoc.localName(extended));
        } else {
            sb.push(extended);
        }
        sb.push('</dd></di>');
    }
    var interfaceName, interfaces = properties["implements"];
    if (interfaces) {
        sb.push(' <di class="implements"><dt>Implements</dt><dd>');
        for (var i=0, L=interfaces.length; i<L; i++) {
            interfaceName = interfaces[i];
            if (javadoc.classes[interfaceName])
                javadoc.linkHTML(sb, interfaceName, javadoc.localName(
                    interfaceName
                    ));
            else
                sb.push(interfaceName);
            sb.push(', ');
        }
        sb.pop();
        sb.push('</dd></di>');
    }
    var constants = properties["constants"];
    if (constants) {
        sb.push('<di class="constants"><dt>Constants</dt><dd>');
        for (var i=0, k; k=constants[i]; i++) {
            javadoc.linkHTML(sb, path + k, k);
            sb.push(', ');
        }
        sb.pop();
        sb.push('</dd></di>');
    }
    var members = properties["members"];
    if (members) {
        sb.push('<di class="members"><dt>Members</dt><dd>');
        for (var i=0, k; k=members[i]; i++) {
            javadoc.linkHTML(sb, path + k, k);
            sb.push(', ');
        }
        sb.pop();
        sb.push('</dd></di>');
    }
    var methods = properties["methods"];
    if (methods) {
        sb.push('<di class="methods"><dt>Methods</dt><dd>');
        for (var i=0, k; k=methods[i]; i++) {
            javadoc.linkHTML(sb, path + k, k);
            sb.push(', ');
        }
        sb.pop();
        sb.push('</dd></di>');
    }
    var functions = properties["functions"];
    if (functions) {
        sb.push('<di class="functions"><dt>Functions</dt><dd>');
        for (var i=0, k; k=functions[i]; i++) {
            javadoc.linkHTML(sb, path + k, k);
            sb.push(', ');
        }
        sb.pop();
        sb.push('</dd></di>');
    }
    sb.push('</dl>');
    return sb;
};
javadoc.objectLink = function (element) {
    var fqn, text = HTML.text(element);
    var fqns = javadoc.index[text];
    if (fqns && fqns.length == 1)
        fqn = fqns[0];
    else
        return;
        
    var href = element.getAttribute('href');
    if (!href) href = fqn;
    HTML.update(element, 
        '<a href="#' 
        + HTML.cdata(href)
        + '" onclick="javadoc.get('
        + HTML.cdata(JSON.encode(href))
        + ')" >' 
        + HTML.cdata(text) 
        + '</a>'
        );
};
javadoc.load = function (fqn) {
    if (fqn == javadoc.current) 
        return false;
        
    HTTP.request(
        'GET', "fragments/" + fqn, {}, null, javadoc.loaded(fqn)
        );
    return true;
};
javadoc.loaded = function (fqn) {
    return function (request) {
        var javadocObject = $('javadocObject');
        HTML.update(javadocObject, request.responseText);
        if (fqn == 'index') {
            javadoc.current = null;
            HTML.update($('javadocPackageName'), javadoc.title);
            HTML.update($('javadocIndex'), javadoc.indexHTML([]).join(''));
        } else {
            var h2 = $$('h2')[0];
            var packageName = javadoc.packageName(fqn);
            var typeName = packageName + '.' + HTML.text(h2);
            HTML.update($('javadocPackageName'), packageName);
            javadoc.historyUpdate(fqn);
            var properties = javadoc.classes[typeName];
            if (properties) {
                HTML.update($('javadocIndex'), javadoc.classHTML(
                    typeName, [], properties
                    ).join(''));
            }
            javadoc.objectLink(h2);
        }
        map(javadoc.objectLink, $$('code', javadocObject));
    };
};
javadoc.historyIndex = function (fqn) {
    if (javadoc.history.indexOf)
        return javadoc.history.indexOf(fqn);

    for (var i=0, l=javadoc.history, L=l.length; i<L; i++)
        if (l[i]==fqn) return i;
        
    return -1;
}
javadoc.historyUpdate = function (fqn) {
    var last = javadoc.current;
    if (last != null && javadoc.historyIndex(last) == -1) {
        javadoc.history.push(last);
        // var sb = [];
        // sb.push('<div>');
        // javadoc.linkHTML(sb, last, javadoc.localName(last));
        // sb.push('</div>');
        // HTML.insert( $('javadocHistory'), sb.join(''), 'afterBegin');
    }
    javadoc.current = fqn;
};
javadoc.historyClear = function () {
    javadoc.history = [];
    HTML.update($('javadocFound'), '');
    HTML.update($('javadocHistory'), '');
};
javadoc.get = function (fqn) {
    return (fqn && javadoc.load(fqn));
};
javadoc.search = function (name) {
    var fqns = javadoc.index[name];
    if (!fqns)
        return;
        
    if (fqns.length == 1)
        javadoc.get(fqns[0])
    else {
        var sb = [];
        for (var i=0, L=fqns.length; i<L; i++) {
            javadoc.linkHTML(sb, fqns[i], javadoc.localName(fqns[i]));
            sb.push(', ');
        }
        sb.pop();
        sb.push('<hr />');
        HTML.update($('javadocFound'), sb.join(''));
    }
}
javadoc.goHome = function () {
    HTTP.request('GET', 'index', {}, null, javadoc.loaded('index'));
    return true;
};
HTML.onload.push (function () {
    var fqn = window.location.toString().split('#')[1];
    if (fqn) 
        javadoc.get(fqn);
    else
        javadoc.goHome();
});
