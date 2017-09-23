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

import com.github.adam_currie.fusenotesshared.ECDSAUtil;
import com.github.adam_currie.fusenotesshared.EncryptedNote;
import com.github.adam_currie.fusenotesshared.NoteDatabase;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Adam Currie
 */
public class NoteStore{
    
    static {
        try{
            Class.forName("org.sqlite.JDBC");
        }catch(ClassNotFoundException ex){
            Logger.getLogger(NoteStore.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
    }
    
    private NoteListener listener;
    private final String URL_STR = "http://localhost:8080/FuseNotesServer/NotesServlet";//todo
    private URL url;
    private NoteDatabase db;
    private final String publicKey;
    private ArrayList<Note> notes = new ArrayList<>();//todo: maybe expose as unmodifiableList;
    
    public NoteStore(String privateKeyStr, NoteListener noteListener) throws SQLException, InvalidKeyException{
        publicKey = ECDSAUtil.publicKeyFromPrivate(privateKeyStr);
        listener = noteListener;
        
        try{
            url = new URL(URL_STR);
        }catch(MalformedURLException ex){
            Logger.getLogger(NoteStore.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        
        db = new LocalDB();
        for(EncryptedNote encryptedNote : db.getAllNotes(publicKey)){
            Note note = new Note(encryptedNote);
            notes.add(note);
            listener.noteLoaded(note);
        }
    }
    
    //debug: test main
    public static void main(String args[]) throws SQLException, InvalidKeyException{
        NoteListener nl = new NoteListener() {
            @Override
            public void noteLoaded(Note note){
                //todo
            }

            @Override
            public void noteUpdateLoaded(Note note){
                //todo
            }
        };
        NoteStore ns = new NoteStore(generateKey(), nl);
    }
    
    /*
     * Method               generateKeyPair
     * Description          generates a private key in base64
     * Returns
     *  String              generated key
     */
    public static String generateKey(){
        return ECDSAUtil.generatePrivateKeyStr();
    }
    
}
