/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.web.server.util.ServiceLocator;

public class AuthenticationManager 
{
	private static AuthenticationManager s_instance;

	private boolean   m_inited;
	private DbService m_dbService;
	private String	  m_dataDir;
	private String password;

	/*private AuthenticationManager() {
		m_inited = false;
	}*/

	protected AuthenticationManager(String psw) {
		password= psw;
		s_instance= this;
	}

	/**
	 * Returns the singleton instance of AuthenticationManager. 
	 * @return AuthenticationManager
	 */
	public static AuthenticationManager getInstance() {
		return s_instance;
	}


	/*public synchronized void init(DbService dbService, String dataDir) 
			throws SQLException 
	{
		if (!s_instance.m_inited) {
			s_instance.m_dataDir = dataDir;
			s_instance.m_dbService = dbService;
			s_instance.initUserStore();
			s_instance.m_inited = true;
		}
	}*/
	
	protected void updatePassword(String psw){
		password= psw;
	}

	public boolean authenticate(String username, String password)
	{
		try {			

			CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
			String sha1Password= cryptoService.sha1Hash(password);
			
			if(sha1Password.equals(this.password)){
				return true;
			}
		}catch (Exception e) {
		}
		return false;
	}

/*
	public void changeAdminPassword(String newPassword)
			throws SQLException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {			
			CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
			conn = m_dbService.getConnection();
			stmt = conn.prepareStatement("UPDATE dn_user SET password = ? WHERE username = ?;");
			stmt.setString(1, cryptoService.encryptAes(cryptoService.sha1Hash(newPassword)));
			stmt.setString(2, "admin");

			stmt.execute();
			conn.commit();

			// If we are using in memory only storage,
			// then save admin password to disk
			if (conn.getMetaData().getURL().startsWith("jdbc:hsqldb:mem")) {
				PrintWriter writer = new PrintWriter(m_dataDir + "/ap_store", "UTF-8");
				writer.println("admin:" + cryptoService.encryptAes(newPassword));
				writer.close();
			}
		}
		catch (SQLException e) {
			throw e;
		}
		catch (Exception e) {
			m_dbService.rollback(conn);
			throw new SQLException(e);
		}
		finally {
			m_dbService.close(stmt);
			m_dbService.close(conn);
		}
	}
*/

	// -------------------------------------------------
	//
	//    Private methods
	//
	// -------------------------------------------------
/*
	private synchronized void initUserStore() 
			throws SQLException
	{
		execute("CREATE TABLE IF NOT EXISTS dn_user (username VARCHAR(255) PRIMARY KEY, password  VARCHAR(255) NOT NULL, version INTEGER NOT NULL);");
		checkAdminUser();
	}


	private synchronized void checkAdminUser() throws SQLException 
	{
		boolean bAdminExists = false;
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {			

			conn = m_dbService.getConnection();
			stmt = conn.prepareStatement("SELECT username, password FROM dn_user WHERE username = ?;");
			stmt.setString(1, "admin");
			rs = stmt.executeQuery();

			if (rs != null && rs.next()) {
				bAdminExists = true;
				try{
					execute("ALTER TABLE dn_user ADD COLUMN version INTEGER DEFAULT 0 NOT NULL;");
					CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
					PreparedStatement stmtUpdate = conn.prepareStatement("UPDATE dn_user SET password = ?, version = ? WHERE username = ?;");
					String oldPassword= rs.getString("password");
					stmtUpdate.setString(1, cryptoService.encryptAes(oldPassword));
					stmtUpdate.setInt(2, 1);
					stmtUpdate.setString(3, "admin");
					stmtUpdate.execute();
					conn.commit();
				}catch(Exception e){

				}
			}

			// If admin not in DB AND we are using in memory only storage,
			// then check if admin has been saved to disk
			if (!bAdminExists && conn.getMetaData().getURL().startsWith("jdbc:hsqldb:mem")) {
				try {
					CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
					File adminFile = new File(m_dataDir + "/ap_store");
					if (adminFile.exists() && !adminFile.isDirectory()) {
						BufferedReader br = new BufferedReader(new FileReader(adminFile));
						String[] adminString = br.readLine().split(":", 2);
						createAdminUser(cryptoService.decryptAes(adminString[1]));
						bAdminExists = true;
					}

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
		catch (SQLException e) {
			throw e;
		}
		finally {
			m_dbService.close(rs);
			m_dbService.close(stmt);
			m_dbService.close(conn);
		}		

		if (!bAdminExists) {
			createAdminUser();
		}
	}

	private synchronized void createAdminUser() throws SQLException {
		createAdminUser("admin");
	}

	private synchronized void createAdminUser(String pwd) 
			throws SQLException 
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {			
			CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
			conn = m_dbService.getConnection();
			stmt = conn.prepareStatement("INSERT INTO dn_user (username, password, version) VALUES (?, ?, ?);");
			stmt.setString(1, "admin");
			stmt.setString(2, cryptoService.encryptAes(cryptoService.sha1Hash(pwd)));
			stmt.setInt(3, 1);

			stmt.execute();
		}
		catch (SQLException e) {
			throw e;
		}
		catch (Exception e) {
			throw new SQLException(e);
		}
		finally {
			m_dbService.close(stmt);
			m_dbService.close(conn);
		}
	}


	private synchronized void execute(String sql) throws SQLException 
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {			

			conn = m_dbService.getConnection();
			stmt = conn.prepareStatement(sql);
			stmt.execute();
			conn.commit();
		}
		catch (SQLException e) {
			m_dbService.rollback(conn);
			throw e;
		}
		finally {
			m_dbService.close(stmt);
			m_dbService.close(conn);
		}
	}
*/
	public static String isDBInitialized(DbService dbService, String dataDir){

		Connection conn = null;
		BufferedReader br = null;
		String result= null;
		try{

			conn = dbService.getConnection();
			File adminFile = new File(dataDir + "/ap_store");
			if(conn.getMetaData().getURL().startsWith("jdbc:hsqldb:mem")){
				if(adminFile.exists() && !adminFile.isDirectory()){

					br = new BufferedReader(new FileReader(adminFile));
					String[] adminString = br.readLine().split(":", 2);


					result = adminString[1];
				}else{

				}
			}else{
				PreparedStatement stmt = null;
				ResultSet rs = null;
				stmt = conn.prepareStatement("SELECT username, password FROM dn_user WHERE username = ?;");
				stmt.setString(1, "admin");
				rs = stmt.executeQuery();

				if (rs != null && rs.next()) {
					stmt = conn.prepareStatement("SELECT password FROM dn_user WHERE username = ?;");
					stmt.setString(1, "admin");
					rs = stmt.executeQuery();
					if (rs != null && rs.next()) {
						result= rs.getString("password");
					}		
				}
			}
		}catch(Exception e){
		} finally {
			try{
				if(br != null){
					br.close();
				}
			}catch (Exception ex){
			}
		}
		return result;
	}
	
}
