package edu.tamu.emop;

import static org.junit.Assert.assertFalse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.xml.sax.SAXException;

public class HocrTransformerTest {

    @Test
    public void testDetectEncoding() throws IOException, SAXException, TransformerException {
        HocrTransformer transformer = new HocrTransformer();
        transformer.initialize();
        File f = resourceToFile("2.html");
        File out = transformer.extractTxt(f.getAbsolutePath(), "out.txt");
        
        FileInputStream fis = new FileInputStream(out);
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        BufferedReader r = new BufferedReader(isr);
        while (true) {
            String line = r.readLine();
            if (line == null) {
                break;
            } else {
                char bad = 0xfffd;
                assertFalse("Bad utf8", line.indexOf(bad)>-1);
            }
        }
        out.delete();
    }
    
    protected File resourceToFile(String resourceName) throws IOException {
        InputStream is = getClass().getResourceAsStream("/"+resourceName);
        File local = new File("test"+resourceName);
        FileOutputStream fos = new FileOutputStream(local);
        IOUtils.copy(is, fos);
        IOUtils.closeQuietly(fos);
        local.deleteOnExit();
        return local;
    }
    
}
