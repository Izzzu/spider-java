package test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Test1 {

	private void wyszukajLinki(String tresc, String adres, Connection conn) throws SQLException {
	

		Pattern p = Pattern.compile("<[Aa][^>]+[hH][rR][eE][fF] *= *[\"']([^\"']+)[\"']");
		Matcher m = p.matcher(tresc);
		while (m.find()) {

			String link = m.group(1);
			if (link.startsWith("/") ) {
				Pattern p2 = Pattern.compile("(https?://[^/]+)");
				Matcher m2 = p2.matcher(adres);
				if(m2.find()) {
					String adres2 = m2.group(1);                
					link = adres2 + link; 
					
				} 
			} else
				if (link.startsWith("./")) {
					Pattern p2 = Pattern.compile("(https?://[^/]+[/]{1,})");
					Matcher m2 = p2.matcher(adres);
					if(m2.find()) {
						String adres2 = m2.group(1);                
						link = adres2 + link.substring(1); 
						
					} 
					
				}
				else
					if (link.startsWith("../")) {
						Pattern p2 = Pattern.compile("(https?://[^/]+)");
						Matcher m2 = p2.matcher(adres);
						if(m2.find()) {
							String adres2 = m2.group(1);                
							link = adres2 + link.substring(2); 
							
						} 
						
					}
				else
					if (link.startsWith("#")) {
						Pattern p2 = Pattern.compile("(https?://[^/]+)");
						Matcher m2 = p2.matcher(adres);
						if(m2.find()) {
							String adres2 = m2.group(1);                
							link = adres2;
						
						} 
					}
					else if (!link.startsWith("http"))
					{
						Pattern p2 = Pattern.compile("(https?://[^/]+)");
						Matcher m2 = p2.matcher(adres);
						if(m2.find()) {
							String adres2 = m2.group(1);                
							if (link.startsWith("?")) link = adres2 + link;
							else 	link = adres2 +"/"+ link;
						} 
					}
			
			if (!czyJestWTabeli(conn, link, "linki_odwiedzone"))
			{
				if (link.length()>250) link = link.substring(0, 250);
				dodajLinkDoTabeli(conn, link, "linki_do_odwiedzenia");
			}
		}
	}
	private String odczytZeStrumienia0(Reader is){
		int x=0;
		StringBuilder s= new StringBuilder();
		try{
			while ((x = is.read())!=-1){
				char c= (char) x;
				s.append(c);
			}
		}catch (IOException ex){
			System.out.println("blad we/wy");
		}
		return s.toString();

	}

	private void dodajLinkDoTabeli(Connection conn, String link, String nazwatabeli)
			throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement("INSERT INTO "+nazwatabeli+" (adres, datadodania)" + " VALUES (?,?)");
		pstmt.setString(1, link);
		pstmt.setTimestamp(2, new Timestamp(new Date().getTime()));
		pstmt.executeUpdate();
	}

	private boolean czyJestWTabeli(Connection conn, String link, String nazwatabeli) throws SQLException {
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("Select adres from "+nazwatabeli+" where adres LIKE '"+link+"'");
		if (rs.next())
		{
			System.out.println("LINK -----------> " + link + " jest w tabeli " + nazwatabeli);
			return true;
		}
		return false;

	}
	@SuppressWarnings("resource")
	public static void main(String[] a) {
		Test1 od = new Test1();
		try {

			Class.forName("org.h2.Driver");
			Connection conn = DriverManager.
					getConnection("jdbc:h2:tcp://localhost/~/test", "sa", "");

			Statement stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT * FROM linki_do_odwiedzenia ORDER BY datadodania");


			int i = 1;
			while (rs.absolute(i++)){
				int id = rs.getInt("id");
				String aktualnyLink = rs.getString("adres");
				Timestamp dataDodania = rs.getTimestamp("datadodania");
				System.out.println("Rekord id: " +id + " \nadres: " + aktualnyLink + " \ndata dodania: " + dataDodania);

				try {
					URL u = new URL(aktualnyLink);

					InputStream is = u.openStream();
					Reader r = new InputStreamReader(is, "UTF-8");
					String dane = od.odczytZeStrumienia0(r);
					od.wyszukajLinki(dane, aktualnyLink, conn);
					rs = stmt.executeQuery("SELECT * FROM linki_do_odwiedzenia ORDER BY datadodania");

					//sprawdzenie czy link jest w tabeli np z poprzedniego uruchomienia programu, zeby nie zapisywac go po raz drugi
					if (!od.czyJestWTabeli(conn,  aktualnyLink,  "linki_odwiedzone")) od.dodajLinkDoTabeli(conn, aktualnyLink, "linki_odwiedzone");
				}
				catch (FileNotFoundException ex) {
					System.out.println("Nie mozna znalezc pliku");
				}
				catch (IOException ex) {
					System.out.println("Blad we/wy");
				}
			}

			System.out.println("Polaczono z baza");
			conn.close();
			System.out.println("Koniec programu");
		}
		catch (ClassNotFoundException ex) {
			System.out.println("Nie znaleziono klasy drivera");
		}
		catch (SQLException ex) {
			System.out.println("Wyjatek sql: "+ ex);
		}

	}
}
