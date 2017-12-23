/*
 * The MIT License
 *
 * Copyright 2017 Adam Currie.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.adam_currie.fusenotesclient;

import com.github.adam_currie.fusenotesshared.ECDSASignature;
import com.github.adam_currie.fusenotesshared.EncryptedNote;
import com.github.adam_currie.fusenotesshared.FragmentID;
import com.github.adam_currie.fusenotesshared.NoteID;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;


//todo: make this class multi threaded so that calling code doesnt need to reduce the amount of code in NoteStore related to the db

/**
 *
 * @author Adam Currie
 */
public class LocalDB{
    private static final String URL_STR = "jdbc:sqlite:local.db";
    
    static {        
        try(Connection connection = DriverManager.getConnection(URL_STR)){
            PreparedStatement noteStatement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS note (" +
                        "note_id BINARY(12)," +
                        "user_id BINARY(33)," +
                        "creation DATETIME," +
                        "meta_edit DATETIME," +
                        "deleted BOOL," +
                        "signature BINARY(66)," +
                        "PRIMARY KEY (note_id)" +
                    ")"
            );
            noteStatement.execute(); 
            
            PreparedStatement noteFragmentStatement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS note_fragment (" +
                        "note_id BINARY(12)," +
                        "fragment_id BINARY(6)," +
                        "creation DATETIME," +
                        "edit DATETIME," +
                        "deleted BOOL," +
                        "note_body TEXT," +
                        "signature BINARY(66)," +
                        "PRIMARY KEY (note_id, fragment_id)" +
                    ")"
            );
            noteFragmentStatement.execute(); 
        }catch(SQLException ex){
            Logger.getLogger(LocalDB.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
    }    
    
    public static ArrayList<Note> getAllNotes(NoteFactory factory) throws SQLException{
        ArrayList<Note> notes = new ArrayList<>();
        
        try(Connection connection = DriverManager.getConnection(URL_STR)) {      
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM note WHERE user_id=?");
            statement.setBytes(1, factory.getUserID());
            ResultSet noteResults = statement.executeQuery();
            
            //setup for getting fragments
            statement = connection.prepareStatement(
                    "SELECT * FROM note_fragment WHERE note_id=?");
            
            while(noteResults.next()){
                
                //GET NOTE META DATA
                Note note = factory.createNote(
                    NoteID.fromBytes(noteResults.getBytes("note_id")),
                    noteResults.getTimestamp("creation"),
                    noteResults.getTimestamp("meta_edit"),
                    noteResults.getBoolean("deleted"),
                    ECDSASignature.fromBytes(noteResults.getBytes("signature"))
                );
                
                //GET NOTE FRAGMENTS
                statement.setBytes(1, note.getEncryptedNote().getNoteId().toBytes());
                ResultSet fragResults = statement.executeQuery();
                while(fragResults.next()){
                    note.getEncryptedNote().addFragment(
                        FragmentID.fromBytes(fragResults.getBytes("fragment_id")),
                        fragResults.getTimestamp("creation"),
                        fragResults.getTimestamp("edit"),
                        fragResults.getString("note_body"),
                        fragResults.getBoolean("deleted"),
                        ECDSASignature.fromBytes(fragResults.getBytes("signature"))
                    );
                }
                
                notes.add(note);
            }
        }
        
        return notes;
    }

    /**
     * Does not take a snapshot of the note before saving, 
     * a snapshot of a note must be taken first and used here if the en is being used by multiple threads.
     * @param note  the note to add to the db
     * @throws SQLException 
     */
    public static void addOrUpdate(Note note) throws SQLException{
        EncryptedNote en = note.getEncryptedNote();//todo: fix this, saving a note note just an encrypted note
        
        byte[] noteIDBytes = en.getNoteId().toBytes();
        
        try(Connection connection = DriverManager.getConnection(URL_STR)){
            connection.setAutoCommit(false);
            
            //META DATA
            PreparedStatement statement = connection.prepareStatement(
                    "REPLACE INTO note (note_id,user_id,creation,meta_edit,deleted,signature) VALUES (?, ?, ?, ?, ?, ?) ");

            statement.setBytes(1, noteIDBytes);
            statement.setBytes(2, en.getUserID());
            statement.setTimestamp(3, en.getCreateDate());
            statement.setTimestamp(4, en.getMetaEditDate());
            statement.setBoolean(5, en.getDeleted());
            statement.setBytes(6, en.getSignature().toBytes());
            
            System.out.println(Arrays.toString(en.getSignature().toBytes()));//debug
            
            statement.execute();  
            
            //FRAGMENTS    
            statement = connection.prepareStatement(
                    "REPLACE INTO note_fragment (note_id,fragment_id,creation,edit,deleted,note_body,signature) VALUES (?, ?, ?, ?, ?, ?, ?) ");
            
            for(EncryptedNote.Fragment frag : en){
                statement.setBytes(1, noteIDBytes);
                statement.setBytes(2, frag.getFragmentId().toBytes());
                statement.setTimestamp(3, frag.getCreateDate());
                statement.setTimestamp(4, frag.getEditDate());
                statement.setBoolean(5, frag.getDeleted());
                statement.setString(6, frag.getNoteBody());
                statement.setBytes(7, frag.getSignature().toBytes());
                
                statement.addBatch();
            }
            
            statement.executeBatch();
            
            connection.commit();
        }
    }
}
