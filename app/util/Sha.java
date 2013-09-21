package util;

import java.security.*;
public class Sha {
	public static String pwdhash(String pwd) throws NoSuchAlgorithmException {
		if (pwd.length() == 128)
			return pwd;
		for (int i=0; i<100; i++) {
			pwd = hash512(pwd);
		}
		return pwd;
	}
	
public static String hash512(String data) throws NoSuchAlgorithmException {
MessageDigest md = MessageDigest.getInstance("SHA-512");
md.update(data.getBytes());
return bytesToHex(md.digest());
}
public static String bytesToHex(byte[] bytes) {
StringBuffer result = new StringBuffer();
for (byte byt : bytes) result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
return result.toString();
}
}