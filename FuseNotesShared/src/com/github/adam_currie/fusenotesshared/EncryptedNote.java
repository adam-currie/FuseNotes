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

import java.io.Serializable;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.ArrayList;
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
public class EncryptedNote implements Iterable<EncryptedNote.Fragment>, Serializable{
    private static final SecureRandom random = new SecureRandom();
    
    private final NoteID noteID;
    private final Timestamp createDate;
    private final ECDSASignerVerifier signerVerifier;
    private final ConcurrentSkipListSet<Fragment> sortedFragments = new ConcurrentSkipListSet<>();
    private final ReentrantLock signatureLock = new ReentrantLock();
    
    /*
     * signatureLock is used for these for writes and snapshots to make sure the signature is synced up with the data
     */
        private Timestamp metaEditDate;
        private AtomicBoolean isDeleted = new AtomicBoolean();
        private ECDSASignature signature;
            
    
    //todo: maybe check signature stuff in constructor, and logical checks
    //clones the byte arrays and timestamps
    public EncryptedNote(NoteID noteID, ECDSASignerVerifier signerVerifier, Timestamp createDate, Timestamp editDate, boolean isDeleted, ECDSASignature signature){
        this.noteID = noteID;
        this.createDate = (Timestamp)createDate.clone();
        this.metaEditDate = (Timestamp)editDate.clone();
        this.isDeleted.set(isDeleted);
        this.signature = signature;
        this.signerVerifier = signerVerifier;
    }
    
    public EncryptedNote(ECDSASignerVerifier signer){
        noteID = new NoteID();
        createDate = new Timestamp(System.currentTimeMillis());
        metaEditDate = new Timestamp(System.currentTimeMillis());
        isDeleted.set(false);
        
        this.signerVerifier = signer;
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
    public Timestamp getCompositeEditDate(){
        Timestamp latest = metaEditDate;
        //todo: cache composite date
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
     * Use {@link #getCompositeEditDate() getCompositeEditDate} to get the last time the note or the latest fragment changed.
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

    public ECDSASignerVerifier getSigner(){
        return signerVerifier;
    }

    /**
     * Gets a snapshot of the note's meta data such that the fields are guaranteed 
     * to be synced up with the signature in a multi-threaded environment.
     * @return 
     */
    public EncryptedNote getMetaDataSnapshot(){
        signatureLock.lock();
        try{
            return new EncryptedNote(noteID, signerVerifier, createDate, metaEditDate, isDeleted.get(), signature);
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

    public NoteID getNoteId(){
        return noteID;
    }

    public byte[] getUserID(){
        return signerVerifier.getPublicKeyBytes();
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

    public void addFragment(FragmentID id, Timestamp create, Timestamp edit, String body, boolean deleted, ECDSASignature sig){
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
        signature = signerVerifier.sign("" + noteID + createDate + metaEditDate + isDeleted);
    }    
    
    public class Fragment implements Comparable<Fragment>, Serializable{        
        private final FragmentID fragmentID;
        private final Timestamp fragCreateDate;
        private final ReentrantLock signatureLock = new ReentrantLock();
        
        /*
         * signatureLock is used for these for writes and snapshots to make sure the signature is synced up with the data
         */
            private Timestamp fragEditDate;
            private AtomicBoolean fragIsDeleted = new AtomicBoolean();
            private String noteBody;
            private ECDSASignature fragSignature;
        
            
        private Fragment(String encryptedNoteBody){
            fragmentID = new FragmentID();
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
        private Fragment(FragmentID fragmentID, Timestamp create, Timestamp edit, String body, boolean deleted, ECDSASignature sig){
            this.fragmentID = fragmentID;
            fragCreateDate = (Timestamp)create.clone();
            fragEditDate = (Timestamp)edit.clone();
            noteBody = body;
            fragIsDeleted.set(deleted);
            fragSignature = sig;
        }

        public FragmentID getFragmentId(){
            return fragmentID;
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
                return outer.new Fragment(fragmentID, fragCreateDate, fragEditDate, noteBody, fragIsDeleted.get(), fragSignature);
            }finally{
                signatureLock.unlock();
            }
        }
        
        //todo: check that the tostring method for these accurately represents them, same for the frag version
        private void sign(){
            fragSignature = signerVerifier.sign(noteBody + noteID + fragmentID + fragCreateDate + fragEditDate + fragIsDeleted);
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
