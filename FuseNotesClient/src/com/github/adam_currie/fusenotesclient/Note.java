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

import com.github.adam_currie.fusenotesshared.*;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.crypto.InvalidCipherTextException;

/**
 * 
 * @author Adam Currie
 */
public class Note{
    private final EncryptedNote encryptedNote;
    private final AESEncryption aes;
    
    private NoteListener noteListener = null;
    
    
    //todo: client changes things with setters, these setters change the underlying note and trigger the updated note to be saved to the db and sent to the server
    
    //hidden from public
    //listener is for internal use
    Note(EncryptedNote encryptedNote, AESEncryption aes){
        if(!encryptedNote.getSigner().canSign()){
            throw new IllegalArgumentException("encryptedNote is not setup for signing");
        }
        this.encryptedNote = encryptedNote;
        this.aes = aes;
    }
    
    //hidden from public
    //listener is for internal use(within the package)
    Note(ECDSASignerVerifier signer, AESEncryption aes){
        if(!signer.canSign()){
            throw new IllegalArgumentException("signer is not setup for signing");
        }
        encryptedNote = new EncryptedNote(signer);
        this.aes = aes;
    }
    
    void setNoteListener(NoteListener nl){
        noteListener = nl;
    }

/*
     * Method           getCreateDate
     * Description      gets a copy of the creation date
     * Returns
     *  Timestamp       creation date/time
     */
    public Timestamp getCreateDate(){
        return (Timestamp)encryptedNote.getCreateDate().clone();
    }
        
    /*
     * Method           getCompositeEditDate
     * Description      returns the time that the note was last edited
     * Returns
     *  Timestamp       edit date/time
     */
    public Timestamp getEditDate(){
        return encryptedNote.getCompositeEditDate();
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
        EncryptedNote subNote = encryptedNote.setNoteBody(aes.encrypt(text));
        
        //cache to avoid race condition
        NoteListener nl = noteListener;
        if(nl != null) nl.noteChanged(this, subNote);
    }

    /*
     * Method                   delete
     * Description              deletes the note and all versions of it
     */
    public void delete(){
        encryptedNote.delete();
        
        //cache to avoid race condition
        NoteListener nl = noteListener;
        if(nl != null) nl.noteChanged(this, encryptedNote.getSnapshot());
    }

    EncryptedNote getEncryptedNote(){
        return encryptedNote;
    }

    public boolean getDeleted(){
        return encryptedNote.getDeleted();
    }
    
}
