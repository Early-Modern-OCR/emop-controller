package edu.tamu.emop.model;

public class PageImage {
    private final String imagePath;
    private final int pageNumber;
    private final int version;
    
    public PageImage(String img, int page, int rev) {
        this.imagePath = img;
        this.pageNumber = page;
        this.version = rev;
    }
    
    public String getTxtFilename() {
        return String.format("%d.%d.txt", getPageNumber(), getVersion());
    }

    public String getImagePath() {
        return this.imagePath;
    }

    public int getPageNumber() {
        return this.pageNumber;
    }

    public int getVersion() {
        return this.version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.imagePath == null) ? 0 : this.imagePath.hashCode());
        result = prime * result + this.pageNumber;
        result = prime * result + this.version;
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
        PageImage other = (PageImage) obj;
        if (this.imagePath == null) {
            if (other.imagePath != null)
                return false;
        } else if (!this.imagePath.equals(other.imagePath))
            return false;
        if (this.pageNumber != other.pageNumber)
            return false;
        if (this.version != other.version)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PageImage [imagePath=" + this.imagePath + ", pageNumber=" + this.pageNumber
            + ", version=" + this.version + "]";
    }
    
}
