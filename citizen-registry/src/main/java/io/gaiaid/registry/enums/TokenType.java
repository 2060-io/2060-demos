package io.gaiaid.registry.enums;

public enum TokenType {

	FACE_CAPTURE("Face Capture"),
	FACE_VERIFICATION("Face Verification"),
	
	FINGERPRINT_CAPTURE("Fingerprint Capture"),
	FINGERPRINT_VERIFICATION("Fingerprint Verification"),
	
	
	;
	
	private String typeName;
	
	private TokenType(String typeName) {
		this.typeName = typeName;
		
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	
	
	
}
