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

import com.github.adam_currie.fusenotesshared.ThreadSafeECDSASigner;
import com.github.adam_currie.fusenotesshared.EncryptedNote;
import com.github.adam_currie.fusenotesshared.NoteDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 *
 * @author Adam Currie
 */
public class LocalDB implements NoteDatabase{
    
    private final String URL_STR = "jdbc:sqlite:local.db";
    
    public LocalDB() throws SQLException{
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
        }
    }    
    
    @Override
    public ArrayList<EncryptedNote> getAllNotes(ThreadSafeECDSASigner signerOrVerfier) throws SQLException{
        try(Connection connection = DriverManager.getConnection(URL_STR)) {      
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM note WHERE user_id=?");
            statement.setBytes(1, signerOrVerfier.getPublicKeyBytes());
            ResultSet noteResults = statement.executeQuery();
            
            //setup for getting fragments
            statement = connection.prepareStatement(
                    "SELECT * FROM note_fragment WHERE note_id=?");
            
            ArrayList<EncryptedNote> notes = new ArrayList();
            while(noteResults.next()){
                
                //GET NOTE META DATA
                EncryptedNote note = new EncryptedNote(
                    noteResults.getBytes("note_id"),
                    signerOrVerfier,
                    noteResults.getTimestamp("creation"),
                    noteResults.getTimestamp("meta_edit"),
                    noteResults.getBoolean("deleted"),
                    noteResults.getBytes("signature")
                );
                
                //GET NOTE FRAGMENTS
                statement.setBytes(1, note.getNoteId());
                ResultSet fragResults = statement.executeQuery();
                while(fragResults.next()){
                    note.addFragment(
                        fragResults.getBytes("fragment_id"),
                        fragResults.getTimestamp("creation"),
                        fragResults.getTimestamp("edit"),
                        fragResults.getString("note_body"),
                        fragResults.getBoolean("deleted"),
                        fragResults.getBytes("signature")
                    );
                }
                
                notes.add(note);
            }
            
            return notes;
        }
    }

    /**
     * Does not take a snapshot of the note before saving, 
     * a snapshot of a note must be taken first and used here if the note is being used by multiple threads.
     * @param note  the note to add to the db
     * @throws SQLException 
     */
    @Override
    public void addOrUpdate(EncryptedNote note) throws SQLException{
        try(Connection connection = DriverManager.getConnection(URL_STR)){
            connection.setAutoCommit(false);
            
            //META DATA
            PreparedStatement statement = connection.prepareStatement(
                    "REPLACE INTO note (note_id,user_id,creation,meta_edit,deleted,signature) VALUES (?, ?, ?, ?, ?, ?) ");

            statement.setBytes(1, note.getNoteId());
            statement.setBytes(2, note.getUserID());
            statement.setTimestamp(3, note.getCreateDate());
            statement.setTimestamp(4, note.getMetaEditDate());
            statement.setBoolean(5, note.getDeleted());
            statement.setBytes(6, note.getSignature());
            
            statement.execute();  
            
            //FRAGMENTS    
            statement = connection.prepareStatement(
                    "REPLACE INTO note_fragment (note_id,fragment_id,creation,edit,deleted,note_body,signature) VALUES (?, ?, ?, ?, ?, ?, ?) ");
            
            for(EncryptedNote.Fragment frag : note){
                statement.setBytes(1, note.getNoteId());
                statement.setBytes(2, frag.getFragmentId());
                statement.setTimestamp(3, frag.getCreateDate());
                statement.setTimestamp(4, frag.getEditDate());
                statement.setBoolean(5, frag.getDeleted());
                statement.setString(6, frag.getNoteBody());
                statement.setBytes(7, frag.getSignature());
                
                statement.addBatch();
            }
            
            statement.executeBatch();
            
            connection.commit();
        }
    }
    
}
