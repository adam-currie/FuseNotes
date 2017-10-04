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

import com.github.adam_currie.fusenotesshared.ECDSASigner;
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
import jdk.nashorn.internal.runtime.JSType;

/**
 *
 * @author Adam Currie
 */
public class NoteStore implements NoteListener{
    
    static {
        try{
            Class.forName("org.sqlite.JDBC");
        }catch(ClassNotFoundException ex){
            Logger.getLogger(NoteStore.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
    }
    
    private NoteStoreListener storeListener;
    private final String URL_STR = "http://localhost:8080/FuseNotesServer/NotesServlet";//todo
    private URL url;
    private NoteDatabase db;
    private ArrayList<Note> notes = new ArrayList<>();//todo: maybe expose as unmodifiableList;
    private final AESEncryption aes;
    private final ECDSASigner signer;
    
    public NoteStore(String privateKeyStr, NoteStoreListener storeListener) throws SQLException, InvalidKeyException{               
        aes = new AESEncryption(privateKeyStr);
        signer = new ECDSASigner(ECDSAUtil.toPrivateKeyParams(privateKeyStr));
        
        this.storeListener = storeListener;
        
        try{
            url = new URL(URL_STR);
        }catch(MalformedURLException ex){
            Logger.getLogger(NoteStore.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        
        db = new LocalDB();
        for(EncryptedNote encryptedNote : db.getAllNotes(signer)){
            Note note = new Note(encryptedNote, aes, this);
            notes.add(note);
            storeListener.noteLoaded(note);
        }
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
    
    
    /*
     * Method               checkKeyValid
     * Description          checks the validity of a key
     * Params           
     *  String key          private key in base 64
     * Returns          
     *  Boolean             whether the key is a valid ecdsa key
     */
    public static boolean checkKeyValid(String keyStr){
        return ECDSAUtil.checkKeyValid(keyStr);
    }

    //debug: test main
    public static void main(String args[]) throws SQLException, InvalidKeyException{
        NoteStoreListener nl = new NoteStoreListener() {
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

    /**
     * Method               addNote
     * Description          adds a new note
     * @param waitForEdit   whether or not to wait for an edit before saving this note
     */
    public Note addNote(boolean waitForEdit){
        Note note = new Note(signer, aes, this);
        notes.add(note);
        
        if(!waitForEdit){
            try{
                db.addOrUpdate(note.getEncryptedNote());
            }catch(SQLException ex){
                Logger.getLogger(NoteStore.class.getName()).log(Level.SEVERE, null, ex);
                //todo retry
            }
            
            //todo: send to server and stuff
        }
        
        return note;
    }
    
    public Note addNote(String noteBody){
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getPrivateKey(){
        return signer.getPrivateKeyString();
    }

    @Override
    public void noteChanged(Note note, EncryptedNote subNote){
        try{
            db.addOrUpdate(subNote);
        }catch(SQLException ex){
            Logger.getLogger(NoteStore.class.getName()).log(Level.SEVERE, null, ex);
            //todo retry
        }
    }

}
