package edu.tamu.emop.model;

import java.io.File;

public class PageImage {
    private final String imagePath;
    private final int pageNumber;
    
    private static final String ECCO_OCR_ROOT = "/data/shared/text-xml/ECCO-IDHMC-ocr";
    private static final String EEBO_OCR_ROOT = "/data/shared/text-xml/EEBO-IDHMC-ocr";
    
    public PageImage(String img, int page) {
        this.imagePath = img;
        this.pageNumber = page;
    }
    
    /**
     * Generate the OCR output file based upon the page image path
     * @param batchId
     * @return
     */
    public String getOcrOutPath(Long batchId ) {
        // Two possible image formats, ecco and eebo:
        // ECCO: /data/ecco/ECCO_2of2/LitAndLang_2/0331501100/images/033150110000010.TIF
        // EEBO: /data/eebo/e0011/53/[file].tif
        
        // get the path to the image file (not includign the image itself)
        File imgFile = new File(this.imagePath);
        String path = imgFile.getParent();
        
        // replace base path with the new path to shared txt results
        // and strip any trailing images directory.
        path = path.replaceAll("/data/ecco", ECCO_OCR_ROOT);
        path = path.replaceAll("/data/eebo", EEBO_OCR_ROOT);
        path = path.replaceAll("/images", "");
        
        // at this point, we have a path to the OCR output for the work
        // now append the batch and final filename
        String fileName = String.format("%d.txt", getPageNumber());
        return path+"/"+batchId+"/"+fileName;
    }

    public String getImagePath() {
        return this.imagePath;
    }

    public int getPageNumber() {
        return this.pageNumber;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.imagePath == null) ? 0 : this.imagePath.hashCode());
        result = prime * result + this.pageNumber;
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
        return true;
    }

    @Override
    public String toString() {
        return "PageImage [imagePath=" + this.imagePath + ", pageNumber=" + this.pageNumber;
    }
    
}
