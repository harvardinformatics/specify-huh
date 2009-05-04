/* This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.harvard.huh.asa;

public class Publication
{
    private Integer id;
 
    private  String isbn;
    private  String pubPlace;
    private  String pubDate;
    private  String publisher;
    private  String url;
    private  String title;
    private Boolean isJournal;
    private  String issn;
    private  String bph;
    private  String abbreviation;
    private  String remarks;
 
    public Publication() {
        ;
    }
    
    public Integer getId() { return this.id; }
    
    public String getIsbn() { return this.isbn; }
    
    public String getPubPlace() { return this.pubPlace; }
    
    public String getPubDate() { return this.pubDate; }
    
    public String getPublisher() { return this.publisher; }
    
    public String getUrl() { return this.url; }
    
    public String getTitle() { return this.title; }
    
    public Boolean isJournal() { return this.isJournal; }
    
    public String getIssn() { return this.issn; }
    
    public String getBph() { return this.bph; }
    
    public String getAbbreviation() { return this.abbreviation; }
    
    public String getRemarks() { return this.remarks; }
    
    public void setId(Integer id) { this.id = id; }
    
    public void setIsbn(String isbn) { this.isbn = isbn; }
    
    public void setPubPlace(String pubPlace) { this.pubPlace = pubPlace; }
    
    public void setPubDate(String pubDate) { this.pubDate = pubDate; }
    
    public void setPublisher(String publisher) { this.publisher = publisher; }
    
    public void setUrl(String url) { this.url = url; }
    
    public void setTitle(String title) { this.title = title; }
    
    public void setJournal(Boolean isJournal) { this.isJournal = isJournal; }
    
    public void setBph(String bph) { this.bph = bph; }
    
    public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }
    
    public void setIssn(String issn) { this.issn = issn; }
    
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
