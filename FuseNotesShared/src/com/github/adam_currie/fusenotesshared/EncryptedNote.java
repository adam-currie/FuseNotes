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
package com.github.adam_currie.fusenotesshared;

import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


/**
 *
 * @author Adam Currie
 */
public class EncryptedNote{
    private static final SecureRandom random = new SecureRandom();
    
    private byte[] noteId = new byte[12];
    private byte[] userId = new byte[33];
    private Timestamp createDate;
    private Timestamp metaEditDate;
    private boolean isDeleted;
    private byte[] signature = new byte[66];
    
    //newest fragment at index 0 //todo: check
    private ArrayList<Fragment> sortedFragments = new ArrayList<Fragment>() {
        @Override
        public boolean add(Fragment frag){
            super.add(frag);
            Collections.sort(this);
            return true;
        }
    };
    
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    //todo: maybe check signature stuff in constructor, and logical checks
    //clones the byte arrays
    public EncryptedNote(byte[] noteId, byte[] userId, Timestamp createDate, Timestamp editDate, boolean isDeleted, byte[] signature){
        this.noteId = noteId.clone();
        this.userId = userId.clone();
        this.createDate = createDate;
        this.metaEditDate = editDate;
        this.isDeleted = isDeleted;
        this.signature = noteId.clone();
    }
    
    //clones the userId
    public EncryptedNote(byte[] userId){
        this.userId = userId.clone();
        random.nextBytes(noteId);
        createDate = new Timestamp(System.currentTimeMillis());
        metaEditDate = new Timestamp(System.currentTimeMillis());
        isDeleted = false;
    }

    public void setNoteBody(String encryptedNoteBody, byte[] signingKey) throws InvalidKeyException{
        sortedFragments.add(new Fragment(encryptedNoteBody, signingKey));
    }

    public Timestamp getCreateDate(){
        return (Timestamp)createDate.clone();
    }
    
    /*
     * Method           getEditDate
     * Description      returns the last time the meta data changed or a fragment was added
     * Returns
     *  Timestamp       edit date/time
     */
    public Timestamp getEditDate(){
        Timestamp latest = metaEditDate;
        
        if(!sortedFragments.isEmpty() && sortedFragments.get(0).getCreateDate().after(latest)){
            latest = sortedFragments.get(0).getCreateDate();
        }
        
        return (Timestamp)latest.clone();
    }

    public String getNoteBody(){
        if(sortedFragments.isEmpty()){
            return "";
        }else{
            return sortedFragments.get(0).getNoteBody();
        }
    }

    class Fragment implements Comparable<Fragment>{        
        private byte[] fragmentId = new byte[6];
        private Timestamp fragCreateDate;
        private Timestamp fragEditDate;
        private boolean fragIsDeleted;
        private String noteBody;
        private byte[] fragSignature = new byte[66];
        
        private Fragment(String encryptedNoteBody, byte[] signingKey) throws InvalidKeyException{
            random.nextBytes(fragmentId);
            fragCreateDate = new Timestamp(System.currentTimeMillis());
            fragEditDate = new Timestamp(System.currentTimeMillis());
            fragIsDeleted = false;
            noteBody = encryptedNoteBody;
            
            fragSignature = ECDSAUtil.signStr(signingKey, noteBody + Arrays.toString(noteId) + Arrays.toString(fragmentId) + fragCreateDate + fragEditDate + fragIsDeleted);
        }

        public Timestamp getCreateDate(){
        return (Timestamp)fragCreateDate.clone();
    }

        @Override
        public int compareTo(Fragment o){
            if(fragCreateDate.after(o.fragCreateDate)){
                return 1;
            }else if(o.fragCreateDate.after(fragCreateDate)){
                return -1;
            }else{
                return 0;
            }
        }

        private String getNoteBody(){
            return noteBody;
        }
    }
    
}
