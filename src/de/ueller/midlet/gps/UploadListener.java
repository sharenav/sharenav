package de.ueller.midlet.gps;

public interface UploadListener {

	public void updateProgress(String message);
	public void completedUpload(boolean success, String message);
	public void uploadAborted();

}