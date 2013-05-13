package edu.tamu.emop.model;

import java.util.Date;

public class EmopJob {
    public enum Status {NOT_STARTED, PROCESSING, PENDING_POSTPROCESS, POSTPROCESSING, DONE, FAILED};
    public enum JobType {GT_COMPARE, OCR};
    private Long id;
    private Long pageId;
    private Long batchId;
    private Status status;
    private JobType jobType;
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
    public Long getBatchId() {
        return batchId;
    }
    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }
    public Status getStatus() {
        return status;
    }
    public void setStatus(Long statusId) {
        int idx = (int)(statusId-1);
        this.status = Status.values()[idx];
    }
    public JobType getJobType() {
        return jobType;
    }
    public void setJobType(Long typeId) {
        int idx = (int)(typeId-1);
        this.jobType = JobType.values()[idx];
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
            + ", jobType=" + jobType + ", results=" + results + ", created=" + created
            + ", updated=" + updated + "]";
    }
    
    
}
