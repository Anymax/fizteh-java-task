package ru.fizteh.fivt.students.fedyuninV.bind;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.fizteh.fivt.bind.AsXmlElement;
import ru.fizteh.fivt.bind.BindingType;
import ru.fizteh.fivt.bind.MembersToBind;
import sun.misc.Unsafe;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Fedyunin Valeriy
 * MIPT FIVT 195
 */
public class XmlBinder<T> extends ru.fizteh.fivt.bind.XmlBinder<T>{
    Map<Class, List<SerializeComponent>> methods;
    Map<Class, List<Field>> fields;
    IdentityHashMap<Object, Object> serialized;
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Object CONTAINS = new Object();
    Unsafe unsafeInstance;

    public XmlBinder(Class<T> clazz) {
        super(clazz);
        methods = new HashMap<>();
        fields = new HashMap<>();
        addToMap(clazz);
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafeInstance = (Unsafe) f.get(Unsafe.class);
        } catch (Throwable t) {
            throw new RuntimeException("Cannot create binder.");
        }
    }


    private void addToMethodMap(Class clazz) {
        if (methods.containsKey(clazz)) { //already in map
            return;
        }
        List<SerializeComponent> components = new ArrayList<>();
        Method[] methodList = clazz.getMethods();
        for (Method method: methodList) {
            String name = null;
            Class[] args = method.getParameterTypes();
            String methodName = method.getName();
            if (checkPrefix("set", methodName)  &&  args.length == 1  &&  method.getReturnType().equals(void.class)) {
                name = methodName.substring(3);
            }
            if (name  != null) {
                SerializeComponent newComponent = new SerializeComponent(name);
                newComponent.setSetter(method);
                components.add(newComponent);
            }
        }
        List<SerializeComponent> result = new ArrayList<>();
        for (SerializeComponent component: components) {
            Method getter = null;
            String prefix = "get";
            Class setterArg = component.setter().getParameterTypes()[0];
            if (setterArg.equals(boolean.class)  ||  setterArg.equals(Boolean.class)) {
                prefix = "is";
            }
            try {
                getter = clazz.getMethod(prefix + component.getName());
            } catch (NoSuchMethodException ignored) {
            }
            if (getter != null) {
                component.setGetter(getter);
                result.add(component);
                addToMap(component.getter().getReturnType());
                setComponentName(component);
            }
        }
        methods.put(clazz, result);
    }

    private void addToFieldMap(Class clazz) {
        if (fields.containsKey(clazz)) {
            return;
        }
        List<Field> result = new ArrayList<>();
        Class parent = clazz;
        while (parent != null) {
            Field[] fieldList = parent.getDeclaredFields();
            result.addAll(Arrays.asList(fieldList));
            parent = parent.getSuperclass();
        }
        fields.put(clazz, result);
        for (Field field: result) {
            addToMap(field.getType());
        }
    }

    private void addToMap(Class clazz) {
        if (possibleToString(clazz)) {
            return;
        }
        BindingType bindingType = (BindingType) clazz.getAnnotation(BindingType.class);
        if (bindingType == null  ||  bindingType.value().equals(MembersToBind.FIELDS)) {
            addToFieldMap(clazz);
        } else {
            addToMethodMap(clazz);
        }
    }

    private boolean checkPrefix(String prefix, String methodName) {
        return (methodName.length() >= prefix.length()  &&  methodName.substring(0, prefix.length()).equals(prefix));
    }

    private String firstCharToLowerCase(String str) {
        if (str.length() > 0) {
            str = Character.toLowerCase(str.charAt(0)) + str.substring(1);
        }
        return str;
    }

    private String getElementName(Object value, String defaultName) {
        AsXmlElement asXmlElement = value.getClass().getAnnotation(AsXmlElement.class);
        if (asXmlElement == null) {
            return firstCharToLowerCase(defaultName);
        } else {
            return asXmlElement.name();
        }
    }

    private String getFieldName(Field value) {
        AsXmlElement asXmlElement = value.getAnnotation(AsXmlElement.class);
        if (asXmlElement == null) {
            return firstCharToLowerCase(value.getName());
        } else {
            return asXmlElement.name();
        }
    }

    private void setComponentName(SerializeComponent component) {
        AsXmlElement getterAnnnotation = component.getter().getAnnotation(AsXmlElement.class);
        AsXmlElement setterAnnnotation = component.setter().getAnnotation(AsXmlElement.class);
        if (setterAnnnotation != null) {
            if (getterAnnnotation != null) {
                if (setterAnnnotation.name().equals(getterAnnnotation.name())) {
                    component.setName(setterAnnnotation.name());
                } else {
                    throw new RuntimeException("Incorrect annotations of methods");
                }
            } else {
                component.setName(setterAnnnotation.name());
            }
        } else {
            if (getterAnnnotation != null) {
                component.setName(getterAnnnotation.name());
            }
        }
    }

    private boolean possibleToString(Class classExample) {
        return (classExample.isPrimitive()
                ||  classExample.equals(Integer.class)
                ||  classExample.equals(Boolean.class)
                ||  classExample.equals(String.class)
                ||  classExample.equals(Double.class)
                ||  classExample.equals(Float.class)
                ||  classExample.equals(Byte.class)
                ||  classExample.equals(Long.class)
                ||  classExample.equals(Short.class)
                ||  classExample.equals(Character.class)
                ||  classExample.isEnum());
    }

    private void writeToDocumentByFields(Document document, Object value, Element root) throws Exception {
        for (Field field: fields.get(value.getClass())) {
            field.setAccessible(true);
            Object fieldValue = field.get(value);
            if (fieldValue != null) {
                Element child = document.createElement(getFieldName(field));
                root.appendChild(child);
                if (possibleToString(fieldValue.getClass())) {
                    child.setTextContent(fieldValue.toString());
                } else {
                    writeToDocument(document, fieldValue, child);
                }
            }
        }
    }

    private void writeToDocumentByMethods(Document document, Object value, Element root) throws Exception {
        for (SerializeComponent component: methods.get(value.getClass())) {
            String name = firstCharToLowerCase(component.getName());
            Object newValue = component.getter().invoke(value);
            if (newValue != null) {
                Element child = document.createElement(name);
                root.appendChild(child);
                if (possibleToString(newValue.getClass())) {
                    child.setTextContent(newValue.toString());
                } else {
                    writeToDocument(document, newValue, child);
                }
            }
        }
    }

    private void writeToDocument(Document document, Object value, Element root) throws Exception{
        BindingType bindingType = value.getClass().getAnnotation(BindingType.class);
        if (serialized.put(value, CONTAINS) != null) {
            throw new RuntimeException("Object contains link to itself, cannot serailize");
        }
        if (bindingType == null  ||  bindingType.value().equals(MembersToBind.FIELDS)) {
            writeToDocumentByFields(document, value, root);
        } else {
            writeToDocumentByMethods(document, value, root);
        }
    }

    @Override
    public byte[] serialize(Object value) {
        if (value != null  &&  !value.getClass().equals(getClazz())) {
            throw new RuntimeException("This class is not supported by this binder!");
        }
        serialized = new IdentityHashMap<>();
        //Creating XML
        try {
            Document document = factory.newDocumentBuilder().newDocument();
            if (value != null) {
                Element root = document.createElement(getElementName(value, value.getClass().getName()));
                writeToDocument(document, value, root);
                document.appendChild(root);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Result result = new StreamResult(out);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount",
                    "2"
            );
            transformer.transform(new DOMSource(document), result);
            return out.toByteArray();
            /*StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            System.out.println(stringWriter.getBuffer().toString());
            return null;                               */
        } catch (Exception ex) {
            throw new RuntimeException("Serializing error", ex);
        }
    }

    private Document bytesToXml(byte[] xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xml));
        } catch (Exception ex) {
            throw new RuntimeException("Incorrect byte array", ex);
        }
    }

    private Object getObjectOfClass(Class clazz) {
        try {
            Constructor constructor = getClazz().getConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException ex) {
            try {
                return unsafeInstance.allocateInstance(clazz);
            } catch (Exception exc) {
                throw new RuntimeException("Error in deserializing", ex);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error in deserializing", ex);
        }
    }

    private Object getValueFromString(Class clazz, String text) {
        if (clazz.equals(boolean.class)  ||  clazz.equals(Boolean.class)) {
            return Boolean.parseBoolean(text);
        }
        if (clazz.equals(int.class)  ||  clazz.equals(Integer.class)) {
            return Integer.parseInt(text);
        }
        if (clazz.equals(long.class)  ||  clazz.equals(Long.class)) {
            return Long.parseLong(text);
        }
        if (clazz.equals(double.class)  ||  clazz.equals(Double.class)) {
            return Double.parseDouble(text);
        }
        if (clazz.equals(float.class)  ||  clazz.equals(Float.class)) {
            return Float.parseFloat(text);
        }
        if (clazz.equals(byte.class)  ||  clazz.equals(Byte.class)) {
            return Byte.parseByte(text);
        }
        if (clazz.equals(short.class)  ||  clazz.equals(Short.class)) {
            return Short.parseShort(text);
        }
        if (clazz.equals(char.class)  ||  clazz.equals(Character.class)) {
            if (text.length() != 1) {
                throw new RuntimeException("Incorrect type");
            }
            return text.charAt(0);
        }
        if (clazz.equals(String.class)) {
            return text;
        }
        if (clazz.isEnum()) {
            return Enum.valueOf(clazz, text);
        }
        return null;
    }

    private Object deserialize(Class clazz, Element root) {
        if (possibleToString(clazz)) {
            return getValueFromString(clazz, root.getTextContent());
        } else {
            Object obj = getObjectOfClass(clazz);
            if (methods.containsKey(clazz)) {
                List<SerializeComponent> components = methods.get(clazz);
                for (SerializeComponent component: components) {
                    NodeList childs = root.getElementsByTagName(component.getName());
                    if (childs.getLength() > 1) {
                        throw new RuntimeException("Incorrect number of fields in XML");
                    }
                    if (childs.getLength() > 0) {
                        Node child = childs.item(0);
                        try {
                            component.setter().invoke(obj, deserialize(component.getter().getReturnType(), (Element) child));
                        } catch (Exception ex) {
                            throw new RuntimeException("Fail in deserializing");
                        }
                    }
                }
            } else if(fields.containsKey(clazz)) {
                List<Field> fieldList = fields.get(clazz);
                for (Field field: fieldList) {
                    field.setAccessible(true);
                    NodeList childs = root.getElementsByTagName(getFieldName(field));
                    if (childs.getLength() > 1) {
                        throw new RuntimeException("Incorrect number of fields in XML");
                    }
                    if (childs.getLength() > 0) {
                        Node child = childs.item(0);
                        try {
                            field.set(obj, deserialize(field.getType(), (Element) child));
                        } catch (Exception ex) {
                            throw new RuntimeException("Fail in deserializing");
                        }
                    }
                }
            } else {
                throw new RuntimeException("Cannot deserialize class");
            }
            return obj;
        }
    }

    @Override
    public T deserialize(byte[] bytes) {
        Document document = bytesToXml(bytes);
        Element root = document.getDocumentElement();
        return (T) deserialize(getClazz(), root);
    }
}
