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

import com.github.adam_currie.fusenotesshared.EncryptedNote;
import com.github.adam_currie.fusenotesshared.NoteDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
                        "notebody TEXT," +
                        "signature BINARY(65)," +
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
                        "notebody TEXT," +
                        "signature BINARY(65)," +
                        "PRIMARY KEY (note_id, fragment_id)" +
                    ")"
            );
            noteFragmentStatement.execute(); 
        }
    }    
    
    @Override
    public ArrayList<EncryptedNote> getAllNotes(String userId){
        //todo
        return new ArrayList<EncryptedNote>();//debug
    }

    @Override
    public void addOrUpdate(EncryptedNote note){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
