package edu.tamu.emop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class Database {
    private Connection connection;
    
    /**
     * connect to the emop database
     * @param host
     * @param db
     * @param user
     * @param pass
     */
    public void connect( final String host, final String db, final String user, final String pass ) {
        String connStr = "jdbc:mysql://"+host+":3306/"+db;
        try {
            //getting database connection to MySQL server
            this.connection = DriverManager.getConnection(connStr, user, pass);
           
//            //getting PreparedStatment to execute query
//            stmt = dbCon.prepareStatement(query);
//           
//            //Resultset returned by query
//            rs = stmt.executeQuery(query);
//           
//            while(rs.next()){
//             int count = rs.getInt(1);
//             System.out.println("count of stock : " + count);
//            }
           
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally{
           //close connection ,stmt and resultset here
        }
    }
    
    /**
     * Terminate DB connection
     */
    public void disconnect() {
        try {
            this.connection.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
