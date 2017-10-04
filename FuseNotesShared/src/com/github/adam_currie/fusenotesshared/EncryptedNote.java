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

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;


/**
 *
 * @author Adam Currie
 */
public class EncryptedNote implements Iterable<EncryptedNote.Fragment>{
    private static final SecureRandom random = new SecureRandom();
    
    private byte[] noteId = new byte[12];
    private Timestamp createDate;
    private Timestamp metaEditDate;
    private boolean isDeleted;
    private byte[] signature = new byte[66];
    private com.github.adam_currie.fusenotesshared.ECDSASigner signer;
    
    //newest fragment at index 0
    private ArrayList<Fragment> sortedFragments = new ArrayList<Fragment>() {
        @Override
        public boolean add(Fragment frag){
            super.add(frag);
            Collections.sort(this, Collections.reverseOrder());
            return true;
        }
    };
    
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    //todo: maybe check signature stuff in constructor, and logical checks
    //clones the byte arrays and timestamps
    public EncryptedNote(byte[] noteId, ECDSASigner signer, Timestamp createDate, Timestamp editDate, boolean isDeleted, byte[] signature){
        this.noteId = noteId.clone();
        this.createDate = (Timestamp)createDate.clone();
        this.metaEditDate = (Timestamp)editDate.clone();
        this.isDeleted = isDeleted;
        this.signature = signature.clone();
        this.signer = signer;
    }
    
    public EncryptedNote(ECDSASigner signer){
        random.nextBytes(noteId);
        createDate = new Timestamp(System.currentTimeMillis());
        metaEditDate = new Timestamp(System.currentTimeMillis());
        isDeleted = false;
        
        this.signer = signer;
        
        signature = signer.sign(Arrays.toString(noteId) + createDate + metaEditDate + isDeleted);
    }
    
    /**
     * 
     * @param encryptedNoteBody the encrypted note body
     * @return  a portion of the EncryptedNote with only the changed fragments
     */
    public EncryptedNote setNoteBody(String encryptedNoteBody){
        Fragment frag = new Fragment(encryptedNoteBody);
        sortedFragments.add(frag);
        ArrayList<Fragment> subList = new ArrayList<>(1);
        subList.add(frag);
        return getSubNote(subList);
    }

    /**
     * Returns the creation date of the note.
     * Must be cloned to avoid changing underlying data.
     * @return the creation date
     */
    public Timestamp getCreateDate(){
        return createDate;
    }
    
    /**
     * Returns the last time the note was edited(meta data changed or fragment added).
     * Must be cloned to avoid changing underlying data.
     * Use {@link #getMetaEditDate() getMetaEditDate} to get just the last time the meta data of the note itself changed.
     * @return last edited date/time
     */
    public Timestamp getEditDate(){
        Timestamp latest = metaEditDate;
        
        if(!sortedFragments.isEmpty() && sortedFragments.get(0).getCreateDate().after(latest)){
            latest = sortedFragments.get(0).getCreateDate();
        }
        
        return latest;
    }
    
    /**
     * Returns the last time the meta data of the note changed.
     * This date is only updated when information about the note itself is changed, as opposed to a fragment.
     * Use {@link #getEditDate() getEditDate} to get the last time the note or the latest fragment changed.
     * Must be cloned to avoid changing underlying data.
     * @return the edit date/time
     */
    public Timestamp getMetaEditDate(){
        return metaEditDate;
    }

    public String getNoteBody(){
        if(sortedFragments.isEmpty()){
            return "";
        }else{
            return sortedFragments.get(0).getNoteBody();
        }
    }

    public ECDSASigner getSigner(){
        return signer;
    }

    private EncryptedNote getSubNote(ArrayList<Fragment> subList){
        EncryptedNote subNote = new EncryptedNote(noteId, signer, createDate, metaEditDate, isDeleted, signature);
        
        for(Fragment frag : subList){
            Fragment copy = subNote.new Fragment(frag.fragmentId, frag.fragCreateDate, frag.fragEditDate, frag.noteBody, frag.fragIsDeleted, frag.fragSignature);
            subNote.sortedFragments.add(copy);
        }
        
        return subNote;
    }

    /**
     * Gets the note id.
     * Must be cloned to avoid changing underlying array.
     * @return the note id
     */
    public byte[] getNoteId(){
        return noteId;
    }

    public byte[] getUserID(){
        return signer.getPublicKeyBytes();
    }

    public boolean getDeleted(){
        return isDeleted;
    }
    
    /**
     * Gets the notes signature.
     * Must be cloned to avoid changing underlying array.
     * @return the signature
     */
    public byte[] getSignature(){
        return signature;
    }

    @Override
    public Iterator<Fragment> iterator(){
        return sortedFragments.iterator();
    }

    public void addFragment(byte[] id, Timestamp create, Timestamp edit, String body, boolean deleted, byte[] sig){
        Fragment frag = new Fragment(id, create, edit, body, deleted, sig);
        sortedFragments.add(frag);
    }
    
    
    public class Fragment implements Comparable<Fragment>{        
        private byte[] fragmentId = new byte[6];
        private Timestamp fragCreateDate;
        private Timestamp fragEditDate;
        private boolean fragIsDeleted;
        private String noteBody;
        private byte[] fragSignature = new byte[66];
        
        private Fragment(String encryptedNoteBody){
            random.nextBytes(fragmentId);
            fragCreateDate = new Timestamp(System.currentTimeMillis());
            fragEditDate = new Timestamp(System.currentTimeMillis());
            fragIsDeleted = false;
            noteBody = encryptedNoteBody;
            
            fragSignature = signer.sign(noteBody + Arrays.toString(noteId) + Arrays.toString(fragmentId) + fragCreateDate + fragEditDate + fragIsDeleted);
        }

        /**
         * Creates a note fragment associated with this note.
         * Clones the byte arrays and timestamps.
         */
        private Fragment(byte[] id, Timestamp create, Timestamp edit, String body, boolean deleted, byte[] sig){
            fragmentId = id.clone();
            fragCreateDate = (Timestamp)create.clone();
            fragEditDate = (Timestamp)edit.clone();
            noteBody = body;
            fragIsDeleted = deleted;
            fragSignature = sig.clone();
        }

        /**
         * Gets the creation date of this fragment.
         * Must be cloned to avoid changing underlying data.
         * @return  the creation date
         */
        public Timestamp getCreateDate(){
            return fragCreateDate;
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
        
        /**
         * Gets the encrypted note text;
         * @return the encrypted note text
         */
        public String getNoteBody(){
            return noteBody;
        }

        /**
         * Gets the id of this fragment.
         * Must be cloned to avoid changing underlying data.
         * @return  the fragment id 
         */
        public byte[] getFragmentId(){
            return fragmentId;
        }

        /**
         * Gets the edit date of this fragment.
         * Must be cloned to avoid changing underlying data.
         * @return  the edit date
         */
        public Timestamp getEditDate(){
            return fragEditDate;
        }
        
        /**
         * Gets the deletion status of this particular fragment.
         * @return whether the fragment is deleted
         */
        public boolean getDeleted(){
            return fragIsDeleted;
        }
        
        /**
         * Gets the signature of this fragment.
         * Must be cloned to avoid changing underlying data.
         * @return  the signature
         */
        public byte[] getSignature(){
            return fragSignature;
        }
    }
    
}
