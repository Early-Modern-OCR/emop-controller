package edu.tamu.emop.model;

import java.util.Date;

public class EmopJob {
    public enum Status {NOT_STARTED, PROCESSING, PENDING_POSTPROCESS, DONE, FAILED};
    public enum JobType {OCR_TESSERACT, OCR_GAMERA, OCR_OCROPUS, GT_COMPARE};
    private Long id;
    private Long pageId;
    private String batchId;
    private Status status;
    private JobType jobType;
    private String parameters;
    private String results;
    private Date created;
    private Date updated;
    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getPageId() {
        return pageId;
    }
    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }
    public String getBatchId() {
        return batchId;
    }
    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }
    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public JobType getJobType() {
        return jobType;
    }
    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }
    public String getParameters() {
        return parameters;
    }
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
    public String getResults() {
        return results;
    }
    public void setResults(String results) {
        this.results = results;
    }
    public Date getCreated() {
        return created;
    }
    public void setCreated(Date created) {
        this.created = created;
    }
    public Date getUpdated() {
        return updated;
    }
    public void setUpdated(Date updated) {
        this.updated = updated;
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
        EmopJob other = (EmopJob) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "EmopJob [id=" + id + ", pageId=" + pageId + ", batchId=" + batchId + ", status=" + status
            + ", jobType=" + jobType + ", parameters=" + parameters + ", results=" + results + ", created=" + created
            + ", updated=" + updated + "]";
    }
    
    
}
