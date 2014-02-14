package edu.tamu.emop.model;


/***
 * Model of an OCR batch run. Includes the OCR engine
 * and the configuration used.
 *
 * @author loufoster
 *
 */
public class BatchJob {
    public enum OcrEngine {GALE, TESSERACT, GAMERA, OCROPUS};
    public enum JobType {GT_COMPARE, OCR, OTHER};

    private Long id;
    private JobType jobType;
    private OcrEngine ocrEngine;
    private String parameters;
    private String name;
    private String notes;

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
    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public JobType getJobType() {
        return jobType;
    }
    public void setJobType(Long jobType) {
        int idx = (int)(jobType-1);
        this.jobType = JobType.values()[idx];
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
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
        BatchJob other = (BatchJob) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "BatchJob [id=" + id + ", jobType=" + jobType + ", ocrEngine=" + ocrEngine + ", parameters="
            + parameters + ", name=" + name + "]";
    }

}
