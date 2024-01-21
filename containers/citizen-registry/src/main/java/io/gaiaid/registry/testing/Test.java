package io.gaiaid.registry.testing;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Test {
	private static DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		LocalDate birthDate = LocalDate.from(df.parse("2020-01-01"));
		System.out.println(birthDate);
	}

}
