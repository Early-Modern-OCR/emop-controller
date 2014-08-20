package edu.tamu.emop.model;

import edu.tamu.emop.model.PageInfo.OutputFormat;

public class WorkInfo {
    private Long id;
    private String title;
    private Long organizationalUnit;
    private String eeboDirectory;
    private String eccoDirectory;
    private String eccoId;
    
    private static final String OCR_ROOT = "/data/shared/text-xml/IDHMC-ocr";
    
    public Long getId() {
        return id;
    }
    public void setId(Long workId) {
        this.id = workId;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public Long getOrganizationalUnit() {
        return organizationalUnit;
    }
    public void setOrganizationalUnit(Long organizationalUnit) {
        this.organizationalUnit = organizationalUnit;
    }
    public String getEeboDirectory() {
        return eeboDirectory;
    }
    public void setEeboDirectory(String eeboDirectory) {
        this.eeboDirectory = eeboDirectory;
    }
    public String getEccoDirectory() {
        return eccoDirectory;
    }
    public void setEccoDirectory(String eccoDirectory) {
        this.eccoDirectory = eccoDirectory;
    }
    public String getEccoNumber() {
        return eccoId;
    }
    public void setEccoNumber(String eccoId) {
        this.eccoId = eccoId;
    }
    
    public boolean isEcco() {
        return (this.eccoDirectory != null && this.eccoDirectory.length() > 0);
    }
    
    public String getOcrOutputDirForBatch(BatchJob batch) {
        // /data/shared/text-xml/IDHMC-OCR/<org_unit>/<work_id>/<batch>
        return OCR_ROOT+"/"+getOrganizationalUnit()+"/"+getId()+"/"+batch.getId();
    }

    public String getOcrOutFile(BatchJob batch, OutputFormat fmt, int pageNum) {
        // Final directory structure is this:
        // /data/shared/text-xml/IDHMC-OCR/<org_unit>/<work_id>/<batch>/<image-file-name>.[txt | xml]
        return getOcrOutputDirForBatch(batch)+"/"+pageNum+"."+fmt.toString().toLowerCase();
    }
    
    public String getOcrRootDirectory() {
        return OCR_ROOT+"/"+getOrganizationalUnit();
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
        WorkInfo other = (WorkInfo) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "WorkInfo [workId=" + id + ", title=" + title + ", organizationalUnit=" + organizationalUnit + "]";
    }
    
    
}
