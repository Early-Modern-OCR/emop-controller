package edu.tamu.emop.model;


/***
 * Model of an OCR batch run. Includes the OCR enine
 * and the configuration used.
 * 
 * @author loufoster
 *
 */
public class OcrBatch {
    public enum OcrEngine {TESSERACT, GAMERA, OCROPUS};
    public Long id;
    public OcrEngine ocrEngine;
    public String parameters;
    public String version;
    public String notes;
    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public OcrEngine getOcrEngine() {
        return ocrEngine;
    }
    public void setOcrEngine(Long ocrEngine) {
        int idx = (int)(ocrEngine-1);
        this.ocrEngine = OcrEngine.values()[idx];
    }
    public String getParameters() {
        return parameters;
    }
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OcrBatch other = (OcrBatch) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "OcrBatch [id=" + id + ", ocrEngine=" + ocrEngine + ", parameters=" + parameters + ", version="
            + version + "]";
    }
    
    
}
