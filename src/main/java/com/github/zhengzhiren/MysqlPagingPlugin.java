package com.github.zhengzhiren;

import org.mybatis.generator.api.FullyQualifiedTable;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.xml.*;

import java.util.*;

/**
 * Created by zhengzhiren on 2017/2/27.
 */
public class MysqlPagingPlugin extends PluginAdapter {

    private FullyQualifiedJavaType javaType;
    private Map<FullyQualifiedTable, List<XmlElement>> elementsToAdd;

    public MysqlPagingPlugin() {
        javaType = new FullyQualifiedJavaType("java.lang.Integer");
        elementsToAdd = new HashMap<>();
    }

    @Override
    public boolean validate(List<String> list) {
        return true;
    }

    @Override
    public boolean sqlMapDocumentGenerated(Document document,
                                           IntrospectedTable introspectedTable) {
        document.getRootElement().getElements().add(3, addWhereClause(document));

        List<XmlElement> elements = elementsToAdd.get(introspectedTable.getFullyQualifiedTable());
        if (elements != null) {
            for (XmlElement element : elements) {
                document.getRootElement().addElement(element);
            }
        }
        return true;
    }

    @Override
    public boolean clientSelectByExampleWithoutBLOBsMethodGenerated(
            Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        if (introspectedTable.getTargetRuntime() == IntrospectedTable.TargetRuntime.MYBATIS3) {
            Method newMethod = new Method(method);
            newMethod.setName(method.getName() + "WithPage");
            Parameter oriParameter = method.getParameters().get(0);
            newMethod.getParameters().clear();
            newMethod.addParameter(new Parameter(oriParameter.getType(), oriParameter.getName(), "@Param(\"example\")"));
            newMethod.addParameter(new Parameter(javaType, "offset", "@Param(\"offset\")"));
            newMethod.addParameter(new Parameter(javaType, "limit", "@Param(\"limit\")"));
            interfaze.addMethod(newMethod);
            interfaze.addImportedType(javaType);
        }
        return true;
    }

    //<select id="selectByExample" parameterType="com.xxx.model.AbcExample" resultMap="BaseResultMap">
    //  select
    //  <if test="distinct">
    //    distinct
    //  </if>
    //  <include refid="Base_Column_List" />
    //  from apply
    //  <if test="_parameter != null">
    //    <include refid="Example_Where_Clause" />
    //  </if>
    //  <if test="orderByClause != null">
    //    order by ${orderByClause}
    //  </if>
    //</select>

    //<select id="selectByExampleWithPage" resultMap="BaseResultMap">
    //  select
    //  <if test="example.distinct">
    //    distinct
    //  </if>
    //  <include refid="Base_Column_List" />
    //  from apply
    //  <if test="example != null">
    //    <include refid="Example_Where_Clause_With_Alias" />
    //  </if>
    //  <if test="example.orderByClause != null">
    //    order by ${example.orderByClause}
    //  </if>
    //  limit #{offset}, #{limit}
    //</select>
    @Override
    public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(
            XmlElement element, IntrospectedTable introspectedTable) {
        XmlElement newElement = new XmlElement(element);
        // remove old id attribute and add a new one with the new name
        for (Iterator<Attribute> iterator = newElement.getAttributes().iterator(); iterator.hasNext(); ) {
            Attribute attribute = iterator.next();
            if ("id".equals(attribute.getName())) {
                iterator.remove();
                Attribute newAttribute = new Attribute("id", attribute.getValue() + "WithPage");
                newElement.addAttribute(newAttribute);
                break;
            }
        }
        removeType(newElement);
        removeOrderElement(newElement);
        replaceDistinct(newElement);
        replaceWhere(newElement);
        appendOrder(newElement);
        appendLimit(newElement);

        FullyQualifiedTable table = introspectedTable.getFullyQualifiedTable();
        List<XmlElement> elements = elementsToAdd.get(table);
        if (elements == null) {
            elements = new ArrayList<>();
            elementsToAdd.put(table, elements);
        }
        elements.add(newElement);

        return true;
    }

    protected void removeType(XmlElement newElement) {
        for (Iterator<Attribute> iterator = newElement.getAttributes().iterator(); iterator.hasNext(); ) {
            Attribute attribute = iterator.next();
            if ("parameterType".equals(attribute.getName())) {
                iterator.remove();
                break;
            }
        }
    }

    protected void removeXmlElement(XmlElement newElement, String elementName, String innerElementContent) {
        for (Iterator<Element> iterator = newElement.getElements().iterator(); iterator.hasNext(); ) {
            Element innerElement = iterator.next();
            if (innerElement instanceof XmlElement) {
                XmlElement xmlElement = (XmlElement) innerElement;
                if (xmlElement.getName().equals(elementName) && xmlElement.getElements().size() == 1) {
                    Element childElement = xmlElement.getElements().get(0);
                    if (childElement instanceof TextElement) {
                        TextElement textElement = (TextElement) childElement;
                        if (textElement.getContent().equals(innerElementContent)) {
                            iterator.remove();
                        }
                    }
                }
            }
        }
    }

    protected void appendLimit(XmlElement newElement) {
        newElement.addElement(new TextElement("limit #{offset}, #{limit}"));
    }

    private void appendOrder(XmlElement newElement) {
        XmlElement pageElement = new XmlElement("if");
        pageElement.addAttribute(new Attribute("test", "example.orderByClause != null"));
        pageElement.addElement(new TextElement("order by ${example.orderByClause}"));
        newElement.addElement(pageElement);
    }

    private void removeOrderElement(XmlElement newElement) {
        removeXmlElement(newElement, "if", "order by ${orderByClause}");
    }

    private void replaceWhere(XmlElement newElement) {
        for (int index = 0; index < newElement.getElements().size(); index++) {
            Element innerElement = newElement.getElements().get(index);
            if (innerElement instanceof XmlElement) {
                XmlElement xmlElement = (XmlElement) innerElement;
                if (xmlElement.getName().equals("if") && xmlElement.getAttributes().size() == 1) {
                    if (xmlElement.getAttributes().get(0).getValue().equals("_parameter != null")) {
                        newElement.getElements().remove(index);
                        XmlElement newInnerElement = new XmlElement(xmlElement);
                        newInnerElement.getAttributes().clear();
                        newInnerElement.addAttribute(new Attribute("test", "example != null"));
                        newInnerElement.getElements().clear();
                        XmlElement includeElement = new XmlElement("include");
                        includeElement.addAttribute(new Attribute("refid", "Example_Where_Clause_With_Alias"));
                        newInnerElement.addElement(includeElement);
                        newElement.addElement(index, newInnerElement);
                        break;
                    }
                }
            }
        }
    }

    private void replaceDistinct(XmlElement newElement) {
        for (int index = 0; index < newElement.getElements().size(); index++) {
            Element innerElement = newElement.getElements().get(index);
            if (innerElement instanceof XmlElement) {
                XmlElement xmlElement = (XmlElement) innerElement;
                if (xmlElement.getName().equals("if") && xmlElement.getElements().size() == 1) {
                    Element childElement = xmlElement.getElements().get(0);
                    if (childElement instanceof TextElement) {
                        TextElement textElement = (TextElement) childElement;
                        if (textElement.getContent().equals("distinct")) {
                            newElement.getElements().remove(index);
                            XmlElement newInnerElement = new XmlElement(xmlElement);
                            newInnerElement.getAttributes().clear();
                            newInnerElement.getAttributes().add(new Attribute("test", "example.distinct"));
                            newElement.addElement(index, newInnerElement);
                            break;
                        }
                    }
                }
            }
        }
    }

    private XmlElement addWhereClause(Document document) {
        for (Element rootElement : document.getRootElement().getElements()) {
            if (rootElement instanceof XmlElement) {
                XmlElement root = (XmlElement) rootElement;
                if (root.getName().equals("sql") && root.getAttributes().size() == 1) {
                    if (root.getAttributes().get(0).getValue().equals("Example_Where_Clause")) {

                        XmlElement newElement = new XmlElement("sql");
                        newElement.addAttribute(new Attribute("id", "Example_Where_Clause_With_Alias"));
                        XmlElement where = new XmlElement("where");
                        XmlElement whereContent = new XmlElement(((XmlElement) ((XmlElement) root.getElements().get(root.getElements().size() - 1)).getElements().get(0)));
                        for (Iterator<Attribute> iterator = whereContent.getAttributes().iterator(); iterator.hasNext(); ) {
                            Attribute attribute = iterator.next();
                            if (attribute.getName().equals("collection")) {
                                iterator.remove();
                            }
                        }
                        whereContent.addAttribute(new Attribute("collection", "example.oredCriteria"));
                        where.addElement(whereContent);
                        newElement.addElement(where);
                        return newElement;
                    }
                }
            }
        }
        return null;
    }

}
