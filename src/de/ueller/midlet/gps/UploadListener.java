package de.ueller.midlet.gps;

public interface UploadListener {

	public void startProgress(String title);
	public void setProgress(String message);
	public void updateProgress(String message);
	public void completedUpload(boolean success, String message);
	public void uploadAborted();

}