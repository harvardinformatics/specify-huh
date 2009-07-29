package edu.harvard.huh.asa;

import java.util.Date;

public class ReturnBatch extends CountableBatch
{
    private String  boxCount;
    private Boolean isAcknowledged;
    private Date    actionDate;
    
    public String getBoxCount() { return boxCount; }
    
    public Boolean isAcknowledged() { return isAcknowledged; }
    
    public Date getActionDate() { return actionDate; }
    
    public void setBoxCount(String boxCount) { this.boxCount = boxCount; }
    
    public void setIsAcknowledged(Boolean isAcknowledged) { this.isAcknowledged = isAcknowledged; }
    
    public void setActionDate(Date actionDate) { this.actionDate = actionDate; }
    
    public String getBoxCountNote()
    {
        String boxCount = getBoxCount();
        
        if (boxCount != null)
        {
            try
            {
                int boxes = Integer.parseInt(boxCount);
                boxCount = boxCount + " box" + (boxes == 1 ? "" : "es");
            }
            catch (NumberFormatException nfe)
            {
                ;
            }
            return boxCount + ".";
        }
        
        return null;
    }
    
    public String getAcknowledgedNote()
    {
        Boolean isAcknowledged = isAcknowledged();
        
        if (isAcknowledged != null)
        {
            if (isAcknowledged) return "Acknowledged.";
            else return "Not acknowledged.";
        }
        
        return null;
    }
}
