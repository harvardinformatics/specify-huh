package edu.harvard.huh.asa;

import edu.harvard.huh.asa.AsaShipment.CARRIER;
import edu.harvard.huh.asa.AsaShipment.METHOD;

public class OutReturnBatch extends ReturnBatch
{
	private CARRIER carrier;
	private METHOD  method;
	private Float   cost;
	private Boolean isEstimatedCost;
	private String  note;

	public CARRIER getCarrier() { return carrier; }
    
    public METHOD getMethod() { return method; }
    
    public Float getCost() { return cost; }
    
    public Boolean isEstimatedCost() { return isEstimatedCost; }
	
	public String getNote() { return note; }
    
    public void setCarrier(CARRIER carrier) { this.carrier = carrier; }
    
    public void setMethod(METHOD method) { this.method = method; }
    
    public void setCost(Float cost) { this.cost = cost; }
    
    public void setIsEstimatedCost(Boolean isEstimatedCost) { this.isEstimatedCost = isEstimatedCost; }
	
	public void setNote(String note) { this.note = note; }
}
