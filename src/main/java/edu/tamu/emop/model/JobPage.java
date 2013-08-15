package edu.tamu.emop.model;

import java.util.Date;

public class JobPage {
    public enum Status {NOT_STARTED, PROCESSING, PENDING_POSTPROCESS, POSTPROCESSING, DONE, FAILED};
    private Long id;
    private Long pageId;
    private BatchJob batch;
    private Status status;
    private String results;
    private Date created;
    private Date updated;
    private String trainingFont = "eng";
    
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
    public BatchJob getBatch() {
        return batch;
    }
    public void setBatch(BatchJob batch) {
        this.batch = batch;
    }
    public Status getStatus() {
        return status;
    }
    public void setStatus(Long statusId) {
        int idx = (int)(statusId-1);
        this.status = Status.values()[idx];
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
    public String getTrainingFont() {
        return trainingFont;
    }
    public void setTrainingFont(String name) {
        this.trainingFont = name;
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
        JobPage other = (JobPage) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "EmopJob [id=" + id + ", pageId=" + pageId + ", batch=" + batch + ", status=" + status
            + ", results=" + results + ", created=" + created + ", updated=" + updated + "]";
    }
    
    
}
