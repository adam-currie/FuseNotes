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
import java.security.InvalidKeyException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;

/**
 *
 * @author Adam Currie
 */
public class Note{
    private EncryptedNote encryptedNote;
    private NoteListener noteListener;
    private AESEncryption aes;
    private final byte[] privateKey;
    
    
    //todo: client changes things with setters, these setters change the underlying not and trigger the updated note to be saved to the db and sent to the server
    
    //hidden from public
    //listener is for internal use
    Note(EncryptedNote encryptedNote, byte[] privateKey, AESEncryption aes, NoteListener listener){
        this.encryptedNote = encryptedNote;
        noteListener = listener;
        this.aes = aes;
        this.privateKey = privateKey.clone();
        //todo
    }
    
    //hidden from public
    //listener is for internal use
    Note(byte[] privateKey, AESEncryption aes, NoteListener listener){
        this.privateKey = privateKey.clone();
        this.aes = aes;
        try{
            encryptedNote = new EncryptedNote(ECDSAUtil.publicFromPrivate(privateKey));
        }catch(InvalidKeyException ex){
            //todo, check validity of this key first in constructor
            Logger.getLogger(Note.class.getName()).log(Level.SEVERE, "todo, check validity of this key first in constructor", ex);
            System.exit(-1);
        }
    }

/*
     * Method           getCreateDate
     * Description      gets a copy of the creation date
     * Returns
     *  Timestamp       creation date/time
     */
    public Timestamp getCreateDate(){
        return encryptedNote.getCreateDate();
    }
        
    /*
     * Method           getEditDate
     * Description      returns the time that the note was last edited
     * Returns
     *  Timestamp       edit date/time
     */
    public Timestamp getEditDate(){
        return encryptedNote.getEditDate();
    }

    public String getNoteBody(){
        String encrypted = encryptedNote.getNoteBody();
        
        if(encrypted == null || encrypted == ""){
            return "";
        }
        
        try{
            return aes.decrypt(encrypted);
        }catch(InvalidCipherTextException ex){
            Logger.getLogger(Note.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
    }

    public void setNoteBody(String text){
        try{
        }catch(InvalidKeyException ex){
            Logger.getLogger(Note.class.getName()).log(Level.SEVERE, "todo: check validity of key when set so that this cant happen", ex);
            System.exit(-1);
        }
    }

    /*
     * Method                   delete
     * Description              deletes the note and all versions of it
     */
    public void delete(){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
