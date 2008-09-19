package org.async.protocols;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.async.simple.Fun;
import org.async.simple.Bytes;
import org.async.simple.Objects;

/**
 * A class that supports a practical subset of SOAP 1.1, just enough
 * to provide a <em>really</em> simple XML object notation that is forward
 * compatible with JSON web services.
 * 
 * To use only to offer some backward compatibility with legacy clients.
 * 
 */
public final class SOAP {

	private static final String _name = "name";
    private static final String _Request = "Request";
    private static final String _Response = "Response";
    private static final String _return = "return";
    
    protected static final Map _NS = Objects.update(
            new HashMap(), new Object[]{
                "http://www.w3.org/2001/XMLSchema", "xsd",
                "http://www.w3.org/2001/XMLSchema-instance", "xsi",
                "http://schemas.xmlsoap.org/soap/envelope/", "SOAP-ENV"
                }
            );
        
    protected static final String 
    _xsi_type = "http://www.w3.org/2001/XMLSchema-instance type";
    protected static final Map _xsd_types = Objects.update(
        new HashMap(), new Object[]{
            "xsd:byte", JSONR.INTEGER,
            "xsd:short", JSONR.INTEGER,
            "xsd:int", JSONR.INTEGER,
            "xsd:integer", JSONR.DECIMAL,
            "xsd:long", JSONR.INTEGER,
            "xsd:float", JSONR.DOUBLE,
            "xsd:double", JSONR.DOUBLE,
            "xsd:decimal", JSONR.DECIMAL,
            "xsd:boolean", JSONR.BOOLEAN
            // TODO: ? base64Binary, dateTime, hexBinary, QName ?
        });

    public static class Element extends XML.Element {
        public JSON.Object json = null;
        public Element (String name) {
            super(name);
        }
        public Element (String name, HashMap attributes) {
            super(name, attributes);
        }
        @Override
        public final XML.Element newElement (String name) {
            return new Element(name);
        }
        @Override
        public final void close(XML.Document document) throws XML.Error {
            XML.Element parent = this.parent;
            while (parent != null && !(parent instanceof SOAP.Element)) {
                parent = this.parent;
            }
            if (parent == null) {// root 
                ;
            } else if (children == null) { // leaf
                if (first != null) { // simple types
                    if (
                        attributes != null && 
                        attributes.containsKey(_xsi_type)
                        ) try {
                        JSONR.Type type = (JSONR.Type) _xsd_types.get(
                            attributes.get(_xsi_type)
                            );
                        if (type != null) {
                            update((Element) parent, type.eval(first));
                        } else {
                            update((Element) parent, first);
                        }
                    } catch (JSON.Error e) {
                        throw new XML.Error (e.getMessage());
                    } else {
                        update((Element) parent, first);
                    }
                } else if (attributes != null) { // complex type of attributes
                    update((Element) parent, attributes);
                }
            } else if (json != null) { // branch, complex type of elements
                if (_xsi_type.equals(_soapenc_Array)) { // Array
                    Iterator<String> names = json.keySet().iterator();
                    // move up and rename the first and only array expected
                    if (names.hasNext()) {
                        update((Element) parent, json.getArray(names.next(), null));
                    }
                } else { // Object
                    update((Element) parent, json);
                }
            }
        }
        public final void update(Element container, Object contained) {
            String tag = this.getLocalName();
            JSON.Object map = container.json; 
            if (map == null) {
                map = new JSON.Object();
                container.json = map;
                map.put(tag, contained);
            } else if (map.containsKey(tag)) {
                Object o = map.get(tag);
                if (o instanceof JSON.Array) {
                    ((JSON.Array) o).add(contained);
                } else {
                    JSON.Array list = new JSON.Array();
                    (list).add(contained);
                    map.put(tag, list);
                }
            } else {
                map.put(tag, contained);
            }
        }
    } 
    public static class Document extends XML.Document {
        public final XML.Element newElement(String name) {
            return new Element(name);
        }
        public final Object call (String action, Fun fun) 
        throws Throwable {
        	return fun.apply(
    			((Element) root).json.getObject(_Body).getObject(action)
    			);
        }
        
    }
    
    protected static final String 
        XSD_NS = "http://www.w3.org/2001/XMLSchema";
    protected static final String XSD_PREFIX = "xsd";
    
    protected static final String xsd_schema = XSD_NS + " schema";
    protected static final String xsd_element = XSD_NS + " element";
    protected static final String xsd_complexType = XSD_NS + " complexType";
    protected static final String xsd_complexContent = XSD_NS + " complexContent";
    protected static final String xsd_sequence = XSD_NS + " sequence";
    protected static final String xsd_all = XSD_NS + " all";
    protected static final String xsd_any = XSD_NS + " any";
    protected static final String xsd_minOccurs = XSD_NS + " minOccurs";
    protected static final String xsd_maxOccurs = XSD_NS + " maxOccurs";
    protected static final String xsd_restriction = XSD_NS + " restriction";
    protected static final String xsd_attribute = XSD_NS + " attribute";
    
    private static final String _soapenc_Array = "SOAP-ENC:Array";
    private static final String _soapenc_ArrayType = "SOAP-ENC:ArrayType";
    private static final String _Array = "Array";
    
    /**
     * Try to translate a regular JSONR expression in a named XSD schema,
     * throws an <code>Exception</code> for models that contain records
     * or dictionaries (both are not supported by XSD in a way that 
     * could fit less4j's simpler understanding of SOAP messages).
     * 
     * <h3>Synopsis</h3>
     * 
     * <pre>JSONR.Type model = JSONR.compile(JSON.object(new Object[]{
     *    "item", "",
     *    "quantity", new Integer(1),
     *    "description", ""
     *    "notification", Boolean.TRUE
     *    }));
     *XML.element type = xsd(model, "PurchaseOrder");</pre>
     * 
     * @param schema to update with complexTypes
     * @param model to translate from JSONR to XSD
     * @param name the name of the XSD type mapped
     * @return an <code>XML.Element</code>
     * @throws Exception
     */
    public static final XML.Element xsd (
        XML.Element schema, JSONR.Type model, String name
        ) 
    throws Exception {
        String type = model.name();
        if (type.equals("boolean")) {
            return new XML.Element(xsd_element, new String[]{
                _name, name, _type, "xsd:boolean"
                }, null, null); // string 
        } else if (type.equals("string")) {
            return new XML.Element(xsd_element, new String[]{
                _name, name, _type, "xsd:string"
                }, null, null); 
        } else if (type.equals("pcre")) {
            return new XML.Element(xsd_element, new String[]{
                _name, name, _type, "xsd:string"
                }, null, null); 
        } else if (type.startsWith("decimal")) {
            return new XML.Element(xsd_element, new String[]{
                _name, name, _type, "xsd:decimal"
                }, null, null);
        } else if (type.startsWith("integer")) {
            return new XML.Element(xsd_element, new String[]{
                _name, name, _type, "xsd:int"
                }, null, null); 
        } else if (type.startsWith("double")) {
            return new XML.Element(xsd_element, new String[]{
                _name, name, _type, "xsd:double"
                }, null, null);
        } else if (type.equals("dictionary")) {
            throw new Exception(
                "JSONR dictionaries are not supported by XSD"
                ); 
        } else if (type.equals("namespace")) {
            JSON.Object ns = (JSON.Object) model.json();
            XML.Element namespace = schema.addChild(
                xsd_complexType, new String[]{_name, name});
            XML.Element all = namespace.addChild(xsd_all);
            Object[] names = ns.keySet().toArray(); 
            Arrays.sort(names);
            String property;
            for (int i=0; i<names.length; i++) {
                property = (String) names[i];
                all.addChild(xsd(
                    schema, (JSONR.Type) ns.get(property), property
                    ));
            }
            return new XML.Element(
                xsd_element, new String[]{
                    _name, name, _type, "tns:" + name
                    }, null, null
                );
        } else if (type.equals("array")) {
            JSON.Array types = (JSON.Array) model.json();
            if (types.size() == 1) {
                xsd(schema, (JSONR.Type) types.get(0), name);
                XML.Element array = schema.addChild(
                    xsd_complexType, new String[]{_name, name + _Array}
                    );
                array.addChild(xsd_complexContent)
                    .addChild(xsd_restriction, new String[]{
                        "base", _soapenc_Array
                        })
                        .addChild(xsd_attribute, new String[]{
                            "ref", _soapenc_ArrayType,
                            wsdl_arrayType, "tns:" + name + "[]"
                            });
                return new XML.Element(
                    xsd_element, new String[]{
                        _name, name, _type, "tns:" + name + _Array
                        }, null, null
                    );
            } else {
                throw new Exception(
                    "JSONR relations are not supported for XSD"
                    );
            }
        } else if (type.equals("undefined")) {
            throw new Exception(
                "JSONR null not the intent of WSDL"
                );
        }
        return null;
    };

    protected static final String 
        SOAP_NS = "http://schemas.xmlsoap.org/wsdl/soap/";    
    protected static final String 
        SOAP_http = "http://schemas.xmlsoap.org/soap/http";
    private static final String 
        SOAP_encoding = "http://schemas.xmlsoap.org/soap/encoding/";
    protected static final String SOAP_PREFIX = "soap";
    protected static final String SOAPENC_PREFIX = "SOAP-ENC";
    protected static final String soap_binding = SOAP_NS + " binding";
    protected static final String soap_operation = SOAP_NS + " operation";
    protected static final String soap_body = SOAP_NS + " body";
    protected static final String soap_address = SOAP_NS + " address";
    
    protected static final String 
        WSDL_NS = "http://schemas.xmlsoap.org/wsdl/";
    protected static final String WSDL_PREFIX = "wsdl";
    
    protected static final String wsdl_types = WSDL_NS + " types";
    protected static final String wsdl_definitions = WSDL_NS + " definitions";
    protected static final String wsdl_message = WSDL_NS + " message";
    protected static final String wsdl_part = WSDL_NS + " part";
    protected static final String wsdl_portType = WSDL_NS + " portType";
    protected static final String wsdl_operation = WSDL_NS + " operation";
    protected static final String wsdl_input = WSDL_NS + " input";
    protected static final String wsdl_output = WSDL_NS + " output";
    protected static final String wsdl_fault = WSDL_NS + " fault";
    protected static final String wsdl_binding = WSDL_NS + " binding";
    protected static final String wsdl_service = WSDL_NS + " service";
    protected static final String wsdl_port = WSDL_NS + " port";
    protected static final String wsdl_arrayType = WSDL_NS + " arrayType";
        
    private static final String _targetNamespace = "targetNamespace";
    private static final String _tns = "tns";
    private static final String _tns_prefix = "tns:";
    private static final String _message = "message";
    private static final String _type = "type";
    private static final String _Port = "Port";
    private static final String _Binding = "Binding"; 
    private static final String _Service = "Service"; 
    private static final String _style = "style"; 
    private static final String _rpc = "rpc";
    private static final String _transport = "transport";
    private static final String _soapAction = "soapAction";
    private static final String _encodingStyle = "encodingStyle"; 
    private static final String _use = "use";
    private static final String _encoded = "encoded"; 
    private static final String _namespace = "namespace"; 
    private static final String _binding = "binding";
    private static final String _location = "location";

    /**
     * Produces a WSDL description of this SOAP function from a regular 
     * JSON expression of the form.
     * 
     * <pre>{"Request": ..., "Response": ...}</pre>
     * 
     * <h3>Synopsis</h3>
     * 
     * <p>To declare and enforce a different model than the default presented
     * above, override the <code>jsonInterface</code> method to return the
     * regular JSON string describing this function's input and output.</p>
     * 
     * <p>Note that all that port, binding and service whoopla is supported
     * for the simplest case: one method, one class, one namespace and one URL
     * for each services. This is functional WSDL, I'm not even trying to
     * make services look like objects with methods, that is not what they
     * are. Well designed web services are stateless functions.</p>
     * 
     * @param url this function's service address and its schema's namespace.
     * @param action the name of this SOAP action
     * @param model of RPC to map from JSONR to WSDL
     * @return an <code>XML.Document</code> tree ready to be serialized
     */
    public static final XML.Document wsdl (
        String url, String action, JSON.Object jsonr
        ) throws Exception {
        String urn = "urn:" + action;
        XML.Document doc = new XML.Document();
        doc.ns.put(urn, _tns);
        doc.ns.put(XSD_NS, XSD_PREFIX);
        doc.ns.put(SOAP_NS, SOAP_PREFIX);
        doc.ns.put(SOAP_encoding, SOAPENC_PREFIX);
        doc.ns.put(WSDL_NS, WSDL_PREFIX);
        doc.root = new XML.Element(wsdl_definitions, new String[]{
            _targetNamespace, urn
            }, null, null);
        // XSD schema <types> declaration
        XML.Element schema = doc.root
            .addChild(wsdl_types)
                .addChild(xsd_schema);
        // SOAP input <message>, RPC style
        XML.Element message = doc.root.addChild(
            wsdl_message, new String[]{_name, action + _Request}
            );
        JSONR.Type inputType = (JSONR.Type) jsonr.get(_Request);
        if (inputType.name().equals("namespace")) {
             JSON.Object types = (JSON.Object) inputType.json();
             Iterator names = types.keySet().iterator();
             String name;
             XML.Element element;
             while (names.hasNext()) {
                 name = (String) names.next();
                 element = xsd(schema, (JSONR.Type) types.get(name), name);
                 message.addChild(wsdl_part, new String[]{
                     _name, element.getAttribute(_name), 
                     _type, element.getAttribute(_type) 
                     });
             }
        } else {
            XML.Element input = xsd(
                schema, (JSONR.Type) inputType, action + _Request
                );
            message.addChild(wsdl_part, new String[]{
                _name, "arg0", 
                _type, input.getAttribute(_type)  
                });
        }
        // SOAP output <message>, RPC style
        XML.Element output = xsd(
            schema, 
            (JSONR.Type) jsonr.get(_Response), 
            action + _Response
            );
        message = doc.root.addChild(wsdl_message, new String[]{
            _name, action + _Response
            });
        message.addChild(wsdl_part, new String[]{
            _name, _return, 
            _type, output.getAttribute(_type)  
            });
        // one WSDL operation's <port> 
        XML.Element operation = doc.root
            .addChild(wsdl_portType, new String[]{_name, action + _Port})
            .addChild(wsdl_operation, new String[]{_name, action});
        operation.addChild(wsdl_input, new String[]{
            _name, action + _Request,
            _message, _tns_prefix + action + _Request
            });
        operation.addChild(wsdl_output, new String[]{
            _name, action + _Response,
            _message, _tns_prefix + action + _Response
            });
        // one WSDL operation's <binding>
        XML.Element binding = doc.root.addChild(wsdl_binding, new String[]{
            _name, action + _Binding, 
            _type, _tns_prefix + action + _Port
            });
        binding.addChild(soap_binding, new String[]{
            _style, _rpc, 
            _transport, SOAP_http    
            });
        operation = binding.addChild(wsdl_operation, new String[]{
            _name, action     
            });
        operation.addChild(soap_operation, new String[]{
            _soapAction, action
            });
        operation
            .addChild(wsdl_input, new String[]{
                _name, action + _Request    
                })
            .addChild(soap_body, new String[]{
                _namespace, urn,
                _encodingStyle, SOAP_encoding, 
                _use, _encoded
                });
        operation
            .addChild(wsdl_output, new String[]{
                _name, action + _Response 
            })
            .addChild(soap_body, new String[]{
                _namespace, urn,
                _encodingStyle, SOAP_encoding, 
                _use, _encoded
                });
        // one WSDL <service> port and address
        doc.root
            .addChild(wsdl_service, new String[]{
                _name, action + _Service
                })
                .addChild(wsdl_port, new String[]{
                    _binding, _tns_prefix + action + _Binding,
                    _name, action
                    })
                    .addChild(soap_address, new String[]{
                        _location, url
                        });
        return doc; // it looks like XML, does it not?
    }
    
    /**
     * Encode an object in a really simple XML notation that is
     * backward-compatible with SOAP 1.1 but supporting "only" the 
     * JSON types: Object, Array, String, Number, Boolean and null.
     * 
     * <h3>Synopsis</h3>
     * 
     * <pre>...</pre>
     * 
     */
    protected static final StringBuilder strb (
		StringBuilder sb, Object value, String name
        ) {
        if (value == null) {
            sb.append('<');
            sb.append(name); // place holder for array size
            sb.append("/>");
        } else if (value instanceof String) {
            sb.append('<');
            sb.append(name);
            sb.append(" xsi:type=\"xsd:string\"><![CDATA[");
            sb.append(value);
            sb.append("]]></");
            sb.append(name);
            sb.append('>');
        } else if (value instanceof Number) {
            if (value instanceof Integer) {
                sb.append('<');
                sb.append(name);
                sb.append(" xsi:type=\"xsd:int\">");
                sb.append(value);
                sb.append("</");
                sb.append(name);
                sb.append('>');
            } else if (value instanceof Double) {
                sb.append('<');
                sb.append(name);
                sb.append(" xsi:type=\"xsd:double\">");
                sb.append(value);
                sb.append("</");
                sb.append(name);
                sb.append('>');
            } else {
                sb.append('<');
                sb.append(name);
                sb.append(" xsi:type=\"xsd:decimal\">");
                sb.append(value);
                sb.append("</");
                sb.append(name);
                sb.append('>');
            }
        } else if (value instanceof Boolean) {
            sb.append('<');
            sb.append(name);
            sb.append(" xsi:type=\"xsd:boolean\">");
            sb.append(value);
            sb.append("</");
            sb.append(name);
            sb.append('>');
        } else if (value instanceof Map) {
            Iterator it = ((Map) value).keySet().iterator();
            if (it.hasNext()) {
                sb.append('<');
                sb.append(name);
                sb.append('>');
                Object key; 
                Map map = (Map) value;
                do {
                    key = it.next();
                    strb(sb, map.get(key), (String) key);
                } while (it.hasNext());
                sb.append("</");
                sb.append(name);
                sb.append('>');
            }
        } else if (value instanceof Iterable) {
            strb(sb, ((Iterable) value).iterator(), name);
        } else if (value instanceof Iterator) {
            sb.append('<');
            sb.append(name+_Array);
            sb.append(" SOAP-ENC:arrayType=\"");
            sb.append(name);
            sb.append("[]\" xsi:type=\"SOAP-ENC:Array\">");
            Iterator it = ((Iterator) value);
            while (it.hasNext()) {
                strb(sb, it.next(), name);
            }
            sb.append("</");
            sb.append(name+_Array);
            sb.append('>');
        } else {
            Class type = null;
            try {type = value.getClass();} catch (Throwable e) {;}
            if (type !=null && type.isArray()) {
                Class component = type.getComponentType();
                if (component.isPrimitive())
                    ; // strb(sb, value, component);
                else
                    strb(sb, Objects.iter((java.lang.Object[]) value), name);
            } else
                strb(sb, value.toString(), name);
        }
        return sb;
    }
    
    /**
     * Encode a SOAP response as a single <code>return</code> element,
     * with support for a few simple types only, just enough <em>not</em> to
     * lock its application in interroperability and dependencies woes.
     * 
     * <p>Most applications are not expected to use or override this method,
     * some may have to in order to cache response as bytes buffers or 
     * supplement legacy types.</p> 
     * 
     * @param value
     * @param name
     * @return
     */
    public static final byte[] encode (Object value, String name) { 
    	StringBuilder sb = new StringBuilder();
        sb.append('<');
        sb.append(name);
        sb.append('>');
        strb(sb, value, _return);
        sb.append("</");
        sb.append(name);
        sb.append('>');
        return Bytes.encode(sb.toString(), Bytes.UTF8); 
    }
    
    private static final String _Body = "Body";
    
    protected static final byte[] RESPONSE_HEAD = Bytes.encode(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<SOAP-ENV:Envelope "
            + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " 
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " 
            + ">"
            + "<SOAP-ENV:Body>", 
        Bytes.UTF8
        );
    protected static final byte[] RESPONSE_TAIL = Bytes.encode(
            "</SOAP-ENV:Body>" 
        + "</SOAP-ENV:Envelope>", 
        Bytes.UTF8	
        );
    
    public static final Iterator<byte[]> response (String name, Object body) {
        return (Iterator<byte[]>) Objects.iter((Object[]) new byte[][]{
            RESPONSE_HEAD, encode(body, name + _Response), RESPONSE_TAIL
            });
    } // isn't this one liner elegant?
    
    protected static final byte[] RESPONSE_ENVELOPE = Bytes.encode(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<SOAP-ENV:Envelope "
            + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " 
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " 
            + ">",
        Bytes.UTF8
        );
    
    protected static final byte[] RESPONSE_BODY = Bytes.encode(
        "<SOAP-ENV:Body>", Bytes.UTF8
        );
    
    public static final Iterator<byte[]> response (
		Object body, String name, XML.Element header
		) {
        return (Iterator<byte[]>) Objects.iter((Object[]) new byte[][]{
            RESPONSE_ENVELOPE, 
            XML.encodeUTF8(header, _NS),
            RESPONSE_BODY, 
            encode(body, name + _Response), 
            RESPONSE_TAIL
            });
    }
    
    protected static final byte[] FAULT_HEAD = Bytes.encode(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<SOAP-ENV:Envelope "
            + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " 
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " 
            + ">" 
            + "<SOAP-ENV:Body>"
                + "<SOAP-ENV:Fault>" 
                    + "<faultstring xsi:type=\"xsd:string\"><![CDATA[",
        Bytes.UTF8
        );
    
    protected static final byte[] FAULT_TAIL = Bytes.encode(
                    "]]></faultstring>" 
                + "</SOAP-ENV:Fault>" 
            + "</SOAP-ENV:Body>"
        + "</SOAP-ENV:Envelope>", 
        Bytes.UTF8
        );
    
    public static final Iterator<byte[]> fault (String name, String message) {
        return (Iterator<byte[]>) Objects.iter((Object[]) new byte[][]{
            RESPONSE_HEAD, Bytes.encode(message, Bytes.UTF8), RESPONSE_TAIL
            });
    }

    protected static final byte[] FAULT_BODY = Bytes.encode(
        "<SOAP-ENV:Fault>" 
            + "<faultstring xsi:type=\"xsd:string\"><![CDATA[",
	    Bytes.UTF8
	    );
    
    public static final Iterator<byte[]> fault (
        String name, String message, XML.Element header
        ) {
        return (Iterator<byte[]>) Objects.iter((Object[]) new byte[][]{
            RESPONSE_ENVELOPE, 
            XML.encodeUTF8(header, _NS),
            FAULT_BODY, 
            Bytes.encode(message, Bytes.UTF8), 
            FAULT_TAIL
            });
        }
    
}
