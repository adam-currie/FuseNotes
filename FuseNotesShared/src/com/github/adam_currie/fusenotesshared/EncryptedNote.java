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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Encrypted note providing signing and verification, and versioning.
 * If the note is being written to by multiple threads, 
 * then a snapshot must be taken before the note and fragment signatures 
 * can be used in conjunction with the data they sign.
 * @author Adam Currie
 */
public class EncryptedNote implements Iterable<EncryptedNote.Fragment>{
    private static final SecureRandom random = new SecureRandom();
    
    private byte[] noteId = new byte[12];
    private final Timestamp createDate;
    private final ThreadSafeECDSASigner signer;
    
    private ConcurrentSkipListSet<Fragment> sortedFragments = new ConcurrentSkipListSet<>();
    
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    ReentrantLock signatureLock = new ReentrantLock();//todo: signatureLock when writing
    
    /*
     * only access these fields atomically so that they don't need to be locked for reads, 
     * signatureLock is only used for writes and snapshots to make sure the signature is synced up with the data
     */
        private Timestamp metaEditDate;//only set by changing reference, not value, would break thread safety
        private AtomicBoolean isDeleted = new AtomicBoolean();
        private ECDSASignature signature;
            
    
    //todo: maybe check signature stuff in constructor, and logical checks
    //clones the byte arrays and timestamps
    public EncryptedNote(byte[] noteId, ThreadSafeECDSASigner signer, Timestamp createDate, Timestamp editDate, boolean isDeleted, ECDSASignature signature){
        this.noteId = noteId.clone();
        this.createDate = (Timestamp)createDate.clone();
        this.metaEditDate = (Timestamp)editDate.clone();
        this.isDeleted.set(isDeleted);
        this.signature = signature;
        this.signer = signer;
    }
    
    public EncryptedNote(ThreadSafeECDSASigner signer){
        random.nextBytes(noteId);
        createDate = new Timestamp(System.currentTimeMillis());
        metaEditDate = new Timestamp(System.currentTimeMillis());
        isDeleted.set(false);
        
        this.signer = signer;
        
        sign();
    }
    
    /**
     * 
     * @param encryptedNoteBody the encrypted note body
     * @return  a partial snapshot of the EncryptedNote with only the changed fragments
     */
    public EncryptedNote setNoteBody(String encryptedNoteBody){
        Fragment frag = new Fragment(encryptedNoteBody);
        sortedFragments.add(frag);
        ArrayList<Fragment> subList = new ArrayList<>(1);
        subList.add(frag);
        return getPartialSnapshot(subList);
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
        
        Fragment last = null; 
        try{
            last = sortedFragments.last();
        }catch(NoSuchElementException ex){}
        
        if(last != null && last.getCreateDate().after(latest)){
            latest = last.getCreateDate();
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
            return sortedFragments.last().getNoteBody();
        }
    }

    public ThreadSafeECDSASigner getSigner(){
        return signer;
    }

    /**
     * Gets a snapshot of the note's meta data such that the fields are guaranteed 
     * to be synced up with the signature in a multi-threaded environment.
     * @return 
     */
    public EncryptedNote getMetaDataSnapshot(){
        signatureLock.lock();
        try{
            return new EncryptedNote(noteId, signer, createDate, metaEditDate, isDeleted.get(), signature);
        }finally{
            signatureLock.unlock();
        }
    }
    
    /**
     * Gets a snapshot of the note and its fragments such that the fields are guaranteed 
     * to be synced up with the signature(s) in a multi-threaded environment.
     * @return 
     */
    public EncryptedNote getSnapshot(){
        return getPartialSnapshot(sortedFragments);
    }
    
    private EncryptedNote getPartialSnapshot(Iterable<Fragment> fragments){
        EncryptedNote subNote = getMetaDataSnapshot();
        
        for(Fragment frag : fragments){
            subNote.sortedFragments.add(frag.getSnapshot(subNote));
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
        return isDeleted.get();
    }
    
    public ECDSASignature getSignature(){
        return signature;
    }

    @Override
    public Iterator<Fragment> iterator(){
        return sortedFragments.iterator();
    }

    public void addFragment(byte[] id, Timestamp create, Timestamp edit, String body, boolean deleted, ECDSASignature sig){
        Fragment frag = new Fragment(id, create, edit, body, deleted, sig);
        sortedFragments.add(frag);
    }

    /**
     * sets the deleted status to true on this and all fragments, updates the edit date and the signature
     */
    public void delete(){
        signatureLock.lock();
        try{
            isDeleted.set(true);
            metaEditDate = new Timestamp(System.currentTimeMillis());
            sign();
        }finally{
            signatureLock.unlock();
        }
        
        for(Fragment frag : sortedFragments){
            frag.delete();
        }
    }

    private void sign(){
        signature = signer.sign("" + noteId + createDate + metaEditDate + isDeleted);
    }
    
    
    public class Fragment implements Comparable<Fragment>{        
        private byte[] fragmentId = new byte[6];
        private final Timestamp fragCreateDate;
        private ReentrantLock signatureLock = new ReentrantLock();
        
        /*
         * only access these fields atomically so that they don't need to be locked for reads, 
         * signatureLock is only used for writes and snapshots to make sure the signature is synced up with the data
         */
            private Timestamp fragEditDate;//only set by changing reference, not value, would break thread safety
            private AtomicBoolean fragIsDeleted = new AtomicBoolean();
            private String noteBody;
            private ECDSASignature fragSignature;
        
            
        private Fragment(String encryptedNoteBody){
            random.nextBytes(fragmentId);
            fragCreateDate = new Timestamp(System.currentTimeMillis());
            fragEditDate = new Timestamp(System.currentTimeMillis());
            fragIsDeleted.set(false);
            
            noteBody = encryptedNoteBody;
            sign();
        }

        /**
         * Creates a note fragment associated with this note.
         * Clones the byte arrays and timestamps.
         */
        private Fragment(byte[] id, Timestamp create, Timestamp edit, String body, boolean deleted, ECDSASignature sig){
            fragmentId = id.clone();
            fragCreateDate = (Timestamp)create.clone();
            fragEditDate = (Timestamp)edit.clone();
            noteBody = body;
            fragIsDeleted.set(deleted);
            fragSignature = sig;
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
        
        private Fragment getSnapshot(EncryptedNote outer){
            signatureLock.lock();
            try{
                return outer.new Fragment(fragmentId, fragCreateDate, fragEditDate, noteBody, fragIsDeleted.get(), fragSignature);
            }finally{
                signatureLock.unlock();
            }
        }
        
        private void sign(){
            fragSignature = signer.sign(noteBody + Arrays.toString(noteId) + Arrays.toString(fragmentId) + fragCreateDate + fragEditDate + fragIsDeleted);
        }
        
        /**
         * Gets the encrypted note text;
         * @return the encrypted note text
         */
        public String getNoteBody(){
            return noteBody;
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
            return fragIsDeleted.get();
        }
        
        /**
         * sets the deleted status to true, removes the notebody, updates the edit date and the signature
         */
        public void delete(){
            signatureLock.lock();
            try{
                fragIsDeleted.set(true);
                noteBody = null;
                fragEditDate = new Timestamp(System.currentTimeMillis());
                sign();
            }finally{
                signatureLock.unlock();
            }
        }
        
        public ECDSASignature getSignature(){
            return fragSignature;
        }
    }
    
}
