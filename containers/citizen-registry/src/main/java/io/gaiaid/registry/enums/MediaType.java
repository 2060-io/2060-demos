package io.gaiaid.registry.enums;

public enum MediaType {

	FACE("Face"),
	FINGERPRINT("Fingerprint"),
	AVATAR("Avatar"),
	;
	
	
	private String mediaName;
	
	private MediaType(String mediaName) {
		this.mediaName = mediaName;
		
	}

	public String getMediaName() {
		return mediaName;
	}

	public void setMediaName(String mediaName) {
		this.mediaName = mediaName;
	}

	
	
}
