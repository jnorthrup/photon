package nars.storage;

public interface BagObserver {

	void setTitle(String title);

	void setBag(Bag<?> concepts);
	
	/**
	 * Post the bag content
	 * @param str The text
	 */
	void post(String str);

	void refresh(String string);

	void stop();

}