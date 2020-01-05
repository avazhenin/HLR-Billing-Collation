package hlr.billing.collation;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * Created by anatoliyvazhenin on 10/20/14.
 */
public class ParseXMLUtilities {

    File xmlFile;
    NodeList nodelist;

    public ParseXMLUtilities() {

    }

    public ParseXMLUtilities(String xmlFilePath) {
        this.xmlFile = new File(xmlFilePath);
    }

    void initiate() {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(this.xmlFile);
            nodelist = document.getDocumentElement().getChildNodes();
        } catch (Exception e) {
        }
    }

    NodeList getChildNodes(String nodeName) {
        return recursiveNodes(this.nodelist, nodeName);
    }

    // get child nodes
    NodeList recursiveNodes(NodeList nodes, String nodeName) {
        if (nodes.getLength() > 0) {

            NodeList list = null;
            // try to find searched node
            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                    list = nodes.item(i).getChildNodes();
                    return list;
                }
            }
            // if searched node not found, start recursive search with child nodes
            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i).getChildNodes().getLength() > 0) {
                    return recursiveNodes(nodes.item(i).getChildNodes(), nodeName);
                }
            }
        }
        return null;
    }

    // loop through node list searching for specific named node's value
    String getNodeValue(NodeList nodes, String nodeName) {
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return nodes.item(i).getChildNodes().item(0).getNodeValue();
            }
        }
        return null;
    }

}
