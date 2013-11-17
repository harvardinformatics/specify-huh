package edu.harvard.huh.specify.plugins;

// mmk: taken from edu.ku.specify.extras.FishBaseInfoGetterListener
public interface IndexFungorumInfoGetterListener {
	/**
	 * Notifies the consumer that the data has arrived ok
	 * @param getter the getter that got the data
	 */
	public void infoArrived(IndexFungorumInfoGetter getter);

	/**
	 * Notifies the consumer that the data was in error
	 * @param getter the getter that got the data
	 */
	public void infoGetWasInError(IndexFungorumInfoGetter getter);
}
