package io.gaiaid.registry.enums;


public enum IdentityClaim {

	ID("id", "id"),
	CITIZEN_ID("citizenId", "Citizen ID"),
	FIRSTNAME("firstname", "Firstname"),
	LASTNAME("lastname", "Lastname"),
	AVATARNAME("avatarname", "Avatarname"),
	AVATARPIC("avatarpic", "Avatarpic"),
	BIRTHDATE("birthdate", "Birthdate"),
	PLACE_OF_BIRTH("placeOfBirth", "Place of Birth"),
	GENRE("genre", "Genre"),
	CITIZEN_SINCE("citizenSince", "Citizen Since"),
	PHOTO("photo", "Photo");
	
	private String claimName;
	private String claimLabel;
	
	private IdentityClaim(String claimName, String claimLabel) {
		this.claimName = claimName;
		this.claimLabel = claimLabel;
	}
	
	
	public static IdentityClaim getEnum(String claimName){
		if (claimName == null)
	return null;

		switch(claimName){
		case "id": return ID;
		case "firstname": return FIRSTNAME;
		case "lastname": return LASTNAME;
		case "avatarname": return AVATARNAME;
		case "avatarpic": return AVATARPIC;
			case "birthdate": return BIRTHDATE;
			case "placeOfBirth": return PLACE_OF_BIRTH;
			case "sex": return GENRE;
			case "citizenSince": return CITIZEN_SINCE;
			case "photo": return PHOTO;
			
			default: return null;
		}
	}


	public String getClaimName() {
		return claimName;
	}


	public void setClaimName(String claimName) {
		this.claimName = claimName;
	}


	public String getClaimLabel() {
		return claimLabel;
	}


	public void setClaimLabel(String claimLabel) {
		this.claimLabel = claimLabel;
	}
	
	
}
