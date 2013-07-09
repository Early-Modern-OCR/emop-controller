package edu.tamu.emop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Utility methods for extracting text content from hOCR results
 */
public  class HocrTransformer {
    private XMLReader xmlReader;
    private Transformer transformer;
    
    public void initialize() throws SAXException, IOException, TransformerConfigurationException {
        this.xmlReader = XMLReaderFactory.createXMLReader();
        this.xmlReader.setEntityResolver(new EntityResolver() {

            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                if (systemId.endsWith(".dtd") || systemId.endsWith(".ent")) {
                    StringReader stringInput = new StringReader(" ");
                    return new InputSource(stringInput);
                }
                else {
                    return null; // use default behavior
                }
            }
        });
        
        // get the xslt
        String xslt = IOUtils.toString( ClassLoader.getSystemResourceAsStream("hocr.xslt"), "utf-8");
        javax.xml.transform.Source xsltSource =  new StreamSource( new StringReader(xslt) );
        
        //create the transformer
        TransformerFactory factory = TransformerFactory.newInstance(  );
        this.transformer = factory.newTransformer(xsltSource);  
        this.transformer.setOutputProperty(OutputKeys.INDENT, "no");
        this.transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        this.transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "text");
    }
    
    /**
     * Parse out the text content from the gived hOCR file
     * 
     * @param hOcrPath
     * @return
     * @throws SAXException 
     * @throws IOException 
     * @throws TransformerException 
     * @throws Exception 
     */
    public File extractTxt( final String hOcrPath, final String outPath ) throws SAXException, IOException, TransformerException {
        FileInputStream fis = new FileInputStream(hOcrPath);
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        SAXSource xmlSource = new SAXSource(this.xmlReader, new InputSource(isr));
        File out = new File(outPath);
        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(out), "UTF-8");
        StreamResult xmlOutput = new StreamResult(osw);       
        this.transformer.transform(xmlSource, xmlOutput);
        IOUtils.closeQuietly(osw);
        return out;
    }
}
