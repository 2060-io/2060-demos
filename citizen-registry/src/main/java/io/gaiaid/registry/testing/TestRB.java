package io.gaiaid.registry.testing;

import java.util.Locale;
import java.util.ResourceBundle;

public class TestRB {

	public static void main(String[] args) {
		
		
		ResourceBundle bundle = ResourceBundle.getBundle("META-INF/resources/Messages", Locale.US);  
		 System.out.println("Message in "+Locale.US +":"+bundle.getString("ROOT_MENU_TITLE"));  
		  
		 
		 Locale es = new Locale("ES");
		 bundle = ResourceBundle.getBundle("META-INF/resources/Messages", es);  
		 System.out.println("Message in "+es +":"+bundle.getString("ROOT_MENU_TITLE"));  
			
		
	}

}
