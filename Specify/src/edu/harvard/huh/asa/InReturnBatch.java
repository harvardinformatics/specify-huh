package edu.harvard.huh.asa;

public class InReturnBatch extends ReturnBatch
{
	private String  transferredTo;
		
	public String getTransferredTo() { return transferredTo; }

	public void setTransferredTo(String transferredTo) { this.transferredTo = transferredTo; }
    
    public String getTransferNote()
    {
        String transferredTo = getTransferredTo();
        
        if (transferredTo != null)
        {
            return "Transferred to " + transferredTo;
        }
        
        return null;
    }
}
