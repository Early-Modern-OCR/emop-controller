package edu.tamu.emop.model;

public class PageInfo {
    private Long id;
    private Long workId;
    private int pageNumber;
    private String groundTruthFile = null;
    private String galeTextFile = null;
    private String pageImage;
    
    
    public enum OutputFormat {TXT, XML};

    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getWorkId() {
        return workId;
    }
    public void setWorkId(Long workId) {
        this.workId = workId;
    }
    public int getPageNumber() {
        return pageNumber;
    }
    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
    public String getGroundTruthFile() {
        return groundTruthFile;
    }
    public void setGroundTruthFile(String groundTruthFile) {
        this.groundTruthFile = groundTruthFile;
    }
    public String getGaleTextFile() {
        return galeTextFile;
    }
    public void setGaleTextFile(String galeTextFile) {
        this.galeTextFile = galeTextFile;
    }
    public String getPageImage() {
        return pageImage;
    }
    public void setPageImage(String img) {
        pageImage = img;
    }
    
    public boolean hasGroundTruth() {
        return (this.groundTruthFile != null && this.groundTruthFile.length() > 0);
    }
    
    public boolean hasGaleText() {
        return (this.galeTextFile != null && this.galeTextFile.length() > 0);
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
        PageInfo other = (PageInfo) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "PageInfo [id=" + id + ", workId=" + workId + ", pageNumber=" + pageNumber + "]";
    }
    
    
}
